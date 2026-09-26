package thaumcraft.common.entities;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import thaumcraft.init.ModEntities;
import net.minecraft.core.registries.Registries;

/**
 * EntityFallingTaint - A falling taint block entity.
 * Similar to FallingBlockEntity but for taint blocks.
 */
public class EntityFallingTaint extends Entity {
    
    public BlockState fallTile;
    private BlockPos oldPos;
    public int fallTime;
    private int fallHurtMax = 40;
    private float fallHurtAmount = 2.0f;
    
    public EntityFallingTaint(EntityType<?> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
    }
    
    public EntityFallingTaint(Level level) {
        this(ModEntities.FALLING_TAINT.get(), level);
    }
    
    @Override
    public boolean hurtServer(net.minecraft.server.level.ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount) {
        return false;
    }
    
    public EntityFallingTaint(Level level, double x, double y, double z, BlockState state, BlockPos originalPos) {
        this(ModEntities.FALLING_TAINT.get(), level);
        this.fallTile = state;
        this.oldPos = originalPos;
        this.blocksBuilding = true;
        setPos(x, y, z);
        setDeltaMovement(Vec3.ZERO);
        xo = x;
        yo = y;
        zo = z;
    }
    
    public BlockState getBlockState() {
        return fallTile;
    }
    
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // No synched data needed
    }
    
    @Override
    public boolean isPickable() {
        return !isRemoved();
    }
    
    @Override
    protected boolean repositionEntityAfterLoad() {
        return false;
    }
    
    @Override
    public void tick() {
        if (fallTile == null || fallTile.isAir()) {
            discard();
            return;
        }
        
        xo = getX();
        yo = getY();
        zo = getZ();
        
        ++fallTime;
        
        // Apply gravity
        setDeltaMovement(getDeltaMovement().add(0.0, -0.04, 0.0));
        move(MoverType.SELF, getDeltaMovement());
        
        // Apply drag
        setDeltaMovement(getDeltaMovement().scale(0.98));
        
        BlockPos currentPos = blockPosition();
        
        if (!level().isClientSide()) {
            // First tick - remove original block
            if (fallTime == 1) {
                if (oldPos != null && !level().getBlockState(oldPos).equals(fallTile)) {
                    discard();
                    return;
                }
                if (oldPos != null) {
                    level().removeBlock(oldPos, false);
                }
            }
            
            // Check if landed
            if (onGround() || isTaintGooBelow(currentPos)) {
                // Bounce slightly
                setDeltaMovement(getDeltaMovement().multiply(0.7, -0.5, 0.7));
                
                // Place block if possible
                if (canPlace(currentPos)) {
                    // Play gore sound
                    if (thaumcraft.init.ModSounds.GORE != null) {
                        level().playSound(null, blockPosition(), thaumcraft.init.ModSounds.GORE.get(),
                            net.minecraft.sounds.SoundSource.NEUTRAL, 0.8f, 1.0f);
                    }
                    discard();
                    level().setBlock(currentPos, fallTile, 3);
                } else {
                    discard();
                }
            } else if ((fallTime > 100 && (currentPos.getY() < level().getMinY() || 
                    currentPos.getY() > level().getMaxY())) || fallTime > 600) {
                discard();
            }
        } else {
            // Client-side particles on landing
            if (onGround() || fallTime == 1) {
                // Taint landing FX
                if (!level().isClientSide()) {
                    for (int i = 0; i < 8; i++) {
                        double dx = (Math.random() - 0.5) * 0.5;
                        double dy = Math.random() * 0.3;
                        double dz = (Math.random() - 0.5) * 0.5;
                        level().addParticle(net.minecraft.core.particles.ParticleTypes.DRIPPING_LAVA,
                            getX() + dx, getY() + dy, getZ() + dz, 0, 0.1, 0);
                    }
                }
            }
        }
    }
    
    private boolean isTaintGooBelow(BlockPos pos) {
        // Check for flux goo below
        if (thaumcraft.init.ModBlocks.FLUX_GOO != null && 
            level().getBlockState(pos.below()).is(thaumcraft.init.ModBlocks.FLUX_GOO.get())) {
            return true;
        }
        return false;
    }
    
    private boolean canPlace(BlockPos pos) {
        BlockState currentState = level().getBlockState(pos);
        // Can place in air or replaceable blocks
        if (currentState.isAir() || currentState.canBeReplaced()) {
            return true;
        }
        // Check for taint fiber or flux goo
        if (thaumcraft.init.ModBlocks.TAINT_FIBRE != null && 
            currentState.is(thaumcraft.init.ModBlocks.TAINT_FIBRE.get())) {
            // Taint fiber slows the fall
            setDeltaMovement(getDeltaMovement().multiply(0.8, 0.5, 0.8));
        } else if (thaumcraft.init.ModBlocks.FLUX_GOO != null && 
                   currentState.is(thaumcraft.init.ModBlocks.FLUX_GOO.get())) {
            // Flux goo absorbs the falling taint
            level().removeBlock(pos, false);
            level().setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        }
        return false;
    }
    
    @Override
    public boolean causeFallDamage(double distance, float multiplier, net.minecraft.world.damagesource.DamageSource source) {
        return false;
    }
    
    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        if (fallTile != null) {
            output.store("BlockState", net.minecraft.world.level.block.state.BlockState.CODEC, fallTile);
        }
        output.putInt("Time", fallTime);
        output.putFloat("FallHurtAmount", fallHurtAmount);
        output.putInt("FallHurtMax", fallHurtMax);
        if (oldPos != null) {
            output.putLong("Old", oldPos.asLong());
        }
    }
    
    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        fallTile = input.read("BlockState", net.minecraft.world.level.block.state.BlockState.CODEC).orElse(Blocks.SAND.defaultBlockState());
        fallTime = input.getIntOr("Time", 0);
        fallHurtAmount = input.getFloatOr("FallHurtAmount", 0.0F);
        fallHurtMax = input.getIntOr("FallHurtMax", 0);
        input.getLong("Old").ifPresent(v -> oldPos = BlockPos.of(v));
    }
    
    @Override
    public SoundSource getSoundSource() {
        return SoundSource.BLOCKS;
    }
    
    @Override
    public boolean displayFireAnimation() {
        return false;
    }
}
