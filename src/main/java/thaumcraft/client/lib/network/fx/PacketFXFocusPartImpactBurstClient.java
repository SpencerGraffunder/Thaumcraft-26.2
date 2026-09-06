package thaumcraft.client.lib.network.fx;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.api.casters.FocusEffect;
import thaumcraft.api.casters.FocusEngine;
import thaumcraft.api.casters.IFocusElement;

import thaumcraft.common.lib.network.fx.PacketFXFocusPartImpactBurst;

/** Client-side handler for {@link PacketFXFocusPartImpactBurst}. */
public class PacketFXFocusPartImpactBurstClient {
    public static void handle(PacketFXFocusPartImpactBurst msg) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) return;
        
        String[] partKeys = msg.parts.split("%");
        // More particles spread across parts
        int amount = Math.max(1, 20 / partKeys.length);
        RandomSource rand = level.getRandom();
        
        for (String key : partKeys) {
            IFocusElement element = FocusEngine.getElement(key);
            if (element instanceof FocusEffect effect) {
                for (int i = 0; i < amount; i++) {
                    // Larger spread velocity for burst effect (0.4 vs 0.15 for normal impact)
                    double mx = rand.nextGaussian() * 0.4;
                    double my = rand.nextGaussian() * 0.4;
                    double mz = rand.nextGaussian() * 0.4;
                    
                    effect.renderParticleFX(level, msg.x, msg.y, msg.z, mx, my, mz);
                }
            }
        }
    }
}
