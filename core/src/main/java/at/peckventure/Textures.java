package at.peckventure;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;

public enum Textures
{
    DIRT("textures/blocks/dirt.png"),
    GRASS_BLOCK("textures/blocks/grass_block.png"),
    GRASSRAMPLEFT("textures/blocks/grass_ramp_left.png"),
    GRASSRAMPRIGHT("textures/blocks/grass_ramp_right.png"),
    BEETLE("textures/criters/garden_foliage_beetle_idle.png"),

    INVENTORY_SLOT("textures/inventory_slot.png"),

    TEST_ITEM("textures/test_item.png"),
    ;

    private Texture texture;
    private final String texturePath;

    Textures(String texturePath)
    {
        this.texturePath = texturePath;
    }

    // Diese Methode sollte im Hauptthread (z.B. in der create()-Methode) aufgerufen werden
    public void loadTexture()
    {
        if (texture == null)
        {
            texture = new Texture(Gdx.files.internal(texturePath));
        }
    }

    public Texture getTexture()
    {
        return texture;
    }

    // Optional: Freigeben der Textur beim Dispose
    public void dispose()
    {
        if (texture != null)
        {
            texture.dispose();
        }
    }

    static
    {
        Textures.DIRT.loadTexture();
        Textures.GRASS_BLOCK.loadTexture();
        Textures.GRASSRAMPLEFT.loadTexture();
        Textures.GRASSRAMPRIGHT.loadTexture();
        Textures.BEETLE.loadTexture();
        Textures.TEST_ITEM.loadTexture();
        Textures.INVENTORY_SLOT.loadTexture();
    }

    public static void init()
    {

    }
}
