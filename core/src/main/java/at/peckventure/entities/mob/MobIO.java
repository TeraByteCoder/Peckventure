package at.peckventure.entities.mob;

import com.badlogic.gdx.physics.box2d.World;
import com.google.gson.Gson;

public class MobIO {
    public static String serializeToJson(Mob mob) {
        Gson gson = new Gson();
        MobData data = new MobData();
        data.id = MobRegistry.getMobId(mob);
        data.x = mob.getX();
        data.y = mob.getY();
        return gson.toJson(data);
    }

    public static Mob deserializeFromJson(String json, World world) {
        Gson gson = new Gson();
        MobData data = gson.fromJson(json, MobData.class);
        return MobRegistry.createMob(data.id, world, data.x, data.y);
    }

    static class MobData {
        int id;
        float x;
        float y;
    }
}
