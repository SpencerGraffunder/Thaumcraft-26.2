package thaumcraft.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;
import thaumcraft.Thaumcraft;

/**
 * Registry IDs for the Thaumcraft enchantments.
 *
 * MC 26.3 loads enchantments from the datapack registry
 * (data/thaumcraft/enchantment/*.json), so this class only holds the
 * ResourceKeys that runtime code (EntityArcaneBore's tool checks) needs.
 * The 1.12 behavioural data (levels, tool classes, IE gating) lives in
 * {@code EnumInfusionEnchantment}, which stores levels in the custom
 * "infench" component rather than the vanilla enchantment stack.
 */
public class ModEnchantments {

    // 1.12: infusion - bonus dig radius per level (checked by the arcane bore)
    public static final ResourceKey<Enchantment> INFUSION = key("infusion");

    // 1.12: burrowing - bonus dig depth per level (checked by the arcane bore)
    public static final ResourceKey<Enchantment> BURROWING = key("burrowing");

    private static ResourceKey<Enchantment> key(String name) {
        return ResourceKey.create(Registries.ENCHANTMENT,
                Identifier.fromNamespaceAndPath(Thaumcraft.MODID, name));
    }
}
