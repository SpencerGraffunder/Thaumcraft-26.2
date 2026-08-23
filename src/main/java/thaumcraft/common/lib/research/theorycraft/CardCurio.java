package thaumcraft.common.lib.research.theorycraft;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.theorycraft.ResearchTableData;
import thaumcraft.api.research.theorycraft.TheorycraftCard;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Curio card - Requires consuming a rare item for variable research bonus.
 * Note: In full implementation, this would use ItemCurio from the curios package.
 * Currently uses valuable vanilla items as placeholders.
 */
public class CardCurio extends TheorycraftCard {

    private ItemStack curio = ItemStack.EMPTY;
    
    // Placeholder curio items until ItemCurio is ported
    private static final ItemStack[] CURIO_TYPES = {
        new ItemStack(Items.AMETHYST_SHARD),
        new ItemStack(Items.ECHO_SHARD),
        new ItemStack(Items.HEART_OF_THE_SEA),
        new ItemStack(Items.NETHER_STAR),
        new ItemStack(Items.ENDER_EYE),
        new ItemStack(Items.TOTEM_OF_UNDYING)
    };

    @Override
    public CompoundTag serialize() {
        CompoundTag nbt = super.serialize();
        nbt.put("stack", curio.save(new CompoundTag()));
        return nbt;
    }

    @Override
    public void deserialize(CompoundTag nbt) {
        super.deserialize(nbt);
        curio = ItemStack.of(nbt.getCompoundOrEmpty("stack"));
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
        
        // Look for valuable items in player inventory
        for (ItemStack stack : player.getInventory().getItems()) {
            if (!stack.isEmpty()) {
                for (ItemStack curioType : CURIO_TYPES) {
                    if (stack.is(curioType.getItem())) {
                        ItemStack c = stack.copy();
                        c.setCount(1);
                        curios.add(c);
                        break;
                    }
                }
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
        
        // Item type specific bonus
        if (curio.is(Items.AMETHYST_SHARD)) {
            data.addTotal("AUROMANCY", Mth.nextInt(player.getRandom(), 25, 35));
        } else if (curio.is(Items.ECHO_SHARD)) {
            data.addTotal("ELDRITCH", Mth.nextInt(player.getRandom(), 25, 35));
        } else if (curio.is(Items.HEART_OF_THE_SEA)) {
            data.addTotal("ALCHEMY", Mth.nextInt(player.getRandom(), 25, 35));
        } else if (curio.is(Items.NETHER_STAR)) {
            data.addTotal("INFUSION", Mth.nextInt(player.getRandom(), 30, 45));
            data.bonusDraws++;
        } else if (curio.is(Items.ENDER_EYE)) {
            data.addTotal("ELDRITCH", Mth.nextInt(player.getRandom(), 20, 30));
            data.addTotal("AUROMANCY", Mth.nextInt(player.getRandom(), 10, 15));
        } else if (curio.is(Items.TOTEM_OF_UNDYING)) {
            data.addTotal("GOLEMANCY", Mth.nextInt(player.getRandom(), 25, 35));
        } else {
            data.addTotal("BASICS", Mth.nextInt(player.getRandom(), 25, 35));
        }
        
        if (player.getRandom().nextBoolean()) data.bonusDraws++;
        if (player.getRandom().nextBoolean()) data.bonusDraws++;
        
        return true;
    }
}
