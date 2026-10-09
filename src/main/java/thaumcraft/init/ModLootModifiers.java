package thaumcraft.init;

import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.common.lib.loot.RefiningMiningLootModifier;

/**
 * Registers global loot modifier serializer types (1.12 special-mining support
 * for the REFINING infusion enchantment).
 */
@EventBusSubscriber(modid = Thaumcraft.MODID)
public class ModLootModifiers {

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        if (event.getRegistryKey().equals(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS)) {
            event.register(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, helper ->
                    helper.register(Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "refining_mining"),
                            RefiningMiningLootModifier.CODEC));
        }
    }
}
