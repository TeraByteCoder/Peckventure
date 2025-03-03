package at.peckventure.world;

import at.peckventure.entities.Player;
import at.peckventure.world.chunk.Chunk;
import at.peckventure.world.chunk.ChunkIO;
import at.peckventure.world.generator.WorldGenerator;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.physics.box2d.World;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class InfiniteTilemap
{
    private final World physicsWorld;
    public static final int RENDER_DISTANCE = 2;
    private final WorldGenerator worldGenerator;
    // Wir nutzen hier ein thread-sicheres Set – so kannst du im Render-Thread ohne Synchronisierung iterieren.
    private final Set<Chunk> loadedChunks = ConcurrentHashMap.newKeySet();
    private final RegionManager regionManager;

    // Thread für das asynchrone Laden/Unloading der Chunks
    private Thread chunkUpdateThread;
    private volatile boolean running = false;

    public InfiniteTilemap(World world, WorldGenerator generator, Set<Chunk> preLoadedChunks, RegionManager regionManager)
    {
        this.physicsWorld = world;
        this.worldGenerator = generator;
        if (preLoadedChunks != null)
        {
            loadedChunks.addAll(preLoadedChunks);
        }
        this.regionManager = regionManager;
    }

    /**
     * Rendern: Hier werden einfach alle aktuell geladenen Chunks gezeichnet.
     */
    public void render(Batch batch)
    {
        for (Chunk chunk : loadedChunks)
        {
            chunk.render(batch);
        }
    }

    public WorldGenerator getWorldGenerator()
    {
        return worldGenerator;
    }

    public Set<Chunk> getLoadedChunks()
    {
        return loadedChunks;
    }

    /**
     * Lädt neue Chunks um den Spieler, falls sie noch nicht vorhanden sind.
     */
    private void loadChunksAroundPlayer(Player player)
    {
        for (int x_offset = -RENDER_DISTANCE - 1; x_offset <= RENDER_DISTANCE; x_offset++)
        {
            for (int y_offset = -RENDER_DISTANCE; y_offset <= RENDER_DISTANCE; y_offset++)
            {
                int targetChunkX = player.getChunkX() + x_offset;
                int targetChunkY = player.getChunkY() + y_offset;
                Chunk dummy = new Chunk(targetChunkX, targetChunkY);
                if (!loadedChunks.contains(dummy))
                {
                    // Bestimme Region- und lokale Koordinaten
                    int regionX = Math.floorDiv(targetChunkX, RegionManager.REGION_SIZE);
                    int regionY = Math.floorDiv(targetChunkY, RegionManager.REGION_SIZE);
                    RegionFile regionFile = regionManager.getRegionFile(regionX, regionY);
                    int localX = Math.floorMod(targetChunkX, RegionManager.REGION_SIZE);
                    int localY = Math.floorMod(targetChunkY, RegionManager.REGION_SIZE);

                    byte[] data = null;
                    try
                    {
                        data = regionFile.readChunk(localX, localY);
                    } catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                    Chunk chunk;
                    if (data != null)
                    {
                        // Chunk existiert bereits – laden!
                        chunk = ChunkIO.deserialize(data, physicsWorld);
                    } else
                    {
                        // Chunk nicht gefunden – generieren
                        chunk = new Chunk(targetChunkX, targetChunkY);
                        worldGenerator.generateChunk(chunk);
                    }
                    loadedChunks.add(chunk);
                }
            }
        }
    }

    /**
     * Unloadet Chunks, die zu weit vom Spieler entfernt sind und speichert sie vorher in der passenden Region-Datei.
     * Dabei wird auch die dispose()-Methode des Chunks aufgerufen, um Ressourcen freizugeben.
     */
    private void unloadChunksOutsideRenderDistance(Player player)
    {
        Iterator<Chunk> iterator = loadedChunks.iterator();
        while (iterator.hasNext())
        {
            Chunk chunk = iterator.next();
            if (Math.abs(chunk.getChunkX() - player.getChunkX()) > RENDER_DISTANCE + 2 ||
                Math.abs(chunk.getChunkY() - player.getChunkY()) > RENDER_DISTANCE + 2)
            {
                // Zuerst serialisieren
                byte[] data = ChunkIO.serialize(chunk);

                // Speichern des Chunks in die Region-Datei
                int regionX = Math.floorDiv(chunk.getChunkX(), RegionManager.REGION_SIZE);
                int regionY = Math.floorDiv(chunk.getChunkY(), RegionManager.REGION_SIZE);
                RegionFile regionFile = regionManager.getRegionFile(regionX, regionY);
                int localX = Math.floorMod(chunk.getChunkX(), RegionManager.REGION_SIZE);
                int localY = Math.floorMod(chunk.getChunkY(), RegionManager.REGION_SIZE);

                try
                {
                    regionFile.writeChunk(localX, localY, data);
                } catch (IOException e)
                {
                    e.printStackTrace();
                }

                // Jetzt die Ressourcen freigeben
                chunk.dispose();
                iterator.remove();
            }
        }
    }


    /**
     * Führt beide Operationen aus – das wird im Hintergrund-Thread regelmäßig aufgerufen.
     */
    private void updateChunks(Player player)
    {
        loadChunksAroundPlayer(player);
        unloadChunksOutsideRenderDistance(player);
    }

    /**
     * Startet einen Hintergrund-Thread, der in regelmäßigen Abständen (z. B. alle 100ms) die Chunk-Liste aktualisiert.
     */
    public void startChunkUpdateThread(Player player)
    {
        if (chunkUpdateThread != null && chunkUpdateThread.isAlive())
        {
            return;
        }
        running = true;
        chunkUpdateThread = new Thread(() ->
        {
            while (running)
            {
                updateChunks(player);
                try
                {
                    Thread.sleep(100); // Anpassbar – hier 100ms Pause zwischen den Updates
                } catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                }
            }
        });
        chunkUpdateThread.setDaemon(true);
        chunkUpdateThread.start();
    }

    /**
     * Stoppt den Hintergrund-Thread.
     */
    public void stopChunkUpdateThread()
    {
        running = false;
        if (chunkUpdateThread != null)
        {
            try
            {
                chunkUpdateThread.join();
            } catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void dispose()
    {
        stopChunkUpdateThread();
        // Optional: Alle noch geladenen Chunks entladen
        for (Chunk chunk : loadedChunks)
        {
            chunk.dispose();
        }
        loadedChunks.clear();
        // Falls weitere Ressourcen freigegeben werden müssen, hier ergänzen.
    }
}
