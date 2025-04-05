package at.peckventure.inventory;

import at.peckventure.inventory.item.Item;

public interface InventoryManager {
    /**
     * Fügt ein Item zum Inventar hinzu.
     * @param item Das hinzuzufügende Item.
     * @param amount Die Menge, die hinzugefügt werden soll.
     * @return true, wenn das Hinzufügen erfolgreich war.
     */
    boolean addItem(Item item, int amount);

    /**
     * Löst ein Item aus dem Inventar aus (z.B. zum Droppen in die Welt).
     * @param slot Das abzulegende Item.
     * @param amount Die Menge, die abfallen soll.
     * @return true, wenn das Droppen erfolgreich war.
     */
    boolean dropItem(int slot, int amount);

    /**
     * Verschiebt Items zwischen Slots.
     * @param fromSlot Quell-Slot-Index.
     * @param toSlot Ziel-Slot-Index.
     * @param count Anzahl der zu verschiebenden Items.
     * @return true, wenn der Verschiebevorgang erfolgreich war.
     */
    boolean moveItem(int fromSlot, int toSlot, int count);

    boolean useItem(int slot);
}
