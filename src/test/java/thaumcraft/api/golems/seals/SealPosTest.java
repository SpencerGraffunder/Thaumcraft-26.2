package thaumcraft.api.golems.seals;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * SealPos: a seal's world position + face, plus its long encoding for storage.
 * Pure logic — runs in a plain JVM.
 */
public class SealPosTest {

    @Test
    public void stores_position_and_face() {
        SealPos sp = new SealPos(new BlockPos(1, 2, 3), Direction.UP);
        assertEquals(new BlockPos(1, 2, 3), sp.pos);
        assertEquals(Direction.UP, sp.face);
    }

    @Test
    public void equality_by_position_and_face() {
        SealPos a = new SealPos(new BlockPos(1, 2, 3), Direction.UP);
        SealPos b = new SealPos(new BlockPos(1, 2, 3), Direction.UP);
        SealPos diffPos = new SealPos(new BlockPos(1, 2, 4), Direction.UP);
        SealPos diffFace = new SealPos(new BlockPos(1, 2, 3), Direction.DOWN);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, diffPos);
        assertNotEquals(a, diffFace);
        assertNotEquals(a, new Object());
    }

    @Test
    public void toLong_fromLong_round_trips_small_positions() {
        // Coordinates small enough that the position fits in the lower bits.
        for (Object[] c : new Object[][] {
            {1, 2, 3, Direction.UP},
            {10, 20, 30, Direction.DOWN},
            {0, 64, 0, Direction.NORTH},
            {-5, 10, -7, Direction.EAST}
        }) {
            BlockPos pos = new BlockPos((Integer) c[0], (Integer) c[1], (Integer) c[2]);
            Direction face = (Direction) c[3];
            SealPos original = new SealPos(pos, face);
            long packed = original.toLong();
            SealPos restored = SealPos.fromLong(packed);
            assertEquals("position for " + pos, pos, restored.pos);
            assertEquals("face for " + face, face, restored.face);
        }
    }

    @Test
    public void toString_contains_position_and_face() {
        String s = new SealPos(new BlockPos(1, 2, 3), Direction.UP).toString();
        // SealPos.toString = "SealPos{pos=<BlockPos>, face=<face>}"
        assertTrue("should include 'pos=': " + s, s.contains("pos="));
        assertTrue("should include 'face=': " + s, s.contains("face="));
    }
}
