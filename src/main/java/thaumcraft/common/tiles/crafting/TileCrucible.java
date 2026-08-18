package thaumcraft.common.tiles.crafting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.ItemHandlerProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.common.tiles.TileThaumcraft;
import thaumcraft.init.ModBlockEntities;
import thaumcraft.common.lib.crafting.ThaumcraftCraftingManager;
import thaumcraft.common.lib.crafting.CrucibleRecipeType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Crucible tile entity - melts items into their aspects and performs crucible crafting.
 * Requires heat from below (lava, fire, nitor, magma block).
 * Can accept water via fluid handlers.
 */
public class TileCrucible extends TileThaumcraft implements IAspectContainer {

    public static final int MAX_ASPECTS = 500;
    public static final int TANK_CAPACITY = 1000;

    public short heat = 0;
    public AspectList aspects = new AspectList();
    
    private final FluidTank tank = new FluidTank(TANK_CAPACITY) {
        @Override
        public boolean isFluidValid(FluidStack stack) {
            return stack.getFluid() == Fluids.WATER;
        }
    };
    private final LazyOptional<IFluidHandler> fluidHandler = LazyOptional.of(() -> tank);

    private int bellows = -1;
    private long tickCounter = 0;

    public TileCrucible(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRUCIBLE.get(), pos, state);
    }

    // ==================== NBT ====================

    @Override
    protected void writeSyncNBT(CompoundTag tag) {
        super.writeSyncNBT(tag);
        tag.putShort("Heat", heat);
        tank.writeToNBT(tag);
        aspects.writeToNBT(tag);
    }

    @Override
    protected void readSyncNBT(CompoundTag tag) {
        super.readSyncNBT(tag);
        heat = tag.getShort("Heat");
        tank.readFromNBT(tag);
        aspects.readFromNBT(tag);
    }

    // ==================== Tick ====================

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileCrucible tile) {
        tile.tickCounter++;
        int prevHeat = tile.heat;

        // Check for heat source below
        if (tile.tank.getFluidAmount() > 0) {
            BlockState below = level.getBlockState(pos.below());
            if (tile.isHeatSource(below)) {
                if (tile.heat < 200) {
                    tile.heat++;
                    if (prevHeat < 151 && tile.heat >= 151) {
                        tile.markDirtyAndSync();
                    }
                }
            } else if (tile.heat > 0) {
                tile.heat--;
                if (tile.heat == 149) {
                    tile.markDirtyAndSync();
                }
            }
        } else if (tile.heat > 0) {
            tile.heat--;
        }

        // Spill excess aspects as flux
        if (tile.aspects.visSize() > MAX_ASPECTS) {
            tile.spillRandom();
        }

        // Periodically spill aspects even under limit
        if (tile.tickCounter >= 100) {
            tile.spillRandom();
            tile.tickCounter = 0;
        }
    }

    private boolean isHeatSource(BlockState state) {
        // Check for lava, fire, magma block, or nitor
        return state.is(Blocks.LAVA) ||
               state.is(Blocks.FIRE) ||
               state.is(Blocks.SOUL_FIRE) ||
               state.is(Blocks.MAGMA_BLOCK);
        // TODO: Add nitor block check when nitor has a block tag
    }

    // ==================== Smelting ====================

    /**
     * Attempt to smelt an item dropped into the crucible.
     */
    public void attemptSmelt(ItemEntity itemEntity) {
        if (level == null || level.isClientSide) return;
        if (heat < 151 || tank.getFluidAmount() <= 0) return;

        ItemStack stack = itemEntity.getItem();
        // In 1.20.1, thrower info is stored differently
        String thrower = itemEntity.getOwner() != null ? 
                itemEntity.getOwner().getName().getString() : "";

        ItemStack result = attemptSmelt(stack, thrower);
        if (result == null || result.isEmpty()) {
            itemEntity.discard();
        } else {
            itemEntity.setItem(result);
        }
    }

    /**
     * Attempt to smelt an item stack.
     * Returns remaining items, or null if fully consumed.
     */
    public ItemStack attemptSmelt(ItemStack item, String username) {
        if (level == null) return item;
        
        boolean itemChanged = false;
        int remaining = item.getCount();

        // Check for crucible recipe
        Player player = level.getPlayerByUUID(java.util.UUID.nameUUIDFromBytes(username.getBytes())); // Approximate player retrieval
        // Better to pass Player entity if possible, but username is what we have from ItemEntity
        // For now, let's just pass null for player if we can't easily get it, or try to get it properly
        if (player == null && !username.isEmpty()) {
             // Try to find player by name if UUID fails or simple name
             player = level.players().stream().filter(p -> p.getName().getString().equals(username)).findFirst().orElse(null);
        }

        CrucibleRecipeType recipe = ThaumcraftCraftingManager.findMatchingCrucibleRecipe(aspects, item, player, level);
        
        if (recipe != null) {
            // Found a recipe!
            AspectList required = recipe.getAspects();
            if (aspects.contains(required)) {
                aspects.remove(required);
                
                // Crafted successfully
                ItemStack result = recipe.assemble(null, level.registryAccess());
                
                // Spawn result in world
                double x = worldPosition.getX() + 0.5;
                double y = worldPosition.getY() + 0.5;
                double z = worldPosition.getZ() + 0.5;
                ItemEntity entity = new ItemEntity(level, x, y + 1.0, z, result.copy());
                entity.setDeltaMovement(0, 0.25, 0);
                level.addFreshEntity(entity);
                
                // Play sound
                level.playSound(null, worldPosition, SoundEvents.EXPERIENCE_ORB_PICKUP,
                        SoundSource.BLOCKS, 0.5f, 1.0f);
                
                // Effects
                if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    // TODO: Send craft FX packet
                }
                
                remaining--;
                itemChanged = true;
                markDirtyAndSync();
                
                if (remaining <= 0) {
                    return null;
                }
                ItemStack ret = item.copy();
                ret.setCount(remaining);
                return ret;
            }
        }

        // For now, just dissolve items into aspects
        // TODO: Use ThaumcraftCraftingManager.getObjectTags(item) when implemented
        AspectList itemAspects = getItemAspects(item);
        if (itemAspects != null && itemAspects.size() > 0) {
            for (Aspect aspect : itemAspects.getAspects()) {
                aspects.add(aspect, itemAspects.getAmount(aspect));
            }
            remaining--;
            itemChanged = true;
            tickCounter = -150; // Reset spill timer

            level.playSound(null, worldPosition, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP,
                    SoundSource.BLOCKS, 0.2f, 1.0f + level.random.nextFloat() * 0.4f);
        }

        if (itemChanged) {
            markDirtyAndSync();
        }

        if (remaining <= 0) {
            return null;
        }
        
        ItemStack result = item.copy();
        result.setCount(remaining);
        return result;
    }

    /**
     * Get aspects from an item using the crafting manager.
     */
    private AspectList getItemAspects(ItemStack item) {
        return ThaumcraftCraftingManager.getObjectTags(item);
    }

    /**
     * Spill a random aspect from the crucible as flux.
     */
    public void spillRandom() {
        if (level == null || aspects.size() <= 0) return;

        Aspect[] aspectArray = aspects.getAspects();
        if (aspectArray.length > 0) {
            Aspect toSpill = aspectArray[level.random.nextInt(aspectArray.length)];
            aspects.remove(toSpill, 1);
            
            // Flux pollutes more than regular aspects
            float pollution = (toSpill == Aspect.FLUX) ? 1.0f : 0.25f;
            AuraHelper.polluteAura(level, worldPosition, pollution, true);
        }
        markDirtyAndSync();
    }

    /**
     * Dump all contents of the crucible as flux.
     */
    public void spillAll() {
        if (level == null) return;
        
        int total = aspects.visSize();
        if (tank.getFluidAmount() > 0 || total > 0) {
            tank.drain(TANK_CAPACITY, IFluidHandler.FluidAction.EXECUTE);
            AuraHelper.polluteAura(level, worldPosition, total * 0.25f, true);

            int flux = aspects.getAmount(Aspect.FLUX);
            if (flux > 0) {
                AuraHelper.polluteAura(level, worldPosition, flux * 0.75f, false);
            }

            aspects = new AspectList();
            markDirtyAndSync();
        }
    }

    // ==================== IAspectContainer ====================

    @Override
    public AspectList getAspects() {
        return aspects;
    }

    @Override
    public void setAspects(AspectList aspects) {
        // Crucible doesn't allow direct setting
    }

    @Override
    public int addToContainer(Aspect tag, int amount) {
        return 0; // Cannot add directly
    }

    @Override
    public boolean takeFromContainer(Aspect tag, int amount) {
        return false; // Cannot take directly
    }

    @Override
    public boolean takeFromContainer(AspectList list) {
        return false;
    }

    @Override
    public boolean doesContainerContainAmount(Aspect tag, int amount) {
        return aspects.getAmount(tag) >= amount;
    }

    @Override
    public boolean doesContainerContain(AspectList list) {
        for (Aspect a : list.getAspects()) {
            if (aspects.getAmount(a) >= list.getAmount(a)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int containerContains(Aspect tag) {
        return aspects.getAmount(tag);
    }

    @Override
    public boolean doesContainerAccept(Aspect tag) {
        return true;
    }

    // ==================== Fluid Handling ====================

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            return fluidHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        fluidHandler.invalidate();
    }

    public FluidTank getTank() {
        return tank;
    }

    public float getFluidHeight() {
        float base = 0.3f + 0.5f * (tank.getFluidAmount() / (float) TANK_CAPACITY);
        float extra = aspects.visSize() / (float) MAX_ASPECTS * (1.0f - base);
        float total = base + extra;
        return Math.min(total, 0.9999f);
    }

    // ==================== Rendering ====================

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                worldPosition.getX() + 1, worldPosition.getY() + 1, worldPosition.getZ() + 1);
    }

    public boolean isHeated() {
        return heat >= 151;
    }
}
