package thaumcraft.common.items.resources;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thaumcraft.Thaumcraft;
import thaumcraft.api.crafting.IDustTrigger;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.common.items.ItemTCBase;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.init.ModSounds;

import java.util.List;
import java.util.Random;

public class ItemMagicDust extends ItemTCBase {

    public ItemMagicDust() {
        super(new Properties().rarity(Rarity.UNCOMMON));
    }

    // 26.3 dispatches "item used on a block" to Item.onItemUseFirst(stack, context) (called
    // BEFORE the block's useItemOn). This is the 1.12 onItemUseFirst equivalent. The old port
    // put this logic in useOn(context), which 26.3 only calls AFTER the block's useItemOn -- so
    // for blocks that consume the interaction (e.g. the crafting table opening its GUI) the dust
    // logic was never reached and the GUI just opened. Must be onItemUseFirst so the dust trigger
    // gets first crack at the block.
    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction face = context.getClickedFace();
        InteractionHand hand = context.getHand();

        if (player == null) return InteractionResult.FAIL;

        // Note: canPlayerEdit check is handled by Forge/Vanilla mostly but explicit check can be good
        // In 1.20.1 we rely on context usually.

        if (player.isCrouching()) {
            return InteractionResult.PASS;
        }

        player.swingAndResetAttackStrength(hand, net.minecraft.world.item.component.SwingAnimation.DEFAULT, false);

        for (IDustTrigger trigger : IDustTrigger.triggers) {
            IDustTrigger.Placement place = trigger.getValidFace(level, player, pos, face);
            if (place != null) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                
                trigger.execute(level, player, pos, place, face);
                
                if (level.isClientSide()) {
                    // Client side effects
                    doSparkles(player, level, pos, context.getClickLocation(), hand, trigger, place);
                } else {
                    // Server side could also trigger some effects if needed, but sparkle logic usually client
                    // However, actual block changes happen here (in trigger.execute)
                }
                
                return InteractionResult.SUCCESS;
            }
        }

        if (level.getBlockState(pos).is(Blocks.CRAFTING_TABLE) && !level.isClientSide()) {
            Thaumcraft.LOGGER.info("[SALIS-DBG] salis onItemUseFirst on CRAFTING_TABLE fell through (no trigger matched) -> super (GUI will open). player={}", player.getName().getString());
        }

        return super.onItemUseFirst(stack, context);
    }

    private void doSparkles(Player player, Level level, BlockPos pos, Vec3 hitVec, InteractionHand hand, IDustTrigger trigger, IDustTrigger.Placement place) {
        // 1.12-faithful: hand-to-block sparkle line (drawSimpleSparkle) + block sparkles
        // (drawBlockSparkles). Replaces the old vanilla ENCHANT-particle stubs.
        if (!level.isClientSide()) return;
        Random rand = new Random();
        Vec3 v1 = player.getEyePosition();
        Vec3 v2 = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5).subtract(v1);
        for (int a = 0; a < 50; a++) {
            boolean floaty = a < 50 / 3;
            float r = 1.0f;
            float g = (189 + rand.nextInt(67)) / 255.0f;
            float b = (64 + rand.nextInt(192)) / 255.0f;
            FXDispatcher.INSTANCE.drawSimpleSparkle(rand, v1.x, v1.y, v1.z,
                    v2.x / 6.0 + rand.nextGaussian() * 0.05,
                    v2.y / 6.0 + rand.nextGaussian() * 0.05 + (floaty ? 0.05 : 0.15),
                    v2.z / 6.0 + rand.nextGaussian() * 0.05,
                    0.5f, r, g, b, rand.nextInt(5),
                    floaty ? (0.3f + rand.nextFloat() * 0.5f) : 0.85f,
                    floaty ? 0.2f : 0.5f, 16);
        }
        if (ModSounds.DUST.get() != null) {
            level.playSound(player, pos, ModSounds.DUST.get(), SoundSource.PLAYERS, 0.33f, 1.0f + (float) rand.nextGaussian() * 0.05f);
        }
        List<BlockPos> sparkles = trigger.sparkle(level, player, pos, place);
        if (sparkles != null) {
            Vec3 v3 = new Vec3(pos.getX() + hitVec.x, pos.getY() + hitVec.y, pos.getZ() + hitVec.z);
            for (BlockPos p : sparkles) {
                FXDispatcher.INSTANCE.drawBlockSparkles(p, v3);
            }
        }
    }
}
