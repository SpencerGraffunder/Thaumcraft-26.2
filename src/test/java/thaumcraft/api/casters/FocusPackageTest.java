package thaumcraft.api.casters;

import net.minecraft.resources.Identifier;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.casters.FocusNode.EnumSupplyType;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Regression tests for FocusPackage.getComplexity() (recently fixed to sum each
 * node's declared complexity instead of a flat 5/node).
 */
public class FocusPackageTest {

    /** Minimal concrete focus node with a configurable complexity. */
    static class TestNode extends FocusNode {
        private final int cx;
        TestNode(int cx) { this.cx = cx; }
        @Override public int getComplexity() { return cx; }
        @Override public Aspect getAspect() {
            return new Aspect("t_fp_aspect", 0x111111, null,
                Identifier.fromNamespaceAndPath("thaumcraft", "a.png"), 1);
        }
        @Override public EnumSupplyType[] mustBeSupplied() { return null; }
        @Override public EnumSupplyType[] willSupply() { return new EnumSupplyType[]{ EnumSupplyType.TARGET }; }
        @Override public String getKey() { return "t_fp_node"; }
        @Override public String getResearch() { return "t_fp_research"; }
        @Override public EnumUnitType getType() { return EnumUnitType.EFFECT; }
    }

    /** Plain element that is NOT a FocusNode (exercises the +5 fallback). */
    static class TestMedium implements IFocusElement {
        @Override public String getKey() { return "t_fp_medium"; }
        @Override public String getResearch() { return "t_fp_research"; }
        @Override public EnumUnitType getType() { return EnumUnitType.MEDIUM; }
    }

    @Test
    public void empty_package_has_minimum_complexity_one() {
        assertEquals(1, new FocusPackage().getComplexity());
    }

    @Test
    public void sums_per_node_complexity_not_flat_five() {
        // Mine=4, SpellBat=8, Touch=2 style: sum of declared values.
        FocusPackage p = new FocusPackage();
        p.nodes.add(new TestNode(4));
        p.nodes.add(new TestNode(8));
        p.nodes.add(new TestNode(2));
        assertEquals(4 + 8 + 2, p.getComplexity());
    }

    @Test
    public void node_with_zero_complexity_counts_as_one() {
        FocusPackage p = new FocusPackage();
        p.nodes.add(new TestNode(0));
        assertEquals(1, p.getComplexity()); // Math.max(1, 0)
    }

    @Test
    public void non_focus_node_element_uses_five_fallback() {
        FocusPackage p = new FocusPackage();
        p.nodes.add(new TestMedium());
        assertEquals(5, p.getComplexity());
    }

    @Test
    public void complexity_is_cached_across_calls() {
        FocusPackage p = new FocusPackage();
        p.nodes.add(new TestNode(3));
        assertEquals(3, p.getComplexity());
        assertEquals(3, p.getComplexity()); // same result on re-call
    }
}
