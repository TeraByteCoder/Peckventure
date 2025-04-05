package at.peckventure.inventory;

import at.peckventure.entities.ControlledPlayer;
import at.peckventure.inventory.item.Item;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop.Payload;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop.Source;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop.Target;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

import java.util.HashMap;
import java.util.Map;

public class InventoryUI
{
    private final Stage stage;
    private Table hotbarTable;
    private Table mainTable;
    private final DragAndDrop dragAndDrop;
    private boolean mainVisible = false;
    private Item heldItem = null;
    private final Image heldItemImage = new Image();
    private final Texture slotTexture;

    // Hier wird der korrekte InventoryManager genutzt (Singleplayer oder Multiplayer)
    private final InventoryManager manager;
    // Mapping von InventorySlot zu linearem Index (Hotbar gefolgt vom Main-Inventar)
    private final Map<InventorySlot, Integer> slotIndexMap = new HashMap<>();

    public InventoryUI(Stage stage, InventoryManager manager)
    {
        this.stage = stage;
        this.manager = manager;
        slotTexture = new Texture(Gdx.files.internal("textures/inventory_slot.png"));
        dragAndDrop = new DragAndDrop();
        setupGlobalDropTarget();
        createUI();
        setupDragAndDrop();
        setupInputListener();
    }

    private void createUI()
    {
        hotbarTable = new Table();
        Inventory inventory = ControlledPlayer.getInstance().getInventory();
        // Hotbar: Indizes 0 .. HOTBAR_SIZE - 1
        InventorySlot[] hotbar = inventory.getHotbar();
        for (int i = 0; i < hotbar.length; i++)
        {
            InventorySlot slot = hotbar[i];
            slotIndexMap.put(slot, i);
            Group slotGroup = new Group();
            slotGroup.setSize(64, 64);
            Image background = new Image(new TextureRegionDrawable(slotTexture));
            background.setSize(64, 64);
            slotGroup.addActor(background);
            slot.setPosition(0, 0);
            slotGroup.addActor(slot);
            hotbarTable.add(slotGroup).pad(5).size(64, 64);
        }
        hotbarTable.pack();
        float hotbarX = (stage.getWidth() - hotbarTable.getWidth()) / 2f;
        float hotbarY = 20;
        hotbarTable.setPosition(hotbarX, hotbarY);
        stage.addActor(hotbarTable);

        mainTable = new Table();
        InventorySlot[][] mainInv = inventory.getMainInventory();
        int baseIndex = hotbar.length; // main-Inventar beginnt ab diesem Index
        for (int row = 0; row < Inventory.MAIN_ROWS; row++)
        {
            for (int col = 0; col < Inventory.MAIN_COLUMNS; col++)
            {
                InventorySlot slot = mainInv[row][col];
                int index = baseIndex + row * Inventory.MAIN_COLUMNS + col;
                slotIndexMap.put(slot, index);
                Group slotGroup = new Group();
                slotGroup.setSize(64, 64);
                Image background = new Image(new TextureRegionDrawable(slotTexture));
                background.setSize(64, 64);
                slotGroup.addActor(background);
                slot.setPosition(0, 0);
                slotGroup.addActor(slot);
                mainTable.add(slotGroup).pad(5).size(64, 64);
            }
            mainTable.row();
        }
        mainTable.setVisible(mainVisible);
        mainTable.pack();
        float mainX = (stage.getWidth() - mainTable.getWidth()) / 2f;
        float mainY = (stage.getHeight() - mainTable.getHeight()) / 2f;
        mainTable.setPosition(mainX, mainY);
        stage.addActor(mainTable);
    }

    private void setupDragAndDrop()
    {
        // Für alle Slots werden Drag-Quellen und Drop-Ziele hinzugefügt.
        for (InventorySlot slot : ControlledPlayer.getInstance().getInventory().getHotbar())
        {
            addDragAndDropForSlot(slot);
        }
        InventorySlot[][] mainInv = ControlledPlayer.getInstance().getInventory().getMainInventory();
        for (int row = 0; row < Inventory.MAIN_ROWS; row++)
        {
            for (int col = 0; col < Inventory.MAIN_COLUMNS; col++)
            {
                addDragAndDropForSlot(mainInv[row][col]);
            }
        }
    }

    private void addDragAndDropForSlot(final InventorySlot slot)
    {
        dragAndDrop.addSource(new Source(slot)
        {
            @Override
            public Payload dragStart(InputEvent event, float x, float y, int pointer)
            {
                if (slot.getItem() == null) return null;
                int button = pointer;
                Item slotItem = slot.getItem();
                Payload payload = new Payload();
                if (button == 0)
                {
                    payload.setObject(slotItem);
                    // Im UI-Feedback kannst du hier entscheiden, ob du das Item sofort aus dem Slot entfernst.
                } else return null;
                Item draggedItem = (Item) payload.getObject();
                Image dragActor = new Image(draggedItem.getTexture());
                dragActor.setSize(slot.getWidth(), slot.getHeight());
                payload.setDragActor(dragActor);
                dragAndDrop.setDragActorPosition(-dragActor.getWidth() / 2f, -dragActor.getHeight() / 2f);
                return payload;
            }
        });
        dragAndDrop.addTarget(new Target(slot)
        {
            @Override
            public boolean drag(Source source, Payload payload, float x, float y, int pointer)
            {
                return true;
            }

            @Override
            public void drop(Source source, Payload payload, float x, float y, int pointer)
            {
                Item draggedItem = (Item) payload.getObject();
                InventorySlot sourceSlot = (InventorySlot) source.getActor();
                // Ermittle die Slot-Indizes anhand des Mappings
                Integer sourceIndex = slotIndexMap.get(sourceSlot);
                Integer targetIndex = slotIndexMap.get(slot);
                if (sourceIndex != null && targetIndex != null)
                {
                    int count = draggedItem.getStackSize();
                    // Delegiere den Verschiebevorgang an den Manager
                    manager.moveItem(sourceIndex, targetIndex, count);
                }
            }
        });
    }

    private void setupGlobalDropTarget()
    {
        Actor backgroundDropArea = new Actor();
        backgroundDropArea.setBounds(0, 0, stage.getWidth(), stage.getHeight());
        backgroundDropArea.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);
        stage.addActor(backgroundDropArea);
        dragAndDrop.addTarget(new Target(backgroundDropArea)
        {
            @Override
            public boolean drag(Source source, Payload payload, float x, float y, int pointer)
            {
                return true;
            }

            @Override
            public void drop(Source source, Payload payload, float x, float y, int pointer)
            {
                Item draggedItem = (Item) payload.getObject();
                InventorySlot sourceSlot = (InventorySlot) source.getActor();
                int count = draggedItem.getStackSize();
                // Delegiere das Droppen an den Manager
                manager.dropItem(slotIndexMap.get(sourceSlot), count);
            }
        });
    }

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
                } else if (keycode == Input.Keys.Q)
                {
                    float x = Gdx.input.getX();
                    float y = stage.getViewport().getScreenHeight() - Gdx.input.getY();
                    Actor actor = stage.hit(x, y, true);
                    InventorySlot slot = getInventorySlot(actor);
                    if (slot != null && slot.getItem() != null)
                    {
                        manager.dropItem(slotIndexMap.get(slot), slot.getItem().getStackSize());
                        return true;
                    }
                }
                return false;
            }

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if (button == Input.Buttons.RIGHT) {
                    Actor actor = stage.hit(x, y, true);
                    InventorySlot slot = getInventorySlot(actor);
                    if (slot != null && slot.getItem() != null) {
                        manager.useItem(slotIndexMap.get(slot));
                        return true;
                    }
                }
                return false;
            }

        });
    }


    private InventorySlot getInventorySlot(Actor actor)
    {
        while (actor != null)
        {
            if (actor instanceof InventorySlot) return (InventorySlot) actor;
            actor = actor.getParent();
        }
        return null;
    }

    public void toggleMainInventory()
    {
        mainVisible = !mainVisible;
        mainTable.setVisible(mainVisible);
    }

    public void update(float delta)
    {
        stage.act(delta);
        if (heldItem != null)
        {
            float x = Gdx.input.getX();
            float y = stage.getViewport().getScreenHeight() - Gdx.input.getY();
            heldItemImage.setPosition(x - heldItemImage.getWidth() / 2f, y - heldItemImage.getHeight() / 2f);
        }
    }


    public void draw()
    {
        stage.draw();
    }

    public Stage getStage()
    {
        return stage;
    }
}
