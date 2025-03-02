package at.peckventure;

import static at.peckventure.Const.savesDir;
import at.peckventure.Textures;
import at.peckventure.chat.CommandRegistry;
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

        BlockRegistration.init();
        MobRegistration.init();
        CommandRegistry.init();
        Textures.init();
        ItemRegistry.init();
    }


}
