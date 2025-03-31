package at.peckventure.status;

/**
 * Repräsentiert einen generischen Statuswert wie Gesundheit, Energie oder Mana.
 */
public class Status {
    private final String name;
    private int current;
    private int max;

    public Status(String name, int max) {
        this.name = name;
        this.max = max;
        this.current = max;
    }

    public void damage(int amount) {
        current = Math.max(0, current - amount);
    }

    public void heal(int amount) {
        current = Math.min(max, current + amount);
    }

    public void consume(int amount) {
        current = Math.max(0, current - amount);
    }

    public void regenerate(int amount) {
        current = Math.min(max, current + amount);
    }

    public int getCurrent() {
        return current;
    }

    public int getMax() {
        return max;
    }

    public String getName() {
        return name;
    }

    public void setCurrent(int current) {
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
