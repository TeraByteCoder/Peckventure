package at.peckventure;

import static at.peckventure.Const.savesDir;
import at.peckventure.Textures;
import at.peckventure.entities.mob.MobRegistration;
import at.peckventure.inventory.ItemRegistry;
import at.peckventure.inventory.item.Item;
import at.peckventure.world.block.BlockRegistration;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import org.w3c.dom.Text;

public abstract class OnStartCheck
{
    public static void checkOnStart()
    {
        if (!savesDir.exists()) savesDir.mkdirs();
        loadTextures();
        BlockRegistration.init();
        MobRegistration.init();
        registerItems();
    }

    public static void loadTextures()
    {
        Textures.DIRT.loadTexture();
        Textures.GRASS_BLOCK.loadTexture();
        Textures.GRASSRAMPLEFT.loadTexture();
        Textures.GRASSRAMPRIGHT.loadTexture();
        Textures.BEETLE.loadTexture();
        Textures.TEST_ITEM.loadTexture();
        Textures.INVENTORY_SLOT.loadTexture();
    }

    public static void registerItems()
    {
        ItemRegistry.register("sword", () ->
            new Item("sword", "Schwert", Textures.TEST_ITEM.getTexture())
        );
    }
}
