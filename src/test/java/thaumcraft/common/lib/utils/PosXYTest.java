package thaumcraft.common.lib.utils;

import org.junit.Test;
import static org.junit.Assert.*;

public class PosXYTest {
    @Test public void constructor_sets_fields() {
        PosXY p = new PosXY(3, 7);
        assertEquals(3, p.x);
        assertEquals(7, p.y);
    }

    @Test public void copy_constructor() {
        PosXY a = new PosXY(1, 2);
        PosXY b = new PosXY(a);
        assertEquals(a, b);
    }

    @Test public void equality_and_hashcode() {
        PosXY a = new PosXY(5, 9);
        PosXY b = new PosXY(5, 9);
        assertNotEquals(new PosXY(5, 10), a);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test public void distance_squared() {
        PosXY p = new PosXY(1, 1);
        assertEquals(5.0f, p.getDistanceSquared(3, 2), 0.0001f); // dx=2, dy=1 -> 4+1=5
    }

    @Test public void comparable_orders_by_y_then_x() {
        PosXY a = new PosXY(9, 1);
        PosXY b = new PosXY(2, 1);
        PosXY c = new PosXY(0, 0);
        assertTrue(a.compareTo(b) > 0);
        assertTrue(b.compareTo(c) > 0);
        assertTrue(c.compareTo(b) < 0);
    }
}
