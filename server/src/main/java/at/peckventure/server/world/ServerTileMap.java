package at.peckventure.server.world;

import at.peckventure.Globals;
import at.peckventure.entities.Player;
import at.peckventure.entities.mob.*;
import at.peckventure.server.GameServer;
import at.peckventure.server.entities.ServerPlayer;
import at.peckventure.multiplayer.NetworkPackets;
import at.peckventure.world.AbstractTileMap;
import at.peckventure.world.RegionFile;
import at.peckventure.world.RegionManager;
import at.peckventure.world.chunk.Chunk;
import at.peckventure.world.chunk.ChunkIO;
import at.peckventure.world.generator.WorldGenerator;
import at.peckventure.world.MobRegionFile;
import at.peckventure.world.MobRegionManager;
import com.badlogic.gdx.physics.box2d.World;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ServerTileMap extends AbstractTileMap {
    private final WorldGenerator worldGenerator;
    private final RegionManager regionManager;
    private final MobRegionManager mobRegionManager;

    public static final float MOB_UPDATE_RADIUS = 1000f; // z. B. 500 Pixel oder passe den Wert an
    private static final int MOB_LOAD_DISTANCE = 6;

    public static final int MOB_UNLOAD_DISTANCE = MOB_LOAD_DISTANCE + 2;

    private static final int LOAD_DISTANCE = 6;  // Größerer Radius zum Vorladen
    private static final int SEND_DISTANCE = 3;  // Kleinerer Radius zum Sendenprivate static final int MOB_SEND_DISTANCE = 4;

    // Globaler Chunk-Pool (wird z. B. zum Caching genutzt – stammt aus der Oberklasse)
    // protected Set<Chunk> loadedChunks;

    // Map zum per-Spieler-Tracking der aktuell geladenen Chunks
    private Map<ServerPlayer, Set<Chunk>> playerLoadedChunks = new HashMap<>();

    // Speichert Informationen über geladene Mobs nach Chunk-Position
    private final Map<String, Set<Integer>> mobsByChunk = new HashMap<>();
    private final Gson gson = new Gson();
    private static final Type LIST_TYPE = new TypeToken<List<MobIO.MobData>>(){}.getType();

    public ServerTileMap(World world, WorldGenerator generator, Set<Chunk> preLoadedChunks, RegionManager regionManager, MobRegionManager mobRegionManager) {
        super(world);
        this.worldGenerator = generator;
        if (preLoadedChunks != null) {
            loadedChunks.addAll(preLoadedChunks);
        }
        this.regionManager = regionManager;
        this.mobRegionManager = mobRegionManager;
    }

    @Override
    public void loadChunksAroundPlayer(Player player) {
        if (player instanceof ServerPlayer) {
            updateChunksForPlayer((ServerPlayer) player);
        }
    }

    @Override
    public void unloadChunksOutsideRenderDistance(Player player) {
        // Wird jetzt im per-Spieler-Update (updateChunksForPlayer) gehandhabt.
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
        for (Map.Entry<Integer, Mob> entry : Globals.mobs.entrySet()) {
            int mobId = entry.getKey();
            Mob mob = entry.getValue();
            String chunkKey = getChunkKey(mob.getChunkX(), mob.getChunkY());

            if (!mobsByChunk.containsKey(chunkKey)) {
                mobsByChunk.put(chunkKey, new HashSet<>());
            }
            mobsByChunk.get(chunkKey).add(mobId);
        }
    }

    public void updateMobsForPlayer(ServerPlayer player) {
        // Erstelle ein neues MobUpdatePacket
        NetworkPackets.MobUpdatePacket mobPacket = new NetworkPackets.MobUpdatePacket();
        mobPacket.mobUpdates = new ArrayList<>();

        // Hole die Position des Spielers
        float playerX = player.getX();
        float playerY = player.getY();

        // Iteriere über alle Mobs in der Globals-Mob-Map
        for (Map.Entry<Integer, Mob> entry : Globals.mobs.entrySet()) {
            Mob mob = entry.getValue();
            // Berechne den Abstand zum Spieler
            float dx = mob.getX() - playerX;
            float dy = mob.getY() - playerY;
            if (dx * dx + dy * dy <= MOB_UPDATE_RADIUS * MOB_UPDATE_RADIUS) {
                // Erstelle ein Update-Paket für diesen Mob
                NetworkPackets.SingleMobUpdatePacket update = new NetworkPackets.SingleMobUpdatePacket();
                update.umid = entry.getKey();
                update.mobid = MobRegistry.getMobId(mob);
                update.x = mob.getX();
                update.y = mob.getY();
                update.direction = mob.isDirection();
                if(mob instanceof ItemActor)
                {
                    ItemActor item = (ItemActor)mob;
                    update.extraItem = item.getInventoryItem().getId();
                }
                mobPacket.mobUpdates.add(update);
            }
        }

        // Sende das Mob-Update-Paket an den Spieler
        try {
            player.getConnection().sendTCP(mobPacket);
        } catch (Exception e) {
            System.err.println("Fehler beim Senden der Mob-Updates an " + player.getUsername());
            e.printStackTrace();
        }
    }

    public void loadMobsAroundPlayer(Player player) {
        // Aktualisiere die Map mit aktuellen Mobs pro Chunk
        updateMobsByChunk();

        for (int x_offset = -MOB_LOAD_DISTANCE; x_offset <= MOB_LOAD_DISTANCE; x_offset++) {
            for (int y_offset = -MOB_LOAD_DISTANCE; y_offset <= MOB_LOAD_DISTANCE; y_offset++) {
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
                        System.out.println("Lade Mob-Daten aus Region (" + localX + ", " + localY + ")");

                        // Prüfe, ob der JSON-String mehrere Mobs enthält
                        if (mobJson.startsWith("[") && mobJson.endsWith("]")) {
                            // Es handelt sich um eine Liste von Mobs
                            List<MobIO.MobData> mobDataList = gson.fromJson(mobJson, LIST_TYPE);

                            // Erstelle alle Mobs in der Liste
                            for (int i = 0; i < mobDataList.size(); i++) {
                                String singleMobJson = gson.toJson(mobDataList.get(i));
                                Mob mob = MobIO.deserializeFromJson(singleMobJson, physicsWorld);
                                int newId = MobMap.getNextId();
                                Globals.mobs.put(newId, mob);

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
                            Globals.mobs.put(newId, mob);

                            // Füge Mob zur Chunk-Liste hinzu
                            if (!mobsByChunk.containsKey(chunkKey)) {
                                mobsByChunk.put(chunkKey, new HashSet<>());
                            }
                            mobsByChunk.get(chunkKey).add(newId);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void unloadMobsOutsideRenderDistance() {
        // Sammle Mobs nach Chunk für Speicherung
        Map<String, List<Mob>> mobsToSave = new HashMap<>();

        Iterator<Map.Entry<Integer, Mob>> iterator = Globals.mobs.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Mob> entry = iterator.next();
            Mob mob = entry.getValue();
            int mobId = entry.getKey();
            boolean shouldUnload = true;

            // Prüfe alle Spieler, ob sie den Mob-Chunk laden
            for (ServerPlayer player : GameServer.instance.players) {
                if (Math.abs(mob.getChunkX() - player.getChunkX()) <= MOB_UNLOAD_DISTANCE &&
                    Math.abs(mob.getChunkY() - player.getChunkY()) <= MOB_UNLOAD_DISTANCE) {
                    shouldUnload = false;
                    break;
                }
            }

            if (shouldUnload) {
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

        // Speichere gesammelte Mobs nach Chunk
        for (Map.Entry<String, List<Mob>> entry : mobsToSave.entrySet()) {
            String[] parts = entry.getKey().split(":");
            int chunkX = Integer.parseInt(parts[0]);
            int chunkY = Integer.parseInt(parts[1]);
            List<Mob> chunkMobs = entry.getValue();

            if (!chunkMobs.isEmpty()) {
                saveMobsForChunk(chunkX, chunkY, chunkMobs);
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

    @Override
    public void updateChunks(Player player) {
        if (player instanceof ServerPlayer) {
            updateChunksForPlayer((ServerPlayer) player);
            // Aktualisiere Mobs für diesen Spieler
            updateMobsForPlayer((ServerPlayer) player);
        }
        loadMobsAroundPlayer(player);
    }

    @Override
    public void dispose() {
        stopChunkUpdateThread();

        // Sammle Mobs nach Chunk
        Map<String, List<Mob>> mobsToSave = new HashMap<>();
        for (Map.Entry<Integer, Mob> entry : Globals.mobs.entrySet()) {
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
        Globals.mobs.clear();

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
        regionManager.closeAll();
        mobRegionManager.closeAll();
    }

    /**
     * Aktualisiert die Chunks für einen einzelnen ServerPlayer.
     * Fehlende Chunks werden geladen und an den Spieler gesendet, nicht mehr benötigte werden aus seinem Set entfernt.
     */
    public void updateChunksForPlayer(ServerPlayer player) {
        Set<Chunk> loadedForPlayer = playerLoadedChunks.computeIfAbsent(player, k -> new HashSet<>());

        // 1. Lade alle Chunks im großen Radius (LOAD_DISTANCE)
        Set<Chunk> chunksToLoad = calculateChunkRadius(player, LOAD_DISTANCE);

        // 2. Bestimme welche Chunks gesendet werden sollen (kleinerer Radius)
        Set<Chunk> chunksToSend = calculateChunkRadius(player, SEND_DISTANCE);

        // Lade und halte Chunks im Speicher
        for (Chunk required : chunksToLoad) {
            if (!loadedChunks.contains(required)) {
                Chunk actualChunk = getOrLoadChunk(required.getChunkX(), required.getChunkY());
                loadedChunks.add(actualChunk);
            }
        }

        // Sende nur Chunks im kleineren Radius
        for (Chunk chunk : loadedChunks) {
            if (chunksToSend.contains(chunk) && !loadedForPlayer.contains(chunk)) {
                sendChunkToPlayer(player, chunk);
                loadedForPlayer.add(chunk);
            }
        }

        // Entferne nicht mehr benötigte Chunks aus dem Spieler-Set
        Iterator<Chunk> it = loadedForPlayer.iterator();
        while (it.hasNext()) {
            Chunk chunk = it.next();
            if (!chunksToSend.contains(chunk)) {
                it.remove();
            }
        }
    }

    /**
     * Versucht, einen Chunk aus dem globalen Pool zu holen oder lädt/generiert ihn, falls er noch nicht existiert.
     */
    private Chunk getOrLoadChunk(int chunkX, int chunkY) {
        Chunk dummy = new Chunk(chunkX, chunkY);
        // Prüfe, ob der Chunk bereits global geladen ist
        for (Chunk loaded : loadedChunks) {
            if (loaded.equals(dummy)) {
                return loaded;
            }
        }
        // Falls nicht vorhanden: Lade aus der Region-Datei oder generiere neu
        int regionX = Math.floorDiv(chunkX, RegionManager.REGION_SIZE);
        int regionY = Math.floorDiv(chunkY, RegionManager.REGION_SIZE);
        RegionFile regionFile = regionManager.getRegionFile(regionX, regionY);
        int localX = Math.floorMod(chunkX, RegionManager.REGION_SIZE);
        int localY = Math.floorMod(chunkY, RegionManager.REGION_SIZE);
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
            chunk = new Chunk(chunkX, chunkY);
            worldGenerator.generateChunk(chunk, true);
        }
        loadedChunks.add(chunk);
        return chunk;
    }

    /**
     * Sollte ein Spieler sich disconnecten, wird dessen Chunk-Set entfernt und
     * es wird geprüft, ob die global geladenen Chunks von keinem anderen Spieler mehr verwendet werden.
     */
    public void removePlayer(ServerPlayer player) {
        Set<Chunk> chunksOfPlayer = playerLoadedChunks.remove(player);
        if (chunksOfPlayer != null) {
            for (Chunk chunk : chunksOfPlayer) {
                boolean inUse = false;
                for (Set<Chunk> otherChunks : playerLoadedChunks.values()) {
                    if (otherChunks.contains(chunk)) {
                        inUse = true;
                        break;
                    }
                }
                if (!inUse) {
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
                    loadedChunks.remove(chunk);
                }
            }
        }
    }

    /**
     * Aktualisiert für alle ServerPlayer die geladenen Chunks, indem
     * updateChunksForPlayer für jeden Spieler aufgerufen wird.
     */
    public void updateChunksForAllPlayers() {
        for (ServerPlayer player : GameServer.instance.players) {
            updateChunksForPlayer(player);
            updateMobsForPlayer(player);
        }

        // Nach der Aktualisierung aller Spieler, entferne Mobs, die zu weit weg sind
        unloadMobsOutsideRenderDistance();
    }

    private void sendChunkToPlayer(ServerPlayer player, Chunk chunk) {
        NetworkPackets.ChunkDataPacket packet = new NetworkPackets.ChunkDataPacket();
        packet.data = ChunkIO.serialize(chunk);
        player.getConnection().sendTCP(packet);
    }

    private Set<Chunk> calculateChunkRadius(ServerPlayer player, int distance) {
        Set<Chunk> chunks = new HashSet<>();
        for (int x = -distance; x <= distance; x++) {
            for (int y = -distance; y <= distance; y++) {
                chunks.add(new Chunk(
                    player.getChunkX() + x,
                    player.getChunkY() + y
                ));
            }
        }
        return chunks;
    }
}
