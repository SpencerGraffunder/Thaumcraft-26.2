package thaumcraft.common.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.network.chat.Component;
import thaumcraft.api.golems.seals.ISeal;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.seals.SealPos;
import thaumcraft.common.golems.seals.SealEntity;
import thaumcraft.common.golems.seals.SealHandler;
import thaumcraft.init.ModMenuTypes;

/**
 * SealMenuProvider - MenuProvider for opening the seal configuration GUI.
 */
public class SealMenuProvider implements MenuProvider {
    
    private final ISealEntity seal;
    
    public SealMenuProvider(ISealEntity seal) {
        this.seal = seal;
    }
    
    @Override
    public Component getDisplayName() {
        // 1.12 titles the seal config GUI with the seal being configured
        ISeal seal = this.seal != null ? this.seal.getSeal() : null;
        if (seal != null) {
            String key = seal.getKey();
            int i = key.indexOf(':');
            return Component.translatable("item.thaumcraft.seal." + (i >= 0 ? key.substring(i + 1) : key));
        }
        return Component.translatable("item.thaumcraft.seal.blank");
    }
    
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new SealMenu(containerId, playerInventory, seal);
    }
    
    /**
     * Write the seal position to the buffer for client-side menu creation.
     */
    public static void writeSealPos(FriendlyByteBuf buf, ISealEntity seal) {
        buf.writeBlockPos(seal.getSealPos().pos);
        buf.writeByte((byte) seal.getSealPos().face.ordinal());
    }
    
    /**
     * Read a seal from the buffer (used by client-side menu creation).
     */
    public static ISealEntity readSealFromBuffer(Player player, FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        Direction face = Direction.values()[buf.readByte()];
        SealPos sealPos = new SealPos(pos, face);
        return SealHandler.getSealEntity(player.level().dimension(), sealPos);
    }
}
