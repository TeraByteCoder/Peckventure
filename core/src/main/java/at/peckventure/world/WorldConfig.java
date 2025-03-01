package at.peckventure.world;

import com.badlogic.gdx.files.FileHandle;

public class WorldConfig {
    private long seed;
    private float playerX;
    private float playerY;
    private String inventoryHotbar = "";
    private String inventoryMain = "";

    public WorldConfig(long seed, float playerX, float playerY, String inventoryHotbar, String inventoryMain) {
        this.seed = seed;
        this.playerX = playerX;
        this.playerY = playerY;
        this.inventoryHotbar = inventoryHotbar;
        this.inventoryMain = inventoryMain;
    }

    public WorldConfig(long seed) {
        this(seed, 0, 0, "", "");
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
    public String getInventoryHotbar() {
        return inventoryHotbar;
    }
    public void setInventoryHotbar(String inventoryHotbar) {
        this.inventoryHotbar = inventoryHotbar;
    }
    public String getInventoryMain() {
        return inventoryMain;
    }
    public void setInventoryMain(String inventoryMain) {
        this.inventoryMain = inventoryMain;
    }

    // Laden aus der Datei – es werden zusätzlich die Inventardaten geparst
    public static WorldConfig load(FileHandle configFile) {
        String config = configFile.readString();
        long seed = System.currentTimeMillis();
        float playerX = 0;
        float playerY = 0;
        String inventoryHotbar = "";
        String inventoryMain = "";
        String[] lines = config.split("\n");
        for (String line : lines) {
            String[] parts = line.split("=");
            if(parts.length >= 2) {
                String key = parts[0].trim();
                String value = parts[1].trim();
                switch(key) {
                    case "seed":
                        try { seed = Long.parseLong(value); } catch(NumberFormatException e) { seed = System.currentTimeMillis(); }
                        break;
                    case "playerX":
                        try { playerX = Float.parseFloat(value); } catch(NumberFormatException e) { playerX = 0; }
                        break;
                    case "playerY":
                        try { playerY = Float.parseFloat(value); } catch(NumberFormatException e) { playerY = 0; }
                        break;
                    case "inventoryHotbar":
                        inventoryHotbar = value;
                        break;
                    case "inventoryMain":
                        inventoryMain = value;
                        break;
                }
            }
        }
        return new WorldConfig(seed, playerX, playerY, inventoryHotbar, inventoryMain);
    }

    // Speichern – es werden alle Felder (einschließlich Inventardaten) in die Datei geschrieben
    public void save(FileHandle configFile) {
        String content = "seed=" + seed + "\n" +
            "playerX=" + playerX + "\n" +
            "playerY=" + playerY + "\n" +
            "inventoryHotbar=" + inventoryHotbar + "\n" +
            "inventoryMain=" + inventoryMain;
        configFile.writeString(content, false);
    }
}
