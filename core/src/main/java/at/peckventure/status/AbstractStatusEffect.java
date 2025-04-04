package at.peckventure.status;

import at.peckventure.entities.Player;

public abstract class AbstractStatusEffect implements StatusEffect {
    protected int level;
    protected float duration;
    protected boolean expired = false;

    public AbstractStatusEffect(int level, float duration) {
        this.level = level;
        this.duration = duration;
    }

    public int getLevel() {
        return level;
    }

    public float getRemainingDuration() {
        return duration;
    }

    /**
     * Liefert die eindeutige ID des Effekts, z.B. "speed_boost".
     */
    public abstract String getId();

    @Override
    public boolean isExpired() {
        return expired;
    }


}
