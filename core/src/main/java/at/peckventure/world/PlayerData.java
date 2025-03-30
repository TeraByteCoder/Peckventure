package at.peckventure.world;

import com.badlogic.gdx.files.FileHandle;
import java.util.HashSet;

public class PlayerData {
    private String uuid;
    private float playerX;
    private float playerY;
    private String inventoryHotbar = "";
    private String inventoryMain = "";
    private boolean operator;
    private int energy;
    private int health;

    // Vollständiger Konstruktor inklusive energy und health
    public PlayerData(String uuid, float playerX, float playerY, String inventoryHotbar, String inventoryMain, boolean operator, int energy, int health) {
        this.uuid = uuid;
        this.playerX = playerX;
        this.playerY = playerY;
        this.inventoryHotbar = inventoryHotbar;
        this.inventoryMain = inventoryMain;
        this.operator = operator;
        this.energy = energy;
        this.health = health;
    }

    // Minimaler Konstruktor mit Standardwerten (100 für energy und health)
    public PlayerData(String uuid) {
        this(uuid, 0, 0, "", "", false, 100, 100);
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

    public boolean isOperator() {
        return operator;
    }

    public void setOperator(boolean operator) {
        this.operator = operator;
    }

    public int getEnergy() {
        return energy;
    }

    public void setEnergy(int energy) {
        this.energy = energy;
    }

    public int getHealth() {
        return health;
    }

    public void setHealth(int health) {
        this.health = health;
    }

    public static PlayerData load(FileHandle worldFolder, String uuid) {
        FileHandle playerFolder = worldFolder.child("playerData");
        FileHandle file = playerFolder.child(uuid + ".dat");
        if (!file.exists()) {
            playerFolder.mkdirs();
            PlayerData pd = new PlayerData(uuid);
            pd.save(worldFolder);
            return pd;
        }
        String config = file.readString();
        float playerX = 0;
        float playerY = 0;
        String inventoryHotbar = "";
        String inventoryMain = "";
        boolean operator = false;
        int energy = 100;
        int health = 100;
        String[] lines = config.split("\n");
        for (String line : lines) {
            String[] parts = line.split("=");
            if (parts.length >= 2) {
                String key = parts[0].trim();
                String value = parts[1].trim();
                switch (key) {
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
                    case "inventoryHotbar":
                        inventoryHotbar = value;
                        break;
                    case "inventoryMain":
                        inventoryMain = value;
                        break;
                    case "operator":
                        operator = Boolean.parseBoolean(value);
                        break;
                    case "energy":
                        try {
                            energy = Integer.parseInt(value);
                        } catch (NumberFormatException e) {
                            energy = 100;
                        }
                        break;
                    case "health":
                        try {
                            health = Integer.parseInt(value);
                        } catch (NumberFormatException e) {
                            health = 100;
                        }
                        break;
                }
            }
        }
        return new PlayerData(uuid, playerX, playerY, inventoryHotbar, inventoryMain, operator, energy, health);
    }

    public void save(FileHandle worldFolder) {
        FileHandle playerFolder = worldFolder.child("playerData");
        FileHandle file = playerFolder.child(uuid + ".dat");
        String content = "uuid=" + uuid + "\n" +
            "playerX=" + playerX + "\n" +
            "playerY=" + playerY + "\n" +
            "inventoryHotbar=" + inventoryHotbar + "\n" +
            "inventoryMain=" + inventoryMain + "\n" +
            "operator=" + operator + "\n" +
            "energy=" + energy + "\n" +
            "health=" + health;
        file.writeString(content, false);
    }

    public static HashSet<String> getPlayerUUIDs(FileHandle worldFolder) {
        FileHandle playerFolder = worldFolder.child("playerData");
        HashSet<String> uuids = new HashSet<>();
        for (FileHandle file : playerFolder.list()) {
            if (file.name().endsWith(".dat")) {
                uuids.add(file.name().substring(0, file.name().length() - 4));
            }
        }
        return uuids;
    }
}
