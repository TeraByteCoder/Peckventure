package at.peckventure.inventory;

import at.peckventure.Globals;
import at.peckventure.entities.ControlledPlayer;
import at.peckventure.inventory.item.Item;
import at.peckventure.NetworkClient;
import at.peckventure.multiplayer.NetworkPackets;

public class MultiplayerInventoryManager implements InventoryManager {

    @Override
    public boolean addItem(Item item, int amount) {
        // Hier wird ein Request an den Server geschickt.
        // Wir nutzen dazu z. B. ein InventoryUpdatePacket, in dem wir eine "ADD"-Aktion signalisieren.
        //NetworkPackets.InventoryUpdatePacket packet = new NetworkPackets.InventoryUpdatePacket();
        //packet.uuid = Globals.uuid; // Annahme: ControlledPlayer liefert eine eindeutige UUID
        // Das inventoryData-Feld wird hier als Kommando interpretiert.
        //packet.inventoryData = "ADD:" + item.getId() + ":" + amount;
        //NetworkClient.getInstance().sendTCP(packet);
        return true;
    }

    @Override
    public boolean dropItem(int slot, int amount) {
        // Für das Droppen senden wir ein ItemDropPacket.
        NetworkPackets.ItemDropPacket packet = new NetworkPackets.ItemDropPacket();
        packet.slot = slot;
        packet.count = amount;
        // Optional: Du könntest hier auch den Slot mitsenden, wenn du das Netzwerkprotokoll erweiterst.
        NetworkClient.getInstance().sendTCP(packet);
        return true;
    }

    @Override
    public boolean moveItem(int fromSlot, int toSlot, int count) {
        // Erstelle ein InventoryMovePacket mit den korrekten Slot-Indizes.
        NetworkPackets.InventoryMovePacket packet = new NetworkPackets.InventoryMovePacket();
        packet.fromSlot = fromSlot;
        packet.toSlot = toSlot;
        packet.count = count;
        NetworkClient.getInstance().sendTCP(packet);
        return true;
    }
}
