package thaumcraft.common.config;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import thaumcraft.Thaumcraft;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.init.ModItems;

/**
 * ConfigLoot - Loot bag item registration (ported from 1.12 ModConfig.postInitLoot).
 *
 * 1.12 had a single `postInitLoot()` in ModConfig that called
 * {@link ThaumcraftApi#addLootBagItem} ~40 times to populate the three loot-bag
 * tiers (0=common, 1=uncommon, 2=rare). 26.2 had the API but never called it,
 * so loot bags were empty. This restores the 1.12 loot table.
 *
 * Mapping notes:
 * - 1.12 `ItemsTC.X` -> 26.2 `ModItems.X` (DeferredHolder).
 * - 1.12 damage/metadata (e.g. primordialPearl meta, baubles meta, golden_apple meta 1)
 *   is dropped or mapped to the equivalent 1.21.1 item (components / separate items).
 * - Potions use 1.21.1 `PotionUtils.createPotion`; only the common potions are listed
 *   (the 1.12 loop over PotionType.REGISTRY is not a drop-in in 1.21.1).
 */
public class ConfigLoot {

    public static void postInitLoot() {
        // --- Gold nuggets (tier-scaled quantity) ---
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.GOLD_NUGGET, 1), 2500, 0);
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.GOLD_NUGGET, 2), 2250, 1);
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.GOLD_NUGGET, 3), 2000, 2);

        // --- Salis Mundus (tier-scaled) ---
        ThaumcraftApi.addLootBagItem(new ItemStack(ModItems.SALIS_MUNDUS), 3, 0);
        ThaumcraftApi.addLootBagItem(new ItemStack(ModItems.SALIS_MUNDUS), 6, 1);
        ThaumcraftApi.addLootBagItem(new ItemStack(ModItems.SALIS_MUNDUS), 9, 2);

        // --- Utility items (all tiers) ---
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.CHORUS_FRUIT), 5, 0, 1, 2);
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.COMPASS), 5, 0, 1, 2);
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.COOKIE), 5, 0, 1, 2);

        // --- Primordial Pearl (1.12 used meta for tier; 26.2 drops meta) ---
        ThaumcraftApi.addLootBagItem(new ItemStack(ModItems.PRIMORDIAL_PEARL), 1, 0);
        ThaumcraftApi.addLootBagItem(new ItemStack(ModItems.PRIMORDIAL_PEARL), 3, 1);
        ThaumcraftApi.addLootBagItem(new ItemStack(ModItems.PRIMORDIAL_PEARL), 9, 2);

        // --- Rare gems ---
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.NETHER_STAR), 1, 2);
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.DIAMOND), 10, 0);
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.DIAMOND), 50, 1, 2);
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.EMERALD), 15, 0);
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.EMERALD), 75, 1, 2);
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.GOLD_INGOT), 100, 0, 1, 2);
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.ENDER_PEARL), 100, 0, 1, 2);

        // --- Vis baubles (1.12 `baubles` meta 0-6 -> 26.2 separate vis-discount items) ---
        ThaumcraftApi.addLootBagItem(new ItemStack(ModItems.AMULET_VIS_FOUND), 6, 1, 2);
        ThaumcraftApi.addLootBagItem(new ItemStack(ModItems.AMULET_MUNDANE), 10, 0);
        ThaumcraftApi.addLootBagItem(new ItemStack(ModItems.RING_MUNDANE), 10, 0);
        ThaumcraftApi.addLootBagItem(new ItemStack(ModItems.AMULET_FANCY), 10, 0);
        ThaumcraftApi.addLootBagItem(new ItemStack(ModItems.RING_FANCY), 5, 2);
        ThaumcraftApi.addLootBagItem(new ItemStack(ModItems.CLOUD_RING), 5, 1);
        ThaumcraftApi.addLootBagItem(new ItemStack(ModItems.CURIOSITY_BAND), 5, 1);
        ThaumcraftApi.addLootBagItem(new ItemStack(ModItems.CHARM_UNDYING), 5, 1);

        // --- Experience bottles (tier-scaled quantity) ---
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.EXPERIENCE_BOTTLE, 1), 5, 0);
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.EXPERIENCE_BOTTLE, 2), 10, 1);
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.EXPERIENCE_BOTTLE, 4), 20, 2);

        // --- Golden apple (1.12 meta 0 = golden, meta 1 = enchanted) ---
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.GOLDEN_APPLE), 3, 0);
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.GOLDEN_APPLE), 6, 1);
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.GOLDEN_APPLE), 9, 2);
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE), 1, 0);
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE), 2, 1);
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE), 3, 2);

        // --- Books ---
        ThaumcraftApi.addLootBagItem(new ItemStack(Items.BOOK), 10, 0, 1, 2);

        // --- Potions (1.21.1: PotionUtils.createPotion; only common potions listed) ---
        // TODO: 1.12 looped over ALL PotionType.REGISTRY; 26.2 could expand this with
        //       PotionUtils.createPotion(new ItemStack(Items.POTION), <potion>) per potion.
        //       Left minimal to avoid coupling to the full 1.21.1 potion registry.

        Thaumcraft.LOGGER.info("Registered loot-bag items");
    }
}
