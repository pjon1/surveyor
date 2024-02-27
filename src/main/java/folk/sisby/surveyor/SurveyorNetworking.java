package folk.sisby.surveyor;

import folk.sisby.surveyor.packet.c2s.C2SPacket;
import folk.sisby.surveyor.packet.c2s.OnJoinWorldC2SPacket;
import folk.sisby.surveyor.packet.s2c.OnJoinWorldS2CPacket;
import folk.sisby.surveyor.structure.StructurePieceSummary;
import folk.sisby.surveyor.structure.StructureSummary;
import it.unimi.dsi.fastutil.Pair;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SurveyorNetworking {
    public static final Identifier C2S_ON_JOIN_WORLD = new Identifier(Surveyor.ID, "c2s_on_join_world");
    public static final Identifier S2C_ON_JOIN_WORLD = new Identifier(Surveyor.ID, "s2c_on_join_world");
    public static Consumer<C2SPacket> C2S_SENDER = p -> {};

    public static void init() {
        ServerPlayNetworking.registerGlobalReceiver(C2S_ON_JOIN_WORLD, (sv, p, h, b, se) -> handleServer(p, b, OnJoinWorldC2SPacket::new, SurveyorNetworking::handleOnJoinWorld));
    }

    private static void handleOnJoinWorld(ServerPlayerEntity player, ServerWorld world, WorldSummary summary, OnJoinWorldC2SPacket packet) {
        Set<ChunkPos> serverChunkKeys = summary.getChunks();
        serverChunkKeys.removeAll(packet.terrainKeys());
        serverChunkKeys.clear();
        Collection<StructureSummary> serverStructures = summary.getStructures().stream().filter(s -> !packet.structureKeys().containsKey(s.getKey()) || !packet.structureKeys().get(s.getKey()).contains(s.getPos())).collect(Collectors.toSet());
        Map<ChunkPos, Map<RegistryKey<Structure>, Pair<RegistryKey<StructureType<?>>, Collection<StructurePieceSummary>>>> structures = new HashMap<>();
        serverStructures.forEach(s -> {
            structures.computeIfAbsent(s.getPos(), p -> new HashMap<>()).put(s.getKey(), Pair.of(s.getType(), s.getChildren()));
        });

        new OnJoinWorldS2CPacket(serverChunkKeys.stream().collect(Collectors.toMap(p -> p, summary::getChunk)), structures).send(player);
    }

    private static <T extends C2SPacket> void handleServer(ServerPlayerEntity player, PacketByteBuf buf, Function<PacketByteBuf, T> reader, ServerPacketHandler<T> handler) {
        T packet = reader.apply(buf);
        handler.handle(player, player.getServerWorld(), ((SurveyorWorld) player.getServerWorld()).surveyor$getWorldSummary(), packet);
    }

    public interface ServerPacketHandler<T extends C2SPacket> {
        void handle(ServerPlayerEntity player, ServerWorld world, WorldSummary summary, T packet);
    }
}