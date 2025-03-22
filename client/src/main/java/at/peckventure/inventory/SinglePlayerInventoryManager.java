package at.peckventure.inventory;

import at.peckventure.entities.ControlledPlayer;
import at.peckventure.inventory.item.Item;

public class SinglePlayerInventoryManager implements InventoryManager
{

    @Override
    public boolean addItem(Item item, int amount)
    {
        // Nutzt das existierende Inventar-Objekt des Spielers.
        return ControlledPlayer.getInstance().getInventory().addItem(item, amount);
    }

    @Override
    public boolean dropItem(int slot, int amount)
    {
        // Sucht das Item im Inventar (zuerst in der Hotbar, dann im Main-Inventar),
        // entfernt es und löst die Drop-Logik aus.
        Inventory inventory = ControlledPlayer.getInstance().getInventory();

        InventorySlot inventorySlot = inventory.getSlotByIndex(slot);
        if (inventorySlot == null) return false;
        if (inventorySlot.getItem() == null) return false;
        boolean dropped = false;
        Item item = inventorySlot.getItem();
        if (inventorySlot.getItem() != null && inventorySlot.getItem().getStackSize() >= amount)
        {
            inventorySlot.setItem(null);
            ControlledPlayer.getInstance().dropItemOutside(item, amount);
            dropped = true;
        }
        return dropped;
    }

    @Override
    public boolean moveItem(int fromSlot, int toSlot, int count)
    {
        Inventory inventory = ControlledPlayer.getInstance().getInventory();
        InventorySlot sourceSlot = inventory.getSlotByIndex(fromSlot);
        InventorySlot targetSlot = inventory.getSlotByIndex(toSlot);
        if (sourceSlot == null || targetSlot == null) return false;
        if (sourceSlot.getItem() == null) return false;

        Item sourceItem = sourceSlot.getItem();
        // Wenn der Zielslot leer ist, wird das Item (oder ein Teil davon) verschoben.
        if (targetSlot.getItem() == null)
        {
            if (sourceItem.getStackSize() > count)
            {
                // Klone das Item für den Zielslot und reduziere die Anzahl im Quellslot.
                Item movedItem = ControlledPlayer.getInstance().getInventory().cloneItem(sourceItem);
                movedItem.setStackSize(count);
                targetSlot.setItem(movedItem);
                sourceItem.setStackSize(sourceItem.getStackSize() - count);
            } else
            {
                // Falls der ganze Stack verschoben wird.
                targetSlot.setItem(sourceItem);
                sourceSlot.setItem(null);
            }
            return true;
        } else
        {
            // Falls im Zielslot bereits ein Item desselben Typs liegt, versuche die Stacks zu mergen.
            if (targetSlot.getItem().getId().equals(sourceItem.getId()))
            {
                int availableSpace = Item.MAX_STACK_SIZE - targetSlot.getItem().getStackSize();
                if (availableSpace <= 0) return false;
                int toMove = Math.min(count, Math.min(sourceItem.getStackSize(), availableSpace));
                targetSlot.getItem().setStackSize(targetSlot.getItem().getStackSize() + toMove);
                sourceItem.setStackSize(sourceItem.getStackSize() - toMove);
                if (sourceItem.getStackSize() <= 0)
                {
                    sourceSlot.setItem(null);
                }
                return true;
            } else
            {
                // Ansonsten tauschen.
                Item temp = targetSlot.getItem();
                targetSlot.setItem(sourceItem);
                sourceSlot.setItem(temp);
                return true;
            }
        }
    }
}
