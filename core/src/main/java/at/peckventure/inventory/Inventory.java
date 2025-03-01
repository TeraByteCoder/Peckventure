package at.peckventure.inventory;

import at.peckventure.inventory.item.Item;
import com.badlogic.gdx.graphics.Texture;

public class Inventory {
    private InventorySlot[] hotbar;
    private InventorySlot[][] mainInventory;

    public static final int HOTBAR_SIZE = 5;
    public static final int MAIN_ROWS = 3;
    public static final int MAIN_COLUMNS = 5;

    public Inventory(Texture slotTexture) {
        hotbar = new InventorySlot[HOTBAR_SIZE];
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            hotbar[i] = new InventorySlot(slotTexture);
        }
        mainInventory = new InventorySlot[MAIN_ROWS][MAIN_COLUMNS];
        for (int row = 0; row < MAIN_ROWS; row++) {
            for (int col = 0; col < MAIN_COLUMNS; col++) {
                mainInventory[row][col] = new InventorySlot(slotTexture);
            }
        }
    }

    public InventorySlot[] getHotbar() {
        return hotbar;
    }

    public InventorySlot[][] getMainInventory() {
        return mainInventory;
    }

    // -- Serialisieren (Hotbar / Main) --

    public String serializeHotbar() {
        // Beispiel-Format: "sword:5,null,apple:2,null,null"
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            if (hotbar[i].getItem() != null) {
                sb.append(hotbar[i].getItem().getId());
                sb.append(":");
                sb.append(hotbar[i].getItem().getStackSize());
            } else {
                sb.append("null");
            }
            if (i < HOTBAR_SIZE - 1) {
                sb.append(",");
            }
        }
        return sb.toString();
    }

    public String serializeMain() {
        // "sword:5,null,apple:2" usw., Zeilen hintereinander
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < MAIN_ROWS; r++) {
            for (int c = 0; c < MAIN_COLUMNS; c++) {
                InventorySlot slot = mainInventory[r][c];
                if (slot.getItem() != null) {
                    sb.append(slot.getItem().getId());
                    sb.append(":");
                    sb.append(slot.getItem().getStackSize());
                } else {
                    sb.append("null");
                }
                // Komma außer beim letzten Slot
                if (r != MAIN_ROWS - 1 || c != MAIN_COLUMNS - 1) {
                    sb.append(",");
                }
            }
        }
        return sb.toString();
    }

    // -- Deserialisieren (Hotbar / Main) --

    public void deserializeHotbar(String data) {
        // z. B. "sword:5,null,apple:2"
        String[] parts = data.split(",");
        for (int i = 0; i < Math.min(parts.length, HOTBAR_SIZE); i++) {
            String token = parts[i].trim();
            parseAndSetSlot(hotbar[i], token);
        }
    }

    public void deserializeMain(String data) {
        String[] parts = data.split(",");
        int index = 0;
        for (int r = 0; r < MAIN_ROWS; r++) {
            for (int c = 0; c < MAIN_COLUMNS; c++) {
                if (index < parts.length) {
                    parseAndSetSlot(mainInventory[r][c], parts[index].trim());
                }
                index++;
            }
        }
    }

    /**
     * Liest einen String wie "sword:5" oder "null" und setzt den Slot entsprechend.
     */
    private void parseAndSetSlot(InventorySlot slot, String token) {
        if (token.equalsIgnoreCase("null")) {
            slot.setItem(null);
            return;
        }
        // Format: "itemId:stackSize"
        String[] itemData = token.split(":");
        if (itemData.length == 2) {
            String id = itemData[0];
            int stackSize = 1;
            try {
                stackSize = Integer.parseInt(itemData[1]);
            } catch (NumberFormatException e) {
                // fallback
            }
            if (ItemRegistry.contains(id)) {
                Item newItem = ItemRegistry.createItem(id);
                newItem.setStackSize(stackSize);
                slot.setItem(newItem);
            } else {
                // item unbekannt -> Slot leer
                slot.setItem(null);
            }
        } else {
            // Format fehlerhaft -> Slot leer
            slot.setItem(null);
        }
    }

    public void deserialize(String hotbarData, String mainData) {
        deserializeHotbar(hotbarData);
        deserializeMain(mainData);
    }


}
