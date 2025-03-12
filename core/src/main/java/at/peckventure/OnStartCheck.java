package at.peckventure;

import static at.peckventure.Const.savesDir;

import at.peckventure.chat.CommandRegistry;
import at.peckventure.entities.mob.MobRegistration;
import at.peckventure.inventory.ItemRegistry;
import at.peckventure.world.block.BlockRegistration;

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
