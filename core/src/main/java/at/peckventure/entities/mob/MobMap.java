package at.peckventure.entities.mob;

import com.badlogic.gdx.scenes.scene2d.Stage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MobMap extends ConcurrentHashMap<Integer, Mob> {
    private final Stage stage;
    private static int nextId = 0; // auto-increment ID

    public MobMap() {
        this.stage = null;
    }

    public MobMap(Stage stage) {
        this.stage = stage;
    }

    public static synchronized int getNextId() {
        return nextId++;
    }

    @Override
    public Mob put(Integer id, Mob mob) {
        Mob previous = super.put(id, mob);
        if (stage != null && mob != null) {
            stage.addActor(mob);
        }
        return previous;
    }

    @Override
    public Mob remove(Object key) {
        Mob removed = super.remove(key);
        if (stage != null && removed != null) {
            removed.remove();
        }
        return removed;
    }

    @Override
    public void clear() {
        if (stage != null) {
            // With ConcurrentHashMap, the iterator is weakly consistent.
            for (Mob mob : values()) {
                mob.remove();
            }
        }
        super.clear();
    }

    public void removeMob(Mob mob) {
        // With ConcurrentHashMap, we can iterate without extra synchronization.
        for (Map.Entry<Integer, Mob> entry : entrySet()) {
            if (entry.getValue().equals(mob)) {
                remove(entry.getKey());
                break;
            }
        }
    }
}
