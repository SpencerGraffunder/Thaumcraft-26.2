package thaumcraft.common.items.casters;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootContext;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * 26.2 port: ICuriosItemHandler implementations require concrete stack handlers
 * (the 1.16/1.18 CurioItemHandler base class no longer exists in the Curios 16.x API).
 * This class provides the ICurioStacksHandler + IDynamicStackHandler pair backing
 * the focus pouch's 18 slots when it is worn as a curio.
 *
 * Data flow: the in-memory {@link #stacks} list is persisted via
 * {@code saveInventory}/{@code loadInventory} (called by the Curios lib on
 * unequip/equip) and via {@code writeTag}/{@code readTag}.
 */
public class PouchCurios {

    public static final String SLOT_TYPE = "thaumcraft_focus";

    private final ItemFocusPouch pouch;
    private final NonNullList<ItemStack> stacks;
    private transient LivingEntity wearer;
    private final IDynamicStackHandler dynamic;
    private ICurioStacksHandler handler;
    private final Map<String, ICurioStacksHandler> handlers = new HashMap<>();

    public PouchCurios(ItemFocusPouch pouch) {
        this.pouch = pouch;
        this.stacks = NonNullList.withSize(ItemFocusPouch.INVENTORY_SIZE, ItemStack.EMPTY);
        this.dynamic = new DynamicStacks();
        this.handlers.put(SLOT_TYPE, new CurioStacks());
    }

    // ==================== ICuriosItemHandler ====================

    public Map<String, ICurioStacksHandler> getCurios() {
        return handlers;
    }

    public void setCurios(Map<String, ICurioStacksHandler> map) {
        handlers.clear();
        handlers.putAll(map);
    }

    public int getSlots() {
        return stacks.size();
    }

    public void reset() {
        for (int i = 0; i < stacks.size(); i++) stacks.set(i, ItemStack.EMPTY);
    }

    @Nullable
    public ICurioStacksHandler getHandler() {
        return handler != null ? handler : (handler = handlers.get(SLOT_TYPE));
    }

    public Optional<ICurioStacksHandler> getStacksHandler(String type) {
        return Optional.ofNullable(handlers.get(type));
    }

    public IDynamicStackHandler getEquippedCurios() {
        return dynamic;
    }

    public void setEquippedCurio(String type, int slot, ItemStack stack) {
        if (type.equals(SLOT_TYPE) && slot >= 0 && slot < stacks.size()) {
            stacks.set(slot, stack);
        }
    }

    public Optional<SlotResult> findFirstCurio(Item item) {
        return findFirstCurio(s -> s.getItem() == item);
    }

    public Optional<SlotResult> findFirstCurio(Predicate<ItemStack> predicate) {
        return findFirstCurio(predicate, SLOT_TYPE);
    }

    public Optional<SlotResult> findFirstCurio(Predicate<ItemStack> predicate, String type) {
        for (int i = 0; i < stacks.size(); i++) {
            if (predicate.test(stacks.get(i))) {
                return Optional.of(new SlotResult(new SlotContext(SLOT_TYPE, wearer, i, false, true), stacks.get(i)));
            }
        }
        return Optional.empty();
    }

    public List<SlotResult> findCurios(Item item) {
        return findCurios(s -> s.getItem() == item);
    }

    public List<SlotResult> findCurios(Predicate<ItemStack> predicate) {
        List<SlotResult> out = new ArrayList<>();
        for (int i = 0; i < stacks.size(); i++) {
            if (predicate.test(stacks.get(i))) {
                out.add(new SlotResult(new SlotContext(SLOT_TYPE, wearer, i, false, true), stacks.get(i)));
            }
        }
        return out;
    }

    public List<SlotResult> findCurios(String... types) {
        for (String type : types) {
            if (type.equals(SLOT_TYPE)) return findCurios(s -> !s.isEmpty());
        }
        return Collections.emptyList();
    }

    public Optional<SlotResult> findCurio(String type, int slot) {
        if (type.equals(SLOT_TYPE) && slot >= 0 && slot < stacks.size() && !stacks.get(slot).isEmpty()) {
            return Optional.of(new SlotResult(new SlotContext(SLOT_TYPE, wearer, slot, false, true), stacks.get(slot)));
        }
        return Optional.empty();
    }

    public LivingEntity getWearer() {
        return wearer;
    }

    public void setWearer(LivingEntity entity) {
        this.wearer = entity;
    }

    public void loseInvalidStack(ItemStack stack) {
        for (int i = 0; i < stacks.size(); i++) {
            if (stacks.get(i) == stack) stacks.set(i, ItemStack.EMPTY);
        }
    }

    public void handleInvalidStacks() {
        for (int i = 0; i < stacks.size(); i++) {
            if (stacks.get(i).isEmpty()) stacks.set(i, ItemStack.EMPTY);
        }
    }

    public int getFortuneLevel(LootContext context) {
        return 0;
    }

    public int getLootingLevel(LootContext context) {
        return 0;
    }

    public ListTag saveInventory(boolean saveContents) {
        if (!saveContents) return new ListTag();
        List<ItemStack> list = new ArrayList<>();
        for (ItemStack s : stacks) list.add(s);
        return (ListTag) ItemFocusPouch.itemStackListCodec().encodeStart(NbtOps.INSTANCE, list).resultOrPartial().orElse(new ListTag());
    }

    public void loadInventory(ListTag tag) {
        List<ItemStack> loaded = ItemFocusPouch.itemStackListCodec().parse(NbtOps.INSTANCE, tag).resultOrPartial().orElse(List.of());
        for (int i = 0; i < stacks.size(); i++) {
            stacks.set(i, i < loaded.size() ? loaded.get(i) : ItemStack.EMPTY);
        }
    }

    public Set<ICurioStacksHandler> getUpdatingInventories() {
        // 26.2: Map.values() returns a Collection; unmodifiableSet requires a Set.
        return Collections.unmodifiableSet(new HashSet<>(handlers.values()));
    }

    public void addPermanentSlotModifiers(Multimap<String, AttributeModifier> modifiers) {
        // The focus pouch carries no slot modifiers.
    }

    public void removeSlotModifiers(Multimap<String, AttributeModifier> modifiers) {
        // No-op.
    }

    public Multimap<String, AttributeModifier> getModifiers() {
        return HashMultimap.create();
    }

    public void loadDatapacks() {
        // No-op.
    }

    /** Snapshot of the 18 slots as a plain list (for ValueIOSerializable). */
    public List<ItemStack> stacksSnapshot() {
        return new ArrayList<>(stacks);
    }

    /** Load a decoded slot list directly (for ValueIOSerializable). */
    public void loadInventoryList(List<ItemStack> loaded) {
        for (int i = 0; i < stacks.size(); i++) {
            stacks.set(i, i < loaded.size() ? loaded.get(i) : ItemStack.EMPTY);
        }
    }

    public Tag writeTag() {
        CompoundTag tag = new CompoundTag();
        tag.put("items", ItemFocusPouch.itemStackListCodec().encodeStart(NbtOps.INSTANCE, new ArrayList<>(stacks)).resultOrPartial().orElse(new ListTag()));
        return tag;
    }

    public void readTag(Tag tag) {
        if (tag instanceof CompoundTag compound && compound.contains("items")) {
            // 26.2: CompoundTag.getList(String) returns Optional<ListTag>; use getListOrEmpty.
            loadInventory(compound.getListOrEmpty("items"));
        }
    }

    public void clearCachedSlotModifiers() {
        // No-op.
    }

    // ==================== ICurioStacksHandler ====================

    private class CurioStacks implements ICurioStacksHandler {
        // 26.2: NonNullList.withSize(int, E) takes a fill value, not a supplier (and NonNullList moved to net.minecraft.core).
        private final NonNullList<Boolean> renders = NonNullList.withSize(ItemFocusPouch.INVENTORY_SIZE, true);

        @Override public IDynamicStackHandler getStacks() { return dynamic; }
        @Override public IDynamicStackHandler getCosmeticStacks() { return dynamic; }
        @Override public NonNullList<Boolean> getRenders() { return renders; }
        @Override public int getSlots() { return stacks.size(); }
        @Override public int getBaseSize() { return stacks.size(); }
        @Override public boolean isVisible() { return true; }
        @Override public boolean hasCosmetic() { return false; }
        @Override public String getIdentifier() { return SLOT_TYPE; }
        @Override public Map<Identifier, AttributeModifier> getModifiers() { return Collections.emptyMap(); }
        @Override public Set<AttributeModifier> getPermanentModifiers() { return Collections.emptySet(); }
        @Override public Set<AttributeModifier> getCachedModifiers() { return Collections.emptySet(); }
        @Override public Collection<AttributeModifier> getModifiersByOperation(AttributeModifier.Operation operation) { return Collections.emptySet(); }
        @Override public void addTransientModifier(AttributeModifier modifier) { }
        @Override public void addPermanentModifier(AttributeModifier modifier) { }
        @Override public void removeModifier(Identifier id) { }
        @Override public void clearModifiers() { }
        @Override public void clearCachedModifiers() { }
        @Override public void copyModifiers(ICurioStacksHandler other) { }
        @Override public void update() { }
        @Override public void serialize(ValueOutput output) { /* persisted via writeTag */ }
        @Override public void deserialize(ValueInput input) { /* restored via readTag */ }
    }

    // ==================== IDynamicStackHandler / IItemHandlerModifiable ====================

    private class DynamicStacks implements IDynamicStackHandler {
        @Override public ItemStack getStackInSlot(int slot) { return slot >= 0 && slot < stacks.size() ? stacks.get(slot) : ItemStack.EMPTY; }
        @Override public void setStackInSlot(int slot, ItemStack stack) { if (slot >= 0 && slot < stacks.size()) stacks.set(slot, stack); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot < 0 || slot >= stacks.size() || !isItemValid(slot, stack)) return stack;
            if (!simulate) {
                stacks.set(slot, stack);
                markDirty();
            }
            return ItemStack.EMPTY;
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot < 0 || slot >= stacks.size() || amount <= 0) return ItemStack.EMPTY;
            ItemStack existing = stacks.get(slot);
            if (existing.isEmpty()) return ItemStack.EMPTY;
            int toExtract = Math.min(amount, existing.getCount());
            if (!simulate) {
                if (toExtract >= existing.getCount()) stacks.set(slot, ItemStack.EMPTY);
                else existing.shrink(toExtract);
                markDirty();
            }
            return existing.copyWithCount(toExtract);
        }
        @Override public int getSlotLimit(int slot) { return 64; }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return true; }
        @Override public void setPreviousStackInSlot(int slot, ItemStack stack) { }
        @Override public ItemStack getPreviousStackInSlot(int slot) { return ItemStack.EMPTY; }
        @Override public int getSlots() { return stacks.size(); }
        @Override public void grow(int by) { }
        @Override public void shrink(int by) { }
        private void markDirty() {
            // Persist into the wearer's curio slot NBT is handled by the Curios lib
            // via saveInventory/writeTag; nothing extra needed here.
        }
        @Override public void serialize(ValueOutput output) { /* persisted via writeTag */ }
        @Override public void deserialize(ValueInput input) { /* restored via readTag */ }
    }
}
