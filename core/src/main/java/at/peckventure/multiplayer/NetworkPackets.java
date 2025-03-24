package at.peckventure.multiplayer;

import java.util.HashSet;
import java.util.Set;

public class NetworkPackets
{
    public static class ChunkRequestPacket
    {
        public int chunkX;
        public int chunkY;
    }

    public static class ChunkDataPacket
    {
        public byte[] data;
    }

    public static class ChatMessagePacket
    {
        public String message;
    }

    public static class InventoryUpdatePacket
    {
        public String mainInventoryData;
        public String hotbarData;
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

        public String inventoryMain;
        public String inventoryHotbar;

    }

    public static class ServerConnectPacket
    {
        public String uuid;
        public String username;

    }

    public static class ClientDisconnectPacket
    {
        public String uuid;
    }

    public static class ItemDropPacket
    {
        public int slot;
        public int count;
    }

    public static class InventoryMovePacket {
        public int fromSlot;   // Quell-Slot (z. B. linearer Index oder kombiniert: Inventartyp + Index)
        public int toSlot;     // Ziel-Slot
        public int count;      // Anzahl der Items, die verschoben werden sollen
    }

    public static class PingPacket
    {
        public int connectedPlayers;
        public long pingTime;
        public String modt;
    }

    public static class PingRequestPacket
    {

    }


}
