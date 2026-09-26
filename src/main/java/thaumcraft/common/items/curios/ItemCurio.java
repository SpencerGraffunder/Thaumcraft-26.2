package thaumcraft.common.items.curios;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.capabilities.IPlayerWarp;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategory;

import javax.annotation.Nullable;
import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Curio - A knowledge-gaining item with 7 variants (damage 0-6).
 *
 * Each variant grants observation and theory knowledge in a specific
 * research category when right-clicked:
 * - 0 (arcane):    AUROMANCY
 * - 1 (preserved): ALCHEMY
 * - 2 (ancient):   GOLEMANCY
 * - 3 (eldritch):  ELDRITCH (+ warp)
 * - 4 (knowledge): INFUSION
 * - 5 (twisted):   ARTIFICE
 * - 6 (rites):     ELDRITCH (Crimson Rites, requires 20+ warp)
 *
 * All variants also grant random knowledge in 2 additional categories.
 */
public class ItemCurio extends Item {

    public static final int MAX_DAMAGE = 7;

    public ItemCurio() {
        super(thaumcraft.init.ItemRegistration.id(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.UNCOMMON)
                .durability(MAX_DAMAGE)));
    }

    /**
     * Get the variant name based on damage level.
     * Used by CardCurio and for display.
     */
    public static String getVariantName(int damage) {
        return switch (damage) {
            case 0 -> "arcane";
            case 1 -> "preserved";
            case 2 -> "ancient";
            case 3 -> "eldritch";
            case 4 -> "knowledge";
            case 5 -> "twisted";
            case 6 -> "rites";
            default -> "arcane";
        };
    }

    /**
     * Get the category key based on damage level.
     */
    private static String getCategoryKey(int damage) {
        return switch (damage) {
            case 0 -> "AUROMANCY";
            case 1 -> "ALCHEMY";
            case 2 -> "GOLEMANCY";
            case 3 -> "ELDRITCH";
            case 4 -> "INFUSION";
            case 5 -> "ARTIFICE";
            case 6 -> "ELDRITCH"; // Crimson Rites
            default -> "AUROMANCY";
        };
    }

    @Override
    public Component getName(ItemStack stack) {
        String suffix = switch (stack.getDamageValue()) {
            case 0 -> "arcane";
            case 1 -> "preserved";
            case 2 -> "ancient";
            case 3 -> "eldritch";
            case 4 -> "knowledge";
            case 5 -> "twisted";
            case 6 -> "rites";
            default -> "arcane";
        };
        return Component.translatable("item.thaumcraft.curio." + suffix);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("item.curio.text").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, display, builder, flag);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        int damage = stack.getDamageValue();

        // Play sound
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ALLAY_ITEM_GIVEN, SoundSource.NEUTRAL,
                0.5f, 0.4f / (level.getRandom().nextFloat() * 0.4f + 0.8f));

        if (!level.isClientSide()) {
            IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);
            if (knowledge == null) {
                return InteractionResult.PASS;
            }

            int oProg = IPlayerKnowledge.EnumKnowledgeType.OBSERVATION.getProgression();
            int tProg = IPlayerKnowledge.EnumKnowledgeType.THEORY.getProgression();

            String catKey = getCategoryKey(damage);
            ResearchCategory category = ResearchCategories.getResearchCategory(catKey);
            if (category == null) {
                return InteractionResult.FAIL;
            }

            // Main category knowledge
            ThaumcraftApi.internalMethods.addKnowledge(player, IPlayerKnowledge.EnumKnowledgeType.OBSERVATION,
                    category, Mth.randomBetweenInclusive(player.getRandom(), oProg / 2, oProg));
            ThaumcraftApi.internalMethods.addKnowledge(player, IPlayerKnowledge.EnumKnowledgeType.THEORY,
                    category, Mth.randomBetweenInclusive(player.getRandom(), tProg / 3, tProg / 2));

            // Special: Eldritch variant (damage 3) gives warp
            if (damage == 3) {
                ThaumcraftApi.internalMethods.addWarpToPlayer(player, 1, IPlayerWarp.EnumWarpType.NORMAL);
                ThaumcraftApi.internalMethods.addWarpToPlayer(player, 5, IPlayerWarp.EnumWarpType.TEMPORARY);
            }

            // Special: Crimson Rites (damage 6) requires 20+ warp
            if (damage == 6) {
                int warp = ThaumcraftApi.internalMethods.getActualWarp(player);
                if (warp > 20) {
                    if (!knowledge.isResearchKnown("CrimsonRites")) {
                        ThaumcraftApi.internalMethods.completeResearch(player, "CrimsonRites");
                    }
                    ThaumcraftApi.internalMethods.addWarpToPlayer(player, 1, IPlayerWarp.EnumWarpType.NORMAL);
                    ThaumcraftApi.internalMethods.addWarpToPlayer(player, 5, IPlayerWarp.EnumWarpType.TEMPORARY);
                    if (player.getRandom().nextBoolean()) {
                        ThaumcraftApi.internalMethods.addWarpToPlayer(player, 1, IPlayerWarp.EnumWarpType.PERMANENT);
                    }
                } else {
                    player.sendSystemMessage(Component.translatable("fail.crimsonrites").withStyle(ChatFormatting.DARK_PURPLE));
                    return InteractionResult.PASS;
                }
            }

            // Random additional categories
            ResearchCategory[] rc = ResearchCategories.researchCategories.values().toArray(new ResearchCategory[0]);
            if (rc.length > 0) {
                ThaumcraftApi.internalMethods.addKnowledge(player, IPlayerKnowledge.EnumKnowledgeType.OBSERVATION,
                        rc[player.getRandom().nextInt(rc.length)],
                        Mth.randomBetweenInclusive(player.getRandom(), oProg / 2, oProg));
                ThaumcraftApi.internalMethods.addKnowledge(player, IPlayerKnowledge.EnumKnowledgeType.THEORY,
                        rc[player.getRandom().nextInt(rc.length)],
                        Mth.randomBetweenInclusive(player.getRandom(), tProg / 3, tProg / 2));
            }

            // Consume the item
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }

            player.sendSystemMessage(Component.translatable("tc.knowledge.gained").withStyle(ChatFormatting.DARK_PURPLE));
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.getDamageValue() >= 3;
    }
}
