package thaumcraft.common.lib.research;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.AspectHelper;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.research.IScanThing;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategory;
import thaumcraft.api.research.ScanningManager;
import net.minecraft.core.registries.Registries;

/**
 * Generic scanner that gives observation knowledge based on an object's aspects.
 * This is the fallback scanner that handles any entity or item with aspects.
 */
public class ScanGeneric implements IScanThing {
    
    @Override
    public boolean checkThing(Player player, Object obj) {
        if (obj == null) return false;
        
        AspectList aspects = null;
        
        if (obj instanceof Entity && !(obj instanceof ItemEntity)) {
            aspects = AspectHelper.getEntityAspects((Entity) obj);
        } else {
            ItemStack stack = ScanningManager.getItemFromParams(player, obj);
            if (stack != null && !stack.isEmpty()) {
                aspects = AspectHelper.getObjectAspects(stack);
            }
        }
        
        return aspects != null && aspects.size() > 0;
    }
    
    @Override
    public void onSuccess(Player player, Object obj) {
        if (obj == null) return;
        
        AspectList aspects = null;
        
        if (obj instanceof Entity && !(obj instanceof ItemEntity)) {
            aspects = AspectHelper.getEntityAspects((Entity) obj);
        } else {
            ItemStack stack = ScanningManager.getItemFromParams(player, obj);
            if (stack != null && !stack.isEmpty()) {
                aspects = AspectHelper.getObjectAspects(stack);
            }
        }
        
        if (aspects != null) {
            // Give observation knowledge based on aspects in each category
            for (ResearchCategory category : ResearchCategories.researchCategories.values()) {
                int knowledge = category.applyFormula(aspects);
                if (knowledge > 0) {
                    ThaumcraftApi.internalMethods.addKnowledge(player, 
                            IPlayerKnowledge.EnumKnowledgeType.OBSERVATION, 
                            category, knowledge);
                }
            }
        }
    }
    
    @Override
    public String getResearchKey(Player player, Object obj) {
        if (obj instanceof Entity entity && !(obj instanceof ItemEntity)) {
            String entityKey = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
            return "!" + entityKey;
        }
        
        ItemStack stack = ScanningManager.getItemFromParams(player, obj);
        if (stack != null && !stack.isEmpty()) {
            String itemKey = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            return "!" + itemKey;
        }
        
        return null;
    }
}
