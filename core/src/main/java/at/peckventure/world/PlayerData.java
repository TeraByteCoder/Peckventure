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

    public PlayerData(String uuid, float playerX, float playerY, String inventoryHotbar, String inventoryMain, boolean operator) {
        this.playerX = playerX;
        this.playerY = playerY;
        this.inventoryHotbar = inventoryHotbar;
        this.inventoryMain = inventoryMain;
        this.operator = operator;
        this.uuid = uuid;
    }

    public PlayerData(String uuid) {
        this(uuid, 0, 0, "", "", false);
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
                }
            }
        }
        return new PlayerData(uuid, playerX, playerY, inventoryHotbar, inventoryMain, operator);
    }

    public void save(FileHandle worldFolder) {
        FileHandle playerFolder = worldFolder.child("playerData");
        FileHandle file = playerFolder.child(uuid + ".dat");
        String content = "uuid=" + uuid + "\n" +
            "playerX=" + playerX + "\n" +
            "playerY=" + playerY + "\n" +
            "inventoryHotbar=" + inventoryHotbar + "\n" +
            "inventoryMain=" + inventoryMain + "\n" +
            "operator=" + operator;
        file.writeString(content, false);
    }

    public static HashSet<String> getPlayerUUIDs(FileHandle worldFolder) {
        FileHandle playerFolder = worldFolder.child("playerData");
        HashSet<String> uuids = new HashSet<>();
        for (FileHandle file : worldFolder.list()) {
            if (file.name().endsWith(".dat")) {
                uuids.add(file.name().substring(0, file.name().length() - 4));
            }
        }
        return uuids;
    }
}
