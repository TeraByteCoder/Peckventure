package at.peckventure.entities.mob;

import at.peckventure.Globals;
import com.badlogic.gdx.physics.box2d.World;
import java.util.HashMap;
import java.util.Map;

public class MobRegistry {
    private static final Map<Integer, MobCreator> registry = new HashMap<>();
    private static final Map<Class<? extends Mob>, Integer> classToId = new HashMap<>();
    private static final Map<String, Integer> stringToId = new HashMap<>();
    private static final Map<Integer, String> idToString = new HashMap<>();

    public static void registerMob(int id, Class<? extends Mob> clazz, MobCreator creator) {
        registry.put(id, creator);
        classToId.put(clazz, id);
    }

    public static void registerMob(int id, String stringId, Class<? extends Mob> clazz, MobCreator creator) {
        registry.put(id, creator);
        classToId.put(clazz, id);
        stringToId.put(stringId, id);
        idToString.put(id, stringId);
    }

    public static Mob createMob(int id, World world, float x, float y, Object... args) {
        Mob mob = createMobObject(id, world, x, y, args);
        Globals.mobs.put(MobMap.getNextId() , mob);
        return mob;
    }

    public static Mob createMob(String stringId, World world, float x, float y, Object... args) {
        Mob mob = createMobObject(stringId, world, x, y, args);
        Globals.mobs.put(MobMap.getNextId() , mob);
        return mob;
    }

    public static Mob createMob(int id, World world, float x, float y, int umid, Object... args) {
        Mob mob = createMobObject(id, world, x, y, args);
        Globals.mobs.put(umid , mob);
        return mob;
    }

    public static Mob createMob(String stringId, World world, float x, float y, int umid, Object... args) {
        Mob mob = createMobObject(stringId, world, x, y, args);
        Globals.mobs.put(umid , mob);
        return mob;
    }

    public static Mob createMobObject(int id, World world, float x, float y, Object... args)
    {
        MobCreator creator = registry.get(id);
        return (creator != null) ? creator.create(world, x, y, args) : null;
    }

    public static Mob createMobObject(String stringId, World world, float x, float y, Object... args) {
        Integer id = stringToId.get(stringId);
        return createMobObject(id, world, x, y, args);
    }

    public static int getMobId(Mob mob) {
        for (Map.Entry<Class<? extends Mob>, Integer> entry : classToId.entrySet()) {
            if (entry.getKey().isAssignableFrom(mob.getClass())) {
                return entry.getValue();
            }
        }
        return 0;
    }

    public static String getMobStringId(int id) {
        return idToString.get(id);
    }

    public static boolean isRegistered(int id) {
        return registry.containsKey(id);
    }

    public static boolean isRegistered(String stringId) {
        return stringToId.containsKey(stringId);
    }
}
