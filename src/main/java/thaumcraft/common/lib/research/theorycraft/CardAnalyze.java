package thaumcraft.common.lib.research.theorycraft;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.theorycraft.ResearchTableData;
import thaumcraft.api.research.theorycraft.TheorycraftCard;

import java.util.Random;

/**
 * Analyze card - converts observation knowledge to category progress.
 * One of the basic cards available in normal rotation.
 */
public class CardAnalyze extends TheorycraftCard {

    private String cat = "BASICS";

    @Override
    public CompoundTag serialize() {
        CompoundTag nbt = super.serialize();
        nbt.putString("cat", cat);
        return nbt;
    }

    @Override
    public void deserialize(CompoundTag nbt) {
        super.deserialize(nbt);
        cat = nbt.getStringOr("cat", "");
    }

    @Override
    public String getResearchCategory() {
        return cat;
    }

    @Override
    public boolean initialize(Player player, ResearchTableData data) {
        Random r = new Random(getSeed());
        String[] categories = ResearchCategories.researchCategories.keySet().toArray(new String[0]);
        if (categories.length == 0) return false;
        cat = categories[r.nextInt(categories.length)];
        return cat != null;
    }

    @Override
    public int getInspirationCost() {
        return 1;
    }

    @Override
    public String getLocalizedName() {
        return "card.analyze.name";
    }

    @Override
    public String getLocalizedText() {
        return "card.analyze.text";
    }

    @Override
    public boolean activate(Player player, ResearchTableData data) {
        data.addTotal(cat, Mth.nextInt(player.getRandom(), 10, 20));
        return true;
    }
}
