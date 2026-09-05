package thaumcraft.api.golems.seals;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.Objects;

/**
 * Represents a seal's position in the world, including the block face it's attached to.
 */
public class SealPos {

    public BlockPos pos;
    public Direction face;

    public SealPos(BlockPos pos, Direction face) {
        this.pos = pos;
        this.face = face;
    }

    @Override
    public int hashCode() {
        byte faceOrdinal = (byte) (face.ordinal() + 1);
        int i = 31 * faceOrdinal + pos.getX();
        i = 31 * i + pos.getY();
        i = 31 * i + pos.getZ();
        return i;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SealPos other)) {
            return false;
        }
        return Objects.equals(pos, other.pos) && face == other.face;
    }

    @Override
    public String toString() {
        return "SealPos{pos=" + pos + ", face=" + face + "}";
    }

    /**
     * Converts this SealPos to a unique long value for storage.
     *
     * BlockPos.asLong() already occupies all 64 bits (x in bits 32-63), so the
     * face cannot simply be XORed onto the top bits -- that collides with x's
     * sign-extension and corrupts the face for any negative x. Instead we pack
     * explicit, non-overlapping fields that cover the full valid world:
     *   bits 0-25   = z      (26-bit signed, +-33.5M, covers MC +-30M)
     *   bits 26-34  = y + 64 (9-bit, covers y in -64..512)
     *   bits 35-60  = x      (26-bit signed, +-33.5M, covers MC +-30M)
     *   bits 61-63  = face   (3-bit ordinal, 0-5)
     */
    public long toLong() {
        long z = (long) pos.getZ() & 0x3FFFFFFL;
        long y = ((long) pos.getY() + 64L) & 0x1FFL;
        long x = (long) pos.getX() & 0x3FFFFFFL;
        return z | (y << 26) | (x << 35) | ((long) face.ordinal() << 61);
    }

    /**
     * Creates a SealPos from a long value produced by {@link #toLong()}.
     */
    public static SealPos fromLong(long value) {
        int faceOrdinal = (int) ((value >>> 61) & 0x7);
        int x = (int) ((value >>> 35) & 0x3FFFFFFL);
        x = (int) (((long) x << 38) >> 38);   // sign-extend 26-bit two's-complement
        int y = (int) ((value >>> 26) & 0x1FFL);
        y -= 64;
        int z = (int) (value & 0x3FFFFFFL);
        z = (int) (((long) z << 38) >> 38);   // sign-extend 26-bit two's-complement
        return new SealPos(new BlockPos(x, y, z), Direction.values()[faceOrdinal]);
    }
}
