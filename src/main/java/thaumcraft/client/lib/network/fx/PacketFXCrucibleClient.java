package thaumcraft.client.lib.network.fx;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.common.lib.network.fx.PacketFXCrucible;
import thaumcraft.common.tiles.crafting.TileCrucible;
import thaumcraft.init.ModSounds;

/**
 * Client-side handler for {@link PacketFXCrucible} - replicates the 1.12
 * TileCrucible.receiveClientEvent FX behavior.
 */
public class PacketFXCrucibleClient {
    public static void handle(PacketFXCrucible msg) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;

        if (msg.type == 99) {
            // Craft done: bamf above the crucible + spill sound
            FXDispatcher.INSTANCE.drawBamf(msg.pos.getX() + 0.5, msg.pos.getY() + 1.25f, msg.pos.getZ() + 0.5, -9999, true, true, Direction.UP);
            level.playLocalSound(msg.pos.getX() + 0.5f, msg.pos.getY() + 0.5f, msg.pos.getZ() + 0.5f,
                    ModSounds.SPILL.get(), SoundSource.BLOCKS, 0.2f, 1.0f, false);
        } else if (msg.type == 1) {
            // Item dropped in: bamf on the block
            FXDispatcher.INSTANCE.drawBamf(msg.pos.above(), -9999, true, true, Direction.UP);
        } else if (msg.type == 2) {
            // Dissolve/boil (data=1) or full spill (data=5): 10x crucibleBoil + spill sound
            level.playLocalSound(msg.pos.getX() + 0.5f, msg.pos.getY() + 0.5f, msg.pos.getZ() + 0.5f,
                    ModSounds.SPILL.get(), SoundSource.BLOCKS, 0.2f, 1.0f, false);
            BlockEntity be = level.getBlockEntity(msg.pos);
            if (be instanceof TileCrucible tile) {
                for (int q = 0; q < 10; q++) {
                    FXDispatcher.INSTANCE.crucibleBoil(msg.pos, tile, msg.data);
                }
            }
        }
    }
}
