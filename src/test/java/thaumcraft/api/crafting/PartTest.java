package thaumcraft.api.crafting;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Part: a single cell in a DustTrigger multiblock blueprint.
 * Fields: source, target, opp, priority, applyPlayerFacing.
 */
public class PartTest {
    @Test
    public void two_arg_constructor_defaults_opp_false_priority_50() {
        Part p = new Part("air", null);
        assertEquals("air", p.getSource());
        assertNull(p.getTarget());
        assertFalse(p.isOpp());
        assertEquals(50, p.getPriority());
        assertFalse(p.getApplyPlayerFacing());
    }

    @Test
    public void three_arg_constructor_sets_opp() {
        Part p = new Part("source", "target", true);
        assertEquals("source", p.getSource());
        assertEquals("target", p.getTarget());
        assertTrue(p.isOpp());
        assertEquals(50, p.getPriority());
    }

    @Test
    public void four_arg_constructor_sets_priority() {
        Part p = new Part("s", "t", false, 7);
        assertEquals(7, p.getPriority());
        assertFalse(p.isOpp());
    }

    @Test
    public void setters_work() {
        Part p = new Part("s", null, false, 50);
        p.setSource("new_source");
        p.setTarget("new_target");
        p.setOpp(true);
        p.setPriority(3);
        assertEquals("new_source", p.getSource());
        assertEquals("new_target", p.getTarget());
        assertTrue(p.isOpp());
        assertEquals(3, p.getPriority());
    }

    @Test
    public void setApplyPlayerFacing_returns_self_and_sets() {
        Part p = new Part("s", null, false, 50);
        Part returned = p.setApplyPlayerFacing(true);
        assertSame(p, returned);
        assertTrue(p.getApplyPlayerFacing());
        p.setApplyPlayerFacing(false);
        assertFalse(p.getApplyPlayerFacing());
    }
}
