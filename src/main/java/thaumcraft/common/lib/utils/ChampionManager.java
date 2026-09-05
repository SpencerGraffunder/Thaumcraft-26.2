package thaumcraft.common.lib.utils;

import net.minecraft.resources.Identifier;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.Difficulty;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.common.entities.monster.mods.ChampionModifier;
import thaumcraft.init.ModItems;

import java.util.HashMap;
import java.util.Map;

/**
 * Champion mob system. Enhances random whitelisted hostile mobs with one of the
 * champion modifiers. Champions gain boosted stats, deal/absorb modified damage,
 * and drop loot bags + extra XP on death.
 *
 * <p>Ported from the 1.12.2 champion system. 1.12.2 stored the active modifier in
 * a custom {@code CHAMPION_MOD} attribute; here we track it in a static
 * {@code entity -> modifier-id} map (champions are transient, spawn-to-death, so
 * this is safe and avoids adding synced data to foreign vanilla entities).
 *
 * <p>The whitelist is keyed by entity-type registry location so it is independent
 * of the (sub-package) location of each vanilla mob class in this MC version.
 */
@EventBusSubscriber(modid = Thaumcraft.MODID)
public class ChampionManager {

    public static final int NONE = -2;

    /** Active champion modifier id per mob (NONE if not a champion). */
    private static final Map<Entity, Integer> CHAMPIONS = new HashMap<>();

    /** Whitelist: entity-type registry key -> minimum required base health tier. */
    public static final Map<Identifier, Integer> CHAMPION_WHITELIST = new HashMap<>();

    private static Identifier key(String path) {
        return Identifier.fromNamespaceAndPath("minecraft", path);
    }

    static {
        CHAMPION_WHITELIST.put(key("zombie"), 0);
        CHAMPION_WHITELIST.put(key("husk"), 0);
        CHAMPION_WHITELIST.put(key("skeleton"), 0);
        CHAMPION_WHITELIST.put(key("spider"), 0);
        CHAMPION_WHITELIST.put(key("cave_spider"), 0);
        CHAMPION_WHITELIST.put(key("blaze"), 0);
        CHAMPION_WHITELIST.put(key("enderman"), 0);
        CHAMPION_WHITELIST.put(key("witch"), 1);
    }

    /** Current champion modifier id for an entity (NONE if not a champion). */
    public static int getMod(Entity entity) {
        Integer t = CHAMPIONS.get(entity);
        return t == null ? NONE : t;
    }

    private static boolean isChampion(Entity entity) {
        int t = getMod(entity);
        return t >= 0 && t < ChampionModifier.MODS.length;
    }

    /**
     * Apply a random champion modifier to a mob: record the id, boost stats, rename
     * (matches 1.12.2 {@code EntityUtils.makeChampion}).
     */
    public static void makeChampion(Mob mob) {
        if (mob == null) return;
        if (CHAMPIONS.containsKey(mob)) return; // already a champion

        int type = mob.level().getRandom().nextInt(ChampionModifier.MODS.length);
        // Creepers always get "bold" (no explosion) per 1.12.2.
        if (key("creeper").equals(EntityType.getKey(mob.getType()))) {
            type = ChampionModifier.MODS[0].id;
        }
        CHAMPIONS.put(mob, type);

        Identifier healthId = Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "champion_health");
        AttributeInstance ai = mob.getAttribute(Attributes.MAX_HEALTH);
        if (ai != null) {
            ai.removeModifier(healthId);
            ai.addPermanentModifier(new AttributeModifier(healthId, 25.0, AttributeModifier.Operation.ADD_VALUE));
        }
        Identifier dmgId = Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "champion_damage");
        AttributeInstance ad = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (ad != null) {
            ad.removeModifier(dmgId);
            ad.addPermanentModifier(new AttributeModifier(dmgId, 2.0, AttributeModifier.Operation.ADD_VALUE));
        }
        if (type == 0) {
            Identifier spdId = Identifier.fromNamespaceAndPath(Thaumcraft.MODID, "champion_speed");
            AttributeInstance as = mob.getAttribute(Attributes.MOVEMENT_SPEED);
            if (as != null) {
                as.removeModifier(spdId);
                as.addPermanentModifier(new AttributeModifier(spdId, 0.1, AttributeModifier.Operation.ADD_VALUE));
            }
        }
        mob.heal(25.0f);

        String base = mob.getName().getString();
        mob.setCustomName(Component.literal(
                ChampionModifier.MODS[type].getModNameLocalized() + " " + base));
    }

    /**
     * Decide if a mob should become a champion on spawn, applying the 1.12.2
     * difficulty / dimension / whitelist / min-health logic.
     */
    public static boolean shouldBecomeChampion(Mob mob, Level level) {
        if (level.isClientSide()) return false;
        if (!(mob instanceof Monster)) return false;

        Identifier typeKey = EntityType.getKey(mob.getType());
        Integer minLevel = CHAMPION_WHITELIST.get(typeKey);
        if (minLevel == null) return false;

        int c = level.getRandom().nextInt(100);
        Difficulty difficulty = level.getDifficulty();
        if (difficulty == Difficulty.EASY) {
            c += 2;
        } else if (difficulty == Difficulty.HARD) {
            c -= 2;
        }
        DimensionType dimType = level.dimensionType();
        if (!dimType.hasSkyLight()) {
            c -= 2;
        }

        // Whitelist minimum base-health requirement
        double maxHealth = 0.0;
        AttributeInstance mh = mob.getAttribute(Attributes.MAX_HEALTH);
        if (mh != null) {
            maxHealth = mh.getValue();
        }
        double minHealth = minLevel == 0 ? 10.0 : 10.0 + minLevel * 2.0;
        if (maxHealth < minHealth) return false;

        return c <= 0;
    }

    // ==================== Event wiring ====================

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getEntity() instanceof Mob mob && mob instanceof Monster) {
            if (shouldBecomeChampion(mob, event.getLevel())) {
                makeChampion(mob);
            }
        }
    }

    /** Incoming damage to a champion (type-2 mods) / outgoing damage (type-1 mods). */
    @SubscribeEvent
    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        if (event.getEntity().level().isClientSide()) return;
        LivingEntity victim = event.getEntity();
        DamageSource source = event.getSource();
        Entity attacker = source.getEntity();

        // Type 2: incoming-damage modification (thorns, armor, etc.)
        if (isChampion(victim)) {
            ChampionModifier mod = ChampionModifier.MODS[getMod(victim)];
            if (mod.type == 2) {
                LivingEntity attackerLe = (attacker instanceof LivingEntity le) ? le : null;
                float modified = mod.effect.performEffect(victim, attackerLe, source, event.getNewDamage());
                event.setNewDamage(modified);
            }
        }

        // Type 1: outgoing-damage effects (champion is the attacker)
        if (attacker instanceof Mob championMob && isChampion(championMob)) {
            ChampionModifier mod = ChampionModifier.MODS[getMod(championMob)];
            if (mod.type == 1) {
                mod.effect.performEffect(championMob, victim, source, event.getNewDamage());
            }
        }
    }

    /** Death: champions drop loot bags + extra XP (1.12.2 behaviour). */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity mob = event.getEntity();
        if (mob.level().isClientSide()) return;
        boolean champion = isChampion(mob);
        if (champion) {
            CHAMPIONS.remove(mob); // no longer needed

            // Extra XP (1.12.2 drops 5-7 orbs)
            int xp = 5 + mob.level().getRandom().nextInt(3);
            ExperienceOrb orb = new ExperienceOrb(mob.level(), mob.getX(), mob.getY() + 0.4, mob.getZ(), xp);
            orb.setDeltaMovement(mob.level().getRandom().nextFloat() * 0.2f - 0.1f, 0.2f,
                    mob.level().getRandom().nextFloat() * 0.2f - 0.1f);
            mob.level().addFreshEntity(orb);

            // Loot bag: 1-2 bags, tier rolled from common/uncommon/rare (1.12.2 behaviour)
            int count = 1 + mob.level().getRandom().nextInt(2);
            int tierRoll = mob.level().getRandom().nextInt(10);
            ItemStack bag;
            if (tierRoll >= 8) {
                bag = new ItemStack(ModItems.LOOT_BAG_RARE.get(), count);
            } else if (tierRoll >= 4) {
                bag = new ItemStack(ModItems.LOOT_BAG_UNCOMMON.get(), count);
            } else {
                bag = new ItemStack(ModItems.LOOT_BAG_COMMON.get(), count);
            }
            ItemEntity entityItem = new ItemEntity(mob.level(), mob.getX(), mob.getY() + 0.5, mob.getZ(), bag);
            mob.level().addFreshEntity(entityItem);
        }
    }
}
