package at.peckventure.inventory;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import at.peckventure.inventory.item.Item;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

public class InventoryUI
{
    private final Stage stage;
    private final Inventory inventory;
    private Table hotbarTable;
    private Table mainTable;
    private final DragAndDrop dragAndDrop;
    private boolean mainVisible = false;

    // Das aktuell aufgehobene Item – null, wenn nichts gehalten wird
    private Item heldItem = null;
    // Ein Image, das das gehaltene Item anzeigt (folgt dem Mauszeiger)
    private final Image heldItemImage = new Image();

    public InventoryUI(Stage stage)
    {
        this.stage = stage;
        // z.B. Slot-Hintergrund
        Texture slotTexture = new Texture(Gdx.files.internal("textures/inventory_slot.png"));
        this.inventory = new Inventory(slotTexture);

        dragAndDrop = new DragAndDrop();

        createUI();
        setupDragAndDrop();
        setupInputListener();
    }

    private void createUI()
    {
        // -- HOTBAR --
        hotbarTable = new Table();
        for (InventorySlot slot : inventory.getHotbar())
        {
            hotbarTable.add(slot).pad(5).size(64, 64);
        }
        hotbarTable.pack();
        // z.B. unten zentriert
        float hotbarX = (stage.getWidth() - hotbarTable.getWidth()) / 2f;
        float hotbarY = 20;
        hotbarTable.setPosition(hotbarX, hotbarY);
        stage.addActor(hotbarTable);

        // -- MAIN INVENTORY --
        mainTable = new Table();
        for (int row = 0; row < Inventory.MAIN_ROWS; row++)
        {
            for (int col = 0; col < Inventory.MAIN_COLUMNS; col++)
            {
                mainTable.add(inventory.getMainInventory()[row][col]).pad(5).size(64, 64);
            }
            mainTable.row();
        }
        mainTable.setVisible(mainVisible);
        mainTable.pack();
        // zentriert
        float mainX = (stage.getWidth() - mainTable.getWidth()) / 2f;
        float mainY = (stage.getHeight() - mainTable.getHeight()) / 2f;
        mainTable.setPosition(mainX, mainY);
        stage.addActor(mainTable);
    }

    /**
     * Drag & Drop: Linksklick = ganzer Stack, Rechtsklick = 1 Item
     * - DragActor wird mittig unter Maus positioniert
     * - Beim Drop wird, wenn möglich, gemergt oder sonst getauscht
     */
    private void setupDragAndDrop()
    {
        // Alle Slots (Hotbar & Main) als Quelle + Ziel registrieren
        for (InventorySlot slot : inventory.getHotbar())
        {
            addDragAndDropForSlot(slot);
        }
        for (int row = 0; row < Inventory.MAIN_ROWS; row++)
        {
            for (int col = 0; col < Inventory.MAIN_COLUMNS; col++)
            {
                addDragAndDropForSlot(inventory.getMainInventory()[row][col]);
            }
        }
    }

    private void addDragAndDropForSlot(final InventorySlot slot)
    {
        // Quelle
        dragAndDrop.addSource(new DragAndDrop.Source(slot)
        {
            @Override
            public DragAndDrop.Payload dragStart(InputEvent event, float x, float y, int pointer)
            {
                if (slot.getItem() == null)
                {
                    return null;
                }

                // Linksklick = pointer 0, Rechtsklick = pointer 1
                // (Achtung: je nach Betriebssystem/Einstellung kann pointer abweichen)
                // Evtl. musst du Input.Buttons.LEFT/RIGHT abfragen.
                int button = pointer;

                Item slotItem = slot.getItem();
                DragAndDrop.Payload payload = new DragAndDrop.Payload();

                if (button == 0)
                { // Linksklick -> gesamter Stack
                    payload.setObject(slotItem);
                    // Slot leeren
                    slot.setItem(null);
                } else if (button == 1)
                { // Rechtsklick -> nur 1 Item
                    if (slotItem.getStackSize() > 1)
                    {
                        // Neues Item mit stackSize=1
                        Item single = new Item(slotItem.getId(), slotItem.getName(), slotItem.getTexture());
                        single.setStackSize(1);

                        payload.setObject(single);
                        // Reduziere Original-Stack
                        slotItem.setStackSize(slotItem.getStackSize() - 1);
                    } else
                    {
                        // Stackgröße = 1, also gesamter Stack
                        payload.setObject(slotItem);
                        slot.setItem(null);
                    }
                } else
                {
                    // Wenn mittlere Maustaste oder was anderes -> kein Drag
                    return null;
                }

                // Erstelle den DragActor (z. B. ein Image des Items)
                Item draggedItem = (Item) payload.getObject();
                Image dragActor = new Image(draggedItem.getTexture());
                dragActor.setSize(slot.getWidth(), slot.getHeight());
                payload.setDragActor(dragActor);

                // Center the actor on the mouse
                dragAndDrop.setDragActorPosition(-dragActor.getWidth() / 2f, -dragActor.getHeight() / 2f);

                return payload;
            }
        });

        // Ziel
        dragAndDrop.addTarget(new DragAndDrop.Target(slot)
        {
            @Override
            public boolean drag(DragAndDrop.Source source, DragAndDrop.Payload payload,
                                float x, float y, int pointer)
            {
                return true; // immer akzeptieren
            }

            @Override
            public void drop(DragAndDrop.Source source, DragAndDrop.Payload payload,
                             float x, float y, int pointer)
            {
                Item draggedItem = (Item) payload.getObject();
                InventorySlot sourceSlot = (InventorySlot) source.getActor();
                Item targetItem = slot.getItem();

                // 1) Wenn kein Item im Zielslot, Item einfach reinlegen
                if (targetItem == null)
                {
                    slot.setItem(draggedItem);
                } else
                {
                    // 2) Wenn selbe ID -> versuchen zu mergen
                    if (targetItem.getId().equals(draggedItem.getId()))
                    {
                        int canAdd = Item.MAX_STACK_SIZE - targetItem.getStackSize();
                        if (canAdd > 0)
                        {
                            int toAdd = Math.min(canAdd, draggedItem.getStackSize());
                            targetItem.setStackSize(targetItem.getStackSize() + toAdd);
                            draggedItem.setStackSize(draggedItem.getStackSize() - toAdd);
                        }
                        // Falls noch Reste im draggedItem übrig bleiben, könnte man
                        // sie zurück in den Source-Slot legen oder wegwerfen etc.
                        if (draggedItem.getStackSize() > 0)
                        {
                            // z.B. zurücklegen in den Quellslot (wenn leer)
                            if (sourceSlot.getItem() == null)
                            {
                                sourceSlot.setItem(draggedItem);
                            }
                            // sonst Item verwerfen oder extra Logik
                        }
                    } else
                    {
                        // 3) Andere ID -> swap
                        slot.setItem(draggedItem);
                        sourceSlot.setItem(targetItem);
                    }
                }
            }
        });
    }

    /**
     * InputListener für Taste E -> Hauptinventar ein-/ausblenden
     */
    private void setupInputListener()
    {
        stage.addListener(new InputListener()
        {
            @Override
            public boolean keyDown(InputEvent event, int keycode)
            {
                if (keycode == Input.Keys.E)
                {
                    toggleMainInventory();
                    return true;
                }
                return false;
            }
        });
    }

    public void toggleMainInventory()
    {
        mainVisible = !mainVisible;
        mainTable.setVisible(mainVisible);
    }

    /**
     * Ruft intern stage.act(delta) auf, falls du es im GameScreen nicht selbst machen willst.
     */
    public void update(float delta)
    {
        stage.act(delta);
        if (heldItem != null)
        {
            float x = Gdx.input.getX();
            // Umrechnung: Stage-Viewport (Y wird von unten gezählt)
            float y = stage.getViewport().getScreenHeight() - Gdx.input.getY();
            heldItemImage.setPosition(x - heldItemImage.getWidth() / 2f, y - heldItemImage.getHeight() / 2f);
        }
    }

    public void draw()
    {
        stage.draw();
    }

    public Inventory getInventory()
    {
        return inventory;
    }

    public Stage getStage()
    {
        return stage;
    }

    /**
     * Fügt ein Item mit der angegebenen Menge ins Inventar ein.
     * Zuerst wird versucht, bestehende Stacks zu mergen, dann werden leere Slots genutzt.
     * Gibt true zurück, wenn alles untergebracht werden konnte, sonst false.
     */
    public boolean addItem(Item newItem, int amount)
    {
        if (newItem == null) return false;
        // Setze die gewünschte Anzahl in den Stack
        newItem.setStackSize(amount);

        // 1) Versuche, in bestehenden Stacks (Hotbar) zu mergen
        newItem = mergeWithExistingStacks(newItem, inventory.getHotbar());

        // 2) Falls noch Rest vorhanden, im Hauptinventar (Zeile für Zeile) mergen
        if (newItem != null && newItem.getStackSize() > 0)
        {
            for (int row = 0; row < Inventory.MAIN_ROWS && newItem.getStackSize() > 0; row++)
            {
                newItem = mergeWithExistingStacks(newItem, inventory.getMainInventory()[row]);
            }
        }

        // 3) Falls noch Items übrig sind, in leere Slots (Hotbar) ablegen
        if (newItem != null && newItem.getStackSize() > 0)
        {
            newItem = placeInEmptySlots(newItem, inventory.getHotbar());
        }

        // 4) Dann im Hauptinventar
        if (newItem != null && newItem.getStackSize() > 0)
        {
            for (int row = 0; row < Inventory.MAIN_ROWS && newItem.getStackSize() > 0; row++)
            {
                newItem = placeInEmptySlots(newItem, inventory.getMainInventory()[row]);
            }
        }

        // Wenn newItem null oder mit 0 Stackgröße übrig ist, passt alles rein
        return newItem == null || newItem.getStackSize() <= 0;
    }

    /* Hilfsmethoden (die du bereits in deiner Klasse InventoryUI oder Inventory implementieren solltest): */

    /**
     * Sucht in den Slots nach vorhandenen Stacks mit gleicher Item-ID und versucht, so viel wie möglich vom newItem einzufügen.
     * Gibt das (reduzierte) newItem zurück.
     */
    private Item mergeWithExistingStacks(Item newItem, InventorySlot[] slots)
    {
        for (InventorySlot slot : slots)
        {
            if (newItem.getStackSize() <= 0) break;
            Item slotItem = slot.getItem();
            if (slotItem != null && slotItem.getId().equals(newItem.getId()))
            {
                int canAdd = Item.MAX_STACK_SIZE - slotItem.getStackSize();
                if (canAdd > 0)
                {
                    int toAdd = Math.min(canAdd, newItem.getStackSize());
                    slotItem.setStackSize(slotItem.getStackSize() + toAdd);
                    newItem.setStackSize(newItem.getStackSize() - toAdd);
                }
            }
        }
        return newItem;
    }

    /**
     * Sucht in den Slots nach einem leeren Slot und legt so viel wie möglich vom newItem dort ab.
     * Falls newItem größer als MAX_STACK_SIZE ist, wird ein voller Stack abgelegt.
     * Gibt das evtl. übrig gebliebene newItem zurück.
     */
    private Item placeInEmptySlots(Item newItem, InventorySlot[] slots)
    {
        for (InventorySlot slot : slots)
        {
            if (newItem.getStackSize() <= 0) break;
            if (slot.getItem() == null)
            {
                if (newItem.getStackSize() > Item.MAX_STACK_SIZE)
                {
                    // Erzeuge eine neue Instanz für einen vollen Stack
                    Item fullStack = new Item(newItem.getId(), newItem.getName(), newItem.getTexture());
                    fullStack.setStackSize(Item.MAX_STACK_SIZE);
                    slot.setItem(fullStack);
                    newItem.setStackSize(newItem.getStackSize() - Item.MAX_STACK_SIZE);
                } else
                {
                    // Alles passt in diesen Slot – erstelle eine Kopie, damit der Slot die richtige Menge behält
                    Item clone = new Item(newItem.getId(), newItem.getName(), newItem.getTexture());
                    clone.setStackSize(newItem.getStackSize());
                    slot.setItem(clone);
                    newItem.setStackSize(0);
                    return newItem;
                }
            }
        }
        return newItem;
    }


    /**
     * Fügt jedem Slot einen ClickListener hinzu, der zwischen Aufheben und Ablegen unterscheidet.
     */
    private void setupSlotClickListeners()
    {
        // Hotbar-Slots
        for (InventorySlot slot : inventory.getHotbar())
        {
            slot.addListener(new ClickListener()
            {
                @Override
                public void clicked(InputEvent event, float x, float y)
                {
                    handleSlotClick(slot, getButton());
                }
            });
        }
        // Main-Inventar
        for (int row = 0; row < Inventory.MAIN_ROWS; row++)
        {
            for (int col = 0; col < Inventory.MAIN_COLUMNS; col++)
            {
                InventorySlot slot = inventory.getMainInventory()[row][col];
                slot.addListener(new ClickListener()
                {
                    @Override
                    public void clicked(InputEvent event, float x, float y)
                    {
                        handleSlotClick(slot, getButton());
                    }
                });
            }
        }
    }

    /**
     * Handhabt den Klick auf einen Slot:
     * - Wenn aktuell nichts gehalten wird, wird das Item aus dem Slot aufgenommen.
     * Left = gesamter Stack, Right = nur eine Einheit.
     * - Wenn bereits ein Item gehalten wird, wird versucht, dieses im Slot abzulegen.
     * Left = ganze Menge ablegen, Right = nur eine Einheit.
     * Ist im Slot ein anderes Item, erfolgt ein Swap.
     */
    private void handleSlotClick(InventorySlot slot, int button)
    {
        // Keine Items in der Hand? Dann Aufheben.
        if (heldItem == null)
        {
            if (slot.getItem() != null)
            {
                if (button == Input.Buttons.LEFT)
                {
                    // Linksklick: ganzen Stack aufnehmen.
                    heldItem = cloneItem(slot.getItem());
                    slot.setItem(null);
                } else if (button == Input.Buttons.RIGHT)
                {
                    // Rechtsklick: nur eine Einheit aufnehmen.
                    heldItem = cloneItem(slot.getItem());
                    heldItem.setStackSize(1);
                    if (slot.getItem().getStackSize() > 1)
                    {
                        slot.getItem().setStackSize(slot.getItem().getStackSize() - 1);
                    } else
                    {
                        slot.setItem(null);
                    }
                }
            }
        } else
        {
            // Es wird bereits ein Item gehalten – also ablegen oder swap.
            if (slot.getItem() == null)
            {
                // Slot ist leer: Ablegen.
                if (button == Input.Buttons.LEFT)
                {
                    // Linksklick: gesamten gehaltenen Stack ablegen.
                    slot.setItem(cloneItem(heldItem));
                    heldItem = null;
                } else if (button == Input.Buttons.RIGHT)
                {
                    // Rechtsklick: nur eine Einheit ablegen.
                    if (heldItem.getStackSize() > 0)
                    {
                        Item one = cloneItem(heldItem);
                        one.setStackSize(1);
                        slot.setItem(one);
                        heldItem.setStackSize(heldItem.getStackSize() - 1);
                        if (heldItem.getStackSize() <= 0)
                        {
                            heldItem = null;
                        }
                    }
                }
            } else
            {
                // Slot enthält bereits ein Item.
                if (slot.getItem().getId().equals(heldItem.getId()))
                {
                    // Gleicher Item-Typ: Mergen.
                    if (button == Input.Buttons.LEFT)
                    {
                        int available = Item.MAX_STACK_SIZE - slot.getItem().getStackSize();
                        int toMerge = Math.min(available, heldItem.getStackSize());
                        slot.getItem().setStackSize(slot.getItem().getStackSize() + toMerge);
                        heldItem.setStackSize(heldItem.getStackSize() - toMerge);
                        if (heldItem.getStackSize() <= 0)
                        {
                            heldItem = null;
                        }
                    } else if (button == Input.Buttons.RIGHT)
                    {
                        if (slot.getItem().getStackSize() < Item.MAX_STACK_SIZE && heldItem.getStackSize() > 0)
                        {
                            slot.getItem().setStackSize(slot.getItem().getStackSize() + 1);
                            heldItem.setStackSize(heldItem.getStackSize() - 1);
                            if (heldItem.getStackSize() <= 0)
                            {
                                heldItem = null;
                            }
                        }
                    }
                } else
                {
                    // Unterschiedlicher Item-Typ: Swap.
                    Item temp = cloneItem(slot.getItem());
                    slot.setItem(cloneItem(heldItem));
                    heldItem = temp;
                }
            }
        }
        updateHeldItemImage();
    }

    /**
     * Erstellt eine Kopie des Items, sodass Änderungen am Original den im Slot gespeicherten Stack nicht beeinflussen.
     */
    private Item cloneItem(Item item)
    {
        Item clone = new Item(item.getId(), item.getName(), item.getTexture());
        clone.setStackSize(item.getStackSize());
        return clone;
    }

    /**
     * Aktualisiert das Held-Item-Image, das dem Mauszeiger folgt.
     */
    private void updateHeldItemImage()
    {
        if (heldItem != null)
        {
            heldItemImage.setDrawable(new TextureRegionDrawable(heldItem.getTexture()));
            heldItemImage.setSize(64, 64);
            heldItemImage.setVisible(true);
        } else
        {
            heldItemImage.setVisible(false);
        }
    }
}
