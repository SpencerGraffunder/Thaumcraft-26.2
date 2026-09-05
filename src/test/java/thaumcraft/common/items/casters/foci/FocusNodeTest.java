package thaumcraft.common.items.casters.foci;

import org.junit.Assert;
import org.junit.Test;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.casters.FocusNode;
import thaumcraft.api.casters.FocusNode.EnumSupplyType;

import java.util.Arrays;

/**
 * Unit tests for the 20 focus nodes' PURE-LOGIC surface (no MC runtime required):
 * complexity, aspect, supply/requirement contracts, exclusivity, power multipliers,
 * and split counts. All expected values were captured by running the foci in a plain
 * JVM and are asserted here so any regression (e.g. a copy-pasted complexity or
 * flipped will/must array) is caught.
 */
public class FocusNodeTest {

    private static FocusNode[] ALL = {
        new FocusEffectAir(), new FocusEffectBreak(), new FocusEffectCurse(), new FocusEffectEarth(),
        new FocusEffectExchange(), new FocusEffectFire(), new FocusEffectFlux(), new FocusEffectFrost(),
        new FocusEffectHeal(), new FocusEffectRift(),
        new FocusMediumBolt(), new FocusMediumCloud(), new FocusMediumMine(), new FocusMediumPlan(),
        new FocusMediumProjectile(), new FocusMediumSpellBat(), new FocusMediumTouch(),
        new FocusModScatter(), new FocusModSplitTarget(), new FocusModSplitTrajectory()
    };

    private static String arr(EnumSupplyType[] a) { return a == null ? "null" : Arrays.toString(a); }

    // ---- individual exact-value assertions (regression guards) ----

    @Test
    public void effectFoci_complexityAndAspect() {
        Assert.assertEquals(2, new FocusEffectAir().getComplexity());
        Assert.assertEquals("aer", new FocusEffectAir().getAspect().getTag());
        Assert.assertEquals(3, new FocusEffectBreak().getComplexity());
        Assert.assertEquals("perditio", new FocusEffectBreak().getAspect().getTag());
        Assert.assertEquals(4, new FocusEffectCurse().getComplexity());
        Assert.assertEquals("mortuus", new FocusEffectCurse().getAspect().getTag());
        Assert.assertEquals(3, new FocusEffectEarth().getComplexity());
        Assert.assertEquals("terra", new FocusEffectEarth().getAspect().getTag());
        Assert.assertEquals(5, new FocusEffectExchange().getComplexity());
        Assert.assertEquals("permutatio", new FocusEffectExchange().getAspect().getTag());
        Assert.assertEquals(2, new FocusEffectFire().getComplexity());
        Assert.assertEquals("ignis", new FocusEffectFire().getAspect().getTag());
        Assert.assertEquals(3, new FocusEffectFlux().getComplexity());
        Assert.assertEquals("vitium", new FocusEffectFlux().getAspect().getTag());
        Assert.assertEquals(4, new FocusEffectFrost().getComplexity());
        Assert.assertEquals("gelum", new FocusEffectFrost().getAspect().getTag());
        Assert.assertEquals(4, new FocusEffectHeal().getComplexity());
        Assert.assertEquals("victus", new FocusEffectHeal().getAspect().getTag());
        Assert.assertEquals(6, new FocusEffectRift().getComplexity());
        Assert.assertEquals("alienis", new FocusEffectRift().getAspect().getTag());
    }

    @Test
    public void effectFoci_areTargets_notSuppliers() {
        // Every effect focus must be supplied a TARGET and supplies nothing itself.
        for (FocusNode n : Arrays.asList(
                new FocusEffectAir(), new FocusEffectBreak(), new FocusEffectCurse(), new FocusEffectEarth(),
                new FocusEffectExchange(), new FocusEffectFire(), new FocusEffectFlux(),
                new FocusEffectFrost(), new FocusEffectHeal(), new FocusEffectRift())) {
            Assert.assertEquals("effect must require TARGET",
                    Arrays.toString(new EnumSupplyType[]{EnumSupplyType.TARGET}), arr(n.mustBeSupplied()));
            Assert.assertTrue("effect supplies nothing", n.willSupply() == null || n.willSupply().length == 0);
            Assert.assertFalse(n.canSupply(EnumSupplyType.TARGET));
            Assert.assertFalse(n.canSupply(EnumSupplyType.TRAJECTORY));
        }
    }

    @Test
    public void mediumFoci_supplyContracts() {
        // Medium foci that supply both targets and trajectories:
        for (FocusNode n : Arrays.asList(new FocusMediumBolt(), new FocusMediumMine(),
                new FocusMediumProjectile(), new FocusMediumTouch())) {
            Assert.assertTrue(n.canSupply(EnumSupplyType.TARGET));
            Assert.assertTrue(n.canSupply(EnumSupplyType.TRAJECTORY));
            Assert.assertTrue("must supply both", n.willSupply() != null && n.willSupply().length == 2);
            Assert.assertTrue("supplies TARGET", n.willSupply() != null && Arrays.asList(n.willSupply()).contains(EnumSupplyType.TARGET));
            Assert.assertTrue("supplies TRAJECTORY", n.willSupply() != null && Arrays.asList(n.willSupply()).contains(EnumSupplyType.TRAJECTORY));
        }
        // Medium foci that only supply targets (need a trajectory from a parent):
        for (FocusNode n : Arrays.asList(new FocusMediumCloud(), new FocusMediumPlan(),
                new FocusMediumSpellBat())) {
            Assert.assertTrue(n.canSupply(EnumSupplyType.TARGET));
            Assert.assertFalse(n.canSupply(EnumSupplyType.TRAJECTORY));
            Assert.assertEquals(Arrays.toString(new EnumSupplyType[]{EnumSupplyType.TRAJECTORY}), arr(n.mustBeSupplied()));
        }
    }

    @Test
    public void mediumFoci_complexityAndAspect() {
        Assert.assertEquals(5, new FocusMediumBolt().getComplexity());
        Assert.assertEquals("potentia", new FocusMediumBolt().getAspect().getTag());
        Assert.assertEquals(7, new FocusMediumCloud().getComplexity());
        Assert.assertEquals("alkimia", new FocusMediumCloud().getAspect().getTag());
        Assert.assertEquals(4, new FocusMediumMine().getComplexity());
        Assert.assertEquals("vinculum", new FocusMediumMine().getAspect().getTag());
        Assert.assertEquals(4, new FocusMediumPlan().getComplexity());
        Assert.assertEquals("fabrico", new FocusMediumPlan().getAspect().getTag());
        Assert.assertEquals(4, new FocusMediumProjectile().getComplexity());
        Assert.assertEquals("motus", new FocusMediumProjectile().getAspect().getTag());
        Assert.assertEquals(8, new FocusMediumSpellBat().getComplexity());
        Assert.assertEquals("bestia", new FocusMediumSpellBat().getAspect().getTag());
        Assert.assertEquals(2, new FocusMediumTouch().getComplexity());
        Assert.assertEquals("aversio", new FocusMediumTouch().getAspect().getTag());
    }

    @Test
    public void powerMultipliers() {
        // Default multiplier is 1.0 for most nodes.
        Assert.assertEquals(1.0f, new FocusEffectAir().getPowerMultiplier(), 1e-6f);
        Assert.assertEquals(1.0f, new FocusMediumBolt().getPowerMultiplier(), 1e-6f);
        // Reduced-power mediums:
        Assert.assertEquals(0.5f, new FocusMediumCloud().getPowerMultiplier(), 1e-6f);
        // SpellBat: 1/3 power
        Assert.assertEquals(0.33f, new FocusMediumSpellBat().getPowerMultiplier(), 0.01f);
        // Scatter modifier is full power:
        Assert.assertEquals(1.0f, new FocusModScatter().getPowerMultiplier(), 1e-6f);
    }

    @Test
    public void exclusivity() {
        Assert.assertTrue("Plan is exclusive", new FocusMediumPlan().isExclusive());
        Assert.assertTrue("Scatter is exclusive", new FocusModScatter().isExclusive());
        Assert.assertFalse("SpellBat is not exclusive", new FocusMediumSpellBat().isExclusive());
        Assert.assertFalse("Bolt is not exclusive", new FocusMediumBolt().isExclusive());
    }

    @Test
    public void splitModifiers() {
        FocusModSplitTarget t = new FocusModSplitTarget();
        Assert.assertEquals("perditio", t.getAspect().getTag());
        Assert.assertEquals(2, t.getSplitCount());
        Assert.assertEquals(4, t.getComplexity());
        Assert.assertTrue(t.canSupply(EnumSupplyType.TARGET));
        Assert.assertFalse(t.canSupply(EnumSupplyType.TRAJECTORY));

        FocusModSplitTrajectory tr = new FocusModSplitTrajectory();
        Assert.assertEquals("perditio", tr.getAspect().getTag());
        Assert.assertEquals(2, tr.getSplitCount());
        Assert.assertEquals(5, tr.getComplexity());
        Assert.assertTrue(tr.canSupply(EnumSupplyType.TRAJECTORY));
        Assert.assertFalse(tr.canSupply(EnumSupplyType.TARGET));
    }

    @Test
    public void modifierFoci_supplyContracts() {
        FocusModScatter sc = new FocusModScatter();
        // Scatter needs a trajectory, supplies a trajectory.
        Assert.assertTrue(sc.canSupply(EnumSupplyType.TRAJECTORY));
        Assert.assertFalse(sc.canSupply(EnumSupplyType.TARGET));
        Assert.assertEquals(Arrays.toString(new EnumSupplyType[]{EnumSupplyType.TRAJECTORY}), arr(sc.mustBeSupplied()));

        FocusModSplitTarget st = new FocusModSplitTarget();
        Assert.assertTrue(st.canSupply(EnumSupplyType.TARGET));
        Assert.assertFalse(st.canSupply(EnumSupplyType.TRAJECTORY));

        FocusModSplitTrajectory str = new FocusModSplitTrajectory();
        Assert.assertTrue(str.canSupply(EnumSupplyType.TRAJECTORY));
        Assert.assertFalse(str.canSupply(EnumSupplyType.TARGET));
    }

    @Test
    public void allFoci_haveNonNullAspectAndPositiveComplexity() {
        for (FocusNode n : ALL) {
            Assert.assertNotNull(n.getClass().getSimpleName() + " aspect is null", n.getAspect());
            Assert.assertTrue(n.getClass().getSimpleName() + " complexity must be > 0",
                    n.getComplexity() > 0);
        }
    }
}
