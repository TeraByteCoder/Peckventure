package at.peckventure.world;

import at.peckventure.entities.Player;
import at.peckventure.world.chunk.Chunk;
import at.peckventure.world.chunk.ChunkIO;
import at.peckventure.world.generator.WorldGenerator;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.physics.box2d.World;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class InfiniteTilemap {
    public static final int RENDER_DISTANCE = 2;
    private WorldGenerator worldGenerator;
    private Set<Chunk> loadedChunks = new HashSet<>();
    private RegionManager regionManager;  // Neuer Verweis auf den RegionManager

    /**
     * Konstruktor: Zusätzlich wird hier der RegionManager übergeben,
     * der für das Speichern der Chunks in Region-Dateien zuständig ist.
     */
    public InfiniteTilemap(World world, WorldGenerator generator, Set<Chunk> preLoadedChunks, RegionManager regionManager) {
        this.worldGenerator = generator;
        if (preLoadedChunks != null) {
            loadedChunks.addAll(preLoadedChunks);
        }
        this.regionManager = regionManager;
    }

    public void render(Batch batch, Player player) {
        loadChunksAroundPlayer(player);
        unloadChunksOutsideRenderDistance(player);
        for (Chunk chunk : loadedChunks) {
            chunk.render(batch);
        }
    }

    public WorldGenerator getWorldGenerator() {
        return worldGenerator;
    }

    public Set<Chunk> getLoadedChunks() {
        return loadedChunks;
    }

    /**
     * Lädt Chunks in der Nähe des Spielers, falls sie noch nicht im Speicher sind.
     */
    public void loadChunksAroundPlayer(Player player) {
        for (int x_offset = -RENDER_DISTANCE-1; x_offset <= RENDER_DISTANCE; x_offset++) {
            for (int y_offset = -RENDER_DISTANCE; y_offset <= RENDER_DISTANCE; y_offset++) {
                int targetChunkX = player.getChunkX() + x_offset;
                int targetChunkY = player.getChunkY() + y_offset;
                Chunk dummy = new Chunk(targetChunkX, targetChunkY);
                if (!loadedChunks.contains(dummy)) {
                    Chunk chunk = new Chunk(targetChunkX, targetChunkY);
                    worldGenerator.generateChunk(chunk);
                    loadedChunks.add(chunk);
                }
            }
        }
    }

    /**
     * Entfernt (unloadet) Chunks, die weit genug vom Spieler entfernt sind.
     * Bevor ein Chunk entfernt wird, wird er sofort in die Region-Datei gespeichert.
     */
    public void unloadChunksOutsideRenderDistance(Player player) {
        Iterator<Chunk> iterator = loadedChunks.iterator();
        while (iterator.hasNext()) {
            Chunk chunk = iterator.next();
            if (Math.abs(chunk.getChunkX() - player.getChunkX()) > RENDER_DISTANCE + 2 ||
                Math.abs(chunk.getChunkY() - player.getChunkY()) > RENDER_DISTANCE + 2) {

                // Bestimme die Region-Koordinaten und lokale Koordinaten
                int regionX = Math.floorDiv(chunk.getChunkX(), RegionManager.REGION_SIZE);
                int regionY = Math.floorDiv(chunk.getChunkY(), RegionManager.REGION_SIZE);
                RegionFile regionFile = regionManager.getRegionFile(regionX, regionY);

                int localX = Math.floorMod(chunk.getChunkX(), RegionManager.REGION_SIZE);
                int localY = Math.floorMod(chunk.getChunkY(), RegionManager.REGION_SIZE);

                // Serialisiere den Chunk und speichere ihn in der entsprechenden Region-Datei
                byte[] data = ChunkIO.serialize(chunk);
                try {
                    regionFile.writeChunk(localX, localY, data);
                    // Optional: Log-Ausgabe zur Kontrolle
                    // Gdx.app.log("InfiniteTilemap", "Saved chunk (" + chunk.getChunkX() + "," + chunk.getChunkY() +
                    //           ") to region (" + regionX + "," + regionY + ") at local (" + localX + "," + localY + ")");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                // Entferne den Chunk aus dem aktiven Set
                iterator.remove();
            }
        }
    }

    public void dispose()
    {

    }
}
