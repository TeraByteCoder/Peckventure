package at.peckventure.world;

import at.peckventure.world.chunk.Chunk;
import at.peckventure.world.chunk.ChunkIO;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.physics.box2d.World;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class WorldIO {

    /**
     * Speichert die Welt:
     * - Schreibt die Weltkonfiguration (z. B. den Seed) in worldconfig.txt
     * - Speichert alle aktuell geladenen Chunks in den entsprechenden Region-Dateien
     */
    public static void saveWorld(String worldName, WorldConfig config, Set<Chunk> loadedChunks) {
        FileHandle worldDir = Gdx.files.absolute(at.peckventure.Const.savesDir + "/" + worldName);
        if (!worldDir.exists()) {
            worldDir.mkdirs();
        }
        // Speichere die Konfiguration
        FileHandle configFile = worldDir.child("worldconfig.txt");
        config.save(configFile);

        // Speichere die Chunks mithilfe des RegionManagers
        RegionManager regionManager = new RegionManager(worldDir);
        for (Chunk chunk : loadedChunks) {
            // Bestimme, in welcher Region dieser Chunk liegt
            int regionX = Math.floorDiv(chunk.getChunkX(), RegionManager.REGION_SIZE);
            int regionY = Math.floorDiv(chunk.getChunkY(), RegionManager.REGION_SIZE);
            RegionFile regionFile = regionManager.getRegionFile(regionX, regionY);

            // Bestimme die lokalen Koordinaten innerhalb der Region
            int localX = Math.floorMod(chunk.getChunkX(), RegionManager.REGION_SIZE);
            int localY = Math.floorMod(chunk.getChunkY(), RegionManager.REGION_SIZE);

            // Serialisiere den Chunk
            byte[] data = ChunkIO.serialize(chunk);
            try {
                regionFile.writeChunk(localX, localY, data);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        regionManager.closeAll();
    }

    /**
     * Lädt eine Welt:
     * - Liest die Weltkonfiguration aus der worldconfig.txt
     * - Lädt alle in den Region-Dateien gespeicherten Chunks
     * (Hier wird zur Vereinfachung jeder gespeicherte Chunk geladen.)
     */
    public static LoadedWorld loadWorld(String worldName, World physicsWorld) {
        FileHandle worldDir = Gdx.files.absolute(at.peckventure.Const.savesDir + "/" + worldName);
        if (!worldDir.exists()) {
            throw new RuntimeException("World does not exist: " + worldName);
        }
        // Lese die Konfiguration
        FileHandle configFile = worldDir.child("worldconfig.txt");
        WorldConfig config = WorldConfig.load(configFile);

        // Lade alle Chunks aus den Region-Dateien
        Set<Chunk> loadedChunks = new HashSet<>();
        FileHandle regionsDir = worldDir.child("regions");
        if (regionsDir.exists()) {
            for (FileHandle regionFileHandle : regionsDir.list()) {
                try {
                    RegionFile regionFile = new RegionFile(regionFileHandle.file());
                    // Der Dateiname hat das Format "r.regionX.regionY.pvr"
                    String[] tokens = regionFileHandle.nameWithoutExtension().split("\\.");
                    if (tokens.length >= 3) {
                        int regionX = Integer.parseInt(tokens[1]);
                        int regionY = Integer.parseInt(tokens[2]);
                        // Versuche für jede mögliche lokale Position den Chunk zu laden
                        for (int localX = 0; localX < RegionFile.CHUNKS_PER_REGION; localX++) {
                            for (int localY = 0; localY < RegionFile.CHUNKS_PER_REGION; localY++) {
                                byte[] data = regionFile.readChunk(localX, localY);
                                if (data != null) {
                                    Chunk chunk = ChunkIO.deserialize(data, physicsWorld);
                                    loadedChunks.add(chunk);
                                }
                            }
                        }
                    }
                    regionFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return new LoadedWorld(config, loadedChunks);
    }

    public static class LoadedWorld {
        private WorldConfig config;
        private Set<Chunk> loadedChunks;

        public LoadedWorld(WorldConfig config, Set<Chunk> loadedChunks) {
            this.config = config;
            this.loadedChunks = loadedChunks;
        }

        public WorldConfig getConfig() {
            return config;
        }

        public Set<Chunk> getLoadedChunks() {
            return loadedChunks;
        }
    }
}
