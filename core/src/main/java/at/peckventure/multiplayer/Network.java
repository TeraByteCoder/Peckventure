package at.peckventure.multiplayer;
import com.esotericsoftware.kryo.Kryo;
public class Network {
    public static void register(Kryo kryo) {
        //Chunk Packets
        kryo.register(NetworkPackets.ChunkRequestPacket.class);
        kryo.register(NetworkPackets.ChunkDataPacket.class);
        kryo.register(byte[].class);

        //Chat Packets
        kryo.register(NetworkPackets.ChatMessagePacket.class);


        //inventory Packets
        kryo.register(NetworkPackets.ItemDropPacket.class);
        kryo.register(NetworkPackets.InventoryMovePacket.class);
        kryo.register(NetworkPackets.InventoryUpdatePacket.class);

        // Player packets
        kryo.register(NetworkPackets.PlayerUpdatePacket.class);
        kryo.register(NetworkPackets.PlayerListPacket.class);
        kryo.register(java.util.HashSet.class);

        // connect/disconnect packets
        kryo.register(NetworkPackets.ClientConnectPacket.class);
        kryo.register(NetworkPackets.ServerConnectPacket.class);
        kryo.register(NetworkPackets.ClientDisconnectPacket.class);

        //ping
        kryo.register(NetworkPackets.PingPacket.class);
        kryo.register(NetworkPackets.PingRequestPacket.class);
    }
}
