package thaumcraft.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import thaumcraft.Thaumcraft;
import thaumcraft.api.golems.IGolemProperties;
import thaumcraft.api.golems.parts.GolemAddon;
import thaumcraft.api.golems.parts.GolemArm;
import thaumcraft.api.golems.parts.GolemHead;
import thaumcraft.api.golems.parts.GolemLeg;
import thaumcraft.api.golems.parts.GolemMaterial;
import thaumcraft.common.golems.GolemProperties;
import thaumcraft.common.golems.ItemGolemPlacer;

/**
 * Creative mode tabs for Thaumcraft.
 * Replaces the old CreativeTabs system from 1.12.2.
 */
public class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Thaumcraft.MODID);

    // Main Thaumcraft creative tab
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> THAUMCRAFT_TAB = CREATIVE_MODE_TABS.register("thaumcraft_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.thaumcraft"))
                    .icon(() -> new ItemStack(ModBlocks.ARCANE_STONE.get()))
                    .displayItems((parameters, output) -> {
                        // === Stone Blocks ===
                        output.accept(ModBlocks.ARCANE_STONE.get());
                        output.accept(ModBlocks.ARCANE_STONE_BRICK.get());
                        output.accept(ModBlocks.ANCIENT_STONE.get());
                        output.accept(ModBlocks.ANCIENT_STONE_TILE.get());
                        output.accept(ModBlocks.ANCIENT_STONE_GLYPHED.get());
                        output.accept(ModBlocks.ELDRITCH_STONE_TILE.get());
                        output.accept(ModBlocks.POROUS_STONE.get());

                        // === Stairs ===
                        output.accept(ModBlocks.ARCANE_STONE_STAIRS.get());
                        output.accept(ModBlocks.ARCANE_STONE_BRICK_STAIRS.get());
                        output.accept(ModBlocks.ANCIENT_STONE_STAIRS.get());

                        // === Slabs ===
                        output.accept(ModBlocks.ARCANE_STONE_SLAB.get());
                        output.accept(ModBlocks.ARCANE_STONE_BRICK_SLAB.get());
                        output.accept(ModBlocks.ANCIENT_STONE_SLAB.get());

                        // === Pillars ===
                        output.accept(ModBlocks.ARCANE_PILLAR.get());
                        output.accept(ModBlocks.ANCIENT_PILLAR.get());
                        output.accept(ModBlocks.ELDRITCH_PILLAR.get());

                        // === Wood Blocks ===
                        output.accept(ModBlocks.GREATWOOD_LOG.get());
                        output.accept(ModBlocks.SILVERWOOD_LOG.get());
                        output.accept(ModBlocks.GREATWOOD_PLANKS.get());
                        output.accept(ModBlocks.SILVERWOOD_PLANKS.get());
                        output.accept(ModBlocks.GREATWOOD_STAIRS.get());
                        output.accept(ModBlocks.SILVERWOOD_STAIRS.get());
                        output.accept(ModBlocks.GREATWOOD_SLAB.get());
                        output.accept(ModBlocks.SILVERWOOD_SLAB.get());

                        // === Metal Blocks ===
                        output.accept(ModBlocks.BRASS_BLOCK.get());
                        output.accept(ModBlocks.THAUMIUM_BLOCK.get());
                        output.accept(ModBlocks.VOID_METAL_BLOCK.get());
                        output.accept(ModBlocks.ALCHEMICAL_BRASS_BLOCK.get());
                        output.accept(ModBlocks.ALCHEMICAL_BRASS_ADVANCED_BLOCK.get());

                        // === Amber Blocks ===
                        output.accept(ModBlocks.AMBER_BLOCK.get());
                        output.accept(ModBlocks.AMBER_BRICK.get());

                        // === Matrix Blocks ===
                        output.accept(ModBlocks.MATRIX_SPEED.get());
                        output.accept(ModBlocks.MATRIX_COST.get());

                        // === Crafting Blocks ===
                        output.accept(ModBlocks.ARCANE_WORKBENCH.get());
                        output.accept(ModBlocks.CRUCIBLE.get());

                        // === Ore Blocks ===
                        output.accept(ModBlocks.AMBER_ORE.get());
                        output.accept(ModBlocks.CINNABAR_ORE.get());
                        output.accept(ModBlocks.QUARTZ_ORE.get());

                        // === Crystal Blocks ===
                        output.accept(ModBlocks.CRYSTAL_AIR.get());
                        output.accept(ModBlocks.CRYSTAL_FIRE.get());
                        output.accept(ModBlocks.CRYSTAL_WATER.get());
                        output.accept(ModBlocks.CRYSTAL_EARTH.get());
                        output.accept(ModBlocks.CRYSTAL_ORDER.get());
                        output.accept(ModBlocks.CRYSTAL_ENTROPY.get());
                        output.accept(ModBlocks.CRYSTAL_FLUX.get());

                        // === Plant Blocks ===
                        output.accept(ModBlocks.SHIMMERLEAF.get());
                        output.accept(ModBlocks.CINDERPEARL.get());
                        output.accept(ModBlocks.VISHROOM.get());

                        // === Leaves and Saplings ===
                        output.accept(ModBlocks.GREATWOOD_LEAVES.get());
                        output.accept(ModBlocks.SILVERWOOD_LEAVES.get());
                        output.accept(ModBlocks.GREATWOOD_SAPLING.get());
                        output.accept(ModBlocks.SILVERWOOD_SAPLING.get());

                        // === Device Blocks ===
                        output.accept(ModBlocks.PEDESTAL_ARCANE.get());
                        output.accept(ModBlocks.PEDESTAL_ANCIENT.get());
                        output.accept(ModBlocks.PEDESTAL_ELDRITCH.get());
                        output.accept(ModBlocks.TABLE_WOOD.get());
                        output.accept(ModBlocks.TABLE_STONE.get());

                        // === Candles (16 colors) ===
                        output.accept(ModBlocks.CANDLE_WHITE.get());
                        output.accept(ModBlocks.CANDLE_ORANGE.get());
                        output.accept(ModBlocks.CANDLE_MAGENTA.get());
                        output.accept(ModBlocks.CANDLE_LIGHT_BLUE.get());
                        output.accept(ModBlocks.CANDLE_YELLOW.get());
                        output.accept(ModBlocks.CANDLE_LIME.get());
                        output.accept(ModBlocks.CANDLE_PINK.get());
                        output.accept(ModBlocks.CANDLE_GRAY.get());
                        output.accept(ModBlocks.CANDLE_LIGHT_GRAY.get());
                        output.accept(ModBlocks.CANDLE_CYAN.get());
                        output.accept(ModBlocks.CANDLE_PURPLE.get());
                        output.accept(ModBlocks.CANDLE_BLUE.get());
                        output.accept(ModBlocks.CANDLE_BROWN.get());
                        output.accept(ModBlocks.CANDLE_GREEN.get());
                        output.accept(ModBlocks.CANDLE_RED.get());
                        output.accept(ModBlocks.CANDLE_BLACK.get());

                        // === Nitor (16 colors) ===
                        output.accept(ModBlocks.NITOR_WHITE.get());
                        output.accept(ModBlocks.NITOR_ORANGE.get());
                        output.accept(ModBlocks.NITOR_MAGENTA.get());
                        output.accept(ModBlocks.NITOR_LIGHT_BLUE.get());
                        output.accept(ModBlocks.NITOR_YELLOW.get());
                        output.accept(ModBlocks.NITOR_LIME.get());
                        output.accept(ModBlocks.NITOR_PINK.get());
                        output.accept(ModBlocks.NITOR_GRAY.get());
                        output.accept(ModBlocks.NITOR_LIGHT_GRAY.get());
                        output.accept(ModBlocks.NITOR_CYAN.get());
                        output.accept(ModBlocks.NITOR_PURPLE.get());
                        output.accept(ModBlocks.NITOR_BLUE.get());
                        output.accept(ModBlocks.NITOR_BROWN.get());
                        output.accept(ModBlocks.NITOR_GREEN.get());
                        output.accept(ModBlocks.NITOR_RED.get());
                        output.accept(ModBlocks.NITOR_BLACK.get());

                        // === Essentia Jars ===
                        output.accept(ModBlocks.JAR_NORMAL.get());
                        output.accept(ModBlocks.JAR_VOID.get());
                        output.accept(ModBlocks.JAR_BRAIN.get());

                        // === Essentia Tubes ===
                        output.accept(ModBlocks.TUBE_NORMAL.get());
                        output.accept(ModBlocks.TUBE_RESTRICTED.get());
                        output.accept(ModBlocks.TUBE_FILTER.get());
                        output.accept(ModBlocks.TUBE_VALVE.get());
                        output.accept(ModBlocks.TUBE_BUFFER.get());

                        // === Advanced Crafting ===
                        output.accept(ModBlocks.RESEARCH_TABLE.get());
                        output.accept(ModBlocks.INFUSION_MATRIX.get());

                        // === More Devices ===
                        output.accept(ModBlocks.BELLOWS.get());
                        output.accept(ModBlocks.LAMP_ARCANE.get());
                        output.accept(ModBlocks.LAMP_GROWTH.get());
                        output.accept(ModBlocks.LAMP_FERTILITY.get());
                        output.accept(ModBlocks.HUNGRY_CHEST.get());
                        output.accept(ModBlocks.MIRROR_ITEM.get());
                        output.accept(ModBlocks.MIRROR_ESSENTIA.get());
                        output.accept(ModBlocks.STABILIZER.get());

                        // === Essentia Processing ===
                        output.accept(ModBlocks.ALEMBIC.get());
                        output.accept(ModBlocks.SMELTER.get());

                        // ============================================
                        // ITEMS
                        // ============================================

                        // === Tools ===
                        output.accept(ModItems.THAUMONOMICON.get());
                        output.accept(ModItems.THAUMOMETER.get());
                        output.accept(ModItems.SCRIBING_TOOLS.get());

                        // === Phials ===
                        output.accept(ModItems.PHIAL_EMPTY.get());
                        output.accept(ModItems.PHIAL_FILLED.get());

                        // === Basic Materials ===
                        output.accept(ModItems.QUICKSILVER.get());
                        output.accept(ModItems.ALUMENTUM.get());
                        output.accept(ModItems.NITOR.get());
                        output.accept(ModItems.SALIS_MUNDUS.get());
                        output.accept(ModItems.BALANCED_SHARD.get());

                        // === Metal Ingots & Nuggets ===
                        output.accept(ModItems.THAUMIUM_INGOT.get());
                        output.accept(ModItems.THAUMIUM_NUGGET.get());
                        output.accept(ModItems.VOID_METAL_INGOT.get());
                        output.accept(ModItems.VOID_METAL_NUGGET.get());
                        output.accept(ModItems.BRASS_INGOT.get());
                        output.accept(ModItems.BRASS_NUGGET.get());
                        output.accept(ModItems.QUICKSILVER_NUGGET.get());
                        output.accept(ModItems.QUARTZ_NUGGET.get());

                        // === Primal Shards ===
                        output.accept(ModItems.SHARD_AIR.get());
                        output.accept(ModItems.SHARD_FIRE.get());
                        output.accept(ModItems.SHARD_WATER.get());
                        output.accept(ModItems.SHARD_EARTH.get());
                        output.accept(ModItems.SHARD_ORDER.get());
                        output.accept(ModItems.SHARD_ENTROPY.get());

                        // === Vis Crystals (6 primal types) ===
                        output.accept(ModItems.VIS_CRYSTAL_AIR.get());
                        output.accept(ModItems.VIS_CRYSTAL_FIRE.get());
                        output.accept(ModItems.VIS_CRYSTAL_WATER.get());
                        output.accept(ModItems.VIS_CRYSTAL_EARTH.get());
                        output.accept(ModItems.VIS_CRYSTAL_ORDER.get());
                        output.accept(ModItems.VIS_CRYSTAL_ENTROPY.get());

                        // === Crafting Components ===
                        output.accept(ModItems.AMBER.get());
                        output.accept(ModItems.AMBER_BEAD.get());
                        output.accept(ModItems.ENCHANTED_FABRIC.get());
                        output.accept(ModItems.PRIMAL_CHARM.get());
                        output.accept(ModItems.SALISITE.get());
                        output.accept(ModItems.BLANK_SEAL.get());
                        output.accept(ModItems.MIRRORED_GLASS.get());
                        output.accept(ModItems.FILTER.get());
                        output.accept(ModItems.MORPHIC_RESONATOR.get());
                        output.accept(ModItems.MIND.get());
                        output.accept(ModItems.MECHANISM_SIMPLE.get());
                        output.accept(ModItems.MECHANISM_COMPLEX.get());

                        // === Plates ===
                        output.accept(ModItems.PLATE_IRON.get());
                        output.accept(ModItems.PLATE_BRASS.get());
                        output.accept(ModItems.PLATE_THAUMIUM.get());
                        output.accept(ModItems.PLATE_VOID.get());

                        // === Clusters ===
                        output.accept(ModItems.CLUSTER_IRON.get());
                        output.accept(ModItems.CLUSTER_GOLD.get());
                        output.accept(ModItems.CLUSTER_COPPER.get());
                        output.accept(ModItems.CLUSTER_CINNABAR.get());

                        // === Golem Materials ===
                        output.accept(ModItems.BRAIN_NORMAL.get());
                        output.accept(ModItems.BRAIN_CLOCKWORK.get());
                        output.accept(ModItems.BRAIN_CURIOUS.get());

                        // === Research ===
                        output.accept(ModItems.RESEARCH_NOTES.get());
                        output.accept(ModItems.COMPLETE_NOTES.get());

                        // === Curiosities ===
                        output.accept(ModItems.CURIOSITY.get());
                        output.accept(ModItems.PRIMORDIAL_PEARL.get());
                        output.accept(ModItems.TAINT_SLIME.get());
                        output.accept(ModItems.TAINT_TENDRIL.get());
                        output.accept(ModItems.ZOMBIE_BRAIN.get());

                        // === Food ===
                        output.accept(ModItems.TRIPLE_MEAT_TREAT.get());
                        output.accept(ModItems.CHUNKS_BEEF.get());
                        output.accept(ModItems.CHUNKS_CHICKEN.get());
                        output.accept(ModItems.CHUNKS_PORK.get());
                        output.accept(ModItems.CHUNKS_FISH.get());

                        // === Armor - Goggles & Robes ===
                        output.accept(ModItems.GOGGLES.get());
                        output.accept(ModItems.CLOTH_CHEST.get());
                        output.accept(ModItems.CLOTH_LEGS.get());
                        output.accept(ModItems.CLOTH_BOOTS.get());

                        // === Tools - Thaumium ===
                        output.accept(ModItems.THAUMIUM_SWORD.get());
                        output.accept(ModItems.THAUMIUM_PICK.get());
                        output.accept(ModItems.THAUMIUM_AXE.get());
                        output.accept(ModItems.THAUMIUM_SHOVEL.get());
                        output.accept(ModItems.THAUMIUM_HOE.get());

                        // === Tools - Void Metal ===
                        output.accept(ModItems.VOID_SWORD.get());
                        output.accept(ModItems.VOID_PICK.get());
                        output.accept(ModItems.VOID_AXE.get());
                        output.accept(ModItems.VOID_SHOVEL.get());
                        output.accept(ModItems.VOID_HOE.get());

                        // === Elemental Tools ===
                        output.accept(ModItems.ELEMENTAL_PICK.get());
                        output.accept(ModItems.ELEMENTAL_AXE.get());
                        output.accept(ModItems.ELEMENTAL_SWORD.get());
                        output.accept(ModItems.ELEMENTAL_SHOVEL.get());
                        output.accept(ModItems.ELEMENTAL_HOE.get());

                        // === Special Tools ===
                        output.accept(ModItems.PRIMAL_CRUSHER.get());
                        output.accept(ModItems.CRIMSON_BLADE.get());

                        // === Utility Tools ===
                        output.accept(ModItems.RESONATOR.get());
                        output.accept(ModItems.SANITY_CHECKER.get());
                        output.accept(ModItems.HAND_MIRROR.get());
                        output.accept(ModItems.GRAPPLE_GUN.get());

                        // === Armor - Thaumium ===
                        output.accept(ModItems.THAUMIUM_HELM.get());
                        output.accept(ModItems.THAUMIUM_CHEST.get());
                        output.accept(ModItems.THAUMIUM_LEGS.get());
                        output.accept(ModItems.THAUMIUM_BOOTS.get());

                        // === Armor - Void Metal ===
                        output.accept(ModItems.VOID_HELM.get());
                        output.accept(ModItems.VOID_CHEST.get());
                        output.accept(ModItems.VOID_LEGS.get());
                        output.accept(ModItems.VOID_BOOTS.get());

                        // === Caster Gauntlets ===
                        output.accept(ModItems.CASTER_BASIC.get());
                        output.accept(ModItems.CASTER_ADVANCED.get());
                        output.accept(ModItems.CASTER_MASTER.get());

                        // === Focus Items ===
                        output.accept(ModItems.FOCUS_BLANK.get());
                        output.accept(ModItems.FOCUS_ADVANCED.get());

                        // === Special Armor ===
                        output.accept(ModItems.TRAVELLER_BOOTS.get());

                        // === Fortress Armor ===
                        output.accept(ModItems.FORTRESS_HELM.get());
                        output.accept(ModItems.FORTRESS_CHEST.get());
                        output.accept(ModItems.FORTRESS_LEGS.get());

                        // === Void Robe Armor ===
                        output.accept(ModItems.VOID_ROBE_HELM.get());
                        output.accept(ModItems.VOID_ROBE_CHEST.get());
                        output.accept(ModItems.VOID_ROBE_LEGS.get());

                        // === Crimson Cult Robe Armor ===
                        output.accept(ModItems.CRIMSON_ROBE_HELM.get());
                        output.accept(ModItems.CRIMSON_ROBE_CHEST.get());
                        output.accept(ModItems.CRIMSON_ROBE_LEGS.get());

                        // === Crimson Cult Plate Armor ===
                        output.accept(ModItems.CRIMSON_PLATE_HELM.get());
                        output.accept(ModItems.CRIMSON_PLATE_CHEST.get());
                        output.accept(ModItems.CRIMSON_PLATE_LEGS.get());

                        // === Crimson Cult Boots ===
                        output.accept(ModItems.CRIMSON_BOOTS.get());

                        // === Crimson Praetor Armor ===
                        output.accept(ModItems.CRIMSON_PRAETOR_HELM.get());
                        output.accept(ModItems.CRIMSON_PRAETOR_CHEST.get());
                        output.accept(ModItems.CRIMSON_PRAETOR_LEGS.get());

                        // === Baubles / Curios ===
                        output.accept(ModItems.AMULET_VIS_FOUND.get());
                        output.accept(ModItems.AMULET_VIS_CRAFTED.get());
                        output.accept(ModItems.CLOUD_RING.get());
                        output.accept(ModItems.CURIOSITY_BAND.get());
                        output.accept(ModItems.CHARM_UNDYING.get());
                        output.accept(ModItems.VERDANT_CHARM.get());
                        output.accept(ModItems.VERDANT_CHARM_LIFE.get());
                        output.accept(ModItems.VERDANT_CHARM_SUSTAIN.get());
                        output.accept(ModItems.VOIDSEER_CHARM.get());

                        // === Consumables ===
                        output.accept(ModItems.BATH_SALTS.get());
                        output.accept(ModItems.SANITY_SOAP.get());
                        output.accept(ModItems.BOTTLE_TAINT.get());
                        output.accept(ModItems.CAUSALITY_COLLAPSER.get());
                        output.accept(ModItems.LABEL_BLANK.get());

                        // === Loot Bags ===
                        output.accept(ModItems.LOOT_BAG_COMMON.get());
                        output.accept(ModItems.LOOT_BAG_UNCOMMON.get());
                        output.accept(ModItems.LOOT_BAG_RARE.get());

                        // === Curios ===
                        output.accept(ModItems.PECH_WAND.get());

                        // === Focus Accessories ===
                        output.accept(ModItems.FOCUS_POUCH.get());

                        // === Golem Items ===
                        output.accept(ModItems.GOLEM_PLACER.get());
                        output.accept(ModItems.GOLEM_BELL.get());
                        
                        // Golem variants - different material/part combinations
                        // Access parts by index since they're dynamically registered
                        // Materials: 0=WOOD, 1=IRON, 2=CLAY, 3=BRASS, 4=THAUMIUM, 5=VOID
                        // Heads: 0=BASIC, 1=SMART, 2=SCOUT
                        // Arms: 0=BASIC, 1=FINE, 2=CLAWS, 3=BREAKERS
                        // Legs: 0=WALKER, 1=ROLLER, 2=CLIMBER, 3=FLYER
                        // Addons: 0=NONE, 1=ARMORED, 2=FIGHTER, 3=HAULER
                        
                        // Wood golem (basic)
                        output.accept(createGolemStackById(0, 0, 0, 0, 0));
                        // Wood golem with smart head
                        output.accept(createGolemStackById(0, 1, 1, 0, 0));
                        // Clay golem
                        output.accept(createGolemStackById(2, 0, 0, 0, 0));
                        // Iron golem with claws
                        output.accept(createGolemStackById(1, 1, 2, 0, 2));
                        // Brass golem with hauler
                        output.accept(createGolemStackById(3, 1, 1, 1, 3));
                        // Thaumium golem (advanced)
                        output.accept(createGolemStackById(4, 1, 3, 2, 1));
                        // Void golem (endgame)
                        output.accept(createGolemStackById(5, 2, 2, 3, 2));
                        
                        // === Golem Builder ===
                        output.accept(ModBlocks.GOLEM_BUILDER.get());

                        // === Golem Seals ===
                        output.accept(ModItems.SEAL_BLANK.get());
                        output.accept(ModItems.SEAL_PICKUP.get());
                        output.accept(ModItems.SEAL_EMPTY.get());
                        output.accept(ModItems.SEAL_FILL.get());
                        output.accept(ModItems.SEAL_GUARD.get());
                        output.accept(ModItems.SEAL_BUTCHER.get());
                        output.accept(ModItems.SEAL_HARVEST.get());
                        output.accept(ModItems.SEAL_LUMBER.get());
                        output.accept(ModItems.SEAL_BREAKER.get());
                        output.accept(ModItems.SEAL_PROVIDE.get());
                    })
                    .build());

    /**
     * Create a golem placer ItemStack with parts specified by ID.
     */
    private static ItemStack createGolemStackById(int materialId, int headId, int armsId, int legsId, int addonId) {
        IGolemProperties props = new GolemProperties();
        props.setMaterial(GolemMaterial.getById(materialId));
        props.setHead(GolemHead.getById(headId));
        props.setArms(GolemArm.getById(armsId));
        props.setLegs(GolemLeg.getById(legsId));
        props.setAddon(GolemAddon.getById(addonId));
        return ItemGolemPlacer.createGolemStack(ModItems.GOLEM_PLACER.get(), props, 0);
    }
}
