package thaumcraft.common.tiles.crafting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import thaumcraft.api.FluidTanks;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.common.tiles.TileThaumcraft;
import thaumcraft.init.ModBlockEntities;
import thaumcraft.common.lib.crafting.ThaumcraftCraftingManager;
import thaumcraft.common.lib.crafting.CrucibleRecipeType;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.fx.PacketFXCrucible;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;

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
    
    private final FluidStacksResourceHandler tank = new FluidStacksResourceHandler(1, TANK_CAPACITY) {
        @Override
        public boolean isValid(int slot, FluidResource resource) {
            return resource.getFluid() == Fluids.WATER;
        }
    };

    private int bellows = -1;
    private long tickCounter = 0;

    public TileCrucible(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRUCIBLE.get(), pos, state);
    }

    // ==================== NBT ====================

    @Override
    protected void writeSyncNBT(ValueOutput output) {
        super.writeSyncNBT(output);
        output.putShort("Heat", heat);
        output.putChild("tank", tank);
        aspects.writeToNBT(output);
    }

    @Override
    protected void readSyncNBT(ValueInput input) {
        super.readSyncNBT(input);
        heat = (short) input.getShortOr("Heat", (short)0);
        input.readChild("tank", tank);
        aspects.readFromNBT(input);
    }

    // ==================== Tick ====================

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileCrucible tile) {
        tile.tickCounter++;
        int prevHeat = tile.heat;

        // Check for heat source below
        if (FluidTanks.getAmount(tile.tank) > 0) {
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
               state.is(Blocks.MAGMA_BLOCK) ||
               state.getBlock() instanceof thaumcraft.common.blocks.misc.BlockNitor;
    }

    // ==================== Smelting ====================

    /**
     * Attempt to smelt an item dropped into the crucible.
     */
    public void attemptSmelt(ItemEntity itemEntity) {
        if (level == null || level.isClientSide()) return;
        if (heat < 151 || FluidTanks.getAmount(tank) <= 0) return;

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
        if (level == null || username == null) return item;

        // 1.12: the thrower name comes from the item entity's NBT; resolve it to
        // the live player (null if they logged off / the item came from a machine).
        Player player = level.getPlayerByUUID(java.util.UUID.nameUUIDFromBytes(username.getBytes()));
        if (player == null && !username.isEmpty()) {
            player = level.players().stream().filter(p -> p.getName().getString().equals(username)).findFirst().orElse(null);
        }
        return attemptSmelt(item, player);
    }

    /**
     * Attempt to smelt an item stack with the given (possibly null) thrower.
     * 1.12 parity: a null player can only dissolve, never craft
     * (ThaumcraftCraftingManager.findMatchingCrucibleRecipe requires player != null).
     * Returns remaining items, or null if fully consumed.
     */
    public ItemStack attemptSmelt(ItemStack item, @Nullable Player player) {
        if (level == null) return item;
        
        boolean itemChanged = false;
        int remaining = item.getCount();

        // Check for crucible recipe
        CrucibleRecipeType recipe = ThaumcraftCraftingManager.findMatchingCrucibleRecipe(aspects, item, player, level);
        
        if (recipe != null) {
            // Found a recipe!
            AspectList required = recipe.getAspects();
            if (aspects.contains(required)) {
                aspects.remove(required);

                // 1.12 parity: each craft consumes 50 mb of water
                // (TileCrucible: this.tank.drain(50, true))
                FluidTanks.drain(tank, 50, false);

                // Crafted successfully
                ItemStack result = recipe.assemble(null);
                
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
                
                // 1.12-faithful: block event 99 (bamf above the crucible + spill sound)
                sendCrucibleFX(99, 0);

                // 1.12: FMLCommonHandler.firePlayerCraftingEvent(player, out, InventoryFake)
                // - sets the "[#]" research craft flags (e.g. crafting the yellow nitor
                //   completes UNLOCKALCHEMY stage 3)
                // - applies item warp (CraftingEvents.onCrafting, wuss-mode aware)
                // - feeds advancements / recipe-used tracking
                if (player != null && !level.isClientSide()) {
                    NeoForge.EVENT_BUS.post(new PlayerEvent.ItemCraftedEvent(player, result.copy(), null));
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

        // 1.12 parity (TileCrucible.attemptSmelt): no recipe matched, so the
        // item dissolves and its aspects go into THE CRUCIBLE'S pool
        // (this.aspects) — not a throwaway list.
        AspectList itemAspects = getItemAspects(item);
        if (itemAspects != null && itemAspects.size() > 0) {
            for (Aspect aspect : itemAspects.getAspects()) {
                this.aspects.add(aspect, itemAspects.getAmount(aspect));
            }
            remaining--;
            itemChanged = true;
            tickCounter = -150; // Reset spill timer

            level.playSound(null, worldPosition, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP,
                    SoundSource.BLOCKS, 0.2f, 1.0f + level.getRandom().nextFloat() * 0.4f);

            // 1.12-faithful: block event 2/1 (10x crucibleBoil + spill sound)
            sendCrucibleFX(2, 1);
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
            Aspect toSpill = aspectArray[level.getRandom().nextInt(aspectArray.length)];
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
        if (FluidTanks.getAmount(tank) > 0 || total > 0) {
            FluidTanks.drain(tank, TANK_CAPACITY, false);
            AuraHelper.polluteAura(level, worldPosition, total * 0.25f, true);

            int flux = aspects.getAmount(Aspect.FLUX);
            if (flux > 0) {
                AuraHelper.polluteAura(level, worldPosition, flux * 0.75f, false);
            }

            aspects = new AspectList();

            // 1.12-faithful: block event 2/5 (10x crucibleBoil + spill sound)
            sendCrucibleFX(2, 5);
            markDirtyAndSync();
        }
    }
    
    /**
     * Send a crucible FX event to tracking clients (1.12 addBlockEvent replacement).
     */
    private void sendCrucibleFX(int type, int data) {
        if (level instanceof ServerLevel serverLevel) {
            PacketHandler.sendToAllTrackingChunk(new PacketFXCrucible(worldPosition, type, data), serverLevel, worldPosition);
        }
    }
    
    /**
     * 1.12-faithful client-side crucible effects (1.12 TileCrucible.drawEffects).
     * Called from BlockCrucible.animateTick while the crucible holds fluid.
     */
    public void drawEffects(RandomSource rand) {
        if (heat > 150) {
            FXDispatcher.INSTANCE.crucibleFroth(worldPosition.getX() + 0.2f + rand.nextFloat() * 0.6f,
                    worldPosition.getY() + getFluidHeight(), worldPosition.getZ() + 0.2f + rand.nextFloat() * 0.6f);
            if (aspects.visSize() > 500) {
                for (int a = 0; a < 2; a++) {
                    FXDispatcher.INSTANCE.crucibleFrothDown(worldPosition.getX(), worldPosition.getY() + 1, worldPosition.getZ() + rand.nextFloat());
                    FXDispatcher.INSTANCE.crucibleFrothDown(worldPosition.getX() + 1, worldPosition.getY() + 1, worldPosition.getZ() + rand.nextFloat());
                    FXDispatcher.INSTANCE.crucibleFrothDown(worldPosition.getX() + rand.nextFloat(), worldPosition.getY() + 1, worldPosition.getZ());
                    FXDispatcher.INSTANCE.crucibleFrothDown(worldPosition.getX() + rand.nextFloat(), worldPosition.getY() + 1, worldPosition.getZ() + 1);
                }
            }
        }
        if (rand.nextInt(6) == 0 && aspects.size() > 0) {
            int color = aspects.getAspects()[rand.nextInt(aspects.getAspects().length)].getColor() - 16777216;
            int x = 5 + rand.nextInt(22);
            int y = 5 + rand.nextInt(22);
            java.awt.Color c = new java.awt.Color(color);
            FXDispatcher.INSTANCE.crucibleBubble(worldPosition.getX() + x / 32.0f + 0.015625f,
                    worldPosition.getY() + 0.05f + getFluidHeight(), worldPosition.getZ() + y / 32.0f + 0.015625f,
                    c.getRed() / 255.0f, c.getGreen() / 255.0f, c.getBlue() / 255.0f);
        }
    }

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

    /**
     * ResourceHandler view of the internal water tank (used by the registered fluid capability).
     */
    public ResourceHandler<FluidResource> getTankHandler() {
        return tank;
    }

    public ResourceHandler<FluidResource> getTank() {
        return tank;
    }

    public float getFluidHeight() {
        float base = 0.3f + 0.5f * (FluidTanks.getAmount(tank) / (float) TANK_CAPACITY);
        float extra = aspects.visSize() / (float) MAX_ASPECTS * (1.0f - base);
        float total = base + extra;
        return Math.min(total, 0.9999f);
    }

    // ==================== Rendering ====================

    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                worldPosition.getX() + 1, worldPosition.getY() + 1, worldPosition.getZ() + 1);
    }

    public boolean isHeated() {
        return heat >= 151;
    }
}
