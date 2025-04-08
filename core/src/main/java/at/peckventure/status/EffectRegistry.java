package at.peckventure.status;

import at.peckventure.status.effects.SpeedBoostEffect;

import java.util.HashMap;
import java.util.Map;

public class EffectRegistry {

    public interface EffectFactory {
        StatusEffect create(int level, float duration);
    }

    private static final Map<String, EffectFactory> registry = new HashMap<>();

    public static void register(String id, EffectFactory factory) {
        registry.put(id, factory);
    }

    public static StatusEffect createEffect(String id, int level, float duration) {
        EffectFactory factory = registry.get(id);
        return (factory != null) ? factory.create(level, duration) : null;
    }

    public static boolean contains(String id) {
        return registry.containsKey(id);
    }

    static {
        // Beispiel-Registrierung für SpeedBoostEffect
        register("speed_boost", (level, duration) -> new SpeedBoostEffect(level, duration));
    }


}
