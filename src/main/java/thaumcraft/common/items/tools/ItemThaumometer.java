package thaumcraft.common.items.tools;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.research.ScanningManager;
import thaumcraft.client.ThaumometerHUD;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.common.items.ItemTC;
import thaumcraft.init.ModSounds;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.server.level.ServerLevel;

/**
 * Thaumometer - the basic scanning tool of Thaumcraft.
 * Right-click to scan entities and blocks to discover their aspects
 * and unlock research.
 */
public class ItemThaumometer extends ItemTC {

    private static final double SCAN_RANGE = 9.0;

    public ItemThaumometer() {
        super(new Properties()
                .stacksTo(1)
                .rarity(Rarity.UNCOMMON));
    }

    /**
     * 1.12 ItemThaumometer.onItemRightClick ran before block activation and returned SUCCESS, so
     * pointing at a chest, fence, crop or sign scanned instead of opening/placing. In 26.3 Item.use()
     * runs last, so the scan has to be intercepted here or every block swallows the scan.
     */
    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        return scanInteraction(context.getLevel(), player, context.getHand());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        return scanInteraction(level, player, hand);
    }

    private InteractionResult scanInteraction(Level level, Player player, InteractionHand hand) {
        // Play scan sound (works on both sides, but server broadcasts to nearby players)
        level.playSound(player, player.getX(), player.getY(), player.getZ(), 
                ModSounds.SCAN.get(), SoundSource.PLAYERS, 0.5f, 1.0f);

        if (level.isClientSide()) {
            // 1.12-faithful: draw blockRunes (runes) on the scan target (entity or block) instead of
            // vanilla ENCHANT particles. Matches 1.12 ItemThaumometer.drawFX.
            Entity target = getTargetEntity(level, player, SCAN_RANGE);
            if (target != null) {
                for (int a = 0; a < 10; ++a) {
                    FXDispatcher.INSTANCE.blockRunes(
                        target.getX() - 0.5, target.getY() + target.getBbHeight() / 2.0f, target.getZ() - 0.5,
                        0.3f + level.getRandom().nextFloat() * 0.7f, 0.0f, 0.3f + level.getRandom().nextFloat() * 0.7f,
                        (int) (target.getBbHeight() * 15.0f), 0.03f);
                }
            } else {
                BlockHitResult blockHit = getTargetBlock(level, player, SCAN_RANGE);
                if (blockHit.getType() == HitResult.Type.BLOCK) {
                    BlockPos pos = blockHit.getBlockPos();
                    for (int a = 0; a < 10; ++a) {
                        FXDispatcher.INSTANCE.blockRunes(
                            pos.getX(), pos.getY() + 0.25, pos.getZ(),
                            0.3f + level.getRandom().nextFloat() * 0.7f, 0.0f, 0.3f + level.getRandom().nextFloat() * 0.7f,
                            15, 0.03f);
                    }
                }
            }
            return InteractionResult.SUCCESS;
        }

        // Server-side: perform the scan
        doScan(level, player);
        return InteractionResult.CONSUME;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        if (!(entity instanceof Player player)) return;
        
        boolean held = slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND; // Main hand or offhand slot
        
        if (!held) return;

        // 2026-10-02: the aura info is shown in the Arcane Workbench GUI, not as a
        // per-second chat message while the thaumometer is held (removed the spam).

        // Client: highlight scannable targets
        if (level.isClientSide() && entity.tickCount % 5 == 0) {
            highlightScannables(level, player);
        }
    }

    /**
     * Perform a scan at the player's look target.
     */
    private void doScan(Level level, Player player) {
        // First try to scan an entity
        Entity targetEntity = getTargetEntity(level, player, SCAN_RANGE);
        if (targetEntity != null) {
            ScanningManager.scanTheThing(player, targetEntity);
            return;
        }

        // Then try to scan a block
        BlockHitResult blockHit = getTargetBlock(level, player, SCAN_RANGE);
        if (blockHit.getType() == HitResult.Type.BLOCK) {
            ScanningManager.scanTheThing(player, blockHit.getBlockPos());
            return;
        }

        // No target - scan the sky/void
        ScanningManager.scanTheThing(player, (BlockPos) null);
    }

    /**
     * Highlight scannable things on the client.
     */
    private void highlightScannables(Level level, Player player) {
        // 1.12-faithful: TC scan sparkles (FXDispatcher.scanHighlight) instead of vanilla ENCHANT.
        // Matches 1.12 ItemThaumometer.onUpdate (scanHighlight every 5 ticks while held).
        Entity target = getTargetEntity(level, player, 16.0);
        // 1.12 RenderEventHandler.thaumTarget: the aspect-tag renderer reads this to draw the
        // target's aspects above it while the thaumometer is held.
        ThaumometerHUD.target = target;
        if (target != null && ScanningManager.isThingStillScannable(player, target)) {
            FXDispatcher.INSTANCE.scanHighlight(target);
        }
        
        // Also highlight blocks
        BlockHitResult blockHit = getTargetBlock(level, player, 16.0);
        if (blockHit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = blockHit.getBlockPos();
            if (ScanningManager.isThingStillScannable(player, pos)) {
                FXDispatcher.INSTANCE.scanHighlight(pos);
            }
        }
    }

    /**
     * Get the entity the player is looking at.
     */
    private Entity getTargetEntity(Level level, Player player, double range) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Vec3 targetPos = eyePos.add(lookVec.scale(range));

        // Simple AABB check for entities
        var aabb = player.getBoundingBox().expandTowards(lookVec.scale(range)).inflate(1.0);
        var entities = level.getEntities(player, aabb, e -> e != player && e.isPickable());

        Entity closest = null;
        double closestDist = range;

        for (Entity entity : entities) {
            var entityAABB = entity.getBoundingBox().inflate(entity.getPickRadius());
            var optional = entityAABB.clip(eyePos, targetPos);
            if (optional.isPresent()) {
                double dist = eyePos.distanceTo(optional.get());
                if (dist < closestDist) {
                    closest = entity;
                    closestDist = dist;
                }
            }
        }

        return closest;
    }

    /**
     * Get the block the player is looking at.
     */
    private BlockHitResult getTargetBlock(Level level, Player player, double range) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Vec3 targetPos = eyePos.add(lookVec.scale(range));

        return level.clip(new ClipContext(eyePos, targetPos,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY, player));
    }
}
