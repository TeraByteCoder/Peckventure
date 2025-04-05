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
        kryo.register(NetworkPackets.ItemUsePacket.class);

        // Player packets
        kryo.register(NetworkPackets.PlayerUpdatePacket.class);
        kryo.register(NetworkPackets.PlayerListPacket.class);
        kryo.register(java.util.HashSet.class);
        kryo.register(NetworkPackets.ServerPositionChangePacket.class);

        // connect/disconnect packets
        kryo.register(NetworkPackets.ClientConnectPacket.class);
        kryo.register(NetworkPackets.ServerConnectPacket.class);
        kryo.register(NetworkPackets.ClientDisconnectPacket.class);

        //ping
        kryo.register(NetworkPackets.PingResponsePacket.class);
        kryo.register(NetworkPackets.PingRequestPacket.class);

        //mobs
        kryo.register(java.util.ArrayList.class);
        kryo.register(NetworkPackets.MobUpdatePacket.class);
        kryo.register(NetworkPackets.SingleMobUpdatePacket.class);

        //status
        kryo.register(NetworkPackets.PlayerStatusUpdatePacket.class);
        kryo.register(NetworkPackets.EffectUpdatePacket.class);
    }
}
