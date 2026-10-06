package thaumcraft.client.lib.network.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.common.entities.monster.EntityWisp;

import java.awt.Color;

import thaumcraft.common.lib.network.fx.PacketFXWispZap;

/** Client-side handler for {@link PacketFXWispZap}. */
public class PacketFXWispZapClient {
    @OnlyIn(Dist.CLIENT)
    private static Entity getEntityById(int entityId, Minecraft mc) {
        if (mc.player != null && entityId == mc.player.getId()) {
            return mc.player;
        }
        return mc.level != null ? mc.level.getEntity(entityId) : null;
    }

    public static void handle(PacketFXWispZap msg) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) {
            return;
        }

        Entity source = getEntityById(msg.sourceEntityId, mc);
        Entity target = getEntityById(msg.targetEntityId, mc);
        if (source == null || target == null) {
            return;
        }

        // 1.12: wisps zap with their own aspect colour, anything else arcs in white.
        float r = 1.0f;
        float g = 1.0f;
        float b = 1.0f;
        if (source instanceof EntityWisp wisp) {
            Aspect aspect = wisp.getAspect();
            if (aspect != null) {
                Color c = new Color(aspect.getColor());
                r = c.getRed() / 255.0f;
                g = c.getGreen() / 255.0f;
                b = c.getBlue() / 255.0f;
            }
        }

        FXDispatcher.INSTANCE.arcBolt(
                source.getX(), source.getY(), source.getZ(),
                target.getX(), target.getY(), target.getZ(),
                r, g, b, 0.6f);
    }
}
