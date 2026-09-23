package thaumcraft.common.lib.research;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import thaumcraft.api.research.IScanThing;
import thaumcraft.api.research.ScanningManager;

import java.util.Collection;

/**
 * Scans for a specific potion effect on an item or entity.
 * Ported from 1.12.2 ScanPotion.
 */
public class ScanPotion implements IScanThing {
    
    private final Holder<MobEffect> effect;
    
    public ScanPotion(Holder<MobEffect> effect) {
        this.effect = effect;
    }
    
    @Override
    public boolean checkThing(Player player, Object obj) {
        if (obj == null) return false;
        
        if (obj instanceof LivingEntity entity) {
            Collection<MobEffectInstance> effects = entity.getActiveEffects();
            for (MobEffectInstance eff : effects) {
                if (eff.getEffect() == effect) {
                    return true;
                }
            }
            return false;
        }
        
        ItemStack is = ScanningManager.getItemFromParams(player, obj);
        if (is == null || is.isEmpty()) return false;
        
        PotionContents contents = is.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        for (MobEffectInstance eff : contents.getAllEffects()) {
            if (eff.getEffect() == effect) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public String getResearchKey(Player player, Object object) {
        var id = BuiltInRegistries.MOB_EFFECT.getKey(effect.value());
        return id != null ? "!" + id.getPath() : "!" + effect.value().toString();
    }
}
