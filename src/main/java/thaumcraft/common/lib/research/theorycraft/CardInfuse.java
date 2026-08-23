package thaumcraft.common.lib.research.theorycraft;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.research.theorycraft.ResearchTableData;
import thaumcraft.api.research.theorycraft.TheorycraftCard;
import java.util.Random;

/**
 * Infuse card - Requires consuming an item and a phial of essentia.
 * Progress depends on the aspect complexity of the item.
 */
public class CardInfuse extends TheorycraftCard {

    private Aspect aspect;
    private ItemStack stack = ItemStack.EMPTY;
    
    // Common items that can be infused
    private static final ItemStack[] OPTIONS = {
        new ItemStack(Items.GOLD_INGOT),
        new ItemStack(Items.IRON_INGOT),
        new ItemStack(Items.DIAMOND),
        new ItemStack(Items.EMERALD),
        new ItemStack(Items.BLAZE_ROD),
        new ItemStack(Items.LEATHER),
        new ItemStack(Items.WOOL.pick(DyeColor.WHITE)),
        new ItemStack(Items.BRICK),
        new ItemStack(Items.ARROW),
        new ItemStack(Items.EGG),
        new ItemStack(Items.FEATHER),
        new ItemStack(Items.GLOWSTONE_DUST),
        new ItemStack(Items.REDSTONE),
        new ItemStack(Items.GHAST_TEAR),
        new ItemStack(Items.GUNPOWDER),
        new ItemStack(Items.BOW),
        new ItemStack(Items.GOLDEN_SWORD),
        new ItemStack(Items.IRON_SWORD),
        new ItemStack(Items.IRON_PICKAXE),
        new ItemStack(Items.GOLDEN_PICKAXE),
        new ItemStack(Items.QUARTZ),
        new ItemStack(Items.APPLE)
    };

    @Override
    public CompoundTag serialize() {
        CompoundTag nbt = super.serialize();
        nbt.putString("aspect", aspect != null ? aspect.getTag() : "");
        nbt.put("stack", (CompoundTag) ItemStack.OPTIONAL_CODEC.encodeStart(NbtOps.INSTANCE, stack).resultOrPartial().orElse(new CompoundTag()));
        return nbt;
    }

    @Override
    public void deserialize(CompoundTag nbt) {
        super.deserialize(nbt);
        aspect = Aspect.getAspect(nbt.getStringOr("aspect", ""));
        stack = nbt.contains("stack")
                ? ItemStack.OPTIONAL_CODEC.parse(NbtOps.INSTANCE, nbt.get("stack")).resultOrPartial().orElse(ItemStack.EMPTY)
                : ItemStack.EMPTY;
    }

    @Override
    public boolean initialize(Player player, ResearchTableData data) {
        Random r = new Random(getSeed());
        if (Aspect.getCompoundAspects().isEmpty()) {
            aspect = Aspect.MAGIC;
        } else {
            int num = r.nextInt(Aspect.getCompoundAspects().size());
            aspect = Aspect.getCompoundAspects().get(num);
        }
        stack = OPTIONS[r.nextInt(OPTIONS.length)].copy();
        return aspect != null && !stack.isEmpty();
    }

    @Override
    public int getInspirationCost() {
        return 1;
    }

    @Override
    public String getResearchCategory() {
        return "INFUSION";
    }

    @Override
    public String getLocalizedName() {
        return "card.infuse.name";
    }

    @Override
    public String getLocalizedText() {
        return "card.infuse.text";
    }

    private int getValue() {
        // Base value plus some variation
        return 15 + (stack.getItem().hashCode() % 10);
    }

    @Override
    public ItemStack[] getRequiredItems() {
        // Requires the item and a glass bottle as placeholder for phial
        // In full implementation, this would use a filled phial of essentia
        ItemStack phial = new ItemStack(Items.GLASS_BOTTLE);
        return new ItemStack[] { stack, phial };
    }

    @Override
    public boolean[] getRequiredItemsConsumed() {
        return new boolean[] { true, true };
    }

    @Override
    public boolean activate(Player player, ResearchTableData data) {
        data.addTotal(getResearchCategory(), getValue());
        return true;
    }
}
