package at.peckventure.inventory;

import at.peckventure.Globals;
import at.peckventure.entities.ControlledPlayer;
import at.peckventure.entities.mob.Mob;
import at.peckventure.entities.mob.MobRegistry;
import at.peckventure.world.Box2DOperationManager;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import at.peckventure.inventory.item.Sword;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

public class InventoryUI {
    private final Stage stage;
    private Table hotbarTable;
    private Table mainTable;
    private final DragAndDrop dragAndDrop;
    private boolean mainVisible = false;
    private Sword heldItem = null;
    private final Image heldItemImage = new Image();

    public InventoryUI(Stage stage) {
        this.stage = stage;
        Texture slotTexture = new Texture(Gdx.files.internal("textures/inventory_slot.png"));
        dragAndDrop = new DragAndDrop();
        setupGlobalDropTarget();
        createUI();
        setupDragAndDrop();
        setupInputListener();
    }

    private void createUI() {
        hotbarTable = new Table();
        for (InventorySlot slot : ControlledPlayer.getInstance().getInventory().getHotbar()) {
            hotbarTable.add(slot).pad(5).size(64, 64);
        }
        hotbarTable.pack();
        float hotbarX = (stage.getWidth() - hotbarTable.getWidth()) / 2f;
        float hotbarY = 20;
        hotbarTable.setPosition(hotbarX, hotbarY);
        stage.addActor(hotbarTable);
        mainTable = new Table();
        for (int row = 0; row < Inventory.MAIN_ROWS; row++) {
            for (int col = 0; col < Inventory.MAIN_COLUMNS; col++) {
                mainTable.add(ControlledPlayer.getInstance().getInventory().getMainInventory()[row][col]).pad(5).size(64, 64);
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

    private void setupDragAndDrop() {
        for (InventorySlot slot : ControlledPlayer.getInstance().getInventory().getHotbar()) {
            addDragAndDropForSlot(slot);
        }
        for (int row = 0; row < Inventory.MAIN_ROWS; row++) {
            for (int col = 0; col < Inventory.MAIN_COLUMNS; col++) {
                addDragAndDropForSlot(ControlledPlayer.getInstance().getInventory().getMainInventory()[row][col]);
            }
        }
    }

    private void addDragAndDropForSlot(final InventorySlot slot) {
        dragAndDrop.addSource(new DragAndDrop.Source(slot) {
            @Override
            public DragAndDrop.Payload dragStart(InputEvent event, float x, float y, int pointer) {
                if (slot.getItem() == null) return null;
                int button = pointer;
                Sword slotItem = slot.getItem();
                DragAndDrop.Payload payload = new DragAndDrop.Payload();
                if (button == 0) {
                    payload.setObject(slotItem);
                    slot.setItem(null);
                } else if (button == 1) {
                    if (slotItem.getStackSize() > 1) {
                        Sword single = ControlledPlayer.getInstance().getInventory().cloneItem(slotItem);
                        single.setStackSize(1);
                        payload.setObject(single);
                        slotItem.setStackSize(slotItem.getStackSize() - 1);
                    } else {
                        payload.setObject(slotItem);
                        slot.setItem(null);
                    }
                } else return null;
                Sword draggedItem = (Sword) payload.getObject();
                Image dragActor = new Image(draggedItem.getTexture());
                dragActor.setSize(slot.getWidth(), slot.getHeight());
                payload.setDragActor(dragActor);
                dragAndDrop.setDragActorPosition(-dragActor.getWidth() / 2f, -dragActor.getHeight() / 2f);
                return payload;
            }
        });
        dragAndDrop.addTarget(new DragAndDrop.Target(slot) {
            @Override
            public boolean drag(DragAndDrop.Source source, DragAndDrop.Payload payload, float x, float y, int pointer) {
                return true;
            }
            @Override
            public void drop(DragAndDrop.Source source, DragAndDrop.Payload payload, float x, float y, int pointer) {
                Sword draggedItem = (Sword) payload.getObject();
                InventorySlot sourceSlot = (InventorySlot) source.getActor();
                Sword targetItem = slot.getItem();
                if (targetItem == null) {
                    slot.setItem(draggedItem);
                } else {
                    if (targetItem.getId().equals(draggedItem.getId())) {
                        int canAdd = Sword.MAX_STACK_SIZE - targetItem.getStackSize();
                        if (canAdd > 0) {
                            int toAdd = Math.min(canAdd, draggedItem.getStackSize());
                            targetItem.setStackSize(targetItem.getStackSize() + toAdd);
                            draggedItem.setStackSize(draggedItem.getStackSize() - toAdd);
                        }
                        if (draggedItem.getStackSize() > 0) {
                            if (sourceSlot.getItem() == null)
                                sourceSlot.setItem(draggedItem);
                        }
                    } else {
                        slot.setItem(draggedItem);
                        sourceSlot.setItem(targetItem);
                    }
                }
            }
        });
    }
    private void setupGlobalDropTarget() {
        Actor backgroundDropArea = new Actor();
        backgroundDropArea.setBounds(0, 0, stage.getWidth(), stage.getHeight());
        backgroundDropArea.setTouchable(Touchable.enabled);
        stage.addActor(backgroundDropArea);
        dragAndDrop.addTarget(new DragAndDrop.Target(backgroundDropArea) {
            @Override
            public boolean drag(DragAndDrop.Source source, DragAndDrop.Payload payload, float x, float y, int pointer) {
                return true;
            }
            @Override
            public void drop(DragAndDrop.Source source, DragAndDrop.Payload payload, float x, float y, int pointer) {
                Sword draggedItem = (Sword) payload.getObject();
                dropItemOutside(draggedItem, draggedItem.getStackSize());
            }
        });
    }


    private void setupInputListener() {
        stage.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Input.Keys.E) {
                    toggleMainInventory();
                    return true;
                } else if (keycode == Input.Keys.Q) {
                    float x = Gdx.input.getX();
                    float y = stage.getViewport().getScreenHeight() - Gdx.input.getY();
                    Actor actor = stage.hit(x, y, true);
                    InventorySlot slot = getInventorySlot(actor);
                    if (slot != null && slot.getItem() != null) {
                        dropItemOutside(slot.getItem(), slot.getItem().getStackSize());
                        slot.setItem(null);
                        return true;
                    }
                }
                return false;
            }
        });
    }

    private InventorySlot getInventorySlot(Actor actor) {
        while (actor != null) {
            if (actor instanceof InventorySlot) return (InventorySlot) actor;
            actor = actor.getParent();
        }
        return null;
    }

    public void toggleMainInventory() {
        mainVisible = !mainVisible;
        mainTable.setVisible(mainVisible);
    }

    public void update(float delta) {
        stage.act(delta);
        if (heldItem != null) {
            float x = Gdx.input.getX();
            float y = stage.getViewport().getScreenHeight() - Gdx.input.getY();
            heldItemImage.setPosition(x - heldItemImage.getWidth() / 2f, y - heldItemImage.getHeight() / 2f);
        }
    }

    public void draw() {
        stage.draw();
    }

    public Stage getStage() {
        return stage;
    }

    public void dropItemOutside(Sword item, int amount) {
        System.out.println("Dropped " + amount + "x " + item.getName() + " outside inventory.");
        Mob mob = MobRegistry.createMob("item", Globals.physicsWorld, ControlledPlayer.getInstance().getX(), ControlledPlayer.getInstance().getY() + 40, item);
        float dropSpeed = 20f;
        float angle = ControlledPlayer.getInstance().getRotation();
        float vx = com.badlogic.gdx.math.MathUtils.cosDeg(angle) * dropSpeed;
        float vy = com.badlogic.gdx.math.MathUtils.sinDeg(angle) * dropSpeed;
        Box2DOperationManager.queueOperation(() -> {
            if (mob.getBody() != null)
                mob.getBody().setLinearVelocity(vx, vy);
        });
    }
}
