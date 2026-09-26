package thaumcraft.init;

import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.Thaumcraft;

import net.minecraft.resources.ResourceKey;

/**
 * Registry for all Thaumcraft enchantments.
 */
public class ModEnchantments {

    // 26.2: DeferredRegister.create(Registries.X) infers the registry value type, so the
    // register is parameterized by Enchantment (not ResourceKey<Enchantment>).
    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(Registries.ENCHANTMENT, Thaumcraft.MODID);

    // 1.12: infusion - bonus dig radius per level
    // 26.2: DeferredHolder takes two type parameters (R, T extends R).
    public static final DeferredHolder<Enchantment, Enchantment> INFUSION =
            ENCHANTMENTS.register("infusion", () -> buildEnchantment("infusion"));

    // 1.12: burrowing - bonus dig depth per level
    public static final DeferredHolder<Enchantment, Enchantment> BURROWING =
            ENCHANTMENTS.register("burrowing", () -> buildEnchantment("burrowing"));

    private static Enchantment buildEnchantment(String name) {
        return new Enchantment.Builder(
                Enchantment.definition(
                        HolderSet.empty(),
                        1,
                        3,
                        new Enchantment.Cost(1, 0),
                        new Enchantment.Cost(1, 0),
                        20,
                        EquipmentSlotGroup.ANY))
                .build(Identifier.fromNamespaceAndPath(Thaumcraft.MODID, name));
    }
}
