package thaumcraft.api.research.theorycraft;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategory;
import thaumcraft.api.research.ResearchEntry;
import thaumcraft.api.research.ResearchEntry.EnumResearchMeta;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Represents the state of a theorycraft session at a research table.
 * 
 * This class tracks:
 * - Current inspiration points
 * - Progress totals for each research category
 * - Available card choices
 * - Blocked categories
 * - Aid cards from nearby objects
 */
public class ResearchTableData {
    
    public BlockEntity table;
    public String player;
    public int inspiration;
    public int inspirationStart;
    public int bonusDraws;
    public int placedCards;
    public int aidsChosen;
    public int penaltyStart;
    
    public List<Long> savedCards = new ArrayList<>();
    public List<String> aidCards = new ArrayList<>();
    
    /**
     * Category totals store the amount of progress per research category.
     * Each point = 1% of progress towards a full theory.
     */
    public TreeMap<String, Integer> categoryTotals = new TreeMap<>();
    public List<String> categoriesBlocked = new ArrayList<>();
    public List<CardChoice> cardChoices = new ArrayList<>();
    
    public CardChoice lastDraw;
    
    /**
     * Represents a card that can be chosen by the player.
     */
    public static class CardChoice {
        public TheorycraftCard card;
        public String key;
        public boolean fromAid;
        public boolean selected;
        
        public CardChoice(String key, TheorycraftCard card, boolean aid, boolean selected) {
            this.key = key;
            this.card = card;
            this.fromAid = aid;
            this.selected = selected;
        }
        
        @Override
        public String toString() {
            return "key:" + key +
                    " card:" + card.getSeed() +
                    " fromAid:" + fromAid +
                    " selected:" + selected;
        }
    }
    
    public ResearchTableData(BlockEntity tileResearchTable) {
        table = tileResearchTable;
    }
    
    public ResearchTableData(Player player, BlockEntity tileResearchTable) {
        this.player = player.getName().getString();
        table = tileResearchTable;
    }
    
    /**
     * @return true if the theorycraft session is complete (no inspiration remaining)
     */
    public boolean isComplete() {
        return inspiration <= 0;
    }
    
    public boolean hasTotal(String cat) {
        return categoryTotals.containsKey(cat);
    }
    
    public int getTotal(String cat) {
        return categoryTotals.getOrDefault(cat, 0);
    }
    
    public void addTotal(String cat, int amt) {
        int current = categoryTotals.getOrDefault(cat, 0);
        current += amt;
        if (current <= 0) {
            categoryTotals.remove(cat);
        } else {
            categoryTotals.put(cat, current);
        }
    }
    
    public void addInspiration(int amt) {
        inspiration += amt;
        if (inspiration > inspirationStart) inspiration = inspirationStart;
    }
    
    // ==================== Serialization ====================
    
    public CompoundTag serialize() {
        CompoundTag nbt = new CompoundTag();
        
        nbt.putString("player", player != null ? player : "");
        nbt.putInt("inspiration", inspiration);
        nbt.putInt("inspirationStart", inspirationStart);
        nbt.putInt("placedCards", placedCards);
        nbt.putInt("bonusDraws", bonusDraws);
        nbt.putInt("aidsChosen", aidsChosen);
        nbt.putInt("penaltyStart", penaltyStart);
        
        // Saved cards
        ListTag savedTag = new ListTag();
        for (Long card : savedCards) {
            CompoundTag gt = new CompoundTag();
            gt.putLong("card", card);
            savedTag.add(gt);
        }
        nbt.put("savedCards", savedTag);
        
        // Blocked categories
        ListTag categoriesBlockedTag = new ListTag();
        for (String category : categoriesBlocked) {
            CompoundTag gt = new CompoundTag();
            gt.putString("category", category);
            categoriesBlockedTag.add(gt);
        }
        nbt.put("categoriesBlocked", categoriesBlockedTag);
        
        // Category totals
        ListTag categoryTotalsTag = new ListTag();
        for (Map.Entry<String, Integer> entry : categoryTotals.entrySet()) {
            CompoundTag gt = new CompoundTag();
            gt.putString("category", entry.getKey());
            gt.putInt("total", entry.getValue());
            categoryTotalsTag.add(gt);
        }
        nbt.put("categoryTotals", categoryTotalsTag);
        
        // Aid cards
        ListTag aidCardsTag = new ListTag();
        for (String mc : aidCards) {
            CompoundTag gt = new CompoundTag();
            gt.putString("aidCard", mc);
            aidCardsTag.add(gt);
        }
        nbt.put("aidCards", aidCardsTag);
        
        // Card choices
        ListTag cardChoicesTag = new ListTag();
        for (CardChoice mc : cardChoices) {
            CompoundTag gt = serializeCardChoice(mc);
            cardChoicesTag.add(gt);
        }
        nbt.put("cardChoices", cardChoicesTag);
        
        if (lastDraw != null) {
            nbt.put("lastDraw", serializeCardChoice(lastDraw));
        }
        
        return nbt;
    }
    
    public CompoundTag serializeCardChoice(CardChoice mc) {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("cardChoice", mc.key);
        nbt.putBoolean("aid", mc.fromAid);
        nbt.putBoolean("select", mc.selected);
        try {
            nbt.put("cardNBT", mc.card.serialize());
        } catch (Exception e) {
            // Ignore serialization errors
        }
        return nbt;
    }
    
    public void deserialize(CompoundTag nbt) {
        if (nbt == null) return;
        
        inspiration = nbt.getIntOr("inspiration", 0);
        inspirationStart = nbt.getIntOr("inspirationStart", 0);
        placedCards = nbt.getIntOr("placedCards", 0);
        bonusDraws = nbt.getIntOr("bonusDraws", 0);
        aidsChosen = nbt.getIntOr("aidsChosen", 0);
        penaltyStart = nbt.getIntOr("penaltyStart", 0);
        player = nbt.getStringOr("player", "");
        
        // Saved cards
        ListTag savedTag = nbt.getListOrEmpty("savedCards");
        savedCards = new ArrayList<>();
        for (int x = 0; x < savedTag.size(); x++) {
            CompoundTag nbtdata = savedTag.getCompoundOrEmpty(x);
            savedCards.add(nbtdata.getLongOr("card", 0L));
        }
        
        // Blocked categories
        ListTag categoriesBlockedTag = nbt.getListOrEmpty("categoriesBlocked");
        categoriesBlocked = new ArrayList<>();
        for (int x = 0; x < categoriesBlockedTag.size(); x++) {
            CompoundTag nbtdata = categoriesBlockedTag.getCompoundOrEmpty(x);
            categoriesBlocked.add(nbtdata.getStringOr("category", ""));
        }
        
        // Category totals
        ListTag categoryTotalsTag = nbt.getListOrEmpty("categoryTotals");
        categoryTotals = new TreeMap<>();
        for (int x = 0; x < categoryTotalsTag.size(); x++) {
            CompoundTag nbtdata = categoryTotalsTag.getCompoundOrEmpty(x);
            categoryTotals.put(nbtdata.getStringOr("category", ""), nbtdata.getIntOr("total", 0));
        }
        
        // Aid cards
        ListTag aidCardsTag = nbt.getListOrEmpty("aidCards");
        aidCards = new ArrayList<>();
        for (int x = 0; x < aidCardsTag.size(); x++) {
            CompoundTag nbtdata = aidCardsTag.getCompoundOrEmpty(x);
            aidCards.add(nbtdata.getStringOr("aidCard", ""));
        }
        
        // Card choices
        ListTag cardChoicesTag = nbt.getListOrEmpty("cardChoices");
        cardChoices = new ArrayList<>();
        for (int x = 0; x < cardChoicesTag.size(); x++) {
            CompoundTag nbtdata = cardChoicesTag.getCompoundOrEmpty(x);
            CardChoice cc = deserializeCardChoice(nbtdata);
            if (cc != null) cardChoices.add(cc);
        }
        
        lastDraw = deserializeCardChoice(nbt.getCompoundOrEmpty("lastDraw"));
    }
    
    @Nullable
    public CardChoice deserializeCardChoice(CompoundTag nbt) {
        if (nbt == null || nbt.isEmpty()) return null;
        String key = nbt.getStringOr("cardChoice", "");
        if (key.isEmpty()) return null;
        TheorycraftCard tc = generateCardWithNBT(key, nbt.getCompoundOrEmpty("cardNBT"));
        if (tc == null) return null;
        return new CardChoice(key, tc, nbt.getBooleanOr("aid", false), nbt.getBooleanOr("select", false));
    }
    
    // ==================== Card Drawing ====================
    
    private boolean isCategoryBlocked(String cat) {
        return categoriesBlocked.contains(cat);
    }
    
    /**
     * Draw a set of cards for the player to choose from.
     * @param draw Number of cards to draw (2 or 3)
     * @param pe The player
     */
    public void drawCards(int draw, Player pe) {
        if (draw == 3) {
            if (bonusDraws > 0) {
                bonusDraws--;
            } else {
                draw = 2;
            }
        }
        
        cardChoices.clear();
        player = pe.getName().getString();
        List<String> availCats = getAvailableCategories(pe);
        List<String> drawnCards = new ArrayList<>();
        boolean aidDrawn = false;
        int failsafe = 0;
        
        while (draw > 0 && failsafe < 10000) {
            failsafe++;
            
            // 25% chance to draw from aid cards if available
            if (!aidDrawn && !aidCards.isEmpty() && pe.getRandom().nextFloat() <= 0.25f) {
                int idx = pe.getRandom().nextInt(aidCards.size());
                String key = aidCards.get(idx);
                TheorycraftCard card = generateCard(key, -1, pe);
                
                if (card == null || card.getInspirationCost() > inspiration || 
                    isCategoryBlocked(card.getResearchCategory())) continue;
                
                if (drawnCards.contains(key)) continue;
                drawnCards.add(key);
                cardChoices.add(new CardChoice(key, card, true, false));
                aidCards.remove(idx);
            } else {
                try {
                    String[] cardKeys = TheorycraftManager.cards.keySet().toArray(new String[0]);
                    if (cardKeys.length == 0) break;
                    
                    int idx = pe.getRandom().nextInt(cardKeys.length);
                    TheorycraftCard card = generateCard(cardKeys[idx], -1, pe);
                    
                    if (card == null || card.isAidOnly() || card.getInspirationCost() > inspiration) continue;
                    
                    // Check if card's category is available
                    if (card.getResearchCategory() != null) {
                        boolean found = false;
                        for (String cn : availCats) {
                            if (cn.equals(card.getResearchCategory())) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) continue;
                    }
                    
                    if (drawnCards.contains(cardKeys[idx])) continue;
                    drawnCards.add(cardKeys[idx]);
                    cardChoices.add(new CardChoice(cardKeys[idx], card, false, false));
                } catch (Exception e) {
                    continue;
                }
            }
            draw--;
        }
    }
    
    @Nullable
    private TheorycraftCard generateCard(String key, long seed, Player pe) {
        if (key == null) return null;
        
        Class<? extends TheorycraftCard> tcc = TheorycraftManager.cards.get(key);
        if (tcc == null) return null;
        
        TheorycraftCard tc = null;
        try {
            tc = tcc.getDeclaredConstructor().newInstance();
            if (seed < 0) {
                if (pe != null) {
                    tc.setSeed(pe.getRandom().nextLong());
                } else {
                    tc.setSeed(System.nanoTime());
                }
            } else {
                tc.setSeed(seed);
            }
            if (pe != null && !tc.initialize(pe, this)) return null;
        } catch (Exception e) {
            // Ignore creation errors
        }
        return tc;
    }
    
    @Nullable
    private TheorycraftCard generateCardWithNBT(String key, CompoundTag nbt) {
        if (key == null) return null;
        
        Class<? extends TheorycraftCard> tcc = TheorycraftManager.cards.get(key);
        if (tcc == null) return null;
        
        TheorycraftCard tc = null;
        try {
            tc = tcc.getDeclaredConstructor().newInstance();
            tc.deserialize(nbt);
        } catch (Exception e) {
            // Ignore creation errors
        }
        return tc;
    }
    
    // ==================== Initialization ====================
    
    /**
     * Initialize a new theorycraft session.
     * @param player The player starting the session
     * @param aids Set of aid keys found near the research table
     */
    public void initialize(Player player, Set<String> aids) {
        inspirationStart = getAvailableInspiration(player);
        inspiration = inspirationStart - aids.size();
        
        for (String muk : aids) {
            ITheorycraftAid mu = TheorycraftManager.aids.get(muk);
            if (mu != null) {
                for (Class<? extends TheorycraftCard> clazz : mu.getCards()) {
                    aidCards.add(clazz.getName());
                }
            }
        }
    }
    
    /**
     * Get list of research categories available to the player (not blocked and researched).
     */
    public List<String> getAvailableCategories(Player player) {
        List<String> cats = new ArrayList<>();
        for (String rck : ResearchCategories.researchCategories.keySet()) {
            ResearchCategory rc = ResearchCategories.getResearchCategory(rck);
            if (rc == null || isCategoryBlocked(rck)) continue;
            if (rc.researchKey == null || ThaumcraftCapabilities.knowsResearchStrict(player, rc.researchKey)) {
                cats.add(rck);
            }
        }
        return cats;
    }
    
    /**
     * Calculate available inspiration based on player's completed research.
     * Base is 5, +0.5 for SPIKY entries, +0.1 for HIDDEN entries, max 15.
     */
    public static int getAvailableInspiration(Player player) {
        float tot = 5;
        IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);
        if (knowledge == null) return 5;
        
        for (String s : knowledge.getResearchList()) {
            if (ThaumcraftCapabilities.knowsResearchStrict(player, s)) {
                ResearchEntry re = ResearchCategories.getResearch(s);
                if (re == null) continue;
                if (re.hasMeta(EnumResearchMeta.SPIKY)) {
                    tot += 0.5f;
                }
                if (re.hasMeta(EnumResearchMeta.HIDDEN)) {
                    tot += 0.1f;
                }
            }
        }
        return Math.min(15, Math.round(tot));
    }
    
    /**
     * Get the level from the table's block entity.
     */
    @Nullable
    public Level getLevel() {
        return table != null ? table.getLevel() : null;
    }
}
