package at.peckventure.world;

import com.badlogic.gdx.files.FileHandle;

public class WorldConfig
{
    private long seed;
    private int spawnX;
    private int spawnY;


    public WorldConfig(long seed)
    {
        this.seed = seed;
    }

    public long getSeed()
    {
        return seed;
    }

    public void setSeed(long seed)
    {
        this.seed = seed;
    }

    // Laden aus der Datei – es werden zusätzlich die Inventardaten geparst
    public static WorldConfig load(FileHandle configFile)
    {
        String config = configFile.readString();

        long seed = System.currentTimeMillis();
        int spawnX = 0;
        int spawnY = 0;

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
                    case "seed":
                        try
                        {
                            seed = Long.parseLong(value);
                        } catch (NumberFormatException e)
                        {
                            seed = System.currentTimeMillis();
                        }
                        break;
                    case "spawnX":
                        try
                        {
                            spawnX = Integer.parseInt(value);
                        } catch (NumberFormatException e)
                        {
                            // todo besseren default spawnpunkt finden
                            spawnX = 0;
                        }
                        break;
                    case "spawnY":
                        try
                        {
                            spawnY = Integer.parseInt(value);
                        } catch (NumberFormatException e)
                        {
                            spawnY = 0;
                        }
                }
            }
        }
        return new WorldConfig(seed);
    }

    // Speichern – es werden alle Felder (einschließlich Inventardaten) in die Datei geschrieben
    public void save(FileHandle configFile)
    {
        String content = "seed=" + seed + "\n" +
            "spawnX=" + spawnX + "\n" +
            "spawnY=" + spawnY;
        configFile.writeString(content, false);
    }
}
