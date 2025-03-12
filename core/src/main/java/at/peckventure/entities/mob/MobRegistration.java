package at.peckventure.entities.mob;

import at.peckventure.inventory.item.Sword;
import com.badlogic.gdx.Gdx;

public class MobRegistration {
    public static final int BEETLE_ID = 1;
    public static final int ITEMACTOR_ID = 2;
    public static final String BEETLE_STRING_ID = "beetle";
    public static final String ITEMACTOR_STRING_ID = "item";

    static {
        MobRegistry.registerMob(BEETLE_ID, BEETLE_STRING_ID, Beetle.class, (world, x, y, args) -> new Beetle(world, x, y));
        Gdx.app.log("MobRegistration", "Registered Beetle with int ID " + BEETLE_ID + " and string ID " + BEETLE_STRING_ID);

        MobRegistry.registerMob(ITEMACTOR_ID, ITEMACTOR_STRING_ID, ItemActor.class, (world, x, y, args) -> {
            if(args != null && args.length > 0 && args[0] instanceof Sword) {
                return new ItemActor(world, x, y, (Sword) args[0]);
            } else {
                return new ItemActor(world, x, y);
            }
        });
    }

    public static void init() {}
}
