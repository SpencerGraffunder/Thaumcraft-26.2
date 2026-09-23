package thaumcraft.common.items.curios;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategory;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Pech Wand - A rare curio that grants research knowledge when used.
 * Can only be used if the player knows basic auromancy.
 * Also unlocks the Pech Focus research if not already known.
 */
public class ItemPechWand extends Item {

    public ItemPechWand() {
        super(thaumcraft.init.ItemRegistration.id(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.RARE)));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);

        if (knowledge == null) {
            return InteractionResult.PASS;
        }

        // Check if player knows basic auromancy
        if (!knowledge.isResearchKnown("BASEAUROMANCY")) {
            if (!level.isClientSide()) {
                player.sendSystemMessage(Component.translatable("not.pechwand")
                        .withStyle(ChatFormatting.RED));
            }
            return InteractionResult.FAIL;
        }

        // Consume the item
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        // Play sound
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS,
                0.5f, 0.4f / (level.getRandom().nextFloat() * 0.4f + 0.8f));

        if (!level.isClientSide()) {
            // Unlock Pech Focus research if not known (1.12 ItemPechWand)
            if (!knowledge.isResearchKnown("FOCUSPECH")) {
                ThaumcraftApi.internalMethods.progressResearch(player, "FOCUSPECH");
                player.sendSystemMessage(Component.translatable("got.pechwand")
                        .withStyle(ChatFormatting.DARK_PURPLE));
            }

            // Grant random observation and theory knowledge (1.12 ItemPechWand)
            ResearchCategory[] rc = ResearchCategories.researchCategories.values().toArray(new ResearchCategory[0]);
            if (rc.length > 0) {
                int oProg = IPlayerKnowledge.EnumKnowledgeType.OBSERVATION.getProgression();
                ThaumcraftApi.internalMethods.addKnowledge(player, IPlayerKnowledge.EnumKnowledgeType.OBSERVATION,
                        rc[player.getRandom().nextInt(rc.length)],
                        Mth.randomBetweenInclusive(player.getRandom(), oProg / 3, oProg / 2));
                int tProg = IPlayerKnowledge.EnumKnowledgeType.THEORY.getProgression();
                ThaumcraftApi.internalMethods.addKnowledge(player, IPlayerKnowledge.EnumKnowledgeType.THEORY,
                        rc[player.getRandom().nextInt(rc.length)],
                        Mth.randomBetweenInclusive(player.getRandom(), tProg / 5, tProg / 4));
            }

            player.sendSystemMessage(Component.translatable("item.thaumcraft.pech_wand.used")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("item.curio.text").withStyle(style -> style.withColor(0x808080)));
        super.appendHoverText(stack, context, display, builder, flag);
    }
}
