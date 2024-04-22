package folk.sisby.surveyor.packet;

import folk.sisby.surveyor.PlayerSummary;
import folk.sisby.surveyor.Surveyor;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.UUID;

public record S2CGroupChangedPacket(Map<UUID, PlayerSummary> players) implements S2CPacket {
    public static final Identifier ID = new Identifier(Surveyor.ID, "s2c_group_changed");

    public static S2CGroupChangedPacket read(PacketByteBuf buf) {
        return new S2CGroupChangedPacket(buf.readMap(PacketByteBuf::readUuid, PlayerSummary.OfflinePlayerSummary::readBuf));
    }

    @Override
    public void writeBuf(PacketByteBuf buf) {
        buf.writeMap(players, PacketByteBuf::writeUuid, PlayerSummary.OfflinePlayerSummary::writeBuf);
    }

    @Override
    public Identifier getId() {
        return ID;
    }
}