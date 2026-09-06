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

import thaumcraft.common.lib.network.fx.PacketFXFocusEffect;

/** Client-side handler for {@link PacketFXFocusEffect}. */
public class PacketFXFocusEffectClient {
    public static void handle(PacketFXFocusEffect msg) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) return;
        
        String[] partKeys = msg.parts.split("%");
        int amount = Math.max(1, 10 / partKeys.length);
        
        for (String key : partKeys) {
            IFocusElement element = FocusEngine.getElement(key);
            if (element instanceof FocusEffect effect) {
                for (int i = 0; i < amount; i++) {
                    // Add some randomness to the motion
                    double mx = msg.motionX + level.getRandom().nextGaussian() / 20.0;
                    double my = msg.motionY + level.getRandom().nextGaussian() / 20.0;
                    double mz = msg.motionZ + level.getRandom().nextGaussian() / 20.0;
                    
                    effect.renderParticleFX(level, msg.x, msg.y, msg.z, mx, my, mz);
                }
            }
        }
    }
}
