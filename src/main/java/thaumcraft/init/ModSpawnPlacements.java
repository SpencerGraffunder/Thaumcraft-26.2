package thaumcraft.init;

import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.entity.SpawnPlacementRegisterEvent;
import net.neoforged.bus.api.SubscribeEvent;
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
 * In 1.20.1, spawn rules are registered via SpawnPlacementRegisterEvent.
 * Actual biome-based spawning is handled via BiomeModifier JSONs in:
 * data/thaumcraft/forge/biome_modifier/
 */
@Mod.EventBusSubscriber(modid = Thaumcraft.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModSpawnPlacements {

    @SubscribeEvent
    public static void registerSpawnPlacements(SpawnPlacementRegisterEvent event) {
        Thaumcraft.LOGGER.info("Registering Thaumcraft spawn placements");

        // Brainy Zombie - spawns like regular zombies (on ground, in dark)
        event.register(
            ModEntities.BRAINY_ZOMBIE.get(),
            SpawnPlacements.Type.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            Monster::checkMonsterSpawnRules,
            SpawnPlacementRegisterEvent.Operation.AND
        );

        // Giant Brainy Zombie
        event.register(
            ModEntities.GIANT_BRAINY_ZOMBIE.get(),
            SpawnPlacements.Type.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            Monster::checkMonsterSpawnRules,
            SpawnPlacementRegisterEvent.Operation.AND
        );

        // Wisp - spawns in the air
        event.register(
            ModEntities.WISP.get(),
            SpawnPlacements.Type.NO_RESTRICTIONS,
            Heightmap.Types.MOTION_BLOCKING,
            EntityWisp::checkWispSpawnRules,
            SpawnPlacementRegisterEvent.Operation.AND
        );

        // Fire Bat - spawns in the Nether
        event.register(
            ModEntities.FIRE_BAT.get(),
            SpawnPlacements.Type.NO_RESTRICTIONS,
            Heightmap.Types.MOTION_BLOCKING,
            EntityFireBat::checkFireBatSpawnRules,
            SpawnPlacementRegisterEvent.Operation.AND
        );

        // Pech - spawns in magical biomes
        event.register(
            ModEntities.PECH.get(),
            SpawnPlacements.Type.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            EntityPech::checkPechSpawnRules,
            SpawnPlacementRegisterEvent.Operation.AND
        );

        // Taint Crawler
        event.register(
            ModEntities.TAINT_CRAWLER.get(),
            SpawnPlacements.Type.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            Monster::checkMonsterSpawnRules,
            SpawnPlacementRegisterEvent.Operation.AND
        );

        // Thaumic Slime - Note: doesn't spawn naturally, only from flux effects
        event.register(
            ModEntities.THAUMIC_SLIME.get(),
            SpawnPlacements.Type.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            EntityThaumicSlime::checkThaumicSlimeSpawnRules,
            SpawnPlacementRegisterEvent.Operation.AND
        );

        // Mind Spider
        event.register(
            ModEntities.MIND_SPIDER.get(),
            SpawnPlacements.Type.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            Monster::checkMonsterSpawnRules,
            SpawnPlacementRegisterEvent.Operation.AND
        );

        // Eldritch Crab
        event.register(
            ModEntities.ELDRITCH_CRAB.get(),
            SpawnPlacements.Type.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            Monster::checkMonsterSpawnRules,
            SpawnPlacementRegisterEvent.Operation.AND
        );

        // Eldritch Guardian
        event.register(
            ModEntities.ELDRITCH_GUARDIAN.get(),
            SpawnPlacements.Type.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            Monster::checkMonsterSpawnRules,
            SpawnPlacementRegisterEvent.Operation.AND
        );

        // Cultist entities
        event.register(
            ModEntities.CULTIST.get(),
            SpawnPlacements.Type.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            Monster::checkMonsterSpawnRules,
            SpawnPlacementRegisterEvent.Operation.AND
        );

        event.register(
            ModEntities.CULTIST_KNIGHT.get(),
            SpawnPlacements.Type.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            Monster::checkMonsterSpawnRules,
            SpawnPlacementRegisterEvent.Operation.AND
        );

        event.register(
            ModEntities.CULTIST_CLERIC.get(),
            SpawnPlacements.Type.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            Monster::checkMonsterSpawnRules,
            SpawnPlacementRegisterEvent.Operation.AND
        );

        // Inhabited Zombie
        event.register(
            ModEntities.INHABITED_ZOMBIE.get(),
            SpawnPlacements.Type.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            Monster::checkMonsterSpawnRules,
            SpawnPlacementRegisterEvent.Operation.AND
        );

        Thaumcraft.LOGGER.info("Registered Thaumcraft spawn placements");
    }
}
