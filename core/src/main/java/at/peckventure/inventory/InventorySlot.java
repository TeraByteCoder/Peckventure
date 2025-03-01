package at.peckventure.inventory;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.scenes.scene2d.Actor;
import at.peckventure.inventory.item.Item;

public class InventorySlot extends Actor {
    private static final BitmapFont font = new BitmapFont(); // einfache Standard-Font
    private static final GlyphLayout layout = new GlyphLayout();

    private Item item;               // das Item (kann null sein)
    private final Texture slotTexture; // Hintergrund

    public InventorySlot(Texture slotTexture) {
        this.slotTexture = slotTexture;
        setSize(64, 64);
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Item getItem() {
        return item;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        // 1) Slot-Hintergrund zeichnen
        batch.draw(slotTexture, getX(), getY(), getWidth(), getHeight());
        // 2) Falls Item vorhanden, dessen Textur
        if (item != null) {
            batch.draw(item.getTexture(), getX(), getY(), getWidth(), getHeight());
            // 3) Falls Stack > 1, die Anzahl unten rechts zeichnen
            if (item.getStackSize() > 1) {
                String countStr = String.valueOf(item.getStackSize());
                layout.setText(font, countStr);
                float textWidth = layout.width;
                float textHeight = layout.height;
                // Platzierung (unten rechts, mit kleinem Abstand)
                float tx = getX() + getWidth() - textWidth - 3;
                float ty = getY() + textHeight + 3;
                font.draw(batch, countStr, tx, ty);
            }
        }
    }
}
