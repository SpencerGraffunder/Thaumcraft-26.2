package thaumcraft.common.golems.seals;

import net.minecraft.resources.Identifier;
import thaumcraft.Thaumcraft;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.seals.ISealConfigToggles;

/**
 * SealBreakerAdvanced - Advanced version of the Breaker seal.
 * 
 * Extends the basic breaker seal with:
 * - 9-slot filter (vs 1-slot)
 * - Silk touch option (harvest blocks without destroying them)
 * - Requires BREAKER + SMART traits
 * 
 * Ported from 1.12.2.
 */
public class SealBreakerAdvanced extends SealBreaker {
    
    private Identifier iconAdvanced;
    protected SealToggle[] propsAdvanced;
    
    public SealBreakerAdvanced() {
        super();
        iconAdvanced = Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "items/seals/seal_breaker_advanced");
        propsAdvanced = new SealToggle[] {
            new SealToggle(true, "pmeta", "golem.prop.meta"),
            new SealToggle(false, "psilk", "golem.prop.silk")  // Silk touch mode
        };
    }
    
    @Override
    public String getKey() {
        return "thaumcraft:breaker_advanced";
    }
    
    @Override
    public int getFilterSize() {
        return 9;
    }
    
    @Override
    public Identifier getSealIcon() {
        return iconAdvanced;
    }
    
    @Override
    public SealToggle[] getToggles() {
        return propsAdvanced;
    }
    
    @Override
    public void setToggle(int index, boolean value) {
        if (index >= 0 && index < propsAdvanced.length) {
            propsAdvanced[index].setValue(value);
        }
    }
    
    @Override
    public EnumGolemTrait[] getRequiredTags() {
        return new EnumGolemTrait[] { EnumGolemTrait.BREAKER, EnumGolemTrait.SMART };
    }
    
    /**
     * Check if silk touch mode is enabled.
     */
    public boolean isSilkTouch() {
        return propsAdvanced[1].getValue();
    }
}
