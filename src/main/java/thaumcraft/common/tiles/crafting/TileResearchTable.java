package thaumcraft.common.tiles.crafting;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.items.IScribeTools;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategory;
import thaumcraft.api.research.theorycraft.ITheorycraftAid;
import thaumcraft.api.research.theorycraft.ResearchTableData;
import thaumcraft.api.research.theorycraft.TheorycraftManager;
import thaumcraft.common.menu.ResearchTableMenu;
import thaumcraft.common.tiles.TileThaumcraftInventory;
import thaumcraft.init.ModBlockEntities;
import thaumcraft.init.ModSounds;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;

/**
 * Research table tile entity - used for the theorycraft minigame.
 * 
 * Slots:
 * - 0: Scribing tools
 * - 1: Paper
 * 
 * The research table allows players to create "theories" by drawing and
 * activating theorycraft cards. Each card costs inspiration and grants
 * progress towards different research categories.
 */
public class TileResearchTable extends TileThaumcraftInventory implements MenuProvider {

    public static final int SLOT_SCRIBING_TOOLS = 0;
    public static final int SLOT_PAPER = 1;

    /** Current theorycraft session data, null if no session active */
    public ResearchTableData data;

    public TileResearchTable(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RESEARCH_TABLE.get(), pos, state, 2);
        this.data = null;
        this.syncedSlots = new int[] { 0, 1 };
    }

    // ==================== MenuProvider ====================

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.thaumcraft.research_table");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ResearchTableMenu(containerId, playerInventory, this);
    }

    // ==================== NBT ====================

    @Override
    protected void writeSyncNBT(ValueOutput output) {
        super.writeSyncNBT(output);
        if (data != null) {
            output.store("note", CompoundTag.CODEC, data.serialize());
        }
    }

    @Override
    protected void readSyncNBT(ValueInput input) {
        super.readSyncNBT(input);
        if (input.keySet().contains("note")) {
            data = new ResearchTableData(this);
            data.deserialize(input.read("note", CompoundTag.CODEC).orElseGet(CompoundTag::new));
        } else {
            data = null;
        }
    }

    // ==================== Theorycraft Session ====================

    /**
     * Start a new theory research session.
     * @param player The player starting the session
     * @param aids Set of aid keys found nearby
     */
    public void startNewTheory(Player player, Set<String> aids) {
        data = new ResearchTableData(player, this);
        data.initialize(player, aids);
        syncTile(false);
        setChanged();
    }

    /**
     * Finish the current theory and grant knowledge to the player.
     * Categories with more progress grant more knowledge.
     * Categories after the penalty threshold get reduced rewards.
     */
    public void finishTheory(Player player) {
        if (data == null) return;

        // Sort categories by total (highest first)
        Map<String, Integer> sortedMap = data.categoryTotals.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));

        int i = 0;
        for (String cat : sortedMap.keySet()) {
            // Each 100 points = 1 full theory knowledge
            int tot = Math.round(sortedMap.get(cat) / 100.0f * IPlayerKnowledge.EnumKnowledgeType.THEORY.getProgression());
            
            // Apply penalty after certain number of categories
            if (i > data.penaltyStart) {
                tot = (int) Math.max(1.0, tot * 0.666666667);
            }
            
            ResearchCategory rc = ResearchCategories.getResearchCategory(cat);
            if (rc != null) {
                // Grant theory knowledge to the player
                ThaumcraftApi.internalMethods.addKnowledge(player, IPlayerKnowledge.EnumKnowledgeType.THEORY, rc, tot);
            }
            i++;
        }

        // Clear the session
        data = null;
        
        // Play completion sound
        if (level != null) {
            level.playSound(null, worldPosition, ModSounds.LEARN.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
            level.blockEvent(worldPosition, getBlockState().getBlock(), 1, 0);
        }
        
        syncTile(false);
        setChanged();
    }

    /**
     * Check if there's an active theory session.
     */
    public boolean hasActiveTheory() {
        return data != null;
    }

    /**
     * Get the current research data.
     */
    public ResearchTableData getResearchData() {
        return data;
    }

    // ==================== Aid Detection ====================

    /**
     * Scan the surrounding area for theorycraft aids (bookshelves, etc.)
     * @return Set of aid class names found
     */
    public Set<String> checkSurroundingAids() {
        Map<String, ITheorycraftAid> foundAids = new HashMap<>();
        
        if (level == null) return foundAids.keySet();

        // Check blocks in a 9x3x9 area
        for (int y = -1; y <= 1; y++) {
            for (int x = -4; x <= 4; x++) {
                for (int z = -4; z <= 4; z++) {
                    BlockPos checkPos = worldPosition.offset(x, y, z);
                    BlockState state = level.getBlockState(checkPos);
                    
                    for (Map.Entry<String, ITheorycraftAid> entry : TheorycraftManager.aids.entrySet()) {
                        String aidKey = entry.getKey();
                        ITheorycraftAid aid = entry.getValue();
                        Object trigger = aid.getAidObject();
                        
                        if (trigger instanceof Block) {
                            if (state.getBlock() == trigger) {
                                foundAids.put(aidKey, aid);
                            }
                        } else if (trigger instanceof ItemStack triggerStack) {
                            // Check if block drops match the trigger item
                            // Simplified: just check if block's item matches
                            ItemStack blockItem = new ItemStack(state.getBlock());
                            if (!blockItem.isEmpty() && ItemStack.isSameItem(blockItem, triggerStack)) {
                                foundAids.put(aidKey, aid);
                            }
                        }
                    }
                }
            }
        }

        // Check entities in range
        AABB searchBox = new AABB(worldPosition).inflate(5.0);
        List<Entity> entities = level.getEntities((Entity) null, searchBox, e -> true);
        
        for (Entity entity : entities) {
            for (Map.Entry<String, ITheorycraftAid> entry : TheorycraftManager.aids.entrySet()) {
                String aidKey = entry.getKey();
                ITheorycraftAid aid = entry.getValue();
                Object trigger = aid.getAidObject();
                
                if (trigger instanceof Class<?> entityClass) {
                    if (entityClass.isAssignableFrom(entity.getClass())) {
                        foundAids.put(aidKey, aid);
                    }
                }
            }
        }

        return foundAids.keySet();
    }

    // ==================== Consumables ====================

    /**
     * Try to consume ink from the scribing tools.
     * @return true if ink was consumed
     */
    public boolean consumeInkFromTable() {
        ItemStack tools = getItem(SLOT_SCRIBING_TOOLS);
        
        // Check for IScribeTools interface
        if (tools.getItem() instanceof IScribeTools) {
            if (tools.getDamageValue() < tools.getMaxDamage()) {
                tools.setDamageValue(tools.getDamageValue() + 1);
                syncTile(false);
                setChanged();
                return true;
            }
        }
        // Fallback: any damageable item as scribing tools
        else if (tools.isDamageableItem() && tools.getDamageValue() < tools.getMaxDamage()) {
            tools.setDamageValue(tools.getDamageValue() + 1);
            syncTile(false);
            setChanged();
            return true;
        }
        
        return false;
    }

    /**
     * Try to consume paper from the paper slot.
     * @return true if paper was consumed
     */
    public boolean consumePaperFromTable() {
        ItemStack paper = getItem(SLOT_PAPER);
        if (!paper.isEmpty() && paper.is(Items.PAPER) && paper.getCount() > 0) {
            paper.shrink(1);
            if (paper.isEmpty()) {
                setItem(SLOT_PAPER, ItemStack.EMPTY);
            }
            syncTile(false);
            setChanged();
            return true;
        }
        return false;
    }

    // ==================== Container Validation ====================

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == SLOT_SCRIBING_TOOLS) {
            // Accept IScribeTools or any damageable item as fallback
            return stack.getItem() instanceof IScribeTools || stack.isDamageableItem();
        }
        if (slot == SLOT_PAPER) {
            return stack.is(Items.PAPER);
        }
        return false;
    }

    // ==================== Block Events ====================

    @Override
    public boolean triggerEvent(int id, int param) {
        if (id == 1) {
            // Play learn sound on client
            if (level != null && level.isClientSide()) {
                level.playLocalSound(
                        worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                        ModSounds.LEARN.get(), SoundSource.BLOCKS,
                        1.0f, 1.0f, false
                );
            }
            return true;
        }
        return super.triggerEvent(id, param);
    }

    // ==================== Renderer Helpers ====================

    /**
     * Check if research data is present (for rendering scroll).
     */
    public boolean hasResearchData() {
        return data != null;
    }

    /**
     * Check if scribe tools are present (for rendering inkwell).
     */
    public boolean hasScribeTools() {
        ItemStack tools = getItem(SLOT_SCRIBING_TOOLS);
        return !tools.isEmpty() && tools.getItem() instanceof IScribeTools;
    }
}
