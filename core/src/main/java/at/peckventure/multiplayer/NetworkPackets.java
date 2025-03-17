package at.peckventure.multiplayer;

import at.peckventure.entities.Player;

import java.util.*;

public class NetworkPackets
{
    public static class ChunkRequestPacket
    {
        public int chunkX;
        public int chunkY;
    }

    public static class ChunkDataPacket
    {
        public int chunkX;
        public int chunkY;
        public byte[] data;
    }

    public static class ChatMessagePacket
    {
        public String message;
        public String uuid;
    }

    public static class InventoryUpdatePacket
    {
        public String uuid;
        public String inventoryData;
    }

    public static class PlayerUpdatePacket
    {
        public String uuid;
        public float x;
        public float y;
    }

    public static class PlayerListPacket
    {
        public Set<PlayerUpdatePacket> players = new HashSet();
    }

    public static class ClientConnectPacket
    {
        public int posx;
        public int posy;

    }

    public static class ServerConnectPacket
    {
        public String uuid;


    }

    public static class ClientDisconnectPacket
    {
        public String uuid;
    }
}
