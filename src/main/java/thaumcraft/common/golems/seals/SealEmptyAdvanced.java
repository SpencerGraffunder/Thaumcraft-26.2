package thaumcraft.common.golems.seals;

import net.minecraft.resources.Identifier;
import thaumcraft.Thaumcraft;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.seals.ISealConfigToggles;

/**
 * SealEmptyAdvanced - Advanced empty seal with 9 filter slots and cycling.
 * Requires SMART trait.
 */
public class SealEmptyAdvanced extends SealEmpty implements ISealConfigToggles {

    private Identifier icon;

    public SealEmptyAdvanced() {
        icon = Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "items/seals/seal_empty_advanced");
    }

    @Override
    public String getKey() {
        return "thaumcraft:empty_advanced";
    }

    @Override
    public int getFilterSize() {
        return 9;
    }

    @Override
    public Identifier getSealIcon() {
        return icon;
    }

    @Override
    public int[] getGuiCategories() {
        return new int[] { 1, 3, 0, 4 };
    }

    @Override
    public EnumGolemTrait[] getRequiredTags() {
        return new EnumGolemTrait[] { EnumGolemTrait.SMART };
    }
}
