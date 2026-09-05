package thaumcraft.api.research;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * ResearchEntry: pure data + parent-reference string logic (prefix/postfix
 * stripping, meta flags). Runs in a plain JVM.
 */
public class ResearchEntryTest {

    @Test
    public void getParents_returns_stored_array() {
        ResearchEntry e = new ResearchEntry();
        e.setParents(new String[]{"alpha", "beta"});
        String[] p = e.getParents();
        assertEquals(2, p.length);
        assertEquals("alpha", p[0]);
        assertEquals("beta", p[1]);
    }

    @Test
    public void getParentsClean_strips_tilde_prefix_and_at_postfix() {
        ResearchEntry e = new ResearchEntry();
        e.setParents(new String[]{"~foo@bar", "plain", "x@y@z"});
        String[] out = e.getParentsClean();
        assertEquals("foo", out[0]);      // ~ prefix stripped, @bar stripped
        assertEquals("plain", out[1]);    // unchanged
        assertEquals("x", out[2]);        // first @ wins -> "x"
    }

    @Test
    public void getParentsStripped_strips_only_tilde_prefix() {
        ResearchEntry e = new ResearchEntry();
        e.setParents(new String[]{"~foo", "bar@baz"});
        String[] out = e.getParentsStripped();
        assertEquals("foo", out[0]);      // ~ removed
        assertEquals("bar@baz", out[1]);  // @ kept (only ~ is a prefix)
    }

    @Test
    public void null_parents_yield_null() {
        ResearchEntry e = new ResearchEntry();
        assertNull(e.getParentsClean());
        assertNull(e.getParentsStripped());
    }

    @Test
    public void hasMeta_checks_flag_presence() {
        ResearchEntry e = new ResearchEntry();
        e.setMeta(new ResearchEntry.EnumResearchMeta[]{
            ResearchEntry.EnumResearchMeta.ROUND,
            ResearchEntry.EnumResearchMeta.SPIKY
        });
        assertTrue(e.hasMeta(ResearchEntry.EnumResearchMeta.ROUND));
        assertTrue(e.hasMeta(ResearchEntry.EnumResearchMeta.SPIKY));
        assertFalse(e.hasMeta(ResearchEntry.EnumResearchMeta.HEX));
    }

    @Test
    public void hasMeta_false_when_no_meta_set() {
        ResearchEntry e = new ResearchEntry();
        assertFalse(e.hasMeta(ResearchEntry.EnumResearchMeta.ROUND));
    }

    @Test
    public void basic_accessors_round_trip() {
        ResearchEntry e = new ResearchEntry();
        e.setKey("my_entry");
        e.setCategory("my_category");
        e.setName("My Entry");
        e.setDisplayColumn(3);
        e.setDisplayRow(7);
        e.setSiblings(new String[]{"sib1", "sib2"});
        assertEquals("my_entry", e.getKey());
        assertEquals("my_category", e.getCategory());
        assertEquals("My Entry", e.getName());
        assertEquals(3, e.getDisplayColumn());
        assertEquals(7, e.getDisplayRow());
        assertEquals(2, e.getSiblings().length);
    }

    @Test
    public void enum_meta_has_all_six_flags() {
        assertEquals(6, ResearchEntry.EnumResearchMeta.values().length);
        assertNotNull(ResearchEntry.EnumResearchMeta.valueOf("ROUND"));
        assertNotNull(ResearchEntry.EnumResearchMeta.valueOf("SPIKY"));
        assertNotNull(ResearchEntry.EnumResearchMeta.valueOf("REVERSE"));
        assertNotNull(ResearchEntry.EnumResearchMeta.valueOf("HIDDEN"));
        assertNotNull(ResearchEntry.EnumResearchMeta.valueOf("AUTOUNLOCK"));
        assertNotNull(ResearchEntry.EnumResearchMeta.valueOf("HEX"));
    }
}
