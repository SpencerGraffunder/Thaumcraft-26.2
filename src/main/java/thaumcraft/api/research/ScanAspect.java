package thaumcraft.api.research;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;

import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectHelper;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.capabilities.IPlayerKnowledge;

/**
 * Scans whatever is pointed at with the Thaumometer for the given aspect.
 *
 * Ported 1:1 from 1.12 thaumcraft.api.research.ScanAspect: an entity (other than a
 * dropped item) is checked with {@link AspectHelper#getEntityAspects(Entity)}, anything
 * else - a held/dropped stack or a scanned block - is checked with
 * {@link AspectHelper#getObjectAspects(ItemStack)}. On success the scanner grants one
 * point of OBSERVATION knowledge in AUROMANCY, BASICS and ALCHEMY, which is how 1.12
 * teaches the hidden "!aspect" research flags (e.g. "!aer") that gate most of the
 * auromancy and artificer research lines.
 */
public class ScanAspect implements IScanThing {

    private final String research;
    private final Aspect aspect;

    public ScanAspect(String research, Aspect aspect) {
        this.research = research;
        this.aspect = aspect;
    }

    @Override
    public boolean checkThing(Player player, Object obj) {
        if (obj == null) {
            return false;
        }

        AspectList aspects = null;
        if (obj instanceof Entity entity && !(obj instanceof ItemEntity)) {
            aspects = AspectHelper.getEntityAspects(entity);
        } else {
            ItemStack stack = ItemStack.EMPTY;

            if (obj instanceof ItemStack itemStack) {
                stack = itemStack;
            } else if (obj instanceof ItemEntity itemEntity) {
                stack = itemEntity.getItem();
            } else if (obj instanceof BlockPos pos && player != null) {
                // 1.12 built ItemStack(block, 1, meta); the block's item form is the
                // equivalent lookup here (metadata no longer exists in 26.3)
                stack = new ItemStack(player.level().getBlockState(pos).getBlock().asItem());
            }

            if (!stack.isEmpty()) {
                aspects = AspectHelper.getObjectAspects(stack);
            }
        }

        return aspects != null && aspects.getAmount(aspect) > 0;
    }

    @Override
    public void onSuccess(Player player, Object object) {
        ThaumcraftApi.internalMethods.addKnowledge(player, IPlayerKnowledge.EnumKnowledgeType.OBSERVATION,
                ResearchCategories.getResearchCategory("AUROMANCY"), 1);
        ThaumcraftApi.internalMethods.addKnowledge(player, IPlayerKnowledge.EnumKnowledgeType.OBSERVATION,
                ResearchCategories.getResearchCategory("BASICS"), 1);
        ThaumcraftApi.internalMethods.addKnowledge(player, IPlayerKnowledge.EnumKnowledgeType.OBSERVATION,
                ResearchCategories.getResearchCategory("ALCHEMY"), 1);
    }

    @Override
    public String getResearchKey(Player player, Object object) {
        return research;
    }
}
