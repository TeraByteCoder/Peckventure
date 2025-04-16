package at.peckventure.multiplayer;

import java.util.ArrayList;
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
        public long time;
        public boolean rotation;
        public float energy;
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

        public PlayerListPacket playerList;
        public PlayerStatusUpdatePacket playerStatus;
        public EffectUpdatePacket effects;

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

    public static class PingResponsePacket
    {
        public int connectedPlayers;
        public long pingTime;
        public String modt;
    }

    public static class PingRequestPacket
    {

    }


    public static class SingleMobUpdatePacket
    {
        public int umid;
        public int mobid;
        public float x;
        public float y;
        public float velx;
        public float vely;
        public String extraItem;

        public boolean direction;
    }

    public static class MobUpdatePacket
    {
        public ArrayList<SingleMobUpdatePacket> mobUpdates;
    }

    public static class PlayerStatusUpdatePacket
    {
        public float energy;
        public float health;
    }

    public static class ServerPositionChangePacket
    {
        public float x;
        public float y;
    }

    public static class EffectUpdatePacket
    {
        public String effects;
    }

    public static class ItemUsePacket
    {
        public int slot;
    }

    public static class PeckRequestPacket {
        public String uuid;        // UUID of the pecking player
        public float targetX;      // X coordinate of the target
        public float targetY;      // Y coordinate of the target
    }

    public static class PeckResponsePacket {
        public String uuid;        // UUID of the pecking player
        public String targetUuid;  // UUID of the target (if it's a player, empty otherwise)
        public float targetX;      // X coordinate of the target
        public float targetY;      // Y coordinate of the target
    }

}
