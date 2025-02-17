package at.peckventure;

import static at.peckventure.Const.savesDir;
import at.peckventure.Textures;
import at.peckventure.world.block.BlockRegistration;

public abstract class OnStartCheck
{
    public static void checkOnStart()
    {
        if (!savesDir.exists()) savesDir.mkdirs();
        loadTextures();
        BlockRegistration.init();
    }

    public static void loadTextures()
    {
        Textures.DIRT.loadTexture();
        Textures.GRASS_BLOCK.loadTexture();
        Textures.GRASSRAMPLEFT.loadTexture();
        Textures.GRASSRAMPRIGHT.loadTexture();
    }
}
