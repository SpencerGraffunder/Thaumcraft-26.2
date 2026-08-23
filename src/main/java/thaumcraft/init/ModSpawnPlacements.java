package thaumcraft.init;

import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import thaumcraft.Thaumcraft;
import thaumcraft.common.entities.monster.EntityBrainyZombie;
import thaumcraft.common.entities.monster.EntityFireBat;
import thaumcraft.common.entities.monster.EntityPech;
import thaumcraft.common.entities.monster.EntityThaumicSlime;
import thaumcraft.common.entities.monster.EntityWisp;

/**
 * ModSpawnPlacements - Registers spawn placement rules for Thaumcraft entities.
 * 
 * In 1.20.1, spawn rules are registered via RegisterSpawnPlacementsEvent.
 * Actual biome-based spawning is handled via BiomeModifier JSONs in:
 * data/thaumcraft/forge/biome_modifier/
 */
@EventBusSubscriber(modid = Thaumcraft.MODID)
public class ModSpawnPlacements {

    @SubscribeEvent
    public static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        Thaumcraft.LOGGER.info("Registering Thaumcraft spawn placements");

        // Brainy Zombie - spawns like regular zombies (on ground, in dark)
        event.register(
            ModEntities.BRAINY_ZOMBIE.get(),
            SpawnPlacements.Type.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            Monster::checkMonsterSpawnRules
        );

        // Giant Brainy Zombie
        event.register(
            ModEntities.GIANT_BRAINY_ZOMBIE.get(),
            SpawnPlacements.Type.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            Monster::checkMonsterSpawnRules
        );

        // Wisp - spawns in the air
        event.register(
            ModEntities.WISP.get(),
            SpawnPlacements.Type.NO_RESTRICTIONS,
            Heightmap.Types.MOTION_BLOCKING,
            EntityWisp::checkWispSpawnRules
        );

        // Fire Bat - spawns in the Nether
        event.register(
            ModEntities.FIRE_BAT.get(),
            SpawnPlacements.Type.NO_RESTRICTIONS,
            Heightmap.Types.MOTION_BLOCKING,
            EntityFireBat::checkFireBatSpawnRules
        );

        // Pech - spawns in magical biomes
        event.register(
            ModEntities.PECH.get(),
            SpawnPlacements.Type.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            EntityPech::checkPechSpawnRules
        );

        // Taint Crawler
        event.register(
            ModEntities.TAINT_CRAWLER.get(),
            SpawnPlacements.Type.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            Monster::checkMonsterSpawnRules
        );

        // Thaumic Slime - Note: doesn't spawn naturally, only from flux effects
        event.register(
            ModEntities.THAUMIC_SLIME.get(),
            SpawnPlacements.Type.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            EntityThaumicSlime::checkThaumicSlimeSpawnRules
        );

        // Mind Spider
        event.register(
            ModEntities.MIND_SPIDER.get(),
            SpawnPlacements.Type.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            Monster::checkMonsterSpawnRules
        );

        // Eldritch Crab
        event.register(
            ModEntities.ELDRITCH_CRAB.get(),
            SpawnPlacements.Type.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            Monster::checkMonsterSpawnRules
        );

        // Eldritch Guardian
        event.register(
            ModEntities.ELDRITCH_GUARDIAN.get(),
            SpawnPlacements.Type.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            Monster::checkMonsterSpawnRules
        );

        // Cultist entities
        event.register(
            ModEntities.CULTIST.get(),
            SpawnPlacements.Type.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            Monster::checkMonsterSpawnRules
        );

        event.register(
            ModEntities.CULTIST_KNIGHT.get(),
            SpawnPlacements.Type.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            Monster::checkMonsterSpawnRules
        );

        event.register(
            ModEntities.CULTIST_CLERIC.get(),
            SpawnPlacements.Type.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            Monster::checkMonsterSpawnRules
        );

        // Inhabited Zombie
        event.register(
            ModEntities.INHABITED_ZOMBIE.get(),
            SpawnPlacements.Type.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            Monster::checkMonsterSpawnRules
        );

        Thaumcraft.LOGGER.info("Registered Thaumcraft spawn placements");
    }
}
