package thaumcraft.common.tiles.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.common.tiles.TileThaumcraftInventory;
import thaumcraft.init.ModBlockEntities;

import java.util.List;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;

/**
 * TilePotionSprayer - Applies potion effects to nearby entities using essentia.
 * 
 * Features:
 * - Accepts a potion item to determine the effect
 * - Draws essentia matching the potion's aspects
 * - Stores up to 8 charges
 * - Sprays effect when triggered by redstone
 */
public class TilePotionSprayer extends TileThaumcraftInventory implements IAspectContainer, IEssentiaTransport, WorldlyContainer {
    
    private static final int[] SLOTS = {0};
    
    public AspectList recipe = new AspectList();
    public AspectList recipeProgress = new AspectList();
    public int charges = 0;
    public int color = 0x333333;
    
    private int counter = 0;
    private boolean activated = false;
    private int venting = 0;
    private Aspect currentSuction = null;
    
    public TilePotionSprayer(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 1);
    }
    
    public TilePotionSprayer(BlockPos pos, BlockState state) {
        this(ModBlockEntities.POTION_SPRAYER.get(), pos, state);
    }
    
    // ==================== Tick ====================
    
    public static void serverTick(Level level, BlockPos pos, BlockState state, TilePotionSprayer tile) {
        tile.counter++;
        Direction facing = tile.getFacing();
        
        // Essentia filling logic
        if (tile.counter % 5 == 0) {
            tile.currentSuction = null;
            
            if (tile.getItem(0).isEmpty() || tile.charges >= 8) {
                return;
            }
            
            // Check if recipe is complete
            boolean done = true;
            for (Aspect aspect : tile.recipe.getAspectsSortedByName()) {
                if (tile.recipeProgress.getAmount(aspect) < tile.recipe.getAmount(aspect)) {
                    tile.currentSuction = aspect;
                    done = false;
                    break;
                }
            }
            
            if (done) {
                // Recipe complete - add a charge
                tile.recipeProgress = new AspectList();
                tile.charges++;
                tile.syncTile(false);
                tile.setChanged();
            } else if (tile.currentSuction != null) {
                // Try to draw essentia
                tile.fill();
            }
        }
        
        // Spray logic - activates on redstone signal going low
        boolean isEnabled = !level.hasNeighborSignal(pos);
        
        if (!isEnabled) {
            if (!tile.activated && tile.charges > 0) {
                tile.charges--;
                
                // Get potion effects from the stored potion
                List<MobEffectInstance> effects = PotionUtils.getMobEffects(tile.getItem(0));
                if (effects != null && !effects.isEmpty()) {
                    // Get targets in range
                    int area = 1;
                    BlockPos targetPos = pos.relative(facing, 2);
                    AABB box = new AABB(targetPos).inflate(area);
                    List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box);
                    
                    for (LivingEntity target : targets) {
                        if (target.isAlive() && target.canBeAffected(new MobEffectInstance(effects.get(0).getEffect(), 1))) {
                            for (MobEffectInstance effect : effects) {
                                Holder<MobEffect> potion = effect.getEffect();
                                if (potion.value().isInstantaneous()) {
                                    potion.value().applyInstantaneousEffect((ServerLevel) level, null, null, target,
                                            effect.getAmplifier(), 1.0);
                                } else {
                                    target.addEffect(new MobEffectInstance(potion,
                                            effect.getDuration(), effect.getAmplifier()));
                                }
                            }
                        }
                    }
                }
                
                // Play sound and visual effect
                level.playSound(null, pos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 
                        0.25f, 2.6f + (level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.8f);
                level.blockEvent(pos, state.getBlock(), 0, 0);
                tile.syncTile(false);
                tile.setChanged();
            }
            tile.activated = true;
        } else if (tile.activated) {
            tile.activated = false;
        }
    }
    
    public static void clientTick(Level level, BlockPos pos, BlockState state, TilePotionSprayer tile) {
        Direction facing = tile.getFacing();
        
        if (tile.venting > 0) {
            tile.venting--;
            for (int a = 0; a < tile.venting / 2; a++) {
                float fx = 0.1f - level.getRandom().nextFloat() * 0.2f;
                float fy = 0.1f - level.getRandom().nextFloat() * 0.2f;
                float fz = 0.1f - level.getRandom().nextFloat() * 0.2f;
                float fx2 = (float)(level.getRandom().nextGaussian() * 0.06);
                float fy2 = (float)(level.getRandom().nextGaussian() * 0.06);
                float fz2 = (float)(level.getRandom().nextGaussian() * 0.06);
                
                FXDispatcher.INSTANCE.drawVentParticles2(
                        pos.getX() + 0.5f + fx + facing.getStepX() / 2.0f,
                        pos.getY() + 0.5f + fy + facing.getStepY() / 2.0f,
                        pos.getZ() + 0.5f + fz + facing.getStepZ() / 2.0f,
                        fx2 + facing.getStepX() * 0.25,
                        fy2 + facing.getStepY() * 0.25,
                        fz2 + facing.getStepZ() * 0.25,
                        tile.color, 4.0f);
            }
        }
    }
    
    private Direction getFacing() {
        BlockState state = getBlockState();
        if (state.hasProperty(BlockStateProperties.FACING)) {
            return state.getValue(BlockStateProperties.FACING);
        }
        return Direction.NORTH;
    }
    
    @Override
    public boolean triggerEvent(int id, int param) {
        if (id >= 0) {
            if (level != null && level.isClientSide()) {
                venting = 15;
            }
            return true;
        }
        return super.triggerEvent(id, param);
    }
    
    // ==================== Essentia Filling ====================
    
    private void fill() {
        Direction facing = getFacing();
        
        for (int y = 0; y <= 1; y++) {
            for (Direction dir : Direction.values()) {
                if (dir != facing) {
                    BlockEntity te = ThaumcraftApiHelper.getConnectableTile(level, worldPosition.above(y), dir);
                    if (te instanceof IEssentiaTransport transport) {
                        Direction opposite = dir.getOpposite();
                        if (transport.getEssentiaAmount(opposite) > 0 &&
                                transport.getSuctionAmount(opposite) < getSuctionAmount(null) &&
                                getSuctionAmount(null) >= transport.getMinimumSuction()) {
                            
                            int taken = transport.takeEssentia(currentSuction, 1, opposite);
                            if (taken > 0) {
                                addToContainer(currentSuction, taken);
                                return;
                            }
                        }
                    }
                }
            }
        }
    }
    
    // ==================== Inventory ====================
    
    @Override
    public void setItem(int slot, ItemStack stack) {
        super.setItem(slot, stack);
        recalcAspects();
    }
    
    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = super.removeItem(slot, amount);
        recalcAspects();
        return result;
    }
    
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return isValidPotion(stack);
    }
    
    public static boolean isValidPotion(ItemStack stack) {
        if (stack.isEmpty()) return false;
        List<MobEffectInstance> effects = PotionUtils.getMobEffects(stack);
        return effects != null && !effects.isEmpty();
    }
    
    private void recalcAspects() {
        if (level == null || level.isClientSide()) return;
        
        ItemStack stack = getItem(0);
        color = 0x333333;
        
        if (stack.isEmpty()) {
            recipe = new AspectList();
        } else {
            // Calculate aspects from potion effects
            recipe = getPotionAspects(stack);
            color = PotionUtils.getColor(stack);
        }
        
        charges = 0;
        recipeProgress = new AspectList();
        syncTile(false);
        setChanged();
    }
    
    /**
     * Calculate aspects required for a potion.
     */
    private AspectList getPotionAspects(ItemStack stack) {
        AspectList aspects = new AspectList();
        List<MobEffectInstance> effects = PotionUtils.getMobEffects(stack);
        
        if (effects != null) {
            for (MobEffectInstance effect : effects) {
                // Basic essentia cost per effect
                aspects.add(Aspect.ALCHEMY, 2 + effect.getAmplifier());
                
                // Add aspects based on effect type
                Holder<MobEffect> potion = effect.getEffect();
                if (potion.value().isBeneficial()) {
                    aspects.add(Aspect.LIFE, 1);
                } else {
                    aspects.add(Aspect.AVERSION, 1);
                }
                
                // Duration cost
                if (effect.getDuration() > 600) {
                    aspects.add(Aspect.ORDER, 1);
                }
            }
        }
        
        // Cap at 10 of any aspect
        AspectList culled = new AspectList();
        for (Aspect a : aspects.getAspects()) {
            culled.add(a, Math.min(10, aspects.getAmount(a)));
        }
        return culled;
    }
    
    // ==================== NBT ====================
    
    @Override
    protected void writeSyncNBT(ValueOutput output) {
        super.writeSyncNBT(output);
        recipe.writeToNBT(output, "recipe");
        recipeProgress.writeToNBT(output, "progress");
        output.putInt("charges", charges);
        output.putInt("color", color);
    }
    
    @Override
    protected void readSyncNBT(ValueInput input) {
        super.readSyncNBT(input);
        recipe = new AspectList();
        recipe.readFromNBT(input, "recipe");
        recipeProgress = new AspectList();
        recipeProgress.readFromNBT(input, "progress");
        charges = input.getIntOr("charges", 0);
        color = input.getIntOr("color", 0);
    }
    
    // ==================== WorldlyContainer ====================
    
    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }
    
    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return canPlaceItem(slot, stack);
    }
    
    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return true;
    }
    
    // ==================== IAspectContainer ====================
    
    @Override
    public AspectList getAspects() {
        return recipeProgress;
    }
    
    @Override
    public void setAspects(AspectList aspects) {
        recipeProgress = aspects;
    }
    
    @Override
    public int addToContainer(Aspect tag, int amount) {
        int needed = recipe.getAmount(tag) - recipeProgress.getAmount(tag);
        if (needed <= 0) return amount;
        
        int add = Math.min(needed, amount);
        recipeProgress.add(tag, add);
        syncTile(false);
        setChanged();
        return amount - add;
    }
    
    @Override
    public boolean takeFromContainer(Aspect tag, int amount) {
        return false;
    }
    
    @Override
    public boolean takeFromContainer(AspectList list) {
        return false;
    }
    
    @Override
    public boolean doesContainerContain(AspectList list) {
        return false;
    }
    
    @Override
    public boolean doesContainerContainAmount(Aspect tag, int amount) {
        return recipeProgress.getAmount(tag) >= amount;
    }
    
    @Override
    public int containerContains(Aspect tag) {
        return recipeProgress.getAmount(tag);
    }
    
    @Override
    public boolean doesContainerAccept(Aspect tag) {
        return true;
    }
    
    // ==================== IEssentiaTransport ====================
    
    @Override
    public boolean isConnectable(Direction face) {
        return face != getFacing();
    }
    
    @Override
    public boolean canInputFrom(Direction face) {
        return face != getFacing();
    }
    
    @Override
    public boolean canOutputTo(Direction face) {
        return false;
    }
    
    @Override
    public void setSuction(Aspect aspect, int amount) {
        currentSuction = aspect;
    }
    
    @Override
    public Aspect getSuctionType(Direction face) {
        return currentSuction;
    }
    
    @Override
    public int getSuctionAmount(Direction face) {
        return currentSuction != null ? 128 : 0;
    }
    
    @Override
    public Aspect getEssentiaType(Direction face) {
        return null;
    }
    
    @Override
    public int getEssentiaAmount(Direction face) {
        return 0;
    }
    
    @Override
    public int takeEssentia(Aspect aspect, int amount, Direction face) {
        return 0;
    }
    
    @Override
    public int addEssentia(Aspect aspect, int amount, Direction face) {
        if (canInputFrom(face)) {
            return amount - addToContainer(aspect, amount);
        }
        return 0;
    }
    
    @Override
    public int getMinimumSuction() {
        return 0;
    }
}
