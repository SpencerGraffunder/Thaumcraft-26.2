package thaumcraft.api.golems.tasks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import thaumcraft.api.golems.seals.SealPos;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Golem Task: state accessors, lifespan bookkeeping, and identity.
 * Uses the block-position constructor (no Entity/Level needed) — plain JVM.
 */
public class TaskTest {

    private Task blockTask(int x, int y, int z) {
        return new Task(new SealPos(new BlockPos(x, y, z), Direction.UP), new BlockPos(x, y, z));
    }

    @Test
    public void block_task_defaults() {
        Task t = blockTask(1, 2, 3);
        assertEquals(Task.TYPE_BLOCK, t.getType());
        assertEquals(300, t.getLifespan());
        assertFalse(t.isCompleted());
        assertFalse(t.isReserved());
        assertFalse(t.isSuspended());
        assertEquals(new BlockPos(1, 2, 3), t.getPos());
        assertNotNull(t.getId());
    }

    @Test
    public void completion_sets_flag_and_extends_lifespan() {
        Task t = blockTask(0, 0, 0);
        long before = t.getLifespan();
        t.setCompletion(true);
        assertTrue(t.isCompleted());
        assertEquals(before + 1, t.getLifespan());
    }

    @Test
    public void reserved_sets_flag_and_extends_lifespan() {
        Task t = blockTask(0, 0, 0);
        long before = t.getLifespan();
        t.setReserved(true);
        assertTrue(t.isReserved());
        assertEquals(before + 120, t.getLifespan());
    }

    @Test
    public void priority_accessor() {
        Task t = blockTask(0, 0, 0);
        assertEquals((byte) 0, t.getPriority());
        t.setPriority((byte) 5);
        assertEquals((byte) 5, t.getPriority());
    }

    @Test
    public void golem_uuid_accessor() {
        Task t = blockTask(0, 0, 0);
        assertNull(t.getGolemUUID());
        java.util.UUID u = java.util.UUID.randomUUID();
        t.setGolemUUID(u);
        assertEquals(u, t.getGolemUUID());
    }

    @Test
    public void data_accessor() {
        Task t = blockTask(0, 0, 0);
        assertEquals(0, t.getData());
        t.setData(42);
        assertEquals(42, t.getData());
    }

    @Test
    public void suspended_accessor() {
        Task t = blockTask(0, 0, 0);
        assertFalse(t.isSuspended());
        t.setSuspended(true);
        assertTrue(t.isSuspended());
    }

    @Test
    public void sealPos_is_stored() {
        Task t = blockTask(7, 8, 9);
        assertNotNull(t.getSealPos());
    }

    @Test
    public void equals_is_reflexive_by_id() {
        Task t = blockTask(1, 1, 1);
        assertEquals(t, t);
        assertEquals(t.hashCode(), t.hashCode());
    }

    @Test
    public void not_equal_to_a_different_task() {
        Task a = blockTask(1, 1, 1);
        Task b = blockTask(9, 9, 9);
        assertNotEquals(a, b);
    }

    @Test
    public void type_constants_are_distinct() {
        assertNotEquals(Task.TYPE_BLOCK, Task.TYPE_ENTITY);
    }
}
