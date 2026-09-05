package thaumcraft.api.aspects;

import net.minecraft.resources.Identifier;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Aspect registration & lookup tests.
 * Uses the 5-arg constructor (no Thaumcraft-class dependency) so it runs in a plain JVM.
 */
public class AspectTest {
    private static Aspect mk(String tag, int color) {
        return new Aspect(tag, color, null, Identifier.fromNamespaceAndPath("thaumcraft", "aspect.png"), 1);
    }

    @Test
    public void registration_registers_and_returns_aspect() {
        Aspect air = mk("test_air", 0x778899);
        assertEquals("test_air", air.getTag());
        assertEquals(0x778899, air.getColor());
        assertSame(air, Aspect.getAspect("test_air"));
    }

    @Test
    public void getAspect_unknown_returns_null() {
        assertNull(Aspect.getAspect("definitely_not_registered_xyz"));
    }

    @Test
    public void name_is_capitalized_tag() {
        Aspect a = mk("test_wind", 0x999999);
        // WordUtils.capitalizeFully capitalizes the first letter of each
        // non-alphanumeric-delimited "word"; underscores are kept.
        assertEquals("Test_wind", a.getName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void duplicate_registration_throws() {
        mk("test_dup", 0x111111);
        mk("test_dup", 0x222222); // second registration must fail
    }

    @Test
    public void components_are_stored() {
        Aspect a = mk("test_c1", 0x111111);
        Aspect b = mk("test_c2", 0x222222);
        Aspect comp = new Aspect("test_comp", 0x333333, new Aspect[]{a, b},
                Identifier.fromNamespaceAndPath("thaumcraft", "aspect.png"), 1);
        Aspect[] comps = comp.getComponents();
        assertNotNull(comps);
        assertEquals(2, comps.length);
        assertSame(a, comps[0]);
        assertSame(b, comps[1]);
    }

    @Test
    public void compound_is_added_to_mixture_list() {
        Aspect a = mk("test_mix_a", 0x111111);
        Aspect b = mk("test_mix_b", 0x222222);
        Aspect comp = new Aspect("test_mix_c", 0x333333, new Aspect[]{a, b},
                Identifier.fromNamespaceAndPath("thaumcraft", "aspect.png"), 1);
        // mixList is keyed by (c1.tag + c2.tag).hashCode()
        int hash = ("test_mix_a" + "test_mix_b").hashCode();
        assertSame(comp, Aspect.mixList.get(hash));
    }

    @Test
    public void blend_is_stored() {
        Aspect a = new Aspect("test_blend", 0x111111, null,
                Identifier.fromNamespaceAndPath("thaumcraft", "aspect.png"), 771);
        assertEquals(771, a.getBlend());
    }

    @Test
    public void image_is_stored() {
        Identifier id = Identifier.fromNamespaceAndPath("thaumcraft", "textures/aspects/test.png");
        Aspect a = new Aspect("test_img", 0x111111, null, id, 1);
        assertEquals(id, a.getImage());
    }
}
