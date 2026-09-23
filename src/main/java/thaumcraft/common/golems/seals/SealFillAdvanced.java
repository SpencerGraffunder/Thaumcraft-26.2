package thaumcraft.common.golems.seals;

import net.minecraft.resources.Identifier;
import thaumcraft.Thaumcraft;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.seals.ISealConfigToggles;

/**
 * SealFillAdvanced - Advanced fill seal with 9 filter slots (base is 3).
 * Requires SMART trait.
 *
 * Ported from 1.12.2 (SealFillAdvanced).
 */
public class SealFillAdvanced extends SealFill implements ISealConfigToggles {

    private Identifier icon;

    public SealFillAdvanced() {
        icon = Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "items/seals/seal_fill_advanced");
    }

    @Override
    public String getKey() {
        return "thaumcraft:fill_advanced";
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
