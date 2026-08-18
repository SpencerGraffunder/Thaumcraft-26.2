package thaumcraft.init;

import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IForgeMenuType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;
import thaumcraft.Thaumcraft;
import thaumcraft.common.menu.ArcaneBoreMenu;
import thaumcraft.common.menu.ArcaneWorkbenchMenu;
import thaumcraft.common.menu.FocalManipulatorMenu;
import thaumcraft.common.menu.FocusPouchMenu;
import thaumcraft.common.menu.GolemBuilderMenu;
import thaumcraft.common.menu.HandMirrorMenu;
import thaumcraft.common.menu.HungryChestMenu;
import thaumcraft.common.menu.PechMenu;
import thaumcraft.common.menu.PotionSprayerMenu;
import thaumcraft.common.menu.ResearchTableMenu;
import thaumcraft.common.menu.SealMenu;
import thaumcraft.common.menu.SmelterMenu;
import thaumcraft.common.menu.SpaMenu;
import thaumcraft.common.menu.ThaumatoriumMenu;
import thaumcraft.common.menu.TurretMenu;
import thaumcraft.common.menu.VoidSiphonMenu;
import thaumcraft.common.menu.LogisticsMenu;

/**
 * Registry for all Thaumcraft menu types.
 * Menus are the 1.20.1 equivalent of 1.12.2 Containers.
 * 
 * Each menu type must be paired with a Screen class registered
 * in ClientModEvents.onClientSetup().
 */
public class ModMenuTypes {
    
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = 
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, Thaumcraft.MODID);
    
    // ==================== Golem System ====================
    
    public static final DeferredHolder<MenuType<GolemBuilderMenu>> GOLEM_BUILDER = 
            MENU_TYPES.register("golem_builder", 
                    () -> IForgeMenuType.create(GolemBuilderMenu::new));
    
    public static final DeferredHolder<MenuType<SealMenu>> SEAL = 
            MENU_TYPES.register("seal", 
                    () -> IForgeMenuType.create(SealMenu::new));
    
    // ==================== Crafting ====================
    
    public static final DeferredHolder<MenuType<ArcaneWorkbenchMenu>> ARCANE_WORKBENCH = 
            MENU_TYPES.register("arcane_workbench", 
                    () -> IForgeMenuType.create(ArcaneWorkbenchMenu::new));
    
    public static final DeferredHolder<MenuType<ThaumatoriumMenu>> THAUMATORIUM = 
            MENU_TYPES.register("thaumatorium", 
                    () -> IForgeMenuType.create(ThaumatoriumMenu::new));
    
    // ==================== Research ====================
    
    public static final DeferredHolder<MenuType<ResearchTableMenu>> RESEARCH_TABLE = 
            MENU_TYPES.register("research_table", 
                    () -> IForgeMenuType.create(ResearchTableMenu::new));
    
    // ==================== Essentia Processing ====================
    
    public static final DeferredHolder<MenuType<SmelterMenu>> SMELTER = 
            MENU_TYPES.register("smelter", 
                    () -> IForgeMenuType.create(SmelterMenu::new));
    
    // ==================== Caster/Focus ====================
    
    public static final DeferredHolder<MenuType<FocalManipulatorMenu>> FOCAL_MANIPULATOR = 
            MENU_TYPES.register("focal_manipulator", 
                    () -> IForgeMenuType.create(FocalManipulatorMenu::new));
    
    public static final DeferredHolder<MenuType<FocusPouchMenu>> FOCUS_POUCH = 
            MENU_TYPES.register("focus_pouch", 
                    () -> IForgeMenuType.create(FocusPouchMenu::new));
    
    // ==================== Tools ====================
    
    public static final DeferredHolder<MenuType<HandMirrorMenu>> HAND_MIRROR = 
            MENU_TYPES.register("hand_mirror", 
                    () -> IForgeMenuType.create(HandMirrorMenu::new));
    
    // ==================== Storage ====================
    
    public static final DeferredHolder<MenuType<HungryChestMenu>> HUNGRY_CHEST = 
            MENU_TYPES.register("hungry_chest", 
                    () -> IForgeMenuType.create(HungryChestMenu::new));
    
    // ==================== Devices ====================
    
    public static final DeferredHolder<MenuType<PotionSprayerMenu>> POTION_SPRAYER = 
            MENU_TYPES.register("potion_sprayer", 
                    () -> IForgeMenuType.create(PotionSprayerMenu::new));
    
    public static final DeferredHolder<MenuType<SpaMenu>> SPA = 
            MENU_TYPES.register("spa", 
                    () -> IForgeMenuType.create(SpaMenu::new));
    
    public static final DeferredHolder<MenuType<VoidSiphonMenu>> VOID_SIPHON = 
            MENU_TYPES.register("void_siphon", 
                    () -> IForgeMenuType.create(VoidSiphonMenu::new));
    
    // ==================== Turrets ====================
    
    public static final DeferredHolder<MenuType<TurretMenu>> TURRET_BASIC = 
            MENU_TYPES.register("turret_basic", 
                    () -> IForgeMenuType.create(TurretMenu::new));
    
    public static final DeferredHolder<MenuType<TurretMenu>> TURRET_ADVANCED = 
            MENU_TYPES.register("turret_advanced", 
                    () -> IForgeMenuType.create(TurretMenu::new));
    
    // ==================== Constructs ====================
    
    public static final DeferredHolder<MenuType<ArcaneBoreMenu>> ARCANE_BORE = 
            MENU_TYPES.register("arcane_bore", 
                    () -> IForgeMenuType.create(ArcaneBoreMenu::new));
    
    // ==================== Trading ====================
    
    public static final DeferredHolder<MenuType<PechMenu>> PECH_TRADING = 
            MENU_TYPES.register("pech_trading", 
                    () -> IForgeMenuType.create(PechMenu::new));
    
    // ==================== Logistics ====================
    
    public static final DeferredHolder<MenuType<LogisticsMenu>> LOGISTICS = 
            MENU_TYPES.register("logistics", 
                    () -> IForgeMenuType.create(LogisticsMenu::new));
}
