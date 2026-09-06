package thaumcraft.client.lib.network.fx;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.api.casters.FocusEffect;
import thaumcraft.api.casters.FocusEngine;
import thaumcraft.api.casters.IFocusElement;
import net.minecraft.util.RandomSource;

import thaumcraft.common.lib.network.fx.PacketFXFocusPartImpact;

/** Client-side handler for {@link PacketFXFocusPartImpact}. */
public class PacketFXFocusPartImpactClient {
    public static void handle(PacketFXFocusPartImpact msg) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) return;
        
        String[] partKeys = msg.parts.split("%");
        int amount = Math.max(1, 15 / partKeys.length);
        RandomSource rand = level.getRandom();
        
        for (String key : partKeys) {
            IFocusElement element = FocusEngine.getElement(key);
            if (element instanceof FocusEffect effect) {
                for (int i = 0; i < amount; i++) {
                    // Random outward motion for impact burst
                    double mx = rand.nextGaussian() * 0.15;
                    double my = rand.nextGaussian() * 0.15;
                    double mz = rand.nextGaussian() * 0.15;
                    
                    effect.renderParticleFX(level, msg.x, msg.y, msg.z, mx, my, mz);
                }
            }
        }
    }
}
