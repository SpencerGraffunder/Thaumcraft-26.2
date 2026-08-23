package thaumcraft.common.tiles;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.tiles.PacketTileToClient;

import javax.annotation.Nullable;

/**
 * Base class for all Thaumcraft tile entities.
 * Provides common functionality for NBT sync and client updates.
 */
public abstract class TileThaumcraft extends BlockEntity {

    public TileThaumcraft(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // ==================== NBT Serialization ====================

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        writeSyncNBT(output);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        readSyncNBT(input);
    }

    /**
     * Write data that should be synced to clients.
     * Override in subclasses to add custom data.
     */
    protected void writeSyncNBT(CompoundTag tag) {
        // Override in subclasses
    }

    /**
     * Read data synced from server.
     * Override in subclasses to read custom data.
     */
    protected void readSyncNBT(CompoundTag tag) {
        // Override in subclasses
    }

    // ==================== Client Sync ====================

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        writeSyncNBT(tag);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /**
     * Mark this tile entity as needing sync to clients.
     */
    public void syncTile(boolean rerender) {
        if (level != null && !level.isClientSide()) {
            setChanged();
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, rerender ? 3 : 2);
        }
    }

    /**
     * Convenience method to mark dirty and sync.
     */
    public void markDirtyAndSync() {
        setChanged();
        syncTile(false);
    }

    // ==================== Custom Packet Messaging ====================

    /**
     * Handle a message received from the server.
     * Override in subclasses to process specific messages.
     * @param nbt The message data
     */
    public void messageFromServer(CompoundTag nbt) {
        // Override in subclasses
    }

    /**
     * Handle a message received from a client.
     * Override in subclasses to process specific messages.
     * @param nbt The message data
     * @param player The player who sent the message
     */
    public void messageFromClient(CompoundTag nbt, ServerPlayer player) {
        // Override in subclasses
    }

    /**
     * Send a custom message to all tracking clients.
     * @param nbt The message data
     */
    public void sendMessageToClients(CompoundTag nbt) {
        if (level != null && !level.isClientSide()) {
            PacketHandler.sendToAllTracking(new PacketTileToClient(worldPosition, nbt), this);
        }
    }

    /**
     * Send a custom message to the server.
     * @param nbt The message data
     */
    public void sendMessageToServer(CompoundTag nbt) {
        if (level != null && level.isClientSide()) {
            PacketHandler.sendToServer(new thaumcraft.common.lib.network.tiles.PacketTileToServer(worldPosition, nbt));
        }
    }
}
