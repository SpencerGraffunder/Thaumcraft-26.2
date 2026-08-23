package thaumcraft.datagen;

import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import thaumcraft.Thaumcraft;

public class DataGenerators {

    // NOTE: registered providers are commented out; kept as a plain helper class
    // so the datagen hook can be re-enabled later via a concrete GatherDataEvent
    // subclass (GatherDataEvent.Client / .Server) when providers are implemented.
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        
        // Block Tags
        // generator.addProvider(event.includeServer(), new ModBlockTagsProvider(packOutput, event.getLookupProvider(), existingFileHelper));
        
        // Item Tags
        // generator.addProvider(event.includeServer(), new ModItemTagsProvider(packOutput, event.getLookupProvider(), existingFileHelper));
        
        // Recipes
        // generator.addProvider(event.includeServer(), new ModRecipeProvider(packOutput));
        
        // Loot Tables
        // generator.addProvider(event.includeServer(), new ModLootTableProvider(packOutput));
        
        // Block States & Models
        // generator.addProvider(event.includeClient(), new ModBlockStateProvider(packOutput, existingFileHelper));
        
        // Item Models
        // generator.addProvider(event.includeClient(), new ModItemModelProvider(packOutput, existingFileHelper));
        
        // Sounds
        // generator.addProvider(event.includeClient(), new ModSoundProvider(packOutput, existingFileHelper));
        
        Thaumcraft.LOGGER.info("Thaumcraft data generation setup complete");
    }
}
