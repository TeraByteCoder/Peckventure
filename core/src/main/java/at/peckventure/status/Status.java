package at.peckventure.status;

/**
 * Repräsentiert einen generischen Statuswert wie Gesundheit, Energie oder Mana.
 */
public class Status {
    private final String name;
    private float current;
    private int max;

    public Status(String name, int max) {
        this.name = name;
        this.max = max;
        this.current = max;
    }

    public void damage(float amount) {
        current = Math.max(0, current - amount);
    }

    public void heal(float amount) {
        current = Math.min(max, current + amount);
    }

    public void consume(float amount) {
        current = Math.max(0, current - amount);
    }

    public void regenerate(float amount) {
        current = Math.min(max, current + amount);
    }

    public float getCurrent() {
        return current;
    }

    public int getMax() {
        return max;
    }

    public String getName() {
        return name;
    }

    public void setCurrent(float current) {
        this.current = Math.max(0, Math.min(current, max));
    }

    public void setMax(int max) {
        this.max = max;
        if (current > max) current = max;
    }

    public float getPercentage() {
        return (float) current / max;
    }
}
