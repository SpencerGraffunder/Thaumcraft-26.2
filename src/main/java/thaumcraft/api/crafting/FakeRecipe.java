package thaumcraft.api.crafting;

import net.minecraft.world.item.ItemStack;

/**
 * A display-only "fake" recipe for the Thaumonomicon.
 *
 * <p>Fake recipes are not craftable and never reach the recipe book; they exist
 * solely to render in the book's recipe popups for dynamic content that has no
 * real recipe backing (infusion enchantments, runic shielding, item-group
 * displays). Register them with {@link thaumcraft.api.ThaumcraftApi#addFakeCraftingRecipe}.
 */
public class FakeRecipe implements IThaumcraftRecipe {

    private final String research;
    private final String title;
    private final ItemStack[] items;

    /**
     * @param research research key the display belongs to (may be empty)
     * @param title    display title shown at the top of the popup
     * @param items    display items, drawn as a row (up to 8 shown)
     */
    public FakeRecipe(String research, String title, ItemStack... items) {
        this.research = research;
        this.title = title;
        this.items = items;
    }

    @Override
    public String getResearch() {
        return research;
    }

    public String getTitle() {
        return title;
    }

    public ItemStack[] getItems() {
        return items;
    }
}
