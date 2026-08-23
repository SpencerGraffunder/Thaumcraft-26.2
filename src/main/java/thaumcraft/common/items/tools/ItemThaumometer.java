package thaumcraft.common.items.tools;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Play scan sound (works on both sides, but server broadcasts to nearby players)
        level.playSound(player, player.getX(), player.getY(), player.getZ(), 
                ModSounds.SCAN.get(), SoundSource.PLAYERS, 0.5f, 1.0f);

        if (level.isClientSide()) {
            // Client-side: spawn particles
            // TODO: FXDispatcher effects - scanning beam, highlight effects
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

        // Server: periodically update aura info
        if (!level.isClientSide() && entity.tickCount % 20 == 0) {
            updateAuraInfo(level, player);
        }

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
     * Update aura information for the player.
     */
    private void updateAuraInfo(Level level, Player player) {
        // TODO: Send aura chunk data to player
        // AuraChunk ac = AuraHandler.getAuraChunk(level, player.blockPosition());
        // if (ac != null) {
        //     PacketHandler.sendTo(new PacketAuraToClient(ac), player);
        // }
    }

    /**
     * Highlight scannable things on the client.
     */
    private void highlightScannables(Level level, Player player) {
        // TODO: Use FXDispatcher to highlight targets
        // Entity target = getTargetEntity(level, player, 16.0);
        // if (target != null && ScanningManager.isThingStillScannable(player, target)) {
        //     FXDispatcher.INSTANCE.scanHighlight(target);
        // }
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
