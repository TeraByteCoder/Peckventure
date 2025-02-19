package at.peckventure;

import static at.peckventure.Const.savesDir;
import at.peckventure.Textures;
import at.peckventure.entities.mob.MobRegistration;
import at.peckventure.world.block.BlockRegistration;
import org.w3c.dom.Text;

public abstract class OnStartCheck
{
    public static void checkOnStart()
    {
        if (!savesDir.exists()) savesDir.mkdirs();
        loadTextures();
        BlockRegistration.init();
        MobRegistration.init();
    }

    public static void loadTextures()
    {
        Textures.DIRT.loadTexture();
        Textures.GRASS_BLOCK.loadTexture();
        Textures.GRASSRAMPLEFT.loadTexture();
        Textures.GRASSRAMPRIGHT.loadTexture();
        Textures.BEETLE.loadTexture();
    }
}
