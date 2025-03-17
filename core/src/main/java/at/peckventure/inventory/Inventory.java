package at.peckventure.inventory;

import at.peckventure.inventory.item.Sword;

public class Inventory {
    private final InventorySlot[] hotbar;
    private final InventorySlot[][] mainInventory;
    public static final int HOTBAR_SIZE = 5;
    public static final int MAIN_ROWS = 3;
    public static final int MAIN_COLUMNS = 5;

    public Inventory() {
        hotbar = new InventorySlot[HOTBAR_SIZE];
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            hotbar[i] = new InventorySlot();
        }
        mainInventory = new InventorySlot[MAIN_ROWS][MAIN_COLUMNS];
        for (int r = 0; r < MAIN_ROWS; r++) {
            for (int c = 0; c < MAIN_COLUMNS; c++) {
                mainInventory[r][c] = new InventorySlot();
            }
        }
    }

    public InventorySlot[] getHotbar() {
        return hotbar;
    }

    public InventorySlot[][] getMainInventory() {
        return mainInventory;
    }

    public String serializeHotbar() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            if (hotbar[i].getItem() != null) {
                sb.append(hotbar[i].getItem().getId()).append(":").append(hotbar[i].getItem().getStackSize());
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
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < MAIN_ROWS; r++) {
            for (int c = 0; c < MAIN_COLUMNS; c++) {
                InventorySlot slot = mainInventory[r][c];
                if (slot.getItem() != null) {
                    sb.append(slot.getItem().getId()).append(":").append(slot.getItem().getStackSize());
                } else {
                    sb.append("null");
                }
                if (r != MAIN_ROWS - 1 || c != MAIN_COLUMNS - 1) {
                    sb.append(",");
                }
            }
        }
        return sb.toString();
    }

    public void deserializeHotbar(String data) {
        String[] parts = data.split(",");
        for (int i = 0; i < Math.min(parts.length, HOTBAR_SIZE); i++) {
            parseAndSetSlot(hotbar[i], parts[i].trim());
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

    private void parseAndSetSlot(InventorySlot slot, String token) {
        if (token.equalsIgnoreCase("null")) {
            slot.setItem(null);
            return;
        }
        String[] itemData = token.split(":");
        if (itemData.length == 2) {
            String id = itemData[0];
            int stackSize = 1;
            try {
                stackSize = Integer.parseInt(itemData[1]);
            } catch (NumberFormatException e) {}
            if (ItemRegistry.contains(id)) {
                Sword newItem = ItemRegistry.createItem(id);
                newItem.setStackSize(stackSize);
                slot.setItem(newItem);
            } else {
                slot.setItem(null);
            }
        } else {
            slot.setItem(null);
        }
    }

    public void deserialize(String hotbarData, String mainData) {
        deserializeHotbar(hotbarData);
        deserializeMain(mainData);
    }

    public boolean addItem(Sword newItem, int amount) {
        if (newItem == null) return false;
        newItem.setStackSize(amount);
        newItem = mergeWithExistingStacks(newItem, hotbar);
        if (newItem != null && newItem.getStackSize() > 0) {
            for (int r = 0; r < MAIN_ROWS && newItem.getStackSize() > 0; r++) {
                newItem = mergeWithExistingStacks(newItem, mainInventory[r]);
            }
        }
        if (newItem != null && newItem.getStackSize() > 0) {
            newItem = placeInEmptySlots(newItem, hotbar);
        }
        if (newItem != null && newItem.getStackSize() > 0) {
            for (int r = 0; r < MAIN_ROWS && newItem.getStackSize() > 0; r++) {
                newItem = placeInEmptySlots(newItem, mainInventory[r]);
            }
        }
        return newItem == null || newItem.getStackSize() <= 0;
    }

    private Sword mergeWithExistingStacks(Sword newItem, InventorySlot[] slots) {
        for (InventorySlot slot : slots) {
            if (newItem.getStackSize() <= 0) break;
            Sword slotItem = slot.getItem();
            if (slotItem != null && slotItem.getId().equals(newItem.getId())) {
                int canAdd = Sword.MAX_STACK_SIZE - slotItem.getStackSize();
                if (canAdd > 0) {
                    int toAdd = Math.min(canAdd, newItem.getStackSize());
                    slotItem.setStackSize(slotItem.getStackSize() + toAdd);
                    newItem.setStackSize(newItem.getStackSize() - toAdd);
                }
            }
        }
        return newItem;
    }

    private Sword placeInEmptySlots(Sword newItem, InventorySlot[] slots) {
        for (InventorySlot slot : slots) {
            if (newItem.getStackSize() <= 0) break;
            if (slot.getItem() == null) {
                if (newItem.getStackSize() > Sword.MAX_STACK_SIZE) {
                    Sword fullStack = new Sword(newItem.getId(), newItem.getName(), newItem.getTexture());
                    fullStack.setStackSize(Sword.MAX_STACK_SIZE);
                    slot.setItem(fullStack);
                    newItem.setStackSize(newItem.getStackSize() - Sword.MAX_STACK_SIZE);
                } else {
                    Sword clone = cloneItem(newItem);
                    slot.setItem(clone);
                    newItem.setStackSize(0);
                    return newItem;
                }
            }
        }
        return newItem;
    }

    public Sword cloneItem(Sword item) {
        Sword clone = new Sword(item.getId(), item.getName(), item.getTexture());
        clone.setStackSize(item.getStackSize());
        return clone;
    }
}
