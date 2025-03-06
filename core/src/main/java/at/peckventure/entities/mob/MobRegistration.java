package at.peckventure.entities.mob;

import com.badlogic.gdx.Gdx;

public class MobRegistration {
    public static final int BEETLE_ID = 1;
    public static final String BEETLE_STRING_ID = "beetle";

    static {
        MobRegistry.registerMob(BEETLE_ID, BEETLE_STRING_ID, Beetle.class, (world, x, y, args) -> new Beetle(world, x, y));
        Gdx.app.log("MobRegistration", "Registered Beetle with int ID " + BEETLE_ID + " and string ID " + BEETLE_STRING_ID);
    }

    public static void init() {}
}
