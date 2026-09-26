package thaumcraft.client.lib.network.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.init.ModSounds;
import thaumcraft.common.config.ModConfig;
import thaumcraft.common.lib.network.misc.PacketMiscEvent;

/**
 * Client-side handler for {@link PacketMiscEvent}.
 *
 * WARP_EVENT  - plays the warp heartbeat sound (suppressed in wuss mode).
 * MIST_EVENT  - thick mist around the player for ~2 minutes (fog in 1.12).
 * MIST_EVENT_SHORT - brief mist around the player.
 */
@OnlyIn(Dist.CLIENT)
public class PacketMiscEventClient {

    /** Remaining ticks of the active mist effect. */
    private static int mistTicks = 0;

    /** Duration of the next tick batch, in ticks. */
    private static final int MIST_LONG = 2400;   // 1.12: fogDuration = 2400
    private static final int MIST_SHORT = 200;   // 1.12: min fogDuration = 200

    public static void handle(PacketMiscEvent msg) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) return;

        switch (msg.type) {
            case PacketMiscEvent.WARP_EVENT -> {
                // Play heartbeat sound for warp effects
                if (!ModConfig.isWussMode() && ModSounds.HEARTBEAT.get() != null) {
                    mc.level.playLocalSound(
                            player.getX(), player.getY(), player.getZ(),
                            ModSounds.HEARTBEAT.get(), SoundSource.AMBIENT,
                            1.0f, 1.0f, false
                    );
                }
            }
            case PacketMiscEvent.MIST_EVENT -> mistTicks = MIST_LONG;
            case PacketMiscEvent.MIST_EVENT_SHORT -> mistTicks = MIST_SHORT;
            default -> { }
        }
    }

    /**
     * Called every client tick (from ClientTickEvents) while mist is active.
     * Spawns a dense cloud of smoke particles around the local player to
     * emulate the 1.12 thick-fog mist effect.
     */
    public static void tickMist() {
        if (mistTicks <= 0) return;
        mistTicks--;

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        Player player = mc.player;
        if (level == null || player == null) {
            mistTicks = 0;
            return;
        }

        float radius = 1.2f;
        for (int i = 0; i < 3; i++) {
            double x = player.getX() + (level.getRandom().nextDouble() - 0.5) * radius;
            double y = player.getY() + level.getRandom().nextDouble() * 1.6;
            double z = player.getZ() + (level.getRandom().nextDouble() - 0.5) * radius;
            level.addParticle(ParticleTypes.CLOUD, x, y, z, 0, 0, 0);
        }
    }
}
