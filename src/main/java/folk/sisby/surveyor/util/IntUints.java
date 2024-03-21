package folk.sisby.surveyor.util;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtInt;
import net.minecraft.network.PacketByteBuf;

import java.util.BitSet;

public record IntUints(int value) implements UIntArray {
    public static final int TYPE = NbtElement.INT_TYPE;

    public static UIntArray fromNbt(NbtElement nbt) {
        return new IntUints(((NbtInt) nbt).intValue());
    }

    public static UIntArray fromBuf(PacketByteBuf buf) {
        return new IntUints(buf.readVarInt());
    }

    @Override
    public int getType() {
        return TYPE;
    }

    @Override
    public int[] getUncompressed() {
        return ArrayUtil.ofSingle(value, 256);
    }

    @Override
    public int[] getUnmasked(BitSet mask) {
        return ArrayUtil.ofSingle(value, 256);
    }

    @Override
    public void writeNbt(NbtCompound nbt, String key) {
        nbt.putInt(key, value);
    }

    @Override
    public void writeBuf(PacketByteBuf buf) {
        buf.writeVarInt(value);
    }

    @Override
    public int get(int i) {
        return value;
    }
}
