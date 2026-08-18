package thaumcraft.init;

import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.SoundType;
import net.neoforged.neoforge.common.SoundTypes;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;
import thaumcraft.Thaumcraft;

/**
 * Registry for all Thaumcraft sound events.
 * Uses DeferredRegister for 1.20.1 Forge.
 * 
 * Sound files are in assets/thaumcraft/sounds/
 * Sound definitions are in assets/thaumcraft/sounds.json
 */
public class ModSounds {
    
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = 
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Thaumcraft.MODID);
    
    // ==================== Player Sounds ====================
    public static final DeferredHolder<SoundEvent> HEARTBEAT = registerSound("heartbeat");
    public static final DeferredHolder<SoundEvent> RUNIC_SHIELD_EFFECT = registerSound("runicshieldeffect");
    public static final DeferredHolder<SoundEvent> RUNIC_SHIELD_CHARGE = registerSound("runicshieldecharge");
    
    // ==================== Block Sounds ====================
    public static final DeferredHolder<SoundEvent> SPILL = registerSound("spill");
    public static final DeferredHolder<SoundEvent> DUST = registerSound("dust");
    public static final DeferredHolder<SoundEvent> BUBBLE = registerSound("bubble");
    public static final DeferredHolder<SoundEvent> CREAK = registerSound("creak");
    public static final DeferredHolder<SoundEvent> SQUEEK = registerSound("squeek");
    public static final DeferredHolder<SoundEvent> JAR = registerSound("jar");
    public static final DeferredHolder<SoundEvent> PUMP = registerSound("pump");
    public static final DeferredHolder<SoundEvent> CRYSTAL = registerSound("crystal");
    public static final DeferredHolder<SoundEvent> GORE = registerSound("gore");
    public static final DeferredHolder<SoundEvent> INFUSER = registerSound("infuser");
    public static final DeferredHolder<SoundEvent> INFUSER_START = registerSound("infuserstart");
    public static final DeferredHolder<SoundEvent> URN_BREAK = registerSound("urnbreak");
    public static final DeferredHolder<SoundEvent> EVIL_PORTAL = registerSound("evilportal");
    public static final DeferredHolder<SoundEvent> GRIND = registerSound("grind");
    
    // ==================== Ambient Sounds ====================
    public static final DeferredHolder<SoundEvent> FLY = registerSound("fly");
    public static final DeferredHolder<SoundEvent> KEY = registerSound("key");
    public static final DeferredHolder<SoundEvent> TICKS = registerSound("ticks");
    public static final DeferredHolder<SoundEvent> CLACK = registerSound("clack");  // Golem ambient/hurt
    public static final DeferredHolder<SoundEvent> POOF = registerSound("poof");
    public static final DeferredHolder<SoundEvent> BRAIN = registerSound("brain");
    public static final DeferredHolder<SoundEvent> RUMBLE = registerSound("rumble");
    public static final DeferredHolder<SoundEvent> JACOBS = registerSound("jacobs");
    public static final DeferredHolder<SoundEvent> WIND = registerSound("wind");
    public static final DeferredHolder<SoundEvent> WHISPERS = registerSound("whispers");
    public static final DeferredHolder<SoundEvent> MONOLITH = registerSound("monolith");
    
    // ==================== Master/UI Sounds ====================
    public static final DeferredHolder<SoundEvent> PAGE = registerSound("page");
    public static final DeferredHolder<SoundEvent> PAGE_TURN = registerSound("pageturn");
    public static final DeferredHolder<SoundEvent> LEARN = registerSound("learn");
    public static final DeferredHolder<SoundEvent> WRITE = registerSound("write");
    public static final DeferredHolder<SoundEvent> ERASE = registerSound("erase");
    public static final DeferredHolder<SoundEvent> WAND = registerSound("wand");
    public static final DeferredHolder<SoundEvent> WAND_FAIL = registerSound("wandfail");
    public static final DeferredHolder<SoundEvent> ICE = registerSound("ice");
    public static final DeferredHolder<SoundEvent> HH_OFF = registerSound("hhoff");
    public static final DeferredHolder<SoundEvent> HH_ON = registerSound("hhon");
    public static final DeferredHolder<SoundEvent> SHOCK = registerSound("shock");
    public static final DeferredHolder<SoundEvent> ZAP = registerSound("zap");  // Golem interaction
    public static final DeferredHolder<SoundEvent> CRAFT_FAIL = registerSound("craftfail");
    public static final DeferredHolder<SoundEvent> SCAN = registerSound("scan");  // Golem mode toggle
    public static final DeferredHolder<SoundEvent> CRAFT_START = registerSound("craftstart");
    public static final DeferredHolder<SoundEvent> TOOL = registerSound("tool");  // Golem death
    public static final DeferredHolder<SoundEvent> UPGRADE = registerSound("upgrade");
    public static final DeferredHolder<SoundEvent> COINS = registerSound("coins");
    
    // ==================== Hostile/Entity Sounds ====================
    public static final DeferredHolder<SoundEvent> SWARM = registerSound("swarm");
    public static final DeferredHolder<SoundEvent> SWARM_ATTACK = registerSound("swarmattack");
    public static final DeferredHolder<SoundEvent> WISP_DEAD = registerSound("wispdead");
    public static final DeferredHolder<SoundEvent> WISP_LIVE = registerSound("wisplive");
    public static final DeferredHolder<SoundEvent> TENTACLE = registerSound("tentacle");
    
    // Pech Sounds
    public static final DeferredHolder<SoundEvent> PECH_IDLE = registerSound("pech_idle");
    public static final DeferredHolder<SoundEvent> PECH_TRADE = registerSound("pech_trade");
    public static final DeferredHolder<SoundEvent> PECH_DICE = registerSound("pech_dice");
    public static final DeferredHolder<SoundEvent> PECH_HIT = registerSound("pech_hit");
    public static final DeferredHolder<SoundEvent> PECH_DEATH = registerSound("pech_death");
    public static final DeferredHolder<SoundEvent> PECH_CHARGE = registerSound("pech_charge");
    
    // Eldritch Guardian Sounds
    public static final DeferredHolder<SoundEvent> EG_IDLE = registerSound("egidle");
    public static final DeferredHolder<SoundEvent> EG_ATTACK = registerSound("egattack");
    public static final DeferredHolder<SoundEvent> EG_DEATH = registerSound("egdeath");
    public static final DeferredHolder<SoundEvent> EG_SCREECH = registerSound("egscreech");
    
    // Taint Crab Sounds
    public static final DeferredHolder<SoundEvent> CRAB_CLAW = registerSound("crabclaw");
    public static final DeferredHolder<SoundEvent> CRAB_DEATH = registerSound("crabdeath");
    public static final DeferredHolder<SoundEvent> CRAB_TALK = registerSound("crabtalk");
    
    // Cultist Sounds
    public static final DeferredHolder<SoundEvent> CHANT = registerSound("chant");
    
    // ==================== Helper Method ====================
    
    private static DeferredHolder<SoundEvent> registerSound(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(
                Identifier.fromNamespaceAndPath(Thaumcraft.MODID, name)));
    }
    
    // ==================== Custom SoundTypes for Blocks ====================
    // These must be initialized lazily since they reference RegistryObjects
    // Use ForgeSoundType which accepts suppliers
    
    public static final SoundType GORE_TYPE = new ForgeSoundType(0.5f, 1.0f, 
            GORE::get, GORE::get, GORE::get, GORE::get, GORE::get);
    
    public static final SoundType CRYSTAL_TYPE = new ForgeSoundType(0.5f, 1.0f, 
            CRYSTAL::get, CRYSTAL::get, CRYSTAL::get, CRYSTAL::get, CRYSTAL::get);
    
    public static final SoundType JAR_TYPE = new ForgeSoundType(0.5f, 1.0f, 
            JAR::get, JAR::get, JAR::get, JAR::get, JAR::get);
    
    public static final SoundType URN_TYPE = new ForgeSoundType(0.5f, 1.5f, 
            URN_BREAK::get, URN_BREAK::get, URN_BREAK::get, URN_BREAK::get, URN_BREAK::get);
}
