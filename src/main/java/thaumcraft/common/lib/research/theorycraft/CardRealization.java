package thaumcraft.common.lib.research.theorycraft;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import thaumcraft.api.research.theorycraft.ResearchTableData;
import thaumcraft.api.research.theorycraft.TheorycraftCard;

import java.util.List;
import java.util.Random;

/**
 * Realization card - A eureka moment that grants significant progress to a random category.
 * Higher cost but better rewards.
 */
public class CardRealization extends TheorycraftCard {

    private String category = "BASICS";

    @Override
    public CompoundTag serialize() {
        CompoundTag nbt = super.serialize();
        nbt.putString("cat", category);
        return nbt;
    }

    @Override
    public void deserialize(CompoundTag nbt) {
        super.deserialize(nbt);
        category = nbt.getStringOr("cat", "");
    }

    @Override
    public String getResearchCategory() {
        return category;
    }

    @Override
    public boolean initialize(Player player, ResearchTableData data) {
        Random r = new Random(getSeed());
        List<String> list = data.getAvailableCategories(player);
        if (list.isEmpty()) return false;
        category = list.get(r.nextInt(list.size()));
        return category != null;
    }

    @Override
    public int getInspirationCost() {
        return 2;
    }

    @Override
    public String getLocalizedName() {
        return "card.realization.name";
    }

    @Override
    public String getLocalizedText() {
        return "card.realization.text";
    }

    @Override
    public boolean activate(Player player, ResearchTableData data) {
        data.addTotal(category, Mth.nextInt(player.getRandom(), 25, 40));
        data.bonusDraws++;
        return true;
    }
}
