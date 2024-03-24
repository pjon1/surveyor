package folk.sisby.surveyor.util.uints;

import folk.sisby.surveyor.util.ArrayUtil;
import net.minecraft.nbt.NbtByteArray;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.PacketByteBuf;

import java.util.BitSet;

public record ByteArrayUInts(byte[] value) implements UIntArray {
    public static final int TYPE = NbtElement.BYTE_ARRAY_TYPE;

    public static UIntArray fromNbt(NbtElement nbt) {
        return new ByteArrayUInts(((NbtByteArray) nbt).getByteArray());
    }

    public static UIntArray fromBuf(PacketByteBuf buf) {
        return new ByteArrayUInts(buf.readByteArray());
    }

    @Override
    public int getType() {
        return TYPE;
    }

    @Override
    public int[] getUncompressed() {
        int[] uncompressed = ArrayUtil.ofSingle(-1, 256);
        for (int i = 0; i < value.length; i++) {
            uncompressed[i] = value[i] + UINT_BYTE_OFFSET;
        }
        return uncompressed;
    }

    @Override
    public int[] getUnmasked(BitSet mask) {
        int[] unmasked = new int[256];
        int maskedIndex = 0;
        for (int i = 0; i < 256; i++) {
            if (mask.get(i)) {
                unmasked[i] = value[maskedIndex] + UINT_BYTE_OFFSET;
                maskedIndex++;
            }
        }
        return unmasked;
    }

    @Override
    public void writeNbt(NbtCompound nbt, String key) {
        nbt.putByteArray(key, value);
    }

    @Override
    public void writeBuf(PacketByteBuf buf) {
        buf.writeByteArray(value);
    }

    @Override
    public int get(int i) {
        return value[i] + UINT_BYTE_OFFSET;
    }
}