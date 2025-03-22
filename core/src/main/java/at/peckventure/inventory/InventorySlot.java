package at.peckventure.inventory;

import at.peckventure.inventory.item.Item;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Actor;

public class InventorySlot extends Actor {
    private Item item;
    private BitmapFont font;

    public InventorySlot() {
        setSize(64, 64);
        if(Gdx.gl != null)
        {
            font = new BitmapFont();
            font.getData().setScale(1f);
        }
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Item getItem() {
        return item;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (item != null) {
            batch.draw(item.getTexture(), getX(), getY(), getWidth(), getHeight());
            if (item.getStackSize() > 1) {
                // Annahme: font ist eine BitmapFont-Instanz, die du vorher initialisiert hast
                font.draw(batch, String.valueOf(item.getStackSize()),
                    getX() + getWidth() - 20, // x-Position, anpassen je nach gewünschtem Layout
                    getY() + 20); // y-Position
            }
        }
    }

}
