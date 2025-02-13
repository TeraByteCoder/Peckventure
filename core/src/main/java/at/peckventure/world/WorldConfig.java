package at.peckventure.world;

import com.badlogic.gdx.files.FileHandle;

public class WorldConfig {
    private long seed;
    // Hier lassen sich später weitere Einstellungen ergänzen

    public WorldConfig(long seed) {
        this.seed = seed;
    }

    public long getSeed() {
        return seed;
    }

    public void setSeed(long seed) {
        this.seed = seed;
    }

    public static WorldConfig load(FileHandle configFile) {
        String config = configFile.readString();
        long seed = System.currentTimeMillis(); // Standard, falls nichts angegeben
        String[] parts = config.split("=");
        if (parts.length >= 2 && parts[0].trim().equals("seed")) {
            try {
                seed = Long.parseLong(parts[1].trim());
            } catch (NumberFormatException e) {
                seed = System.currentTimeMillis();
            }
        }
        // Hier können weitere Einstellungen geparst werden.
        return new WorldConfig(seed);
    }

    public void save(FileHandle configFile) {
        String content = "seed=" + seed;
        // Hier können weitere Einstellungen angehängt werden.
        configFile.writeString(content, false);
    }
}
