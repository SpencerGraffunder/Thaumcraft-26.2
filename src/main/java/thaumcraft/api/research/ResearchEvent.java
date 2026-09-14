package thaumcraft.api.research;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;
import thaumcraft.api.capabilities.IPlayerKnowledge;

/**
 * These events trigger whenever a player gains knowledge or progresses research.
 * They are notification events - other mods can subscribe to track research progress.
 * 
 * Ported from 1.12.2 with NeoForge event system.
 */
public abstract class ResearchEvent extends Event {
    
    private final Player player;
    
    protected ResearchEvent(Player player) {
        this.player = player;
    }
    
    public Player getPlayer() {
        return player;
    }
    
    /**
     * Fired when a player gains knowledge points.
     */
    public static class Knowledge extends ResearchEvent {
        private final IPlayerKnowledge.EnumKnowledgeType type;
        private final ResearchCategory category;
        private final int amount;
        
        public Knowledge(Player player, IPlayerKnowledge.EnumKnowledgeType type, ResearchCategory category, int amount) {
            super(player);
            this.type = type;
            this.category = category;
            this.amount = amount;
        }
        
        public IPlayerKnowledge.EnumKnowledgeType getType() {
            return type;
        }
        
        public ResearchCategory getCategory() {
            return category;
        }
        
        public int getAmount() {
            return amount;
        }
    }
    
    /**
     * Fired when a player completes research.
     */
    public static class Research extends ResearchEvent {
        private final String researchKey;
        
        public Research(Player player, String researchKey) {
            super(player);
            this.researchKey = researchKey;
        }
        
        public String getResearchKey() {
            return researchKey;
        }
    }
}
