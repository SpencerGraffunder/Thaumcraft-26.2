package thaumcraft.api.capabilities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.common.lib.capabilities.PlayerKnowledge;
import thaumcraft.common.lib.capabilities.PlayerWarp;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * ThaumcraftCapabilities - Access point for Thaumcraft's player data attachments.
 * 
 * @author Azanor
 * Ported to 1.20.1 / NeoForge 26.2
 */
@EventBusSubscriber(modid = Thaumcraft.MODID)
public class ThaumcraftCapabilities {

    // ==================== Player Knowledge ====================

    /**
     * The data attachment type for IPlayerKnowledge
     */
    public static AttachmentType<IPlayerKnowledge> KNOWLEDGE;

    /**
     * The data attachment type for IPlayerWarp
     */
    public static AttachmentType<IPlayerWarp> WARP;

    /**
     * Registers the player data attachment types.
     */
    @SubscribeEvent
    public static void registerAttachmentTypes(RegisterEvent event) {
        if (event.getRegistryKey().equals(NeoForgeRegistries.Keys.ATTACHMENT_TYPES)) {
            KNOWLEDGE = AttachmentType.<IPlayerKnowledge>builder(PlayerKnowledge.DefaultImpl::new)
                    .serialize(new IAttachmentSerializer<IPlayerKnowledge>() {
                        @Override
                        public IPlayerKnowledge read(IAttachmentHolder holder, ValueInput input) {
                            IPlayerKnowledge knowledge = new PlayerKnowledge.DefaultImpl();
                            input.read("data", CompoundTag.CODEC).ifPresent(knowledge::deserializeNBT);
                            return knowledge;
                        }

                        @Override
                        public boolean write(IPlayerKnowledge knowledge, ValueOutput output) {
                            output.store("data", CompoundTag.CODEC, knowledge.serializeNBT());
                            return true;
                        }
                    })
                    .copyOnDeath()
                    .build();
            WARP = AttachmentType.<IPlayerWarp>builder(PlayerWarp.DefaultImpl::new)
                    .serialize(new IAttachmentSerializer<IPlayerWarp>() {
                        @Override
                        public IPlayerWarp read(IAttachmentHolder holder, ValueInput input) {
                            IPlayerWarp warp = new PlayerWarp.DefaultImpl();
                            input.read("data", CompoundTag.CODEC).ifPresent(warp::deserializeNBT);
                            return warp;
                        }

                        @Override
                        public boolean write(IPlayerWarp warp, ValueOutput output) {
                            output.store("data", CompoundTag.CODEC, warp.serializeNBT());
                            return true;
                        }
                    })
                    .copyOnDeath()
                    .build();

            event.register(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, helper -> {
                helper.register(Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "knowledge"), KNOWLEDGE);
                helper.register(Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "warp"), WARP);
            });
        }
    }

    /**
     * Retrieves the knowledge attachment for the supplied player.
     * @param player The player to get knowledge for
     * @return The knowledge data, or null if not registered yet
     */
    @Nullable
    public static IPlayerKnowledge getKnowledge(@Nonnull Player player) {
        return KNOWLEDGE != null ? player.getData(KNOWLEDGE) : null;
    }

    /**
     * Shortcut method to check if player knows the passed research entries.
     * All must be true. Research does not need to be complete, just 'in progress'.
     * 
     * Individual entries can contain && for 'and' check, e.g. "basicgolemancy&&infusion"
     * Individual entries can contain || for 'or' check, e.g. "basicgolemancy||infusion"
     * Queries should NOT contain both && and || - shennanigans will occur.
     * 
     * @param player The player to check
     * @param research The research keys to check
     * @return true if all research is known
     */
    public static boolean knowsResearch(@Nonnull Player player, @Nonnull String... research) {
        IPlayerKnowledge knowledge = getKnowledge(player);
        if (knowledge == null) return false;
        
        for (String r : research) {
            if (r.contains("&&")) {
                String[] rr = r.split("&&");
                if (!knowsResearch(player, rr)) return false;
            } else if (r.contains("||")) {
                String[] rr = r.split("\\|\\|");
                boolean anyTrue = false;
                for (String str : rr) {
                    if (knowsResearch(player, str)) {
                        anyTrue = true;
                        break;
                    }
                }
                if (!anyTrue) return false;
            } else {
                if (!knowledge.isResearchKnown(r)) return false;
            }
        }
        return true;
    }

    /**
     * Shortcut method to check if player knows all the passed research entries.
     * Research needs to be complete and 'in progress' research will only count 
     * if a stage is passed in the research parameter (using @, eg. "FOCUSFIRE@2")
     * 
     * @param player The player to check
     * @param research The research keys to check
     * @return true if all research is complete
     */
    public static boolean knowsResearchStrict(@Nonnull Player player, @Nonnull String... research) {
        IPlayerKnowledge knowledge = getKnowledge(player);
        if (knowledge == null) return false;
        
        for (String r : research) {
            if (r.contains("&&")) {
                String[] rr = r.split("&&");
                if (!knowsResearchStrict(player, rr)) return false;
            } else if (r.contains("||")) {
                String[] rr = r.split("\\|\\|");
                boolean anyTrue = false;
                for (String str : rr) {
                    if (knowsResearchStrict(player, str)) {
                        anyTrue = true;
                        break;
                    }
                }
                if (!anyTrue) return false;
            } else if (r.contains("@")) {
                if (!knowledge.isResearchKnown(r)) return false;
            } else {
                if (!knowledge.isResearchComplete(r)) return false;
            }
        }
        return true;
    }

    /**
     * Simple check if player knows a specific research.
     * @param player The player to check
     * @param researchKey The research key to check
     * @return true if the research is known
     */
    public static boolean isResearchKnown(@Nonnull Player player, @Nonnull String researchKey) {
        IPlayerKnowledge knowledge = getKnowledge(player);
        return knowledge != null && knowledge.isResearchKnown(researchKey);
    }

    // ==================== Player Warp ====================

    /**
     * Retrieves the warp attachment for the supplied player.
     * @param player The player to get warp for
     * @return The warp data, or null if not registered yet
     */
    @Nullable
    public static IPlayerWarp getWarp(@Nonnull Player player) {
        return WARP != null ? player.getData(WARP) : null;
    }
}
