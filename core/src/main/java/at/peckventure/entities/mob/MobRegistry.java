package at.peckventure.entities.mob;

import com.badlogic.gdx.physics.box2d.World;

import java.util.HashMap;
import java.util.Map;

public class MobRegistry
{
    private static final Map<Integer, MobCreator> registry = new HashMap<>();
    private static final Map<Class<? extends Mob>, Integer> classToId = new HashMap<>();

    public static void registerMob(int id, Class<? extends Mob> clazz, MobCreator creator)
    {
        registry.put(id, creator);
        classToId.put(clazz, id);
    }

    public static Mob createMob(int id, World world, float x, float y, Object... args)
    {
        MobCreator creator = registry.get(id);
        return (creator != null) ? creator.create(world, x, y, args) : null;
    }

    public static int getMobId(Mob mob)
    {
        for (Map.Entry<Class<? extends Mob>, Integer> entry : classToId.entrySet())
        {
            if (entry.getKey().isAssignableFrom(mob.getClass()))
            {
                return entry.getValue();
            }
        }
        return 0;
    }
}
