package thaumcraft.common.items.casters;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;
import thaumcraft.common.menu.FocusPouchMenu;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Focus Pouch - A portable container for storing foci.
 * Can be worn as a curio (belt slot) for quick access.
 * Holds up to 18 foci.
 */
public class ItemFocusPouch extends Item implements ICuriosItemHandler {

    public static final int INVENTORY_SIZE = 18;

    /** 26.2: ICuriosItemHandler is no longer a marker interface; concrete stack
     *  handlers are required. Delegation lives in PouchCurios. */
    private final transient PouchCurios curios = new PouchCurios(this);

    /** Codec for the pouch's 18-slot item list, shared with PouchCurios. */
    public static com.mojang.serialization.Codec<java.util.List<ItemStack>> itemStackListCodec() {
        return ItemStack.OPTIONAL_CODEC.listOf();
    }

    public ItemFocusPouch() {
        super(thaumcraft.init.ItemRegistration.id(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.UNCOMMON)));
    }

    // ==================== ValueIOSerializable (via ICuriosItemHandler, 26.2) ====================

    @Override
    public void deserialize(ValueInput input) {
        input.read("items", itemStackListCodec()).ifPresent(curios::loadInventoryList);
    }

    @Override
    public void serialize(ValueOutput output) {
        output.store("items", itemStackListCodec(), curios.stacksSnapshot());
    }

    // ==================== ICuriosItemHandler (26.2 full implementation) ====================
    // Delegates to PouchCurios, which owns the 18-slot curio inventory.

    @Override
    public Map<String, ICurioStacksHandler> getCurios() {
        return curios.getCurios();
    }

    @Override
    public void setCurios(Map<String, ICurioStacksHandler> curios) {
        this.curios.setCurios(curios);
    }

    @Override
    public int getSlots() {
        return curios.getSlots();
    }

    @Override
    public void reset() {
        curios.reset();
    }

    @Override
    public Optional<ICurioStacksHandler> getStacksHandler(String type) {
        return curios.getStacksHandler(type);
    }

    @Override
    public IDynamicStackHandler getEquippedCurios() {
        return curios.getEquippedCurios();
    }

    @Override
    public void setEquippedCurio(String type, int slot, ItemStack stack) {
        curios.setEquippedCurio(type, slot, stack);
    }

    @Override
    public Optional<SlotResult> findFirstCurio(Item item) {
        return curios.findFirstCurio(item);
    }

    @Override
    public Optional<SlotResult> findFirstCurio(Predicate<ItemStack> predicate) {
        return curios.findFirstCurio(predicate);
    }

    @Override
    public Optional<SlotResult> findFirstCurio(Predicate<ItemStack> predicate, String type) {
        return curios.findFirstCurio(predicate, type);
    }

    @Override
    public List<SlotResult> findCurios(Item item) {
        return curios.findCurios(item);
    }

    @Override
    public List<SlotResult> findCurios(Predicate<ItemStack> predicate) {
        return curios.findCurios(predicate);
    }

    @Override
    public List<SlotResult> findCurios(String... types) {
        return curios.findCurios(types);
    }

    @Override
    public Optional<SlotResult> findCurio(String type, int slot) {
        return curios.findCurio(type, slot);
    }

    @Override
    public LivingEntity getWearer() {
        return curios.getWearer();
    }

    @Override
    public void loseInvalidStack(ItemStack stack) {
        curios.loseInvalidStack(stack);
    }

    @Override
    public void handleInvalidStacks() {
        curios.handleInvalidStacks();
    }

    @Override
    public int getFortuneLevel(LootContext context) {
        return curios.getFortuneLevel(context);
    }

    @Override
    public int getLootingLevel(LootContext context) {
        return curios.getLootingLevel(context);
    }

    @Override
    public ListTag saveInventory(boolean saveContents) {
        return curios.saveInventory(saveContents);
    }

    @Override
    public void loadInventory(ListTag tag) {
        curios.loadInventory(tag);
    }

    @Override
    public Set<ICurioStacksHandler> getUpdatingInventories() {
        return curios.getUpdatingInventories();
    }

    @Override
    public void addPermanentSlotModifiers(Multimap<String, AttributeModifier> modifiers) {
        curios.addPermanentSlotModifiers(modifiers);
    }

    @Override
    public void removeSlotModifiers(Multimap<String, AttributeModifier> modifiers) {
        curios.removeSlotModifiers(modifiers);
    }

    @Override
    public Multimap<String, AttributeModifier> getModifiers() {
        return curios.getModifiers();
    }

    @Override
    public void loadDatapacks() {
        curios.loadDatapacks();
    }

    @Override
    public Tag writeTag() {
        return curios.writeTag();
    }

    @Override
    public void readTag(Tag tag) {
        curios.readTag(tag);
    }

    @Override
    public void clearCachedSlotModifiers() {
        // No cached slot modifiers to clear - this item carries no attribute modifiers
    }

    @Override
    public void clearSlotModifiers() {
        // No slot modifiers to clear.
    }

    @Override
    public void addTransientSlotModifiers(Multimap<String, AttributeModifier> modifiers) {
        // The focus pouch carries no slot modifiers.
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            final InteractionHand usedHand = hand;
            serverPlayer.openMenu(new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.translatable("container.thaumcraft.focus_pouch");
                }

                @Override
                public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player menuPlayer) {
                    return new FocusPouchMenu(containerId, playerInventory, usedHand);
                }
            }, (buf) -> {
                buf.writeEnum(usedHand);
            });
        }
        
        return InteractionResult.SUCCESS;
    }

    /**
     * Get the inventory contents of this pouch.
     */
    public NonNullList<ItemStack> getInventory(ItemStack item) {
        NonNullList<ItemStack> stackList = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
        CompoundTag data = item.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (data != null && data.contains("items")) {
            List<ItemStack> loaded = ItemStack.OPTIONAL_CODEC.listOf()
                    .parse(NbtOps.INSTANCE, data.get("items")).resultOrPartial().orElse(List.of());
            for (int i = 0; i < stackList.size() && i < loaded.size(); i++) {
                stackList.set(i, loaded.get(i));
            }
        }
        return stackList;
    }

    /**
     * Set the inventory contents of this pouch.
     */
    public void setInventory(ItemStack item, NonNullList<ItemStack> stackList) {
        CompoundTag tag = item.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        List<ItemStack> list = new java.util.ArrayList<>();
        for (ItemStack stack : stackList) {
            list.add(stack);
        }
        tag.put("items", ItemStack.OPTIONAL_CODEC.listOf()
                .encodeStart(NbtOps.INSTANCE, list).resultOrPartial().orElse(new ListTag()));
        CustomData.set(DataComponents.CUSTOM_DATA, item, tag);
    }

    /**
     * Count how many foci are stored in this pouch.
     */
    public int getFociCount(ItemStack item) {
        NonNullList<ItemStack> inv = getInventory(item);
        int count = 0;
        for (ItemStack stack : inv) {
            if (!stack.isEmpty() && stack.getItem() instanceof ItemFocus) {
                count++;
            }
        }
        return count;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        int count = getFociCount(stack);
        if (count > 0) {
            builder.accept(Component.translatable("item.thaumcraft.focus_pouch.contents", count, INVENTORY_SIZE)
                    .withStyle(style -> style.withColor(0x808080)));
        } else {
            builder.accept(Component.translatable("item.thaumcraft.focus_pouch.empty")
                    .withStyle(style -> style.withColor(0x808080)));
        }
        super.appendHoverText(stack, context, display, builder, flag);
    }

    // ==================== Curios Integration ====================
    // The pouch can be worn in the Thaumcraft belt slot for quick access.
    // The slot definitions are provided by the Thaumcraft "thaumcraft_focus" curio
    // slot type; the handler implementation is in PouchCurios.
}
