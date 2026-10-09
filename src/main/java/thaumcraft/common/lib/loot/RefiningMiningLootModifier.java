package thaumcraft.common.lib.loot;

import java.util.Optional;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.LootModifier;
import thaumcraft.common.lib.enchantment.EnumInfusionEnchantment;
import thaumcraft.common.lib.utils.Utils;

/**
 * 1.12 REFINING infusion enchantment: mining an ore with a REFINING tool has a
 * chance ((1 + level) * 0.125) to drop the metal cluster instead of the normal
 * ore drops. 1.12 implemented this in ToolEvents.onBlockHarvest; 26.3 has no
 * drop-list hook on BreakBlockEvent, so it is done as a global loot modifier
 * (applied to block drops only, via the BLOCK_STATE context parameter).
 */
public class RefiningMiningLootModifier extends LootModifier {

    public static final MapCodec<RefiningMiningLootModifier> CODEC = RecordCodecBuilder.mapCodec(
            (RecordCodecBuilder.Instance<RefiningMiningLootModifier> instance) ->
                    codecStart(instance)
                            .apply(instance, RefiningMiningLootModifier::new)
    );

    protected RefiningMiningLootModifier(Optional<Holder<LootItemCondition>> condition, int priority) {
        super(condition, priority);
    }

    @Override
    public MapCodec<? extends net.neoforged.neoforge.common.loot.IGlobalLootModifier> codec() {
        return CODEC;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> drops, LootContext context) {
        // Only block drops participate (1.12 only ran this on block harvest).
        if (context.getOptional(LootContextParams.BLOCK_STATE) == null) {
            return drops;
        }

        // The tool that broke the block (1.12: the player's held item).
        Entity interacting = context.getOptional(LootContextParams.INTERACTING_ENTITY);
        ItemStack tool = ItemStack.EMPTY;
        ItemInstance toolParam = context.getOptional(LootContextParams.TOOL);
        if (toolParam instanceof ItemStack stack) {
            tool = stack;
        }
        if (tool.isEmpty() && interacting instanceof Player player) {
            tool = player.getMainHandItem();
            if (tool.isEmpty()) {
                tool = player.getOffhandItem();
            }
        }
        int refining = tool.isEmpty() ? 0
                : EnumInfusionEnchantment.getInfusionEnchantmentLevel(tool, EnumInfusionEnchantment.REFINING);
        if (refining <= 0) {
            return drops;
        }

        float chance = (1 + refining) * 0.125f;
        boolean converted = false;
        for (int i = 0; i < drops.size(); i++) {
            ItemStack original = drops.get(i);
            ItemStack result = Utils.findSpecialMiningResult(original, chance, context.getRandom());
            if (result.getItem() != original.getItem()) {
                drops.set(i, result);
                converted = true;
            }
        }

        // 1.12 played the item-pickup sound when a conversion happened.
        if (converted && context.getLevel() != null && interacting != null) {
            var pos = interacting.blockPosition();
            context.getLevel().playSound(null, pos, SoundEvents.ITEM_PICKUP,
                    SoundSource.PLAYERS, 0.2F, 1.0F);
        }
        return drops;
    }
}
