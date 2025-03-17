package at.peckventure.inventory;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import at.peckventure.inventory.item.Sword;

public class InventorySlot extends Actor {
    private Sword item;

    public InventorySlot() {
        setSize(64, 64);
    }

    public void setItem(Sword item) {
        this.item = item;
    }

    public Sword getItem() {
        return item;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (item != null) {
            batch.draw(item.getTexture(), getX(), getY(), getWidth(), getHeight());
        }
    }
}
