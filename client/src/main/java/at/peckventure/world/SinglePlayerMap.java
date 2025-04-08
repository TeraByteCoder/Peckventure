package at.peckventure.world;

import at.peckventure.Globals;
import at.peckventure.entities.Player;
import at.peckventure.entities.mob.Mob;
import at.peckventure.entities.mob.MobIO;
import at.peckventure.entities.mob.MobMap;
import at.peckventure.world.chunk.Chunk;
import at.peckventure.world.chunk.ChunkIO;
import at.peckventure.world.generator.WorldGenerator;
import com.badlogic.gdx.physics.box2d.World;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static at.peckventure.Globals.mobs;

public class SinglePlayerMap extends AbstractTileMap {
    private final WorldGenerator worldGenerator;
    private final RegionManager regionManager;
    private final MobRegionManager mobRegionManager;

    // Reduziere MOB_DISTANCE im Vergleich zu RENDER_DISTANCE
    public static final int MOB_DISTANCE = Math.max(RENDER_DISTANCE - 2, 1);

    // Speichert Informationen über geladene Mobs nach Chunk-Position
    private final Map<String, Set<Integer>> mobsByChunk = new HashMap<>();
    private final Gson gson = new Gson();
    private static final Type LIST_TYPE = new TypeToken<List<MobIO.MobData>>(){}.getType();

    public SinglePlayerMap(World world, WorldGenerator generator, Set<Chunk> preLoadedChunks, RegionManager regionManager, MobRegionManager mobRegionManager) {
        super(world);
        worldGenerator = generator;
        if (preLoadedChunks != null) {
            loadedChunks.addAll(preLoadedChunks);
        }
        this.regionManager = regionManager;
        this.mobRegionManager = mobRegionManager;
    }

    @Override
    public void loadChunksAroundPlayer(Player player) {
        for (int x_offset = -RENDER_DISTANCE - 1; x_offset <= RENDER_DISTANCE; x_offset++) {
            for (int y_offset = -RENDER_DISTANCE; y_offset <= RENDER_DISTANCE; y_offset++) {
                int targetChunkX = player.getChunkX() + x_offset;
                int targetChunkY = player.getChunkY() + y_offset;
                Chunk dummy = new Chunk(targetChunkX, targetChunkY);
                if (!loadedChunks.contains(dummy)) {
                    int regionX = Math.floorDiv(targetChunkX, RegionManager.REGION_SIZE);
                    int regionY = Math.floorDiv(targetChunkY, RegionManager.REGION_SIZE);
                    RegionFile regionFile = regionManager.getRegionFile(regionX, regionY);
                    int localX = Math.floorMod(targetChunkX, RegionManager.REGION_SIZE);
                    int localY = Math.floorMod(targetChunkY, RegionManager.REGION_SIZE);
                    byte[] data = null;
                    try {
                        data = regionFile.readChunk(localX, localY);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Chunk chunk;
                    if (data != null) {
                        chunk = ChunkIO.deserialize(data, physicsWorld);
                    } else {
                        chunk = new Chunk(targetChunkX, targetChunkY);
                        worldGenerator.generateChunk(chunk, true);
                    }
                    loadedChunks.add(chunk);
                }
            }
        }
    }

    @Override
    public void unloadChunksOutsideRenderDistance(Player player) {
        Iterator<Chunk> iterator = loadedChunks.iterator();
        while (iterator.hasNext()) {
            Chunk chunk = iterator.next();
            if (Math.abs(chunk.getChunkX() - player.getChunkX()) > RENDER_DISTANCE + 2 ||
                Math.abs(chunk.getChunkY() - player.getChunkY()) > RENDER_DISTANCE + 2) {
                int regionX = Math.floorDiv(chunk.getChunkX(), RegionManager.REGION_SIZE);
                int regionY = Math.floorDiv(chunk.getChunkY(), RegionManager.REGION_SIZE);
                RegionFile regionFile = regionManager.getRegionFile(regionX, regionY);
                int localX = Math.floorMod(chunk.getChunkX(), RegionManager.REGION_SIZE);
                int localY = Math.floorMod(chunk.getChunkY(), RegionManager.REGION_SIZE);
                byte[] data = ChunkIO.serialize(chunk);
                try {
                    regionFile.writeChunk(localX, localY, data);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                chunk.dispose();
                iterator.remove();
            }
        }
    }

    @Override
    public Chunk loadChunk(int targetChunkX, int targetChunkY, boolean trees) {
        Chunk dummy = new Chunk(targetChunkX, targetChunkY);
        if (!loadedChunks.contains(dummy)) {
            int regionX = Math.floorDiv(targetChunkX, RegionManager.REGION_SIZE);
            int regionY = Math.floorDiv(targetChunkY, RegionManager.REGION_SIZE);
            RegionFile regionFile = regionManager.getRegionFile(regionX, regionY);
            int localX = Math.floorMod(targetChunkX, RegionManager.REGION_SIZE);
            int localY = Math.floorMod(targetChunkY, RegionManager.REGION_SIZE);
            byte[] data = null;
            try {
                data = regionFile.readChunk(localX, localY);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Chunk chunk;
            if (data != null) {
                chunk = ChunkIO.deserialize(data, physicsWorld);
            } else {
                chunk = new Chunk(targetChunkX, targetChunkY);
                worldGenerator.generateChunk(chunk, trees);
            }
            loadedChunks.add(chunk);
            return chunk;
        }

        // Wenn der Chunk bereits geladen ist, gib ihn zurück
        for (Chunk chunk : loadedChunks) {
            if (chunk.getChunkX() == targetChunkX && chunk.getChunkY() == targetChunkY) {
                return chunk;
            }
        }

        return null;
    }

    /**
     * Überprüft, ob ein Chunk bereits vollständig geladen ist
     */
    private boolean isChunkLoaded(int chunkX, int chunkY) {
        for (Chunk chunk : loadedChunks) {
            if (chunk.getChunkX() == chunkX && chunk.getChunkY() == chunkY) {
                return true;
            }
        }
        return false;
    }

    /**
     * Erzeugt einen eindeutigen Schlüssel für die Chunk-Position
     */
    private String getChunkKey(int chunkX, int chunkY) {
        return chunkX + ":" + chunkY;
    }

    /**
     * Aktualisiert die interne Map mit Mobs pro Chunk
     */
    private void updateMobsByChunk() {
        mobsByChunk.clear();
        for (Map.Entry<Integer, Mob> entry : mobs.entrySet()) {
            int mobId = entry.getKey();
            Mob mob = entry.getValue();
            String chunkKey = getChunkKey(mob.getChunkX(), mob.getChunkY());

            if (!mobsByChunk.containsKey(chunkKey)) {
                mobsByChunk.put(chunkKey, new HashSet<>());
            }
            mobsByChunk.get(chunkKey).add(mobId);
        }
    }

    public void loadMobsAroundPlayer(Player player) {
        // Aktualisiere die Map mit aktuellen Mobs pro Chunk
        updateMobsByChunk();

        for (int x_offset = -MOB_DISTANCE; x_offset <= MOB_DISTANCE; x_offset++) {
            for (int y_offset = -MOB_DISTANCE; y_offset <= MOB_DISTANCE; y_offset++) {
                int targetChunkX = player.getChunkX() + x_offset;
                int targetChunkY = player.getChunkY() + y_offset;

                // Überprüfe, ob der Chunk geladen ist, bevor Mobs geladen werden
                if (!isChunkLoaded(targetChunkX, targetChunkY)) {
                    continue;
                }

                // Überprüfe, ob dieser Chunk bereits Mobs hat
                String chunkKey = getChunkKey(targetChunkX, targetChunkY);
                if (mobsByChunk.containsKey(chunkKey) && !mobsByChunk.get(chunkKey).isEmpty()) {
                    continue; // Dieser Chunk hat bereits Mobs geladen
                }

                // Lade Mobs aus der Regionsdatei
                int regionX = Math.floorDiv(targetChunkX, MobRegionManager.REGION_SIZE);
                int regionY = Math.floorDiv(targetChunkY, MobRegionManager.REGION_SIZE);
                MobRegionFile mobRegionFile = mobRegionManager.getMobRegionFile(regionX, regionY);
                int localX = Math.floorMod(targetChunkX, MobRegionManager.REGION_SIZE);
                int localY = Math.floorMod(targetChunkY, MobRegionManager.REGION_SIZE);

                try {
                    byte[] mobData = mobRegionFile.readMobs(localX, localY);
                    if (mobData != null) {
                        String mobJson = new String(mobData, StandardCharsets.UTF_8);

                        // Jetzt prüfen, ob der JSON-String mehrere Mobs enthält
                        if (mobJson.startsWith("[") && mobJson.endsWith("]")) {
                            // Es handelt sich um eine Liste von Mobs
                            List<MobIO.MobData> mobDataList = gson.fromJson(mobJson, LIST_TYPE);

                            // Erstelle alle Mobs in der Liste
                            for (int i = 0; i < mobDataList.size(); i++) {
                                String singleMobJson = gson.toJson(mobDataList.get(i));
                                Mob mob = MobIO.deserializeFromJson(singleMobJson, physicsWorld);
                                int newId = MobMap.getNextId();
                                mobs.put(newId, mob);

                                // Füge Mob zur Chunk-Liste hinzu
                                if (!mobsByChunk.containsKey(chunkKey)) {
                                    mobsByChunk.put(chunkKey, new HashSet<>());
                                }
                                mobsByChunk.get(chunkKey).add(newId);
                            }
                        } else {
                            // Es handelt sich um einen einzelnen Mob
                            Mob mob = MobIO.deserializeFromJson(mobJson, physicsWorld);
                            int newId = MobMap.getNextId();
                            mobs.put(newId, mob);

                            // Füge Mob zur Chunk-Liste hinzu
                            if (!mobsByChunk.containsKey(chunkKey)) {
                                mobsByChunk.put(chunkKey, new HashSet<>());
                            }
                            mobsByChunk.get(chunkKey).add(newId);
                        }

                        // Lösche den Mob aus der Datei, nachdem er geladen wurde
                        mobRegionFile.clearMobs(localX, localY);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void unloadMobsOutsideRenderDistance(Player player) {
        // Sammle Mobs nach Chunk
        Map<String, List<Mob>> mobsToSave = new HashMap<>();

        // Da mobs jetzt eine Map ist, iterieren wir über die Entry-Set
        Iterator<Map.Entry<Integer, Mob>> iterator = mobs.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Mob> entry = iterator.next();
            Mob mob = entry.getValue();
            int mobId = entry.getKey();

            // Prüfe, ob der Mob außerhalb der Mob-Distanz ist
            if (Math.abs(mob.getChunkX() - player.getChunkX()) > MOB_DISTANCE + 1 ||
                Math.abs(mob.getChunkY() - player.getChunkY()) > MOB_DISTANCE + 1) {

                String chunkKey = getChunkKey(mob.getChunkX(), mob.getChunkY());

                // Sammle Mobs nach Chunk für die Speicherung
                if (!mobsToSave.containsKey(chunkKey)) {
                    mobsToSave.put(chunkKey, new ArrayList<>());
                }
                mobsToSave.get(chunkKey).add(mob);

                // Entferne aus der mobsByChunk-Map
                if (mobsByChunk.containsKey(chunkKey)) {
                    mobsByChunk.get(chunkKey).remove(mobId);
                    if (mobsByChunk.get(chunkKey).isEmpty()) {
                        mobsByChunk.remove(chunkKey);
                    }
                }

                // Entferne den Mob aus dem Spiel
                mob.dispose();
                mob.remove();
                iterator.remove();
            }
        }

        // Speichere gesammelte Mobs nach Chunks
        for (Map.Entry<String, List<Mob>> entry : mobsToSave.entrySet()) {
            String[] parts = entry.getKey().split(":");
            int chunkX = Integer.parseInt(parts[0]);
            int chunkY = Integer.parseInt(parts[1]);
            List<Mob> chunkMobs = entry.getValue();

            if (chunkMobs.isEmpty()) continue;

            // Prüfe, ob der Chunk noch geladen ist
            if (isChunkLoaded(chunkX, chunkY)) {
                saveMobsForChunk(chunkX, chunkY, chunkMobs);
            } else {
                // Wenn der Chunk nicht mehr geladen ist, suche den nächsten Chunk
                Chunk nearestChunk = findNearestLoadedChunk(chunkX, chunkY);
                if (nearestChunk != null) {
                    saveMobsForChunk(nearestChunk.getChunkX(), nearestChunk.getChunkY(), chunkMobs);
                }
            }
        }
    }

    /**
     * Speichert eine Liste von Mobs für einen bestimmten Chunk
     */
    private void saveMobsForChunk(int chunkX, int chunkY, List<Mob> mobsToSave) {
        int regionX = Math.floorDiv(chunkX, MobRegionManager.REGION_SIZE);
        int regionY = Math.floorDiv(chunkY, MobRegionManager.REGION_SIZE);
        MobRegionFile mobRegionFile = mobRegionManager.getMobRegionFile(regionX, regionY);
        int localX = Math.floorMod(chunkX, MobRegionManager.REGION_SIZE);
        int localY = Math.floorMod(chunkY, MobRegionManager.REGION_SIZE);

        try {
            // Lese vorhandene Mobs, falls vorhanden
            byte[] existingMobData = mobRegionFile.readMobs(localX, localY);
            List<MobIO.MobData> allMobData = new ArrayList<>();

            if (existingMobData != null) {
                String existingJson = new String(existingMobData, StandardCharsets.UTF_8);
                if (existingJson.startsWith("[") && existingJson.endsWith("]")) {
                    allMobData = gson.fromJson(existingJson, LIST_TYPE);
                } else {
                    // Ein einzelner Mob
                    MobIO.MobData singleMob = gson.fromJson(existingJson, MobIO.MobData.class);
                    if (singleMob != null) {
                        allMobData.add(singleMob);
                    }
                }
            }

            // Serialisiere alle neuen Mobs und füge sie zur Liste hinzu
            for (Mob mob : mobsToSave) {
                String mobJson = MobIO.serializeToJson(mob);
                MobIO.MobData mobData = gson.fromJson(mobJson, MobIO.MobData.class);
                allMobData.add(mobData);
            }

            // Speichere alle Mobs zusammen
            String finalJson = gson.toJson(allMobData);
            byte[] combinedMobData = finalJson.getBytes(StandardCharsets.UTF_8);
            mobRegionFile.writeMobs(localX, localY, combinedMobData);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Findet den nächsten geladenen Chunk zu den gegebenen Koordinaten
     */
    private Chunk findNearestLoadedChunk(int chunkX, int chunkY) {
        Chunk nearestChunk = null;
        double minDistance = Double.MAX_VALUE;

        for (Chunk chunk : loadedChunks) {
            double distance = Math.sqrt(
                Math.pow(chunk.getChunkX() - chunkX, 2) +
                    Math.pow(chunk.getChunkY() - chunkY, 2)
            );

            if (distance < minDistance) {
                minDistance = distance;
                nearestChunk = chunk;
            }
        }

        return nearestChunk;
    }

    @Override
    public void updateChunks(Player player) {
        // Zuerst Chunks laden und entladen
        loadChunksAroundPlayer(player);
        unloadChunksOutsideRenderDistance(player);

        // Dann Mobs laden und entladen (nachdem Chunks aktualisiert wurden)
        loadMobsAroundPlayer(player);
        unloadMobsOutsideRenderDistance(player);
    }

    @Override
    public void dispose() {
        stopChunkUpdateThread();

        // Sammle Mobs nach Chunk
        Map<String, List<Mob>> mobsToSave = new HashMap<>();
        for (Map.Entry<Integer, Mob> entry : mobs.entrySet()) {
            Mob mob = entry.getValue();
            String chunkKey = getChunkKey(mob.getChunkX(), mob.getChunkY());

            if (!mobsToSave.containsKey(chunkKey)) {
                mobsToSave.put(chunkKey, new ArrayList<>());
            }
            mobsToSave.get(chunkKey).add(mob);

            // Entferne den Mob aus dem Spiel
            mob.dispose();
            mob.remove();
        }
        mobs.clear();

        // Speichere alle Mobs
        for (Map.Entry<String, List<Mob>> entry : mobsToSave.entrySet()) {
            String[] parts = entry.getKey().split(":");
            int chunkX = Integer.parseInt(parts[0]);
            int chunkY = Integer.parseInt(parts[1]);
            List<Mob> chunkMobs = entry.getValue();

            if (!chunkMobs.isEmpty()) {
                saveMobsForChunk(chunkX, chunkY, chunkMobs);
            }
        }

        // Speichere und entlade alle Chunks
        for (Chunk chunk : loadedChunks) {
            int regionX = Math.floorDiv(chunk.getChunkX(), RegionManager.REGION_SIZE);
            int regionY = Math.floorDiv(chunk.getChunkY(), RegionManager.REGION_SIZE);
            RegionFile regionFile = regionManager.getRegionFile(regionX, regionY);
            int localX = Math.floorMod(chunk.getChunkX(), RegionManager.REGION_SIZE);
            int localY = Math.floorMod(chunk.getChunkY(), RegionManager.REGION_SIZE);
            byte[] data = ChunkIO.serialize(chunk);
            try {
                regionFile.writeChunk(localX, localY, data);
            } catch (IOException e) {
                e.printStackTrace();
            }
            chunk.dispose();
        }
        loadedChunks.clear();

        // Schließe alle RegionFiles
        regionManager.closeAll();
        mobRegionManager.closeAll();
    }
}
