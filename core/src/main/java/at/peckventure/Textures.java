package at.peckventure;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;

public enum Textures
{
    DIRT("textures/blocks/dirt.png"),
    GRASS_BLOCK("textures/blocks/grass_block.png"),
    GRASSRAMPLEFT("textures/blocks/grass_ramp_left.png"),
    GRASSRAMPRIGHT("textures/blocks/grass_ramp_right.png"),
    BEETLE("textures/criters/garden_foliage_beetle_idle.png"),
    INVENTORY_SLOT("textures/inventory_slot.png"),
    TEST_ITEM("textures/items/test_item.png"),
    SPEED_POTION("textures/items/speed_potion.png"),
    PHYTON("textures/criters/garden_foliage_beetle_idle.png"),
    WOOD("textures/items/wood.png"),
    SPRUCE_LOG("textures/blocks/log_spruce.png"),
    SPRUCE_LEAVES("textures/blocks/leaves_spruce.png"),


    ;
    private Texture texture;
    private final String texturePath;

    Textures(String texturePath)
    {
        this.texturePath = texturePath;
    }

    public void loadTexture()
    {
        // Lade nur, wenn eine Grafikumgebung existiert
        if (Gdx.graphics != null)
        {
            texture = new Texture(Gdx.files.internal(texturePath));
        }
    }

    public Texture getTexture()
    {
        // Falls keine Grafikumgebung vorhanden ist, gib null zurück
        if (Gdx.graphics == null)
        {
            return null;
        }
        return texture;
    }

    public void dispose()
    {
        if (texture != null)
        {
            texture.dispose();
        }
    }

    static
    {
        // Nur laden, wenn Grafik verfügbar ist
        if (Gdx.gl != null)
        {
            for (Textures t : values())
            {
                t.loadTexture();
            }
        }
    }

    public static void init()
    {
        // Kann leer bleiben – statischer Block übernimmt das Laden, wenn möglich
    }
}
