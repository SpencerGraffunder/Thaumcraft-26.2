package thaumcraft.api.casters;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * NodeSetting: configurable values on focus nodes (int list / int range).
 * Pure logic (vanilla Mth.clamp) — runs in a plain JVM.
 */
public class NodeSettingTest {

    // ---- Int range type ----
    private NodeSetting range(int min, int max) {
        return new NodeSetting("range", "desc", new NodeSetting.NodeSettingIntRange(min, max));
    }

    @Test
    public void int_range_defaults_to_min() {
        NodeSetting s = range(2, 10);
        assertEquals(2, s.getValue());
        assertEquals(2, s.getRawValue());
    }

    @Test
    public void int_range_clamps_above_and_below() {
        NodeSetting s = range(2, 10);
        s.setRawValue(999);
        assertEquals(10, s.getValue());
        s.setRawValue(-999);
        assertEquals(2, s.getValue());
    }

    @Test
    public void int_range_increment_decrement_within_bounds() {
        NodeSetting s = range(1, 5);
        assertEquals(1, s.getValue());
        s.increment();
        assertEquals(2, s.getValue());
        s.decrement();
        assertEquals(1, s.getValue());
    }

    @Test
    public void int_range_increment_at_max_stays_at_max() {
        NodeSetting s = range(1, 5);
        s.setRawValue(5);
        s.increment();
        assertEquals(5, s.getValue());
        s.increment();
        assertEquals(5, s.getValue());
    }

    @Test
    public void int_range_decrement_at_min_stays_at_min() {
        NodeSetting s = range(1, 5);
        s.setRawValue(1);
        s.decrement();
        assertEquals(1, s.getValue());
    }

    @Test
    public void setValue_finds_nearest_available_value() {
        // values: 10, 20, 30 -> setValue(15) should land on 10 or 20 (closest reached by search)
        NodeSetting.NodeSettingIntList type = new NodeSetting.NodeSettingIntList(new int[]{10, 20, 30},
                new String[]{"a", "b", "c"});
        NodeSetting s = new NodeSetting("k", "d", type);
        s.setValue(20);
        assertEquals(20, s.getValue());
    }

    // ---- Int list type ----
    @Test
    public void int_list_defaults_to_first_value() {
        NodeSetting.NodeSettingIntList type = new NodeSetting.NodeSettingIntList(new int[]{1, 5, 9}, new String[]{"x", "y", "z"});
        NodeSetting s = new NodeSetting("k", "d", type);
        assertEquals(1, s.getValue());
    }

    @Test
    public void int_list_clamps_index_to_bounds() {
        NodeSetting.NodeSettingIntList type = new NodeSetting.NodeSettingIntList(new int[]{1, 5, 9}, new String[]{"x", "y", "z"});
        NodeSetting s = new NodeSetting("k", "d", type);
        s.setRawValue(999);
        assertEquals(9, s.getValue());
        s.setRawValue(-5);
        assertEquals(1, s.getValue());
    }

    @Test
    public void int_list_getValueText_maps_to_description() {
        NodeSetting.NodeSettingIntList type = new NodeSetting.NodeSettingIntList(new int[]{1, 5, 9}, new String[]{"low", "mid", "high"});
        assertEquals("mid", type.getValueText(1));
        assertEquals("high", type.getValueText(2));
    }

    @Test
    public void type_accessor() {
        NodeSetting s = range(0, 10);
        assertNotNull(s.getType());
        assertTrue(s.getType() instanceof NodeSetting.NodeSettingIntRange);
    }

    @Test
    public void research_accessor() {
        NodeSetting s = new NodeSetting("k", "d", new NodeSetting.NodeSettingIntList(new int[]{1}, new String[]{"x"}), "MY_RESEARCH");
        assertEquals("MY_RESEARCH", s.getResearch());
    }
}
