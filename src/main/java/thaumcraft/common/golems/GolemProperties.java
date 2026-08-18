package thaumcraft.common.golems;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.IGolemProperties;
import thaumcraft.api.golems.parts.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Implementation of IGolemProperties.
 * Packs all golem component data into a long value for efficient storage.
 * 
 * Byte layout in long:
 * - Byte 0: Material ID
 * - Byte 1: Head ID
 * - Byte 2: Arms ID
 * - Byte 3: Legs ID
 * - Byte 4: Addon ID
 * - Byte 5: Rank
 * - Bytes 6-7: Reserved
 */
public class GolemProperties implements IGolemProperties {

    private long data = 0L;
    private Set<EnumGolemTrait> traitCache = null;

    public GolemProperties() {
    }

    @Override
    public Set<EnumGolemTrait> getTraits() {
        if (traitCache == null) {
            traitCache = new HashSet<>();
            for (EnumGolemTrait trait : getMaterial().traits) {
                addTraitSmart(trait);
            }
            for (EnumGolemTrait trait : getHead().traits) {
                addTraitSmart(trait);
            }
            for (EnumGolemTrait trait : getArms().traits) {
                addTraitSmart(trait);
            }
            for (EnumGolemTrait trait : getLegs().traits) {
                addTraitSmart(trait);
            }
            for (EnumGolemTrait trait : getAddon().traits) {
                addTraitSmart(trait);
            }
        }
        return traitCache;
    }

    /**
     * Add a trait, handling opposing traits (they cancel each other out)
     */
    private void addTraitSmart(EnumGolemTrait trait) {
        if (trait.opposite != null && traitCache.contains(trait.opposite)) {
            traitCache.remove(trait.opposite);
        } else {
            traitCache.add(trait);
        }
    }

    @Override
    public boolean hasTrait(EnumGolemTrait trait) {
        return getTraits().contains(trait);
    }

    // ==================== Component Accessors ====================

    @Override
    public void setMaterial(GolemMaterial mat) {
        data = setByteInLong(data, mat.id, 0);
        traitCache = null;
    }

    @Override
    public GolemMaterial getMaterial() {
        return GolemMaterial.getById(getByteInLong(data, 0));
    }

    @Override
    public void setHead(GolemHead head) {
        data = setByteInLong(data, head.id, 1);
        traitCache = null;
    }

    @Override
    public GolemHead getHead() {
        return GolemHead.getById(getByteInLong(data, 1));
    }

    @Override
    public void setArms(GolemArm arms) {
        data = setByteInLong(data, arms.id, 2);
        traitCache = null;
    }

    @Override
    public GolemArm getArms() {
        return GolemArm.getById(getByteInLong(data, 2));
    }

    @Override
    public void setLegs(GolemLeg legs) {
        data = setByteInLong(data, legs.id, 3);
        traitCache = null;
    }

    @Override
    public GolemLeg getLegs() {
        return GolemLeg.getById(getByteInLong(data, 3));
    }

    @Override
    public void setAddon(GolemAddon addon) {
        data = setByteInLong(data, addon.id, 4);
        traitCache = null;
    }

    @Override
    public GolemAddon getAddon() {
        return GolemAddon.getById(getByteInLong(data, 4));
    }

    @Override
    public void setRank(int rank) {
        data = setByteInLong(data, (byte) rank, 5);
    }

    @Override
    public int getRank() {
        return getByteInLong(data, 5);
    }

    // ==================== Serialization ====================

    @Override
    public long toLong() {
        return data;
    }

    public static IGolemProperties fromLong(long d) {
        GolemProperties out = new GolemProperties();
        out.data = d;
        return out;
    }

    // ==================== Component Generation ====================

    @Override
    public ItemStack[] generateComponents() {
        ArrayList<ItemStack> comps = new ArrayList<>();
        ItemStack base = getMaterial().componentBase;
        ItemStack mech = getMaterial().componentMechanism;
        addToList(comps, base, 2);
        addToList(comps, mech, 1);
        addToListFromComps(comps, getArms().components, getMaterial());
        addToListFromComps(comps, getLegs().components, getMaterial());
        addToListFromComps(comps, getHead().components, getMaterial());
        addToListFromComps(comps, getAddon().components, getMaterial());
        return comps.toArray(new ItemStack[0]);
    }

    private static void addToListFromComps(ArrayList<ItemStack> comps, Object[] objs, GolemMaterial mat) {
        for (Object o : objs) {
            if (o instanceof ItemStack stack) {
                addToList(comps, stack, 1);
            } else if (o instanceof String s) {
                if (s.equalsIgnoreCase("base")) {
                    addToList(comps, mat.componentBase, 1);
                } else if (s.equalsIgnoreCase("mech")) {
                    addToList(comps, mat.componentMechanism, 1);
                }
            }
        }
    }

    private static void addToList(ArrayList<ItemStack> comps, ItemStack newItem, int mult) {
        for (ItemStack stack : comps) {
            if (ItemStack.isSameItemSameTags(stack, newItem)) {
                stack.grow(newItem.getCount() * mult);
                return;
            }
        }
        ItemStack stack2 = newItem.copy();
        stack2.setCount(stack2.getCount() * mult);
        comps.add(stack2);
    }

    // ==================== Byte Utilities ====================

    private static int getByteInLong(long value, int byteIndex) {
        return (int) ((value >> (byteIndex * 8)) & 0xFF);
    }

    private static long setByteInLong(long value, byte newByte, int byteIndex) {
        long mask = ~(0xFFL << (byteIndex * 8));
        return (value & mask) | ((long) (newByte & 0xFF) << (byteIndex * 8));
    }

    // ==================== Static Initialization ====================

    /**
     * Register all default golem parts. Called during mod initialization.
     */
    public static void registerDefaultParts() {
        // Materials
        GolemMaterial.register(new GolemMaterial("WOOD", 
                new String[]{"MATSTUDWOOD"}, 
                Identifier.fromNamespaceAndPath("thaumcraft", "textures/entity/golems/mat_wood.png"), 
                0x4D3B1F, 6, 2, 1, 
                new ItemStack(Blocks.OAK_PLANKS), // TODO: Use greatwood when implemented
                new ItemStack(Items.CLOCK), // TODO: Use mechanismSimple when implemented
                new EnumGolemTrait[]{EnumGolemTrait.LIGHT}));

        GolemMaterial.register(new GolemMaterial("IRON", 
                new String[]{"MATSTUDIRON"}, 
                Identifier.fromNamespaceAndPath("thaumcraft", "textures/entity/golems/mat_iron.png"), 
                0xFFFFFF, 20, 8, 3, 
                new ItemStack(Items.IRON_INGOT), 
                new ItemStack(Items.CLOCK), 
                new EnumGolemTrait[]{EnumGolemTrait.HEAVY, EnumGolemTrait.FIREPROOF, EnumGolemTrait.BLASTPROOF}));

        GolemMaterial.register(new GolemMaterial("CLAY", 
                new String[]{"MATSTUDCLAY"}, 
                Identifier.fromNamespaceAndPath("thaumcraft", "textures/entity/golems/mat_clay.png"), 
                0xC7A37A, 10, 4, 2, 
                new ItemStack(Blocks.TERRACOTTA), 
                new ItemStack(Items.CLOCK), 
                new EnumGolemTrait[]{EnumGolemTrait.FIREPROOF}));

        GolemMaterial.register(new GolemMaterial("BRASS", 
                new String[]{"MATSTUDBRASS"}, 
                Identifier.fromNamespaceAndPath("thaumcraft", "textures/entity/golems/mat_brass.png"), 
                0xEEA04C, 16, 6, 3, 
                new ItemStack(Items.GOLD_INGOT), // TODO: Use brass plate when implemented
                new ItemStack(Items.CLOCK), 
                new EnumGolemTrait[]{EnumGolemTrait.LIGHT}));

        GolemMaterial.register(new GolemMaterial("THAUMIUM", 
                new String[]{"MATSTUDTHAUMIUM"}, 
                Identifier.fromNamespaceAndPath("thaumcraft", "textures/entity/golems/mat_thaumium.png"), 
                0x503A72, 24, 10, 4, 
                new ItemStack(Items.DIAMOND), // TODO: Use thaumium plate when implemented
                new ItemStack(Items.CLOCK), 
                new EnumGolemTrait[]{EnumGolemTrait.HEAVY, EnumGolemTrait.FIREPROOF, EnumGolemTrait.BLASTPROOF}));

        GolemMaterial.register(new GolemMaterial("VOID", 
                new String[]{"MATSTUDVOID"}, 
                Identifier.fromNamespaceAndPath("thaumcraft", "textures/entity/golems/mat_void.png"), 
                0x160929, 20, 6, 4, 
                new ItemStack(Items.OBSIDIAN), // TODO: Use void plate when implemented
                new ItemStack(Items.CLOCK), 
                new EnumGolemTrait[]{EnumGolemTrait.REPAIR}));

        // Heads
        GolemHead.register(new GolemHead("BASIC", 
                new String[]{"MINDCLOCKWORK"}, 
                Identifier.fromNamespaceAndPath("thaumcraft", "textures/misc/golem/head_basic.png"), 
                new PartModel(Identifier.fromNamespaceAndPath("thaumcraft", "models/obj/golem_head_basic.obj"), 
                             null, PartModel.EnumAttachPoint.HEAD), 
                new Object[]{new ItemStack(Items.REDSTONE)}, // TODO: Use clockwork mind
                new EnumGolemTrait[]{}));

        GolemHead.register(new GolemHead("SMART", 
                new String[]{"MINDBIOTHAUMIC"}, 
                Identifier.fromNamespaceAndPath("thaumcraft", "textures/misc/golem/head_smart.png"), 
                new PartModel(Identifier.fromNamespaceAndPath("thaumcraft", "models/obj/golem_head_smart.obj"), 
                             Identifier.fromNamespaceAndPath("thaumcraft", "textures/entity/golems/golem_head_other.png"), 
                             PartModel.EnumAttachPoint.HEAD), 
                new Object[]{new ItemStack(Items.ENDER_PEARL)}, // TODO: Use biothaumic mind
                new EnumGolemTrait[]{EnumGolemTrait.SMART, EnumGolemTrait.FRAGILE}));

        GolemHead.register(new GolemHead("SCOUT", 
                new String[]{"GOLEMVISION"}, 
                Identifier.fromNamespaceAndPath("thaumcraft", "textures/misc/golem/head_scout.png"), 
                new PartModel(Identifier.fromNamespaceAndPath("thaumcraft", "models/obj/golem_head_scout.obj"), 
                             Identifier.fromNamespaceAndPath("thaumcraft", "textures/entity/golems/golem_head_other.png"), 
                             PartModel.EnumAttachPoint.HEAD), 
                new Object[]{new ItemStack(Items.REDSTONE), new ItemStack(Items.SPYGLASS)}, 
                new EnumGolemTrait[]{EnumGolemTrait.SCOUT, EnumGolemTrait.FRAGILE}));

        // Arms
        GolemArm.register(new GolemArm("BASIC", 
                new String[]{"MINDCLOCKWORK"}, 
                Identifier.fromNamespaceAndPath("thaumcraft", "textures/misc/golem/arms_basic.png"), 
                new PartModel(Identifier.fromNamespaceAndPath("thaumcraft", "models/obj/golem_arms_basic.obj"), 
                             null, PartModel.EnumAttachPoint.ARMS), 
                new Object[]{}, 
                new EnumGolemTrait[]{}));

        GolemArm.register(new GolemArm("FINE", 
                new String[]{"MATSTUDBRASS"}, 
                Identifier.fromNamespaceAndPath("thaumcraft", "textures/misc/golem/arms_fine.png"), 
                new PartModel(Identifier.fromNamespaceAndPath("thaumcraft", "models/obj/golem_arms_fine.obj"), 
                             null, PartModel.EnumAttachPoint.ARMS), 
                new Object[]{new ItemStack(Items.CLOCK), "base"}, 
                new EnumGolemTrait[]{EnumGolemTrait.DEFT, EnumGolemTrait.FRAGILE}));

        GolemArm.register(new GolemArm("CLAWS", 
                new String[]{"GOLEMCOMBATADV"}, 
                Identifier.fromNamespaceAndPath("thaumcraft", "textures/misc/golem/arms_claws.png"), 
                new PartModel(Identifier.fromNamespaceAndPath("thaumcraft", "models/obj/golem_arms_claws.obj"), 
                             Identifier.fromNamespaceAndPath("thaumcraft", "textures/entity/golems/golem_arms_claws.png"), 
                             PartModel.EnumAttachPoint.ARMS), 
                new Object[]{new ItemStack(Items.SHEARS, 2), "base"}, 
                new EnumGolemTrait[]{EnumGolemTrait.FIGHTER, EnumGolemTrait.CLUMSY, EnumGolemTrait.BRUTAL}));

        GolemArm.register(new GolemArm("BREAKERS", 
                new String[]{"GOLEMBREAKER"}, 
                Identifier.fromNamespaceAndPath("thaumcraft", "textures/misc/golem/arms_breakers.png"), 
                new PartModel(Identifier.fromNamespaceAndPath("thaumcraft", "models/obj/golem_arms_breakers.obj"), 
                             Identifier.fromNamespaceAndPath("thaumcraft", "textures/entity/golems/golem_arms_breakers.png"), 
                             PartModel.EnumAttachPoint.ARMS), 
                new Object[]{new ItemStack(Items.DIAMOND, 2), "base", new ItemStack(Blocks.PISTON, 2)}, 
                new EnumGolemTrait[]{EnumGolemTrait.BREAKER, EnumGolemTrait.CLUMSY, EnumGolemTrait.BRUTAL}));

        // Legs
        GolemLeg.register(new GolemLeg("WALKER", 
                new String[]{"MINDCLOCKWORK"}, 
                Identifier.fromNamespaceAndPath("thaumcraft", "textures/misc/golem/legs_walker.png"), 
                new PartModel(Identifier.fromNamespaceAndPath("thaumcraft", "models/obj/golem_legs_walker.obj"), 
                             null, PartModel.EnumAttachPoint.LEGS), 
                new Object[]{"base", "mech"}, 
                new EnumGolemTrait[]{}));

        GolemLeg.register(new GolemLeg("ROLLER", 
                new String[]{"MINDCLOCKWORK"}, 
                Identifier.fromNamespaceAndPath("thaumcraft", "textures/misc/golem/legs_roller.png"), 
                new PartModel(Identifier.fromNamespaceAndPath("thaumcraft", "models/obj/golem_legs_wheel.obj"), 
                             Identifier.fromNamespaceAndPath("thaumcraft", "textures/entity/golems/golem_legs_wheel.png"), 
                             PartModel.EnumAttachPoint.BODY), 
                new Object[]{new ItemStack(Items.BOWL, 2), new ItemStack(Items.LEATHER), "mech"}, 
                new EnumGolemTrait[]{EnumGolemTrait.WHEELED}));

        GolemLeg.register(new GolemLeg("CLIMBER", 
                new String[]{"GOLEMCLIMBER"}, 
                Identifier.fromNamespaceAndPath("thaumcraft", "textures/misc/golem/legs_climber.png"), 
                new PartModel(Identifier.fromNamespaceAndPath("thaumcraft", "models/obj/golem_legs_climber.obj"), 
                             Identifier.fromNamespaceAndPath("thaumcraft", "textures/blocks/base_metal.png"), 
                             PartModel.EnumAttachPoint.LEGS), 
                new Object[]{new ItemStack(Items.FLINT, 4), "base", "mech", "mech"}, 
                new EnumGolemTrait[]{EnumGolemTrait.CLIMBER}));

        GolemLeg.register(new GolemLeg("FLYER", 
                new String[]{"GOLEMFLYER"}, 
                Identifier.fromNamespaceAndPath("thaumcraft", "textures/misc/golem/legs_flyer.png"), 
                new PartModel(Identifier.fromNamespaceAndPath("thaumcraft", "models/obj/golem_legs_floater.obj"), 
                             Identifier.fromNamespaceAndPath("thaumcraft", "textures/entity/golems/golem_legs_floater.png"), 
                             PartModel.EnumAttachPoint.BODY), 
                new Object[]{new ItemStack(Items.PHANTOM_MEMBRANE), new ItemStack(Items.SLIME_BALL), "mech"}, 
                new EnumGolemTrait[]{EnumGolemTrait.FLYER, EnumGolemTrait.FRAGILE}));

        // Addons
        GolemAddon.register(new GolemAddon("NONE", 
                new String[]{"MINDCLOCKWORK"}, 
                Identifier.fromNamespaceAndPath("thaumcraft", "textures/blocks/blank.png"), 
                null, 
                new Object[]{}, 
                new EnumGolemTrait[]{}));

        GolemAddon.register(new GolemAddon("ARMORED", 
                new String[]{"GOLEMCOMBATADV"}, 
                Identifier.fromNamespaceAndPath("thaumcraft", "textures/misc/golem/addon_armored.png"), 
                new PartModel(Identifier.fromNamespaceAndPath("thaumcraft", "models/obj/golem_armor.obj"), 
                             null, PartModel.EnumAttachPoint.BODY), 
                new Object[]{"base", "base", "base", "base"}, 
                new EnumGolemTrait[]{EnumGolemTrait.ARMORED, EnumGolemTrait.HEAVY}));

        GolemAddon.register(new GolemAddon("FIGHTER", 
                new String[]{"SEALGUARD"}, 
                Identifier.fromNamespaceAndPath("thaumcraft", "textures/misc/golem/addon_fighter.png"), 
                null, 
                new Object[]{new ItemStack(Items.IRON_SWORD), "mech"}, 
                new EnumGolemTrait[]{EnumGolemTrait.FIGHTER}));

        GolemAddon.register(new GolemAddon("HAULER", 
                new String[]{"MINDCLOCKWORK"}, 
                Identifier.fromNamespaceAndPath("thaumcraft", "textures/misc/golem/addon_hauler.png"), 
                new PartModel(Identifier.fromNamespaceAndPath("thaumcraft", "models/obj/golem_hauler.obj"), 
                             Identifier.fromNamespaceAndPath("thaumcraft", "textures/entity/golems/golem_hauler.png"), 
                             PartModel.EnumAttachPoint.BODY), 
                new Object[]{new ItemStack(Items.LEATHER), new ItemStack(Blocks.CHEST)}, 
                new EnumGolemTrait[]{EnumGolemTrait.HAULER}));
    }
}
