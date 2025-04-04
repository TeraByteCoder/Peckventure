package at.peckventure.status;

import at.peckventure.entities.Player;

public class SpeedBoostEffect extends AbstractStatusEffect {

    public SpeedBoostEffect(int level, float duration) {
        super(level, duration);
    }

    @Override
    public void apply(Player player) {
        // Beispiel: Basis-Multiplikator 1.5, der sich mit steigendem Level erhöht
        float multiplier = 1.5f + (level - 1) * 0.1f;
        player.setSpeed(player.getSpeed() * multiplier);
    }

    @Override
    public void remove(Player player) {
        float multiplier = 1.5f + (level - 1) * 0.1f;
        player.setSpeed(player.getSpeed() / multiplier);
    }

    @Override
    public void update(float deltaTime) {
        duration -= deltaTime;
        if (duration <= 0) {
            expired = true;
        }
    }

    @Override
    public String getId() {
        return "speed_boost";
    }
}
