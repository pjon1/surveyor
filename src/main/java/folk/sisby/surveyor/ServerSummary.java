package folk.sisby.surveyor;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.World;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class ServerSummary {
    public static ServerSummary of(MinecraftServer server) {
        return ((SurveyorServer) server).surveyor$getServerSummary();
    }

    public static final String KEY_GROUPS = "groups";

    private final Map<UUID, PlayerSummary> offlineSummaries;
    private final Map<UUID, Set<UUID>> shareGroups;
    private boolean dirty = false;

    public ServerSummary(Map<UUID, PlayerSummary> offlineSummaries, Map<UUID, Set<UUID>> shareGroups) {
        this.offlineSummaries = offlineSummaries;
        this.shareGroups = shareGroups;
    }

    public static ServerSummary load(MinecraftServer server) {
        // Load Share Groups
        File folder = Surveyor.getSavePath(World.OVERWORLD, server);
        NbtCompound sharingNbt = new NbtCompound();
        File sharingFile = new File(folder, "landmarks.dat");
        if (sharingFile.exists()) {
            try {
                sharingNbt = NbtIo.readCompressed(sharingFile);
            } catch (IOException e) {
                Surveyor.LOGGER.error("[Surveyor] Error loading sharing file.", e);
            }
        }
        Map<UUID, Set<UUID>> shareGroups = new ConcurrentHashMap<>();
        sharingNbt.getList(KEY_GROUPS, NbtElement.LIST_TYPE).stream().map(l -> ((NbtList) l).stream().map(s -> UUID.fromString(s.asString())).collect(Collectors.toCollection(HashSet::new))).forEach(set -> {
            for (UUID uuid : set) {
                shareGroups.put(uuid, set);
            }
        });

        // Load Offline Summaries
        File playerFolder = server.getSavePath(WorldSavePath.ROOT).resolve("playerdata").toFile();
        Map<UUID, PlayerSummary> offlineSummaries = new ConcurrentHashMap<>(); // Only needed when there are share groups
        for (UUID uuid : new HashSet<>(shareGroups.keySet())) {
            File playerFile = playerFolder.toPath().resolve(uuid.toString() + ".dat").toFile();
            try {
                NbtCompound playerNbt = NbtIo.readCompressed(playerFile);
                offlineSummaries.put(uuid, new PlayerSummary.OfflinePlayerSummary(playerNbt));
            } catch (IOException e) {
                Surveyor.LOGGER.error("[Surveyor] Error loading offline player data for {}, removing from share groups...", uuid, e);
                shareGroups.get(uuid).remove(uuid);
                shareGroups.remove(uuid);
            }
        }

        return new ServerSummary(offlineSummaries, shareGroups);
    }

    public void save(MinecraftServer server, boolean force, boolean suppressLogs) {
        if (!suppressLogs) Surveyor.LOGGER.info("[Surveyor] Saving server data");
        for (ServerWorld world : server.getWorlds()) {
            if (!world.savingDisabled || force) WorldSummary.of(world).save(world, Surveyor.getSavePath(world.getRegistryKey(), server), suppressLogs);
        }
        File folder = Surveyor.getSavePath(World.OVERWORLD, server);
        if (dirty) {
            File sharingFile = new File(folder, "sharing.dat");
            try {
                NbtIo.writeCompressed(writeNbt(new NbtCompound()), sharingFile);
            } catch (IOException e) {
                Surveyor.LOGGER.error("[Surveyor] Error writing sharing file.", e);
            }
        }
        if (!suppressLogs) Surveyor.LOGGER.info("[Surveyor] Finished saving server data");
    }

    private NbtCompound writeNbt(NbtCompound nbt) {
        Set<Set<UUID>> groups = new HashSet<>(shareGroups.values());
        nbt.put(KEY_GROUPS, new NbtList(groups.stream().filter(s -> s.size() > 1).map(s -> (NbtElement) new NbtList(s.stream().map(u -> (NbtElement) NbtString.of(u.toString())).toList(), NbtElement.STRING_TYPE)).toList(), NbtElement.LIST_TYPE));
        return nbt;
    }

    public PlayerSummary getPlayer(UUID uuid, MinecraftServer server) {
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
        if (player != null) {
            return PlayerSummary.of(player);
        } else {
            return offlineSummaries.get(uuid);
        }
    }

    public void updatePlayer(UUID uuid, NbtCompound nbt) {
        offlineSummaries.put(uuid, new PlayerSummary.OfflinePlayerSummary(nbt));
    }

    public SurveyorExploration getExploration(UUID uuid) {
        return offlineSummaries.get(uuid).exploration();
    }

    private Set<UUID> getGroup(UUID player) {
        return shareGroups.computeIfAbsent(player, p -> new HashSet<>(Set.of(p)));
    }

    public void joinGroup(UUID player1, UUID player2) {
        if (getGroup(player1).size() > 1 && getGroup(player2).size() > 1) throw new IllegalStateException("Can't merge two groups!");
        if (getGroup(player1).size() > 1) {
            getGroup(player1).add(player2);
            shareGroups.put(player2, getGroup(player1));
        } else {
            getGroup(player2).add(player1);
            shareGroups.put(player1, getGroup(player2));
        }
        dirty = true;
    }

    public void leaveGroup(UUID player) {
        getGroup(player).remove(player); // Shares set instance with group members.
        shareGroups.put(player, new HashSet<>());
        getGroup(player).add(player);
        dirty = true;
    }

    public int groupSize(UUID player) {
        return getGroup(player).size();
    }

    public Set<PlayerSummary> groupPlayers(UUID player, MinecraftServer server) {
        return getGroup(player).stream().map(u -> getPlayer(u, server)).collect(Collectors.toSet());
    }
}
