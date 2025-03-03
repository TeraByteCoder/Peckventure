package at.peckventure.entities.mob;

import com.badlogic.gdx.Gdx;

public class MobRegistration
{
    public static final int BEETLE_ID = 1;

    static
    {
        MobRegistry.registerMob(BEETLE_ID, Beetle.class, (world, x, y, args) -> new Beetle(world, x, y));
        Gdx.app.log("MobRegistration", "Registered Beetle with ID " + BEETLE_ID);
    }

    public static void init()
    {
        // Statische Initialisierung erfolgt automatisch
    }
}
