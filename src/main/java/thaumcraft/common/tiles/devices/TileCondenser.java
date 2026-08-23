package thaumcraft.common.tiles.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.common.tiles.TileThaumcraft;
import thaumcraft.init.ModBlockEntities;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Flux Condenser tile entity - converts flux from the aura into Vitium essentia.
 * Requires a lattice structure above it to function efficiently.
 * Consumes essentia as fuel and drains flux from the local aura.
 */
public class TileCondenser extends TileThaumcraft implements IEssentiaTransport {

    private static final int MAX_ESSENTIA = 100;
    private static final int MAX_FLUX = 100;
    private static final int MAX_LATTICE_COUNT = 40;
    private static final int BASE_INTERVAL = 600;
    private static final int MIN_INTERVAL = 5;

    private int essentia = 0;   // Fuel essentia
    private int flux = 0;       // Output Vitium essentia
    private int count = 0;
    
    // Lattice tracking
    private float latticeCount = -1.0f;
    public int interval = 0;
    public int cost = 0;
    private Set<Long> blockList = new HashSet<>();
    private List<Long> uncloggedList = new ArrayList<>();

    public TileCondenser(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public TileCondenser(BlockPos pos, BlockState state) {
        this(ModBlockEntities.CONDENSER.get(), pos, state);
    }

    // ==================== NBT ====================

    @Override
    protected void writeSyncNBT(CompoundTag tag) {
        super.writeSyncNBT(tag);
        tag.putShort("Essentia", (short) essentia);
        tag.putShort("Flux", (short) flux);
    }

    @Override
    protected void readSyncNBT(CompoundTag tag) {
        super.readSyncNBT(tag);
        essentia = tag.getShortOr("Essentia", (short)0);
        flux = tag.getShortOr("Flux", (short)0);
    }

    // ==================== Tick ====================

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileCondenser tile) {
        // Initial lattice check
        if (tile.latticeCount < 0) {
            tile.triggerCheck();
        }

        tile.count++;

        if (!tile.isEnabled(state) || tile.latticeCount <= 0) return;

        // Fill essentia from connected sources
        if (tile.count % 5 == 0 && tile.essentia < MAX_ESSENTIA) {
            tile.fillEssentia();
        }

        // Condense flux from aura
        if (tile.interval > 0 && 
            tile.essentia >= tile.cost && 
            tile.flux < MAX_FLUX && 
            tile.count % tile.interval == 0) {
            
            float auraFlux = AuraHelper.getFlux(level, pos);
            if (auraFlux >= 1.0f) {
                AuraHelper.drainFlux(level, pos, 1.0f, false);
                tile.essentia -= tile.cost;
                tile.flux++;
                
                // Small chance to dirty a lattice block
                if (level.getRandom().nextInt(50) == 0) {
                    tile.makeLatticeDirty();
                }
                
                tile.syncTile(false);
                tile.setChanged();
            }
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileCondenser tile) {
        // Client-side particle effects
        if (tile.essentia > 0 && !tile.uncloggedList.isEmpty() && tile.count % Math.max(3, tile.interval / 50) == 0) {
            // TODO: Spawn spark particles at random lattice block
        }
        tile.count++;
    }

    private boolean isEnabled(BlockState state) {
        if (state.hasProperty(BlockStateProperties.ENABLED)) {
            return state.getValue(BlockStateProperties.ENABLED);
        }
        return true;
    }

    /**
     * Fill essentia fuel from connected essentia sources.
     */
    private void fillEssentia() {
        if (level == null) return;

        for (Direction face : Direction.Plane.HORIZONTAL) {
            BlockEntity te = ThaumcraftApiHelper.getConnectableTile(level, worldPosition, face);
            if (te instanceof IEssentiaTransport transport) {
                Direction opposite = face.getOpposite();
                
                if (!transport.canOutputTo(opposite)) continue;
                
                if (transport.getEssentiaAmount(opposite) > 0 &&
                    transport.getSuctionAmount(opposite) < getSuctionAmount(face) &&
                    getSuctionAmount(face) >= transport.getMinimumSuction()) {
                    
                    Aspect type = transport.getEssentiaType(opposite);
                    if (type != null && type != Aspect.FLUX) {
                        int taken = transport.takeEssentia(type, 1, opposite);
                        essentia += taken;
                        syncTile(false);
                        setChanged();
                        
                        if (essentia >= MAX_ESSENTIA) break;
                    } else if (type == Aspect.FLUX) {
                        // Flux essentia dirties the lattice
                        makeLatticeDirty();
                    }
                }
            }
        }
    }

    /**
     * Mark a random lattice block as dirty (clogged).
     */
    private void makeLatticeDirty() {
        // TODO: When condenser lattice blocks are implemented,
        // pick a random unclogged lattice and change it to dirty state
        if (!uncloggedList.isEmpty() && level != null) {
            int index = level.getRandom().nextInt(uncloggedList.size());
            if (index == 0 && uncloggedList.size() > 1) {
                index = level.getRandom().nextInt(uncloggedList.size());
            }
            // BlockPos p = BlockPos.of(uncloggedList.get(index));
            // Change block to dirty lattice
        }
    }

    /**
     * Scan for lattice blocks above the condenser.
     */
    public void triggerCheck() {
        blockList.clear();
        uncloggedList.clear();
        latticeCount = 0;
        interval = 0;
        
        // Perform recursive lattice search
        Set<Long> visited = new HashSet<>();
        performCheck(worldPosition, true, false, visited);
        
        if (latticeCount <= 0) {
            latticeCount = 0;
        } else {
            if (latticeCount > MAX_LATTICE_COUNT) {
                latticeCount = MAX_LATTICE_COUNT;
            }
            // Calculate interval based on lattice count
            interval = Math.round(BASE_INTERVAL - latticeCount * 15.0f);
            if (interval < MIN_INTERVAL) {
                interval = MIN_INTERVAL;
            }
            // Calculate cost based on structure size
            cost = (int) (4.0 + Math.sqrt(blockList.size()));
        }
    }

    private void performCheck(BlockPos pos, boolean skip, boolean clogged, Set<Long> visited) {
        if (latticeCount < 0 || level == null) return;
        
        visited.add(pos.asLong());
        boolean found = false;
        int sides = 0;

        for (Direction face : Direction.values()) {
            if (skip && face != Direction.UP) continue;

            BlockPos checkPos = pos.relative(face);
            BlockState state = level.getBlockState(checkPos);
            
            // TODO: Check for condenser lattice blocks
            // boolean isLattice = state.is(ModBlocks.CONDENSER_LATTICE.get());
            // boolean isDirtyLattice = state.is(ModBlocks.CONDENSER_LATTICE_DIRTY.get());
            boolean isLattice = false;
            boolean isDirtyLattice = false;

            if (skip && isDirtyLattice) {
                clogged = true;
            }
            if (isLattice || isDirtyLattice) {
                sides++;
            }

            if (!visited.contains(checkPos.asLong())) {
                // Check for another condenser below (invalid)
                if (face == Direction.DOWN) {
                    // TODO: Check if block is condenser
                    // if (state.is(ModBlocks.CONDENSER.get())) {
                    //     latticeCount = -99;
                    //     return;
                    // }
                }

                // Only count lattice blocks above the condenser
                if (worldPosition.getY() < checkPos.getY()) {
                    double distSq = worldPosition.distSqr(checkPos);
                    if (distSq <= 74) { // Within range
                        if (isLattice || isDirtyLattice) {
                            blockList.add(checkPos.asLong());
                            if (isLattice) {
                                uncloggedList.add(checkPos.asLong());
                            }
                            found = true;
                            performCheck(checkPos, false, clogged || isDirtyLattice, visited);
                            if (skip) break;
                        }
                    }
                }
            }
        }

        if (found && !clogged) {
            float bonus = 1.0f - 0.15f * sides;
            latticeCount += bonus;
        }
    }

    // ==================== IEssentiaTransport ====================

    @Override
    public boolean isConnectable(Direction face) {
        return face != Direction.UP;
    }

    @Override
    public boolean canInputFrom(Direction face) {
        return face != Direction.UP && face != Direction.DOWN;
    }

    @Override
    public boolean canOutputTo(Direction face) {
        return face == Direction.DOWN;
    }

    @Override
    public void setSuction(Aspect aspect, int amount) {
        // Not used
    }

    @Override
    public Aspect getSuctionType(Direction face) {
        return null; // Accepts any non-flux essentia
    }

    @Override
    public int getSuctionAmount(Direction face) {
        if (face == Direction.DOWN || essentia >= MAX_ESSENTIA) {
            return 0;
        }
        return 128;
    }

    @Override
    public int takeEssentia(Aspect aspect, int amount, Direction face) {
        if (!canOutputTo(face)) return 0;
        if (aspect != null && aspect != Aspect.FLUX) return 0;
        
        int taken = Math.min(amount, flux);
        if (taken > 0) {
            flux -= taken;
            syncTile(false);
            setChanged();
        }
        return taken;
    }

    @Override
    public int addEssentia(Aspect aspect, int amount, Direction face) {
        if (!canInputFrom(face)) return 0;
        
        int added = Math.min(amount, MAX_ESSENTIA - essentia);
        if (added > 0) {
            essentia += added;
            syncTile(false);
            setChanged();
        }
        return added;
    }

    @Override
    public Aspect getEssentiaType(Direction face) {
        return Aspect.FLUX;
    }

    @Override
    public int getEssentiaAmount(Direction face) {
        return flux;
    }

    @Override
    public int getMinimumSuction() {
        return 0;
    }

    // ==================== Getters ====================

    public int getEssentia() {
        return essentia;
    }

    public int getFlux() {
        return flux;
    }

    public float getLatticeCount() {
        return latticeCount;
    }
}
