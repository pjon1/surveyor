package folk.sisby.surveyor.landmark;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import folk.sisby.surveyor.Surveyor;
import folk.sisby.surveyor.SurveyorEvents;
import folk.sisby.surveyor.packet.c2s.OnLandmarkAddedC2SPacket;
import folk.sisby.surveyor.packet.c2s.OnLandmarkRemovedC2SPacket;
import folk.sisby.surveyor.packet.s2c.OnLandmarkAddedS2CPacket;
import folk.sisby.surveyor.packet.s2c.OnLandmarkRemovedS2CPacket;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class WorldLandmarks {
    protected final Map<LandmarkType<?>, Map<BlockPos, Landmark<?>>> landmarks;
    protected boolean dirty = true;

    public WorldLandmarks(Map<LandmarkType<?>, Map<BlockPos, Landmark<?>>> landmarks) {
        this.landmarks = landmarks;
    }

    @SuppressWarnings("unchecked")
    public <T extends Landmark<T>> Landmark<T> get(LandmarkType<T> type, BlockPos pos) {
        return (Landmark<T>) landmarks.get(type).get(pos);
    }

    @SuppressWarnings("unchecked")
    public <T extends Landmark<T>> Map<BlockPos, Landmark<T>> getAll(LandmarkType<T> type) {
        Map<BlockPos, Landmark<T>> outMap = new HashMap<>();
        if (landmarks.containsKey(type)) landmarks.get(type).forEach((pos, landmark) -> outMap.put(pos, (Landmark<T>) landmark));
        return outMap;
    }

    @SuppressWarnings("unchecked")
    public <T extends Landmark<T>> Map<BlockPos, Landmark<T>> getAll(Class<T> clazz) {
        Map<BlockPos, Landmark<T>> outMap = new HashMap<>();
        landmarks.forEach((type, map) -> {
            map.forEach((pos, landmark) -> {
                if (clazz.isAssignableFrom(landmark.getClass())) {
                    outMap.put(pos, (Landmark<T>) landmark);
                }
            });

        });
        return outMap;
    }

    public Multimap<LandmarkType<?>, BlockPos> keySet() {
        Multimap<LandmarkType<?>, BlockPos> outMap = HashMultimap.create();
        landmarks.forEach((type, map) -> outMap.putAll(type, map.keySet()));
        return outMap;
    }

    public void putLocal(World world, Landmark<?> landmark) {
        landmarks.computeIfAbsent(landmark.type(), t -> new HashMap<>()).put(landmark.pos(), landmark);
        SurveyorEvents.Invoke.landmarkAdded(world, this, landmark);
    }

    public void put(World world, Landmark<?> landmark) {
        putLocal(world, landmark);
        if (world instanceof ServerWorld sw) {
            new OnLandmarkAddedS2CPacket(landmark).send(sw);
        } else {
            new OnLandmarkAddedC2SPacket(landmark).send();
        }
    }

    public void put(PlayerEntity sender, ServerWorld world, Landmark<?> landmark) {
        putLocal(world, landmark);
        OnLandmarkAddedS2CPacket packet = new OnLandmarkAddedS2CPacket(landmark);
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.equals(sender)) continue;
            packet.send(player);
        }
    }

    public Landmark<?> removeLocal(World world, LandmarkType<?> type, BlockPos pos) {
        Landmark<?> landmark = landmarks.get(type).remove(pos);
        SurveyorEvents.Invoke.landmarkRemoved(world, this, type, pos);
        return landmark;
    }

    public void remove(World world, LandmarkType<?> type, BlockPos pos) {
        removeLocal(world, type, pos);
        if (world instanceof ServerWorld sw) {
            new OnLandmarkRemovedS2CPacket(type, pos).send(sw);
        } else {
            new OnLandmarkRemovedC2SPacket(type, pos).send();
        }
    }

    public void removeAll(World world, Class<?> clazz, BlockPos pos) {
        landmarks.forEach((type, map) -> {
            if (map.containsKey(pos) && clazz.isAssignableFrom(map.get(pos).getClass())) {
                remove(world, map.get(pos).type(), pos);
            }
        });
    }

    public void remove(ServerPlayerEntity sender, ServerWorld world, LandmarkType<?> type, BlockPos pos) {
        removeLocal(world, type, pos);
        OnLandmarkRemovedS2CPacket packet = new OnLandmarkRemovedS2CPacket(type, pos);
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.equals(sender)) continue;
            packet.send(player);
        }
    }

    public void save(World world, File folder) {
        if (dirty) {
            File landmarksFile = new File(folder, "landmarks.dat");
            try {
                NbtIo.writeCompressed(Landmarks.writeNbt(landmarks, new NbtCompound()), landmarksFile);
            } catch (IOException e) {
                Surveyor.LOGGER.error("[Surveyor] Error writing landmarks file for {}.", world.getRegistryKey().getValue(), e);
            }
        }
    }

    public static WorldLandmarks load(World world, File folder) {
        NbtCompound landmarkNbt = new NbtCompound();
        File landmarksFile = new File(folder, "landmarks.dat");
        if (landmarksFile.exists()) {
            try {
                landmarkNbt = NbtIo.readCompressed(landmarksFile);
            } catch (IOException e) {
                Surveyor.LOGGER.error("[Surveyor] Error loading landmarks file for {}.", world.getRegistryKey().getValue(), e);
            }
        }
        return new WorldLandmarks(Landmarks.fromNbt(landmarkNbt));
    }
}