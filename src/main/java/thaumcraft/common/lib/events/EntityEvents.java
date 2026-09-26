package thaumcraft.common.lib.events;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.TriState;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.hurtingprojectile.Fireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.util.FakePlayer;
import thaumcraft.common.items.armor.ItemFortressArmor;
import net.neoforged.neoforge.event.entity.EntityEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.item.ItemExpireEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import thaumcraft.Thaumcraft;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectHelper;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.damagesource.DamageSourceThaumcraft;
import thaumcraft.common.items.armor.ItemFortressArmor;
import thaumcraft.init.ModBlocks;
import thaumcraft.init.ModItems;

/**
 * EntityEvents - Handles entity-related events.
 * 
 * Features:
 * - Bath salts creating purifying fluid when expired in water
 * - Research triggers from damage types (fire, projectiles)
 * - Fortress armor mask effects (life leech, wither)
 * - Runic shield visual effects
 * - Champion mob system (handled by ChampionManager)
 * - Zombie brain drops
 * - Dissolve damage dropping crystals
 * - Preventing fake player item pickup
 * 
 * Ported from Thaumcraft 1.12.2 to 1.20.1
 */
@EventBusSubscriber(modid = Thaumcraft.MODID)
public class EntityEvents {

    /**
     * Handle bath salts expiring in water - create purifying fluid.
     */
    @SubscribeEvent
    public static void onItemExpire(ItemExpireEvent event) {
        ItemEntity itemEntity = event.getEntity();
        if (itemEntity == null) return;
        
        ItemStack stack = itemEntity.getItem();
        if (stack.isEmpty()) return;
        
        // Check if it's bath salts
        if (ModItems.BATH_SALTS != null && stack.getItem() == ModItems.BATH_SALTS.get()) {
            BlockPos pos = itemEntity.blockPosition();
            BlockState state = itemEntity.level().getBlockState(pos);
            
            // Check if in water source block
            if (state.is(Blocks.WATER) && state.getFluidState().isSource()) {
                if (ModBlocks.PURIFYING_FLUID != null) {
                    itemEntity.level().setBlock(pos, ModBlocks.PURIFYING_FLUID.get().defaultBlockState(), 3);
                }
            }
        }
    }

    /**
     * Handle living entity tick - champion mob effects.
     * Champion mod tick effects are handled by ChampionManager.
     */
    @SubscribeEvent
    public static void onLivingTick(EntityTickEvent.Post event) {
        // Champion mob tick effects are handled by ChampionManager
    }

    /**
     * Handle entity hurt events.
     * - Trigger research from damage types
     * - Fortress armor effects
     * - Runic shield visuals
     */
    @SubscribeEvent
    public static void onEntityHurt(LivingDamageEvent.Pre event) {
        Entity entity = event.getEntity();
        DamageSource source = event.getSource();
        
        // Player-specific hurt handling
        if (entity instanceof Player player) {
            handlePlayerHurt(player, source, event.getNewDamage());
        }
        
        // Attacker-related effects
        Entity attacker = source.getEntity();
        if (attacker instanceof Player attackingPlayer) {
            handlePlayerAttack(attackingPlayer, event.getEntity(), event.getNewDamage());
        }
    }

    /**
     * Handle player being hurt - research triggers and armor effects.
     */
    private static void handlePlayerHurt(Player player, DamageSource source, float amount) {
        if (player.level().isClientSide()) return;
        
        // Fire damage research trigger
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) {
            if (ThaumcraftCapabilities.knowsResearchStrict(player, "BASEAUROMANCY@2") &&
                !ThaumcraftCapabilities.knowsResearch(player, "f_onfire")) {
                IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);
                if (knowledge != null) {
                    knowledge.addResearch("f_onfire");
                    if (player instanceof ServerPlayer serverPlayer) {
                        knowledge.sync(serverPlayer);
                    }
                    if (player instanceof ServerPlayer sp) {
                        sp.sendSystemMessage(Component.literal("§e§oResearch discovered: §7Burning").copy());
                    }
                }
            }
        }
        
        // Projectile damage research triggers
        Entity directSource = source.getDirectEntity();
        if (directSource != null && ThaumcraftCapabilities.knowsResearchStrict(player, "FOCUSPROJECTILE@2")) {
            IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);
            if (knowledge != null) {
                if (directSource instanceof AbstractArrow && 
                    !ThaumcraftCapabilities.knowsResearch(player, "f_arrow")) {
                    knowledge.addResearch("f_arrow");
                    if (player instanceof ServerPlayer sp) knowledge.sync(sp);
                }
                if (directSource instanceof Fireball && 
                    !ThaumcraftCapabilities.knowsResearch(player, "f_fireball")) {
                    knowledge.addResearch("f_fireball");
                    if (player instanceof ServerPlayer sp) knowledge.sync(sp);
                }
            }
        }
        
        // Fortress armor wither mask effect (mask type 1)
        if (directSource instanceof LivingEntity attackerLe) {
            ItemStack helm = player.getItemBySlot(EquipmentSlot.HEAD);
            if (!helm.isEmpty() && helm.getItem() instanceof ItemFortressArmor armor && armor.hasMask(helm)) {
                int maskType = ItemFortressArmor.getMaskType(helm);
                if (maskType == 1) {
                    // Wither mask: apply wither to attacker
                    attackerLe.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                            net.minecraft.world.effect.MobEffects.WITHER, 200, 0));
                } else if (maskType == 2) {
                    // Life leech mask: heal player for 25% of damage
                    player.heal(amount * 0.25f);
                }
            }
        }
        
        // Runic shield effect visuals
        float absorption = player.getAbsorptionAmount();
        if (absorption > 0) {
            if (player instanceof ServerPlayer sp) {
                for (int i = 0; i < 10; i++) {
                    sp.level().addParticle(ParticleTypes.ENCHANT,
                            player.getX() + 0.5, player.getY() + 1.0, player.getZ() + 0.5,
                            0.3, 0.5, 0.3);
                }
            }
        }
    }

    /**
     * Handle player attacking - fortress armor life leech.
     */
    private static void handlePlayerAttack(Player player, LivingEntity target, float damage) {
        // Fortress armor life leech mask (mask type 2)
        ItemStack helm = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!helm.isEmpty() && helm.getItem() instanceof ItemFortressArmor armor && armor.hasMask(helm)) {
            int maskType = ItemFortressArmor.getMaskType(helm);
            if (maskType == 2) {
                player.heal(damage * 0.25f);
            }
        }
    }

    /**
     * Prevent fake players from picking up items.
     * Thaumcraft uses fake players for some automation.
     */
    @SubscribeEvent
    public static void onItemPickup(ItemEntityPickupEvent.Pre event) {
        Player player = event.getPlayer();
        if (player != null && player.getName().getString().startsWith("FakeThaumcraft")) {
            event.setCanPickup(TriState.FALSE);
        }
    }

    /**
     * Handle living entity drops.
     * - Zombie brain drops
     * - Dissolve damage crystal drops
     * - Champion loot bags (handled by ChampionManager)
     */
    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;
        
        DamageSource source = event.getSource();
        boolean fakePlayer = source.getEntity() instanceof FakePlayer;
        
        // Zombie brain drops
        if (entity instanceof Zombie && !(entity.getType().getDescriptionId().contains("brainy"))) {
            if (event.isRecentlyHit() && entity.level().getRandom().nextInt(10) < 1) {
                if (ModItems.ZOMBIE_BRAIN != null) {
                    ItemEntity brainDrop = new ItemEntity(
                            entity.level(),
                            entity.getX(),
                            entity.getY() + entity.getEyeHeight(),
                            entity.getZ(),
                            new ItemStack(ModItems.ZOMBIE_BRAIN.get()));
                    event.getDrops().add(brainDrop);
                }
            }
        }
        
        // Dissolve damage - drop aspect crystals
        if (source.getMsgId().equals("thaumcraft.dissolve")) {
            AspectList aspects = AspectHelper.getEntityAspects(entity);
            if (aspects != null && aspects.size() > 0) {
                Aspect[] aspectArray = aspects.getAspects();
                int dropCount = Math.min(1 + aspects.visSize() / 10, 5);
                dropCount = Math.max(1, entity.level().getRandom().nextInt(dropCount + 1));
                
                for (int i = 0; i < dropCount && aspectArray.length > 0; i++) {
                    Aspect aspect = aspectArray[entity.level().getRandom().nextInt(aspectArray.length)];
                    ItemStack crystal = ThaumcraftApiHelper.makeCrystal(aspect);
                    if (!crystal.isEmpty()) {
                        ItemEntity crystalDrop = new ItemEntity(
                                entity.level(),
                                entity.getX(),
                                entity.getY() + entity.getEyeHeight(),
                                entity.getZ(),
                                crystal);
                        event.getDrops().add(crystalDrop);
                    }
                }
            }
        }
        
        // Champion mob loot bags are handled by ChampionManager.onLivingDeath
    }

    /**
     * Handle entity spawning - champion mob assignment.
     * Champion mob assignment is handled by ChampionManager.onEntityJoinLevel.
     */
    @SubscribeEvent
    public static void onEntitySpawn(EntityJoinLevelEvent event) {
        // Champion mob assignment is handled by ChampionManager.onEntityJoinLevel
    }

    /**
     * Handle entity construction - register custom attributes.
     * Champion attributes are registered by ChampionManager.makeChampion.
     */
    @SubscribeEvent
    public static void onEntityConstruct(EntityEvent.EntityConstructing event) {
        // Champion attributes are registered by ChampionManager.makeChampion
    }
}
