package at.peckventure.multiplayer;
import com.esotericsoftware.kryo.Kryo;
public class Network {
    public static void register(Kryo kryo) {
        kryo.register(NetworkPackets.ChunkRequestPacket.class);
        kryo.register(NetworkPackets.ChunkDataPacket.class);
        kryo.register(NetworkPackets.ChatMessagePacket.class);
        kryo.register(NetworkPackets.InventoryUpdatePacket.class);
        kryo.register(NetworkPackets.PlayerUpdatePacket.class);
        kryo.register(NetworkPackets.PlayerListPacket.class);
        kryo.register(NetworkPackets.ClientConnectPacket.class);
        kryo.register(NetworkPackets.ServerConnectPacket.class);
        kryo.register(java.util.HashSet.class);
        kryo.register(NetworkPackets.ClientDisconnectPacket.class);
    }
}
