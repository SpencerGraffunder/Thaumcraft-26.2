package thaumcraft.common.lib.research;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import thaumcraft.api.research.IScanThing;
import thaumcraft.api.research.ScanningManager;

/**
 * Scans for a specific enchantment on an item.
 * Ported from 1.12.2 ScanEnchantment.
 */
public class ScanEnchantment implements IScanThing {
    
    private final Holder<Enchantment> enchantment;
    
    public ScanEnchantment(Holder<Enchantment> ench) {
        this.enchantment = ench;
    }
    
    @Override
    public boolean checkThing(Player player, Object obj) {
        if (obj == null) return false;
        ItemStack is = ScanningManager.getItemFromParams(player, obj);
        if (is == null || is.isEmpty()) return false;
        ItemEnchantments enchs = EnchantmentHelper.getEnchantmentsForCrafting(is);
        return enchs.getLevel(enchantment) > 0;
    }
    
    @Override
    public String getResearchKey(Player player, Object object) {
        // 26.2: Holder.unwrapKey() -> Optional<ResourceKey<Enchantment>>; ResourceKey.getLocation() renamed to identifier()
        return enchantment.unwrapKey().map(k -> "!" + k.identifier().getPath()).orElse("?");
    }
}
