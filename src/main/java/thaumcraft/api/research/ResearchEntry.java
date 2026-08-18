package thaumcraft.api.research;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.research.ResearchStage.Knowledge;

import java.util.Arrays;

/**
 * Represents a single research entry in the Thaumonomicon.
 * Research entries contain stages, requirements, and rewards.
 */
public class ResearchEntry {

    /** A short string used as a key for this research. Must be unique */
    private String key;

    /** A short string used as a reference to the research category to which this must be added */
    private String category;

    /** A text name of the research entry. Can be a localizable string key */
    private String name;

    /** Links to any research that needs to be completed before this research can be discovered or learned */
    private String[] parents;

    /** Any research linked to this that will be unlocked automatically when this research is complete */
    private String[] siblings;

    /** The horizontal position of the research icon */
    private int displayColumn;

    /** The vertical position of the research icon */
    private int displayRow;

    /** The icon(s) to be used for this research (can be ItemStack, Identifier, or Aspect) */
    private Object[] icons;

    /** Special meta-data tags that indicate how this research must be handled */
    private EnumResearchMeta[] meta;

    /** Items the player will receive on completion of this research */
    private ItemStack[] rewardItem;

    /** Knowledge the player will receive on completion of this research */
    private Knowledge[] rewardKnow;

    /** The various stages present in this research entry */
    private ResearchStage[] stages;

    /** The various addenda present in this research entry */
    private ResearchAddendum[] addenda;

    /**
     * Meta tags that modify research behavior
     */
    public enum EnumResearchMeta {
        /** Round icon frame */
        ROUND,
        /** Spiky icon frame - grants 0.5 bonus inspiration for theorycrafting */
        SPIKY,
        /** Reverse direction of parent connections */
        REVERSE,
        /** Hidden until requirements are met - grants 0.1 bonus inspiration for theorycrafting */
        HIDDEN,
        /** Automatically unlocked when parents are complete */
        AUTOUNLOCK,
        /** Hexagonal icon frame */
        HEX
    }

    // Getters and Setters

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getName() {
        return name;
    }

    /**
     * @return the localized name of this research entry
     */
    public Component getLocalizedName() {
        return Component.translatable(getName());
    }

    public void setName(String name) {
        this.name = name;
    }

    public String[] getParents() {
        return parents;
    }

    /**
     * @return parents with ALL prefixes and postfixes stripped away
     */
    public String[] getParentsClean() {
        if (parents == null) return null;
        
        String[] out = getParentsStripped();
        for (int i = 0; i < out.length; i++) {
            if (out[i].contains("@")) {
                out[i] = out[i].substring(0, out[i].indexOf("@"));
            }
        }
        return out;
    }

    /**
     * @return parents with prefixes stripped away
     */
    public String[] getParentsStripped() {
        if (parents == null) return null;
        
        String[] out = new String[parents.length];
        for (int i = 0; i < out.length; i++) {
            out[i] = parents[i];
            if (out[i].startsWith("~")) {
                out[i] = out[i].substring(1);
            }
        }
        return out;
    }

    public void setParents(String[] parents) {
        this.parents = parents;
    }

    public String[] getSiblings() {
        return siblings;
    }

    public void setSiblings(String[] siblings) {
        this.siblings = siblings;
    }

    public int getDisplayColumn() {
        return displayColumn;
    }

    public void setDisplayColumn(int displayColumn) {
        this.displayColumn = displayColumn;
    }

    public int getDisplayRow() {
        return displayRow;
    }

    public void setDisplayRow(int displayRow) {
        this.displayRow = displayRow;
    }

    public Object[] getIcons() {
        return icons;
    }

    public void setIcons(Object[] icons) {
        this.icons = icons;
    }

    public EnumResearchMeta[] getMeta() {
        return meta;
    }

    public boolean hasMeta(EnumResearchMeta me) {
        return meta != null && Arrays.asList(meta).contains(me);
    }

    public void setMeta(EnumResearchMeta[] meta) {
        this.meta = meta;
    }

    public ResearchStage[] getStages() {
        return stages;
    }

    public void setStages(ResearchStage[] stages) {
        this.stages = stages;
    }

    public ItemStack[] getRewardItem() {
        return rewardItem;
    }

    public void setRewardItem(ItemStack[] rewardItem) {
        this.rewardItem = rewardItem;
    }

    public Knowledge[] getRewardKnow() {
        return rewardKnow;
    }

    public void setRewardKnow(Knowledge[] rewardKnow) {
        this.rewardKnow = rewardKnow;
    }

    public ResearchAddendum[] getAddenda() {
        return addenda;
    }

    public void setAddenda(ResearchAddendum[] addenda) {
        this.addenda = addenda;
    }
}
