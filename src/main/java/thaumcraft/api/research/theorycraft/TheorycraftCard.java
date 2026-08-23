package thaumcraft.api.research.theorycraft;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;

/**
 * Abstract base class for theorycraft cards used in the research table mini-game.
 * 
 * Cards represent actions a player can take during a theorycraft session to
 * gain progress towards research categories. Each card has an inspiration cost,
 * optional item requirements, and an activation effect.
 * 
 * See CardAnalyze for an example implementation.
 * 
 * @author Azanor
 */
public abstract class TheorycraftCard {
    
    private long seed = -1;
    
    /**
     * A seed value used to determine random attributes associated with the card.
     * @return The random seed for this card instance
     */
    public long getSeed() {
        if (seed < 0) setSeed(System.nanoTime());
        return seed;
    }
    
    /**
     * This method is run when card is initially created.
     * @param player The player using the research table
     * @param data The current research table session data
     * @return If the card cannot be initialized for some reason it will be discarded and a new one created.
     */
    public boolean initialize(Player player, ResearchTableData data) {
        return true;
    }
    
    /**
     * If true this card cannot come up in the normal draw rotation - it only appears if added by a mutator block
     * @return true if this is an aid-only card
     */
    public boolean isAidOnly() {
        return false;
    }
    
    /**
     * How much inspiration this card costs to activate. Can be zero. Negative numbers will return inspiration.
     * @return The inspiration cost
     */
    public abstract int getInspirationCost();
    
    /**
     * The research category this card is associated with. Can be null if it is not linked to anything.
     * @return The category key (e.g., "BASICS", "ALCHEMY", "AUROMANCY") or null
     */
    public String getResearchCategory() {
        return null;
    }
    
    /**
     * Localized name of the card. Will be localized in GUI.
     * @return The translation key for the card name
     */
    public abstract String getLocalizedName();
    
    /**
     * Localized text/description of the card. Will be localized in GUI.
     * @return The translation key for the card description
     */
    public abstract String getLocalizedText();
    
    /**
     * The items required to complete this operation. 
     * If a null is returned no items are required. The array itself can contain null itemstacks - 
     * that signifies an item is required, but it will display as a ? in the GUI.
     * You need to take care of consuming and checking for those items yourself in the activate method (see below). 
     * Non-null items will be handled automatically.
     * @return Array of required items, or null if no items needed
     */
    public ItemStack[] getRequiredItems() {
        return null;
    }
    
    /**
     * Will the listed items be consumed when the card is picked.
     * @return Boolean array matching getRequiredItems(), true = consumed
     */
    public boolean[] getRequiredItemsConsumed() {
        if (getRequiredItems() != null) {
            boolean[] b = new boolean[getRequiredItems().length];
            Arrays.fill(b, false);
            return b;
        }
        return null;
    }
    
    /**
     * Perform the card's functionality on the current research table data.
     * You need to do all the proper checks for items carried and so forth in this method, 
     * as well as consuming them where needed.
     * @param player The player activating the card
     * @param data The current research table session data
     * @return true if the action was successful
     */
    public abstract boolean activate(Player player, ResearchTableData data);
    
    /**
     * Internal use only. This should not be called unless you want to mess things up.
     * @param seed The seed value to set
     */
    public void setSeed(long seed) {
        this.seed = Math.abs(seed);
    }
    
    /**
     * Called when card is saved to NBT.
     * Override to save additional card-specific data.
     * @return The serialized card data
     */
    public CompoundTag serialize() {
        CompoundTag nbt = new CompoundTag();
        nbt.putLong("seed", seed);
        return nbt;
    }
    
    /**
     * Called when card is loaded from NBT.
     * Override to load additional card-specific data.
     * @param nbt The NBT data to load from
     */
    public void deserialize(CompoundTag nbt) {
        if (nbt == null) return;
        seed = nbt.getLongOr("seed", 0L);
    }
}
