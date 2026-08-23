package thaumcraft.common.lib.research.theorycraft;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.capabilities.IPlayerWarp;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.research.theorycraft.ResearchTableData;
import thaumcraft.api.research.theorycraft.TheorycraftCard;
import net.minecraft.world.item.Items;

import net.minecraft.util.RandomSource;

/**
 * Celestial card - Requires celestial notes. 
 * Grants significant progress to highest category, with variable bonuses based on celestial bodies.
 * Requires CELESTIALSCANNING research.
 */
public class CardCelestial extends TheorycraftCard {

    private int noteType1;
    private int noteType2;
    private String category = "BASICS";

    @Override
    public CompoundTag serialize() {
        CompoundTag nbt = super.serialize();
        nbt.putInt("md1", noteType1);
        nbt.putInt("md2", noteType2);
        nbt.putString("cat", category);
        return nbt;
    }

    @Override
    public void deserialize(CompoundTag nbt) {
        super.deserialize(nbt);
        noteType1 = nbt.getIntOr("md1", 0);
        noteType2 = nbt.getIntOr("md2", 0);
        category = nbt.getStringOr("cat", "");
    }

    @Override
    public String getResearchCategory() {
        return category;
    }

    @Override
    public boolean initialize(Player player, ResearchTableData data) {
        if (data.categoryTotals.isEmpty() || !ThaumcraftCapabilities.knowsResearch(player, "CELESTIALSCANNING")) {
            return false;
        }
        
        RandomSource r = RandomSource.create(getSeed());
        noteType1 = Mth.nextInt(r, 0, 12);
        noteType2 = noteType1;
        while (noteType1 == noteType2) {
            noteType2 = Mth.nextInt(r, 0, 12);
        }
        
        // Find highest value category
        int highestValue = 0;
        String highestKey = "BASICS";
        for (String cat : data.categoryTotals.keySet()) {
            int value = data.getTotal(cat);
            if (value > highestValue) {
                highestValue = value;
                highestKey = cat;
            }
        }
        category = highestKey;
        
        return category != null;
    }

    @Override
    public int getInspirationCost() {
        return 1;
    }

    @Override
    public String getLocalizedName() {
        return "card.celestial.name";
    }

    @Override
    public String getLocalizedText() {
        return "card.celestial.text";
    }

    @Override
    public ItemStack[] getRequiredItems() {
        // Two different celestial notes - uses paper as placeholder
        // In full implementation, these would be celestial note items
        return new ItemStack[] { 
            new ItemStack(Items.PAPER),
            new ItemStack(Items.PAPER)
        };
    }

    @Override
    public boolean[] getRequiredItemsConsumed() {
        return new boolean[] { true, true };
    }

    @Override
    public boolean activate(Player player, ResearchTableData data) {
        data.addTotal(getResearchCategory(), Mth.nextInt(player.getRandom(), 25, 50));
        
        boolean sun = noteType1 == 0 || noteType2 == 0;
        boolean moon = noteType1 > 4 || noteType2 > 4;
        boolean stars = (noteType1 > 0 && noteType1 < 5) || (noteType2 > 0 && noteType2 < 5);
        
        if (stars) {
            int amt = Mth.nextInt(player.getRandom(), 0, 5);
            data.addTotal("ELDRITCH", amt * 2);
            ThaumcraftApi.internalMethods.addWarpToPlayer(player, amt, IPlayerWarp.EnumWarpType.TEMPORARY);
        }
        if (sun) {
            data.penaltyStart++;
        }
        if (moon) {
            data.bonusDraws++;
        }
        
        return true;
    }
}
