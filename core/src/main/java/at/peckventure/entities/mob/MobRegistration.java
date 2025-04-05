package at.peckventure.entities.mob;

import at.peckventure.inventory.item.Item;
import com.badlogic.gdx.Gdx;

public class MobRegistration {
    public static final int BEETLE_ID = 1;

    public static final int ITEMACTOR_ID = 2;

    public static final int PHYTON_ID = 3;
    public static final String BEETLE_STRING_ID = "beetle";

    public static final String PHYTON_STRING_ID = "phyton";
    public static final String ITEMACTOR_STRING_ID = "item";

    static {
        MobRegistry.registerMob(BEETLE_ID, BEETLE_STRING_ID, Beetle.class, (world, x, y, args) -> new Beetle(world, x, y));
        Gdx.app.log("MobRegistration", "Registered Beetle with int ID " + BEETLE_ID + " and string ID " + BEETLE_STRING_ID);

        MobRegistry.registerMob(PHYTON_ID, PHYTON_STRING_ID, Phyton.class, (world, x, y, args) -> new Phyton(world, x, y, 10));
        Gdx.app.log("MobRegistration", "Registered Beetle with int ID " + PHYTON_ID + " and string ID " + PHYTON_STRING_ID);

        MobRegistry.registerMob(ITEMACTOR_ID, ITEMACTOR_STRING_ID, ItemActor.class, (world, x, y, args) -> {
            if(args != null && args.length > 0 && args[0] instanceof Item) {
                return new ItemActor(world, x, y, (Item) args[0]);
            } else {
                return new ItemActor(world, x, y);
            }
        });
    }

    public static void init() {}
}
