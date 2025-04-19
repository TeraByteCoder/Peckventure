package at.peckventure.entities.mob;

import at.peckventure.inventory.ItemRegistry;
import com.badlogic.gdx.physics.box2d.World;
import com.google.gson.Gson;

public class MobIO {
    public static String serializeToJson(Mob mob) {
        Gson gson = new Gson();
        MobData data = new MobData();
        data.id = MobRegistry.getMobId(mob);
        data.x = mob.getX();
        data.y = mob.getY();
        if (mob instanceof ItemActor) {
            data.extraItem = ((ItemActor) mob).getInventoryItem().getId();
            data.amount = ((ItemActor) mob).getAmount();  // Save the amount
        }
        return gson.toJson(data);
    }

    public static Mob deserializeFromJson(String json, World world) {
        Gson gson = new Gson();
        MobData data = gson.fromJson(json, MobData.class);
        if (data.id == MobRegistration.ITEMACTOR_ID && data.extraItem != null) {
            int amount = (data.amount > 0) ? data.amount : 1;  // Use 1 as default if amount is not set
            return MobRegistry.createMobObject(data.id, world, data.x, data.y,
                ItemRegistry.createItem(data.extraItem), amount);
        } else {
            return MobRegistry.createMobObject(data.id, world, data.x, data.y);
        }
    }

    public static class MobData {
        int id;
        float x;
        float y;
        String extraItem; // For ItemActor; null for other mobs
        int amount = 1;   // Default value is 1
    }

}
