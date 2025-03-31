package at.peckventure.entities.mob;

import com.badlogic.gdx.scenes.scene2d.Stage;
import java.util.HashMap;
import java.util.Map;

public class MobMap extends HashMap<Integer, Mob> {
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
    public synchronized Mob put(Integer id, Mob mob) {
        Mob previous = super.put(id, mob);
        if (stage != null && mob != null) {
            stage.addActor(mob);
        }
        return previous;
    }

    @Override
    public synchronized Mob remove(Object key) {
        Mob removed = super.remove(key);
        if (stage != null && removed != null) {
            removed.remove();
        }
        return removed;
    }

    @Override
    public synchronized void clear() {
        if (stage != null) {
            for (Mob mob : values()) {
                mob.remove();
            }
        }
        super.clear();
    }

    public synchronized void removeMob(Mob mob) {
        Integer keyToRemove = null;
        for (Map.Entry<Integer, Mob> entry : this.entrySet()) {
            if (entry.getValue().equals(mob)) {
                keyToRemove = entry.getKey();
                break;
            }
        }
        if (keyToRemove != null) {
            remove(keyToRemove);
        }
    }
}
