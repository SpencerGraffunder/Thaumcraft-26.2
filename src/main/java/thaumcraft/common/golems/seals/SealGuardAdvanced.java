package thaumcraft.common.golems.seals;

import net.minecraft.resources.Identifier;
import thaumcraft.Thaumcraft;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.seals.ISealConfigToggles;

/**
 * SealGuardAdvanced - Advanced guard seal.
 * Requires FIGHTER + SMART traits.
 *
 * Ported from 1.12.2 (SealGuardAdvanced).
 */
public class SealGuardAdvanced extends SealGuard implements ISealConfigToggles {

    private Identifier icon;

    public SealGuardAdvanced() {
        icon = Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "items/seals/seal_guard_advanced");
    }

    @Override
    public String getKey() {
        return "thaumcraft:guard_advanced";
    }

    @Override
    public Identifier getSealIcon() {
        return icon;
    }

    @Override
    public int[] getGuiCategories() {
        return new int[] { 2, 3, 0, 4 };
    }

    @Override
    public EnumGolemTrait[] getRequiredTags() {
        return new EnumGolemTrait[] { EnumGolemTrait.FIGHTER, EnumGolemTrait.SMART };
    }
}
