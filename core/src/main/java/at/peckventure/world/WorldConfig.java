package at.peckventure.world;

import com.badlogic.gdx.files.FileHandle;

public class WorldConfig {
    private long seed;
    private float playerX;
    private float playerY;

    // Konstruktor mit Spielerpositionen
    public WorldConfig(long seed, float playerX, float playerY) {
        this.seed = seed;
        this.playerX = playerX;
        this.playerY = playerY;
    }

    // Standard-Konstruktor (ohne Spielerpositionen)
    public WorldConfig(long seed) {
        this(seed, 0, 0);
    }

    public long getSeed() {
        return seed;
    }

    public void setSeed(long seed) {
        this.seed = seed;
    }

    public float getPlayerX() {
        return playerX;
    }

    public void setPlayerX(float playerX) {
        this.playerX = playerX;
    }

    public float getPlayerY() {
        return playerY;
    }

    public void setPlayerY(float playerY) {
        this.playerY = playerY;
    }

    // Laden der Konfiguration aus der Datei
    public static WorldConfig load(FileHandle configFile) {
        String config = configFile.readString();
        long seed = System.currentTimeMillis(); // Standardwert
        float playerX = 0;
        float playerY = 0;
        String[] lines = config.split("\n");
        for (String line : lines) {
            String[] parts = line.split("=");
            if (parts.length >= 2) {
                String key = parts[0].trim();
                String value = parts[1].trim();
                switch (key) {
                    case "seed":
                        try {
                            seed = Long.parseLong(value);
                        } catch (NumberFormatException e) {
                            seed = System.currentTimeMillis();
                        }
                        break;
                    case "playerX":
                        try {
                            playerX = Float.parseFloat(value);
                        } catch (NumberFormatException e) {
                            playerX = 0;
                        }
                        break;
                    case "playerY":
                        try {
                            playerY = Float.parseFloat(value);
                        } catch (NumberFormatException e) {
                            playerY = 0;
                        }
                        break;
                }
            }
        }
        return new WorldConfig(seed, playerX, playerY);
    }

    // Speichern der Konfiguration in die Datei
    public void save(FileHandle configFile) {
        String content = "seed=" + seed + "\n" +
            "playerX=" + playerX + "\n" +
            "playerY=" + playerY;
        configFile.writeString(content, false);
    }
}
