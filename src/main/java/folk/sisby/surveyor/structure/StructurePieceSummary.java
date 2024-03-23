package folk.sisby.surveyor.structure;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.structure.StructureContext;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;

public class StructurePieceSummary extends StructurePiece {
    protected final NbtCompound pieceNbt;

    public StructurePieceSummary(StructurePieceType type, int chainLength, BlockBox boundingBox, NbtCompound pieceNbt) {
        super(type, chainLength, boundingBox);
        this.pieceNbt = pieceNbt;
    }

    public StructurePieceSummary(NbtCompound nbt) {
        super(Registries.STRUCTURE_PIECE.get(new Identifier(nbt.getString("id"))), nbt);
        this.pieceNbt = nbt.getCompound("nbt");
    }

    public static StructurePieceSummary fromPiece(StructureContext context, StructurePiece piece) {
        StructurePieceSummary summary = new StructurePieceSummary(piece.getType(), piece.getChainLength(), piece.getBoundingBox(), new NbtCompound());
        NbtCompound summaryNbt = summary.toNbt();
        NbtCompound pieceNbt = piece.toNbt(context);
        for (String key : summaryNbt.getKeys()) {
            pieceNbt.remove(key);
        }
        for (String key : pieceNbt.getKeys()) {
            summary.pieceNbt.put(key, pieceNbt.get(key));
        }
        return summary;
    }

    public final NbtCompound toNbt() {
        return toNbt(null); // context only used for writeNbt
    }

    @Override
    protected void writeNbt(StructureContext context, NbtCompound nbt) {
        if (!pieceNbt.isEmpty()) nbt.put("nbt", pieceNbt);
    }

    @Override
    public void generate(StructureWorldAccess world, StructureAccessor structureAccessor, ChunkGenerator chunkGenerator, Random random, BlockBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
    }
}
