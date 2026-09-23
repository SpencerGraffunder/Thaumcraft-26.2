package thaumcraft.common.config;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import thaumcraft.Thaumcraft;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.init.ModItems;

/**
 * Populates the loot-bag loot tables. Ported from 1.12 {@code ModConfig.postInitLoot()}.
 *
 * <p>1.12 item-to-1.21.1 mapping notes:
 * <ul>
 *   <li>{@code ItemsTC.salisMundus} &rarr; {@link ModItems#SALIS_MUNDUS}</li>
 *   <li>{@code ItemsTC.primordialPearl} (meta 7/6/5/3/-) &rarr; base {@link ModItems#PRIMORDIAL_PEARL}
 *       (1.21.1 uses components; the pearl's aspect content is data-driven, so the base item is used).</li>
 *   <li>{@code ItemsTC.amuletVis} (meta 0) &rarr; {@link ModItems#AMULET_VIS_FOUND} (1.21.1 splits found/crafted).</li>
 *   <li>{@code ItemsTC.baubles} meta 0-6 (amulet/ring/girdle mundane, ring apprentice,
 *       amulet/ring/girdle fancy) &rarr; the discrete 26.2 gear items.</li>
 *   <li>{@code Items.GOLDEN_APPLE} meta 1 &rarr; {@link Items#ENCHANTED_GOLDEN_APPLE}; meta 0 &rarr; {@link Items#GOLDEN_APPLE}.</li>
 *   <li>Potions: 1.12 {@code PotionUtils.addPotionToItemStack} &rarr;
 *       1.21.1 {@link PotionContents#createItemStack(Item, Holder)}; the 1.12
 *       {@code PotionType.REGISTRY} loop &rarr; iterate {@link BuiltInRegistries#POTION} holders.</li>
 * </ul>
 */
public class ConfigLoot {

    public static void postInitLoot() {
        // Gold nuggets (1.12 kept the stack size in the ItemStack).
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.GOLD_NUGGET, 1), 2500, 0);
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.GOLD_NUGGET, 2), 2250, 1);
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.GOLD_NUGGET, 3), 2000, 2);

        // Salis Mundus (dust).
        ThaumcraftApi.addLootBagItem(new ItemStack(ModItems.SALIS_MUNDUS.get()), 3, 0);
        ThaumcraftApi.addLootBagItem(new ItemStack(ModItems.SALIS_MUNDUS.get()), 6, 1);
        ThaumcraftApi.addLootBagItem(new ItemStack(ModItems.SALIS_MUNDUS.get()), 9, 2);

        // Common vanilla drops across all tiers.
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.CHORUS_FRUIT), 5, 0, 1, 2);
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.COMPASS), 5, 0, 1, 2);
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.COOKIE), 5, 0, 1, 2);

        // Primordial pearls. 1.12 used metadata to pick the pearl "level"; 1.21.1
        // drops that distinction (aspect content is data-driven), so the base item
        // is used for every tier while the 1.12 weights are preserved.
        ThaumcraftApi.addLootBagItem(new ItemStack(ModItems.PRIMORDIAL_PEARL.get()), 1, 0);
        ThaumcraftApi.addLootBagItem(new ItemStack(ModItems.PRIMORDIAL_PEARL.get()), 3, 1);
        ThaumcraftApi.addLootBagItem(new ItemStack(ModItems.PRIMORDIAL_PEARL.get()), 1, 1);
        ThaumcraftApi.addLootBagItem(new ItemStack(ModItems.PRIMORDIAL_PEARL.get()), 9, 2);
        ThaumcraftApi.addLootBagItem(new ItemStack(ModItems.PRIMORDIAL_PEARL.get()), 3, 2);
        ThaumcraftApi.addLootBagItem(new ItemStack(ModItems.PRIMORDIAL_PEARL.get()), 1, 2);

        // Rare / value items.
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.NETHER_STAR), 1, 2);
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.DIAMOND), 10, 0);
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.DIAMOND), 50, 1, 2);
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.EMERALD), 15, 0);
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.EMERALD), 75, 1, 2);
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.GOLD_INGOT), 100, 0, 1, 2);
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.ENDER_PEARL), 100, 0, 1, 2);

        // Vis amulet (1.12 amuletVis meta 0 = the "found" variant).
        ThaumcraftApi.addLootBagItem(new ItemStack(ModItems.AMULET_VIS_FOUND.get()), 6, 1, 2);

        // Vis discount gear (1.12 ItemsTC.baubles meta 0-6).
        ThaumcraftApi.addLootBagItem(new ItemStack(ModItems.AMULET_MUNDANE.get()), 10, 0);
        ThaumcraftApi.addLootBagItem(new ItemStack(ModItems.RING_MUNDANE.get()), 10, 0);
        ThaumcraftApi.addLootBagItem(new ItemStack(ModItems.GIRDLE_MUNDANE.get()), 10, 0);
        ThaumcraftApi.addLootBagItem(new ItemStack(ModItems.RING_APPRENTICE.get()), 5, 2);
        ThaumcraftApi.addLootBagItem(new ItemStack(ModItems.AMULET_FANCY.get()), 5, 1);
        ThaumcraftApi.addLootBagItem(new ItemStack(ModItems.RING_FANCY.get()), 5, 1);
        ThaumcraftApi.addLootBagItem(new ItemStack(ModItems.GIRDLE_FANCY.get()), 5, 1);

        // Experience bottles.
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.EXPERIENCE_BOTTLE), 5, 0);
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.EXPERIENCE_BOTTLE), 10, 1);
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.EXPERIENCE_BOTTLE), 20, 2);

        // Golden apples: 1.12 meta 1 = enchanted, meta 0 = normal.
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE), 1, 0);
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE), 2, 1);
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE), 3, 2);
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.GOLDEN_APPLE), 3, 0);
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.GOLDEN_APPLE), 6, 1);
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.GOLDEN_APPLE), 9, 2);

        // Books.
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.BOOK), 10, 0, 1, 2);

        // Potions. 1.12 iterated PotionType.REGISTRY; 1.21.1 iterates the potion
        // registry holders and builds stacks via PotionContents (replaces the
        // removed PotionUtils.addPotionToItemStack helper).
        BuiltInRegistries.POTION.listElements().forEach(potion -> {
            ThaumcraftApi.addLootBagItem(PotionContents.createItemStack(Items.POTION, potion), 2, 0, 1, 2);
            ThaumcraftApi.addLootBagItem(PotionContents.createItemStack(Items.SPLASH_POTION, potion), 2, 0, 1, 2);
            ThaumcraftApi.addLootBagItem(PotionContents.createItemStack(Items.LINGERING_POTION, potion), 2, 1, 2);
        });

        Thaumcraft.LOGGER.info("Registered loot-bag items");
    }
}
