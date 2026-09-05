package thaumcraft.api.aspects;

import net.minecraft.resources.Identifier;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * AspectList collection logic: add/merge/remove/reduce/contains/sort.
 * Runs in a plain JVM (only MC types used are Identifier, which is a value class).
 */
public class AspectListTest {
    private static Aspect A, B, C;

    private static Aspect mk(String tag) {
        return new Aspect(tag, 0x123456, null, Identifier.fromNamespaceAndPath("thaumcraft", "a.png"), 1);
    }

    @BeforeClass
    public static void setup() {
        A = mk("t_al_a");
        B = mk("t_al_b");
        C = mk("t_al_c");
    }

    @Test
    public void add_accumulates_for_existing_aspect() {
        AspectList l = new AspectList();
        l.add(A, 3);
        l.add(A, 2);
        assertEquals(5, l.getAmount(A));
        assertEquals(1, l.size());
        assertEquals(5, l.visSize());
    }

    @Test
    public void size_counts_distinct_aspects_visSize_sums_amounts() {
        AspectList l = new AspectList();
        l.add(A, 4).add(B, 6);
        assertEquals(2, l.size());
        assertEquals(10, l.visSize());
    }

    @Test
    public void merge_keeps_max() {
        AspectList l = new AspectList();
        l.add(A, 3);
        l.merge(A, 5);  // higher -> 5
        assertEquals(5, l.getAmount(A));
        l.merge(A, 2);  // lower -> stays 5
        assertEquals(5, l.getAmount(A));
    }

    @Test
    public void reduce_success_and_failure() {
        AspectList l = new AspectList();
        l.add(A, 10);
        assertTrue(l.reduce(A, 4));
        assertEquals(6, l.getAmount(A));
        assertFalse(l.reduce(A, 99)); // not enough
        assertEquals(6, l.getAmount(A)); // unchanged
    }

    @Test
    public void remove_partial_and_full() {
        AspectList l = new AspectList();
        l.add(A, 5);
        l.remove(A, 2);
        assertEquals(3, l.getAmount(A));
        assertEquals(1, l.size());
        l.remove(A, 3);
        assertEquals(0, l.getAmount(A));
        assertEquals(0, l.size()); // removed when it hits 0
    }

    @Test
    public void remove_whole_aspect() {
        AspectList l = new AspectList();
        l.add(A, 5).add(B, 1);
        l.remove(A);
        assertFalse(l.contains(A));
        assertEquals(1, l.size());
    }

    @Test
    public void contains_aspect_and_list() {
        AspectList l = new AspectList();
        l.add(A, 5).add(B, 5);
        assertTrue(l.contains(A));
        assertFalse(l.contains(C));

        AspectList req = new AspectList();
        req.add(A, 3).add(B, 5);
        assertTrue(l.contains(req));       // A>=3, B>=5
        req.add(A, 9);
        assertFalse(l.contains(req));      // A<9
    }

    @Test
    public void add_and_remove_lists() {
        AspectList base = new AspectList();
        AspectList other = new AspectList();
        other.add(A, 3).add(B, 2);
        base.add(other);
        assertEquals(3, base.getAmount(A));
        assertEquals(2, base.getAmount(B));
        base.remove(other);
        assertEquals(0, base.size());
    }

    @Test
    public void copy_is_independent() {
        AspectList l = new AspectList();
        l.add(A, 3);
        AspectList c = l.copy();
        assertEquals(3, c.getAmount(A));
        c.add(A, 2);
        assertEquals(5, c.getAmount(A));
        assertEquals(3, l.getAmount(A)); // original untouched
    }

    @Test
    public void sorted_by_name_is_ascending() {
        AspectList l = new AspectList();
        l.add(C, 1).add(A, 1).add(B, 1); // added c,a,b
        Aspect[] out = l.getAspectsSortedByName();
        assertEquals("t_al_a", out[0].getTag());
        assertEquals("t_al_b", out[1].getTag());
        assertEquals("t_al_c", out[2].getTag());
    }

    @Test
    public void sorted_by_amount_is_descending() {
        AspectList l = new AspectList();
        l.add(A, 1).add(B, 10).add(C, 5);
        Aspect[] out = l.getAspectsSortedByAmount();
        assertEquals(10, l.getAmount(out[0]));
        assertEquals(5, l.getAmount(out[1]));
        assertEquals(1, l.getAmount(out[2]));
    }

    @Test
    public void add_null_aspect_is_ignored() {
        AspectList l = new AspectList();
        l.add(null, 5);
        assertEquals(0, l.size());
    }
}
