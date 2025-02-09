package at.peckventure;

import static at.peckventure.Const.savesDir;

public abstract class OnStartCheck
{
    public static void checkOnStart()
    {
        if (!savesDir.exists()) savesDir.mkdirs();
    }
}
