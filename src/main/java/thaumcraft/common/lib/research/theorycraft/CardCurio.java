package thaumcraft.common.lib.research.theorycraft;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.theorycraft.ResearchTableData;
import thaumcraft.api.research.theorycraft.TheorycraftCard;
import thaumcraft.common.items.curios.ItemCurio;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Curio card - Requires consuming a Curio item for variable research bonus.
 * The curio variant (damage 0-6) determines which category gets the bonus:
 * - arcane:    AUROMANCY
 * - preserved: ALCHEMY
 * - ancient:   GOLEMANCY
 * - eldritch:  ELDRITCH
 * - knowledge: INFUSION
 * - twisted:   ARTIFICE
 * - rites:     ELDRITCH + AUROMANCY
 */
public class CardCurio extends TheorycraftCard {

    private ItemStack curio = ItemStack.EMPTY;

    @Override
    public CompoundTag serialize() {
        CompoundTag nbt = super.serialize();
        nbt.put("stack", (CompoundTag) ItemStack.OPTIONAL_CODEC.encodeStart(NbtOps.INSTANCE, curio).resultOrPartial().orElse(new CompoundTag()));
        return nbt;
    }

    @Override
    public void deserialize(CompoundTag nbt) {
        super.deserialize(nbt);
        curio = nbt.contains("stack")
                ? ItemStack.OPTIONAL_CODEC.parse(NbtOps.INSTANCE, nbt.get("stack")).resultOrPartial().orElse(ItemStack.EMPTY)
                : ItemStack.EMPTY;
    }

    @Override
    public int getInspirationCost() {
        return 1;
    }

    @Override
    public String getLocalizedName() {
        return "card.curio.name";
    }

    @Override
    public String getLocalizedText() {
        return "card.curio.text";
    }

    @Override
    public ItemStack[] getRequiredItems() {
        return new ItemStack[] { curio };
    }

    @Override
    public boolean[] getRequiredItemsConsumed() {
        return new boolean[] { true };
    }

    @Override
    public boolean initialize(Player player, ResearchTableData data) {
        Random r = new Random(getSeed());
        List<ItemStack> curios = new ArrayList<>();

        // Look for ItemCurio instances in player inventory
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (!stack.isEmpty() && stack.getItem() instanceof ItemCurio) {
                ItemStack c = stack.copy();
                c.setCount(1);
                curios.add(c);
            }
        }

        if (!curios.isEmpty()) {
            curio = curios.get(r.nextInt(curios.size()));
        }

        return !curio.isEmpty();
    }

    @Override
    public boolean activate(Player player, ResearchTableData data) {
        data.addTotal("BASICS", 5);

        // Random category bonus
        String[] categories = ResearchCategories.researchCategories.keySet().toArray(new String[0]);
        if (categories.length > 0) {
            data.addTotal(categories[player.getRandom().nextInt(categories.length)], 5);
        }

        // Curio variant specific bonus (matches 1.12 CardCurio)
        int damage = curio.getDamageValue();
        String variant = ItemCurio.getVariantName(damage);
        switch (variant) {
            case "arcane":
                data.addTotal("AUROMANCY", Mth.nextInt(player.getRandom(), 25, 35));
                break;
            case "preserved":
                data.addTotal("ALCHEMY", Mth.nextInt(player.getRandom(), 25, 35));
                break;
            case "ancient":
                data.addTotal("GOLEMANCY", Mth.nextInt(player.getRandom(), 25, 35));
                break;
            case "eldritch":
                data.addTotal("ELDRITCH", Mth.nextInt(player.getRandom(), 25, 35));
                break;
            case "knowledge":
                data.addTotal("INFUSION", Mth.nextInt(player.getRandom(), 25, 35));
                break;
            case "twisted":
                data.addTotal("ARTIFICE", Mth.nextInt(player.getRandom(), 25, 35));
                break;
            case "rites":
                data.addTotal("ELDRITCH", Mth.nextInt(player.getRandom(), 15, 20));
                data.addTotal("AUROMANCY", Mth.nextInt(player.getRandom(), 10, 15));
                break;
            default:
                data.addTotal("BASICS", Mth.nextInt(player.getRandom(), 25, 35));
                break;
        }

        if (player.getRandom().nextBoolean()) data.bonusDraws++;
        if (player.getRandom().nextBoolean()) data.bonusDraws++;

        return true;
    }
}
