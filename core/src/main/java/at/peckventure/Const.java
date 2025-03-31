package at.peckventure;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

public abstract class Const
{
    public static final FileHandle savesDir = Gdx.files.absolute(System.getenv("APPDATA") + "/Peckventure/saves/");
    public static final FileHandle gameDir = Gdx.files.absolute(System.getenv("APPDATA") + "/Peckventure/");

    public static final int MAXHEALTH = 100;
    public static final int MAXENERGY = 50;

}
