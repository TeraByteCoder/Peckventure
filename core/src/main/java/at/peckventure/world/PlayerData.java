package at.peckventure.world;

import at.peckventure.Const;
import com.badlogic.gdx.files.FileHandle;

import java.util.HashSet;

public class PlayerData
{
    private String uuid;
    private float playerX;
    private float playerY;
    private String inventoryHotbar = "";
    private String inventoryMain = "";
    private boolean operator;
    private int energy;
    private int health;
    private int maxHealth;
    private int maxEnergy;

    private String effects;

    // Vollständiger Konstruktor inklusive energy und health
    public PlayerData(String uuid, float playerX, float playerY, String inventoryHotbar, String inventoryMain, boolean operator, int energy, int health, int maxHealth, int maxEnergy, String effects)
    {
        this.uuid = uuid;
        this.playerX = playerX;
        this.playerY = playerY;
        this.inventoryHotbar = inventoryHotbar;
        this.inventoryMain = inventoryMain;
        this.operator = operator;
        this.energy = energy;
        this.health = health;
        this.effects = effects;
        this.maxHealth = maxHealth;
        this.maxEnergy = maxEnergy;
    }

    // Minimaler Konstruktor mit Standardwerten (100 für energy und health)
    public PlayerData(String uuid)
    {
        this(uuid, 0, 0, "", "", false, Const.MAXENERGY, Const.MAXHEALTH, Const.MAXHEALTH, Const.MAXENERGY, "");
    }

    public float getPlayerX()
    {
        return playerX;
    }

    public void setPlayerX(float playerX)
    {
        this.playerX = playerX;
    }

    public float getPlayerY()
    {
        return playerY;
    }

    public void setPlayerY(float playerY)
    {
        this.playerY = playerY;
    }

    public String getInventoryHotbar()
    {
        return inventoryHotbar;
    }

    public void setInventoryHotbar(String inventoryHotbar)
    {
        this.inventoryHotbar = inventoryHotbar;
    }

    public String getInventoryMain()
    {
        return inventoryMain;
    }

    public void setInventoryMain(String inventoryMain)
    {
        this.inventoryMain = inventoryMain;
    }

    public boolean isOperator()
    {
        return operator;
    }

    public void setOperator(boolean operator)
    {
        this.operator = operator;
    }

    public int getEnergy()
    {
        return energy;
    }

    public void setEnergy(int energy)
    {
        this.energy = energy;
    }

    public int getHealth()
    {
        return health;
    }

    public void setHealth(int health)
    {
        this.health = health;
    }


    public int getMaxHealth()
    {
        return maxHealth;
    }

    public void setMaxHealth(int maxHealth)
    {
        this.maxHealth = maxHealth;
    }

    public int getMaxEnergy()
    {
        return maxEnergy;
    }

    public void setMaxEnergy(int maxEnergy)
    {
        this.maxEnergy = maxEnergy;
    }

    public String getEffects()
    {
        return effects;
    }

    public void setEffects(String effects)
    {
        this.effects = effects;
    }

    public static PlayerData load(FileHandle worldFolder, String uuid)
    {
        FileHandle playerFolder = worldFolder.child("playerData");
        FileHandle file = playerFolder.child(uuid + ".dat");
        if (!file.exists())
        {
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
        int energy = Const.MAXENERGY;
        int health = Const.MAXHEALTH;
        int maxHealth = Const.MAXHEALTH;
        int maxEnergy = Const.MAXENERGY;
        String effects = "";
        String[] lines = config.split("\n");
        for (String line : lines)
        {
            String[] parts = line.split("=");
            if (parts.length >= 2)
            {
                String key = parts[0].trim();
                String value = parts[1].trim();
                switch (key)
                {
                    case "playerX":
                        try
                        {
                            playerX = Float.parseFloat(value);
                        } catch (NumberFormatException e)
                        {
                            playerX = 0;
                        }
                        break;
                    case "playerY":
                        try
                        {
                            playerY = Float.parseFloat(value);
                        } catch (NumberFormatException e)
                        {
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
                        try
                        {
                            energy = Integer.parseInt(value);
                        } catch (NumberFormatException e)
                        {
                            energy = Const.MAXENERGY;
                        }
                        break;
                    case "health":
                        try
                        {
                            health = Integer.parseInt(value);
                        } catch (NumberFormatException e)
                        {
                            health = Const.MAXHEALTH;
                        }
                        break;
                    case "maxHealth":
                        try
                        {
                            if (Integer.parseInt(value) != 0)
                                maxHealth = Integer.parseInt(value);
                        } catch (NumberFormatException e)
                        {
                            maxHealth = Const.MAXHEALTH;
                        }
                        break;
                    case "maxEnergy":
                        try
                        {
                            if (Integer.parseInt(value) != 0)
                                maxEnergy = Integer.parseInt(value);
                        } catch (NumberFormatException e)
                        {
                            maxEnergy = Const.MAXENERGY;
                        }
                        break;
                    case "effects":
                        effects = value;
                }
            }
        }
        return new PlayerData(uuid, playerX, playerY, inventoryHotbar, inventoryMain, operator, energy, health, maxHealth, maxEnergy, effects);
    }

    public void save(FileHandle worldFolder)
    {
        FileHandle playerFolder = worldFolder.child("playerData");
        FileHandle file = playerFolder.child(uuid + ".dat");
        String content = "uuid=" + uuid + "\n" +
            "playerX=" + playerX + "\n" +
            "playerY=" + playerY + "\n" +
            "inventoryHotbar=" + inventoryHotbar + "\n" +
            "inventoryMain=" + inventoryMain + "\n" +
            "operator=" + operator + "\n" +
            "energy=" + energy + "\n" +
            "health=" + health + "\n" +
            "maxHealth=" + maxHealth + "\n" +
            "maxEnergy=" + maxEnergy + "\n" +
            "effects=" + effects + "\n";
        file.writeString(content, false);
    }

    public static HashSet<String> getPlayerUUIDs(FileHandle worldFolder)
    {
        FileHandle playerFolder = worldFolder.child("playerData");
        HashSet<String> uuids = new HashSet<>();
        for (FileHandle file : playerFolder.list())
        {
            if (file.name().endsWith(".dat"))
            {
                uuids.add(file.name().substring(0, file.name().length() - 4));
            }
        }
        return uuids;
    }
}
