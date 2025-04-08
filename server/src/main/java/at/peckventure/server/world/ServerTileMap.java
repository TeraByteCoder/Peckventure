package at.peckventure.server.world;

import at.peckventure.Globals;
import at.peckventure.entities.Player;
import at.peckventure.entities.mob.*;
import at.peckventure.multiplayer.NetworkPackets;
import at.peckventure.server.GameServer;
import at.peckventure.server.entities.ServerPlayer;
import at.peckventure.world.*;
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

public class ServerTileMap extends AbstractTileMap
{
    private final WorldGenerator worldGenerator;
    private final RegionManager regionManager;
    private final MobRegionManager mobRegionManager;

    private final Set<String> visitedChunks = new HashSet<>();


    public static final float MOB_UPDATE_RADIUS = 1000f; // z. B. 500 Pixel oder passe den Wert an
    public static final int MOB_UNLOAD_DISTANCE = MOB_DISTANCE + 2;

    private static final int LOAD_DISTANCE = 6;  // Größerer Radius zum Vorladen
    private static final int SEND_DISTANCE = 3;  // Kleinerer Radius zum Senden
    private static final int MOB_LOAD_DISTANCE = 4;

    // Für die Unterstützung von mehreren Mobs pro Chunk
    private final Gson gson = new Gson();
    private static final Type MOB_LIST_TYPE = new TypeToken<List<MobIO.MobData>>()
    {
    }.getType();

    // Map zum Speichern von Informationen über geladene Mobs nach Chunk-Position
    private final Map<String, Set<Integer>> mobsByChunk = new HashMap<>();

    // Map zum per-Spieler-Tracking der aktuell geladenen Chunks
    private Map<ServerPlayer, Set<Chunk>> playerLoadedChunks = new HashMap<>();

    public ServerTileMap(World world, WorldGenerator generator, Set<Chunk> preLoadedChunks, RegionManager regionManager, MobRegionManager mobRegionManager)
    {
        super(world);
        this.worldGenerator = generator;
        if (preLoadedChunks != null)
        {
            loadedChunks.addAll(preLoadedChunks);
        }
        this.regionManager = regionManager;
        this.mobRegionManager = mobRegionManager;
    }

    @Override
    public void loadChunksAroundPlayer(Player player)
    {
        if (player instanceof ServerPlayer)
        {
            updateChunksForPlayer((ServerPlayer) player);
        }
    }

    @Override
    public void unloadChunksOutsideRenderDistance(Player player)
    {
        // Wird jetzt im per-Spieler-Update (updateChunksForPlayer) gehandhabt.
    }

    @Override
    public Chunk loadChunk(int targetChunkX, int targetChunkY, boolean trees)
    {
        Chunk dummy = new Chunk(targetChunkX, targetChunkY);
        if (!loadedChunks.contains(dummy))
        {
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
                chunk = ChunkIO.deserialize(data, physicsWorld);
            } else
            {
                chunk = new Chunk(targetChunkX, targetChunkY);
                worldGenerator.generateChunk(chunk, trees);
            }
            loadedChunks.add(chunk);
            return chunk;
        }

        // Wenn der Chunk bereits geladen ist, gib ihn zurück
        for (Chunk chunk : loadedChunks)
        {
            if (chunk.getChunkX() == targetChunkX && chunk.getChunkY() == targetChunkY)
            {
                return chunk;
            }
        }

        return null;
    }

    /**
     * Erzeugt einen eindeutigen Schlüssel für die Chunk-Position
     */
    private String getChunkKey(int chunkX, int chunkY)
    {
        return chunkX + ":" + chunkY;
    }

    /**
     * Aktualisiert die interne Map mit Mobs pro Chunk
     */
    private void updateMobsByChunk() {
        // Zuerst leeren wir die Map
        mobsByChunk.clear();

        // Dann füllen wir sie neu mit allen aktuellen Mobs
        int totalMobs = 0;
        for (Map.Entry<Integer, Mob> entry : Globals.mobs.entrySet()) {
            int mobId = entry.getKey();
            Mob mob = entry.getValue();

            // Stelle sicher, dass der Mob gültige Chunk-Koordinaten hat
            int chunkX = mob.getChunkX();
            int chunkY = mob.getChunkY();

            String chunkKey = getChunkKey(chunkX, chunkY);
            if (!mobsByChunk.containsKey(chunkKey)) {
                mobsByChunk.put(chunkKey, new HashSet<>());
            }
            mobsByChunk.get(chunkKey).add(mobId);
            totalMobs++;
        }

        // Log-Ausgabe für Debugging
        System.out.println("updateMobsByChunk: Insgesamt " + totalMobs + " Mobs in " +
            mobsByChunk.size() + " verschiedenen Chunks");
    }

    public void updateMobsForPlayer(ServerPlayer player)
    {
        // Erstelle ein neues MobUpdatePacket
        NetworkPackets.MobUpdatePacket mobPacket = new NetworkPackets.MobUpdatePacket();
        mobPacket.mobUpdates = new ArrayList<>();

        // Hole die Position des Spielers
        float playerX = player.getX();
        float playerY = player.getY();

        // Iteriere über alle Mobs in der Globals-Mob-Map
        for (Map.Entry<Integer, Mob> entry : at.peckventure.Globals.mobs.entrySet())
        {
            Mob mob = entry.getValue();
            // Berechne den Abstand zum Spieler
            float dx = mob.getX() - playerX;
            float dy = mob.getY() - playerY;
            if (dx * dx + dy * dy <= MOB_UPDATE_RADIUS * MOB_UPDATE_RADIUS)
            {
                // Erstelle ein Update-Paket für diesen Mob
                NetworkPackets.SingleMobUpdatePacket update = new NetworkPackets.SingleMobUpdatePacket();
                update.umid = entry.getKey();  // Stelle sicher, dass diese Methode in der Mob-Klasse vorhanden ist.
                update.mobid = MobRegistry.getMobId(mob);     // Ebenso: Implementiere diese Methode, falls noch nicht vorhanden.
                update.x = mob.getX();
                update.y = mob.getY();
                update.direction = mob.isDirection();
                if (mob instanceof ItemActor)
                {
                    ItemActor item = (ItemActor) mob;
                    update.extraItem = item.getInventoryItem().getId();
                }
                mobPacket.mobUpdates.add(update);
            }
        }
        GameServer.instance.getServer().sendToUDP(player.getConnection().getID(), mobPacket);

        // Sende das Mob-Update-Paket an den Spieler
        try
        {
            player.getConnection().sendTCP(mobPacket);
        } catch (Exception e)
        {
            System.err.println("Fehler beim Senden der Mob-Updates an " + player.getUsername());
            e.printStackTrace();
        }
    }

    /**
     * Überprüft, ob ein Chunk bereits vollständig geladen ist
     */
    private boolean isChunkLoaded(int chunkX, int chunkY)
    {
        for (Chunk chunk : loadedChunks)
        {
            if (chunk.getChunkX() == chunkX && chunk.getChunkY() == chunkY)
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Prüft, ob an dieser Chunk-Position bereits ein Mob existiert
     */
    private boolean doesMobExistInChunk(int chunkX, int chunkY) {
        String chunkKey = getChunkKey(chunkX, chunkY);
        boolean result = mobsByChunk.containsKey(chunkKey) && !mobsByChunk.get(chunkKey).isEmpty();

        // Log-Ausgabe für Debugging
        if (result) {
            System.out.println("Chunk " + chunkKey + " enthält bereits " +
                mobsByChunk.get(chunkKey).size() + " Mobs");
        }
        return result;
    }

    public void loadMobsAroundPlayer(Player player) {
        // Aktualisiere die Map mit aktuellen Mobs pro Chunk
        updateMobsByChunk();

        // Markiere Chunks um den Spieler als besucht
        for (int x_offset = -MOB_DISTANCE; x_offset <= MOB_DISTANCE; x_offset++) {
            for (int y_offset = -MOB_DISTANCE; y_offset <= MOB_DISTANCE; y_offset++) {
                int targetChunkX = player.getChunkX() + x_offset;
                int targetChunkY = player.getChunkY() + y_offset;
                markChunkAsVisited(targetChunkX, targetChunkY);
            }
        }

        for (int x_offset = -MOB_DISTANCE - 1; x_offset <= MOB_DISTANCE; x_offset++) {
            for (int y_offset = -MOB_DISTANCE; y_offset <= MOB_DISTANCE; y_offset++) {
                int targetChunkX = player.getChunkX() + x_offset;
                int targetChunkY = player.getChunkY() + y_offset;

                // Überprüfe, ob der Chunk geladen ist, bevor Mobs geladen werden
                if (!isChunkLoaded(targetChunkX, targetChunkY)) {
                    continue;
                }

                // Überprüfe, ob an dieser Position bereits ein Mob geladen ist
                if (doesMobExistInChunk(targetChunkX, targetChunkY)) {
                    System.out.println("Chunk (" + targetChunkX + "," + targetChunkY + ") hat bereits Mobs - überspringe");
                    continue; // Überspringe bereits geladene Positionen
                }

                // Hole die Region-Koordinaten
                int regionX = Math.floorDiv(targetChunkX, MobRegionManager.REGION_SIZE);
                int regionY = Math.floorDiv(targetChunkY, MobRegionManager.REGION_SIZE);
                MobRegionFile mobRegionFile = mobRegionManager.getMobRegionFile(regionX, regionY);
                int localX = Math.floorMod(targetChunkX, MobRegionManager.REGION_SIZE);
                int localY = Math.floorMod(targetChunkY, MobRegionManager.REGION_SIZE);

                try {
                    byte[] mobData = mobRegionFile.readMobs(localX, localY);
                    if (mobData != null) {
                        String mobJson = new String(mobData, StandardCharsets.UTF_8);
                        System.out.println("Lade Mob-Daten aus Region (" + regionX + "," + regionY +
                            ") Chunk (" + localX + "," + localY + ") - JSON: " +
                            (mobJson.length() > 100 ? mobJson.substring(0, 100) + "..." : mobJson));

                        int mobsLoaded = 0;

                        // Prüfe, ob es sich um ein JSON-Array handelt
                        if (mobJson.startsWith("[") && mobJson.endsWith("]")) {
                            // Es handelt sich um eine Liste von Mobs
                            List<MobIO.MobData> mobDataList = gson.fromJson(mobJson, MOB_LIST_TYPE);
                            System.out.println("Gefundene Mobs im Array: " + mobDataList.size());

                            // Erstelle alle Mobs in der Liste
                            for (int i = 0; i < mobDataList.size(); i++) {
                                String singleMobJson = gson.toJson(mobDataList.get(i));
                                Mob mob = MobIO.deserializeFromJson(singleMobJson, physicsWorld);
                                if (mob != null) {
                                    int newId = MobMap.getNextId();
                                    Globals.mobs.put(newId, mob);

                                    // Füge Mob zur Chunk-Liste hinzu
                                    String chunkKey = getChunkKey(targetChunkX, targetChunkY);
                                    if (!mobsByChunk.containsKey(chunkKey)) {
                                        mobsByChunk.put(chunkKey, new HashSet<>());
                                    }
                                    mobsByChunk.get(chunkKey).add(newId);
                                    mobsLoaded++;
                                } else {
                                    System.out.println("FEHLER: Mob konnte nicht deserialisiert werden: " + singleMobJson);
                                }
                            }
                        } else {
                            // Es handelt sich um einen einzelnen Mob
                            Mob mob = MobIO.deserializeFromJson(mobJson, physicsWorld);
                            if (mob != null) {
                                int newId = MobMap.getNextId();
                                Globals.mobs.put(newId, mob);

                                // Füge Mob zur Chunk-Liste hinzu
                                String chunkKey = getChunkKey(targetChunkX, targetChunkY);
                                if (!mobsByChunk.containsKey(chunkKey)) {
                                    mobsByChunk.put(chunkKey, new HashSet<>());
                                }
                                mobsByChunk.get(chunkKey).add(newId);
                                mobsLoaded++;
                            } else {
                                System.out.println("FEHLER: Einzelner Mob konnte nicht deserialisiert werden: " +
                                    (mobJson.length() > 100 ? mobJson.substring(0, 100) + "..." : mobJson));
                            }
                        }

                        System.out.println("Mobs geladen aus Chunk (" + targetChunkX + "," + targetChunkY + "): " + mobsLoaded);
                    }
                } catch (IOException e) {
                    System.out.println("FEHLER beim Laden von Mobs aus Chunk (" + targetChunkX + "," + targetChunkY + "): " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Gruppiert Mobs nach ihrer Chunk-Position
     */
    private Map<String, List<Mob>> groupMobsByChunk(Collection<Mob> mobCollection)
    {
        Map<String, List<Mob>> mobsByChunkPos = new HashMap<>();

        for (Mob mob : mobCollection)
        {
            String chunkKey = getChunkKey(mob.getChunkX(), mob.getChunkY());

            if (!mobsByChunkPos.containsKey(chunkKey))
            {
                mobsByChunkPos.put(chunkKey, new ArrayList<>());
            }
            mobsByChunkPos.get(chunkKey).add(mob);
        }

        return mobsByChunkPos;
    }

    /**
     * Speichert eine Liste von Mobs für einen bestimmten Chunk
     */
    /**
     * Speichert eine Liste von Mobs für einen bestimmten Chunk
     */
    private void saveMobsForChunk(int chunkX, int chunkY, List<Mob> mobsToSave) {
        if (mobsToSave.isEmpty()) return;

        int regionX = Math.floorDiv(chunkX, MobRegionManager.REGION_SIZE);
        int regionY = Math.floorDiv(chunkY, MobRegionManager.REGION_SIZE);
        MobRegionFile mobRegionFile = mobRegionManager.getMobRegionFile(regionX, regionY);
        int localX = Math.floorMod(chunkX, MobRegionManager.REGION_SIZE);
        int localY = Math.floorMod(chunkY, MobRegionManager.REGION_SIZE);

        try {
            // WICHTIG: Lese vorhandene Mobs, falls vorhanden
            byte[] existingMobData = mobRegionFile.readMobs(localX, localY);
            List<MobIO.MobData> allMobData = new ArrayList<>();

            if (existingMobData != null) {
                String existingJson = new String(existingMobData, StandardCharsets.UTF_8);
                if (existingJson.startsWith("[") && existingJson.endsWith("]")) {
                    allMobData = gson.fromJson(existingJson, MOB_LIST_TYPE);
                } else {
                    // Ein einzelner Mob
                    MobIO.MobData singleMob = gson.fromJson(existingJson, MobIO.MobData.class);
                    if (singleMob != null) {
                        allMobData.add(singleMob);
                    }
                }
            }

            // Wandle die neuen Mobs in MobData-Objekte um und füge sie hinzu
            for (Mob mob : mobsToSave) {
                String mobJson = MobIO.serializeToJson(mob);
                MobIO.MobData mobData = gson.fromJson(mobJson, MobIO.MobData.class);
                allMobData.add(mobData);
            }

            // Speichere alle Mobs zusammen als JSON-Array
            String finalJson = gson.toJson(allMobData);
            byte[] mobData = finalJson.getBytes(StandardCharsets.UTF_8);

            // Schreibe die Daten in die Regionsdatei
            mobRegionFile.writeMobs(localX, localY, mobData);
            System.out.println("Mobs gespeichert in Region (" + regionX + "," + regionY +
                ") Chunk (" + localX + "," + localY + ") - Anzahl: " + allMobData.size() +
                " (davon neu: " + mobsToSave.size() + ")");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void unloadMobsOutsideRenderDistance()
    {
        // Aktualisiere die Map mit aktuellen Mobs pro Chunk
        updateMobsByChunk();

        // Sammle Mobs nach Chunk für die Speicherung
        Map<String, List<Mob>> mobsToSave = new HashMap<>();
        Set<Integer> mobIdsToRemove = new HashSet<>();

        // Überprüfe jedes Mob, ob es außerhalb der Reichweite aller Spieler ist
        for (Map.Entry<Integer, Mob> entry : Globals.mobs.entrySet())
        {
            int mobId = entry.getKey();
            Mob mob = entry.getValue();
            boolean shouldUnload = true;

            // Prüfe alle Spieler, ob sie den Mob-Chunk laden
            for (ServerPlayer player : GameServer.instance.players)
            {
                if (Math.abs(mob.getChunkX() - player.getChunkX()) <= MOB_UNLOAD_DISTANCE &&
                    Math.abs(mob.getChunkY() - player.getChunkY()) <= MOB_UNLOAD_DISTANCE)
                {
                    shouldUnload = false;
                    break;
                }
            }

            if (shouldUnload)
            {
                // Füge das Mob zur Liste der zu speichernden Mobs hinzu
                String chunkKey = getChunkKey(mob.getChunkX(), mob.getChunkY());
                if (!mobsToSave.containsKey(chunkKey))
                {
                    mobsToSave.put(chunkKey, new ArrayList<>());
                }
                mobsToSave.get(chunkKey).add(mob);

                // Markiere das Mob zum Entfernen
                mobIdsToRemove.add(mobId);

                // Entferne es aus der mobsByChunk-Map
                String mobChunkKey = getChunkKey(mob.getChunkX(), mob.getChunkY());
                if (mobsByChunk.containsKey(mobChunkKey))
                {
                    mobsByChunk.get(mobChunkKey).remove(mobId);
                    if (mobsByChunk.get(mobChunkKey).isEmpty())
                    {
                        mobsByChunk.remove(mobChunkKey);
                    }
                }
            }
        }

        // Speichere und entferne die Mobs
        for (Map.Entry<String, List<Mob>> entry : mobsToSave.entrySet())
        {
            String[] parts = entry.getKey().split(":");
            int chunkX = Integer.parseInt(parts[0]);
            int chunkY = Integer.parseInt(parts[1]);
            List<Mob> chunkMobs = entry.getValue();

            if (!chunkMobs.isEmpty())
            {
                saveMobsForChunk(chunkX, chunkY, chunkMobs);
            }
        }

        // Entferne die Mobs aus der globalen Map
        for (Integer mobId : mobIdsToRemove)
        {
            Mob mob = Globals.mobs.get(mobId);
            if (mob != null)
            {
                mob.dispose();
                mob.remove();
                Globals.mobs.remove(mobId);
            }
        }
    }

    /**
     * Markiert einen Chunk als besucht
     */
    private void markChunkAsVisited(int chunkX, int chunkY)
    {
        visitedChunks.add(getChunkKey(chunkX, chunkY));
    }

    /**
     * Prüft, ob ein Chunk bereits besucht wurde
     */
    private boolean isChunkVisited(int chunkX, int chunkY)
    {
        return visitedChunks.contains(getChunkKey(chunkX, chunkY));
    }

    @Override
    public void updateChunks(Player player)
    {
        if (player instanceof ServerPlayer)
        {
            updateChunksForPlayer((ServerPlayer) player);
        }
        loadMobsAroundPlayer(player);
    }

    @Override
    public void dispose()
    {
        stopChunkUpdateThread();

        // Speichere alle noch geladenen Mobs
        Map<String, List<Mob>> mobsByChunk = groupMobsByChunk(Globals.mobs.values());

        // Speichere alle Mobs für jeden Chunk
        for (Map.Entry<String, List<Mob>> entry : mobsByChunk.entrySet())
        {
            String[] parts = entry.getKey().split(":");
            int chunkX = Integer.parseInt(parts[0]);
            int chunkY = Integer.parseInt(parts[1]);
            List<Mob> chunkMobs = entry.getValue();

            if (!chunkMobs.isEmpty())
            {
                saveMobsForChunk(chunkX, chunkY, chunkMobs);
            }
        }

        // Entferne alle Mobs
        for (Mob mob : Globals.mobs.values())
        {
            mob.dispose();
            mob.remove();
        }
        Globals.mobs.clear();

        // Speichere alle Chunks
        for (Chunk chunk : loadedChunks)
        {
            int regionX = Math.floorDiv(chunk.getChunkX(), RegionManager.REGION_SIZE);
            int regionY = Math.floorDiv(chunk.getChunkY(), RegionManager.REGION_SIZE);
            RegionFile regionFile = regionManager.getRegionFile(regionX, regionY);
            int localX = Math.floorMod(chunk.getChunkX(), RegionManager.REGION_SIZE);
            int localY = Math.floorMod(chunk.getChunkY(), RegionManager.REGION_SIZE);
            byte[] data = ChunkIO.serialize(chunk);
            try
            {
                regionFile.writeChunk(localX, localY, data);
            } catch (IOException e)
            {
                e.printStackTrace();
            }
            chunk.dispose();
        }
        loadedChunks.clear();

        // Schließe alle RegionFiles
        regionManager.closeAll();
        mobRegionManager.closeAll();
    }

    /**
     * Aktualisiert die Chunks für einen einzelnen ServerPlayer.
     * Fehlende Chunks werden geladen und an den Spieler gesendet, nicht mehr benötigte werden aus seinem Set entfernt.
     */
    public void updateChunksForPlayer(ServerPlayer player)
    {
        Set<Chunk> loadedForPlayer = playerLoadedChunks.computeIfAbsent(player, k -> new HashSet<>());

        // 1. Lade alle Chunks im großen Radius (LOAD_DISTANCE)
        Set<Chunk> chunksToLoad = calculateChunkRadius(player, LOAD_DISTANCE);

        // 2. Bestimme welche Chunks gesendet werden sollen (kleinerer Radius)
        Set<Chunk> chunksToSend = calculateChunkRadius(player, SEND_DISTANCE);

        // Lade und halte Chunks im Speicher
        for (Chunk required : chunksToLoad)
        {
            if (!loadedChunks.contains(required))
            {
                Chunk actualChunk = getOrLoadChunk(required.getChunkX(), required.getChunkY());
                loadedChunks.add(actualChunk);
            }
        }

        // Sende nur Chunks im kleineren Radius
        for (Chunk chunk : loadedChunks)
        {
            if (chunksToSend.contains(chunk) && !loadedForPlayer.contains(chunk))
            {
                sendChunkToPlayer(player, chunk);
                loadedForPlayer.add(chunk);
            }
        }

        // Entferne nicht mehr benötigte Chunks aus dem Spieler-Set
        Iterator<Chunk> it = loadedForPlayer.iterator();
        while (it.hasNext())
        {
            Chunk chunk = it.next();
            if (!chunksToSend.contains(chunk))
            {
                it.remove();
            }
        }
    }


    /**
     * Versucht, einen Chunk aus dem globalen Pool zu holen oder lädt/generiert ihn, falls er noch nicht existiert.
     */
    private Chunk getOrLoadChunk(int chunkX, int chunkY)
    {
        Chunk dummy = new Chunk(chunkX, chunkY);
        // Prüfe, ob der Chunk bereits global geladen ist
        for (Chunk loaded : loadedChunks)
        {
            if (loaded.equals(dummy))
            {
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
            chunk = ChunkIO.deserialize(data, physicsWorld);
        } else
        {
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
    public void removePlayer(ServerPlayer player)
    {
        Set<Chunk> chunksOfPlayer = playerLoadedChunks.remove(player);
        if (chunksOfPlayer != null)
        {
            for (Chunk chunk : chunksOfPlayer)
            {
                boolean inUse = false;
                for (Set<Chunk> otherChunks : playerLoadedChunks.values())
                {
                    if (otherChunks.contains(chunk))
                    {
                        inUse = true;
                        break;
                    }
                }
                if (!inUse)
                {
                    int regionX = Math.floorDiv(chunk.getChunkX(), RegionManager.REGION_SIZE);
                    int regionY = Math.floorDiv(chunk.getChunkY(), RegionManager.REGION_SIZE);
                    RegionFile regionFile = regionManager.getRegionFile(regionX, regionY);
                    int localX = Math.floorMod(chunk.getChunkX(), RegionManager.REGION_SIZE);
                    int localY = Math.floorMod(chunk.getChunkY(), RegionManager.REGION_SIZE);
                    byte[] data = ChunkIO.serialize(chunk);
                    try
                    {
                        regionFile.writeChunk(localX, localY, data);
                    } catch (IOException e)
                    {
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
    public void updateChunksForAllPlayers()
    {
        for (ServerPlayer player : GameServer.instance.players)
        {
            updateChunksForPlayer(player);
        }
    }


    private void sendChunkToPlayer(ServerPlayer player, Chunk chunk)
    {
        NetworkPackets.ChunkDataPacket packet = new NetworkPackets.ChunkDataPacket();
        packet.data = ChunkIO.serialize(chunk);
        player.getConnection().sendTCP(packet);
    }

    private Set<Chunk> calculateChunkRadius(ServerPlayer player, int distance)
    {
        Set<Chunk> chunks = new HashSet<>();
        for (int x = -distance; x <= distance; x++)
        {
            for (int y = -distance; y <= distance; y++)
            {
                chunks.add(new Chunk(
                    player.getChunkX() + x,
                    player.getChunkY() + y
                ));
            }
        }
        return chunks;
    }

    /**
     * Speichert die Welt (alle Chunks und Mobs)
     */
    /**
     * Speichert die Welt (alle Chunks und Mobs)
     */
    public void saveWorld()
    {
        // Alle Chunks speichern
        for (Chunk chunk : loadedChunks)
        {
            int regionX = Math.floorDiv(chunk.getChunkX(), RegionManager.REGION_SIZE);
            int regionY = Math.floorDiv(chunk.getChunkY(), RegionManager.REGION_SIZE);
            RegionFile regionFile = regionManager.getRegionFile(regionX, regionY);
            int localX = Math.floorMod(chunk.getChunkX(), RegionManager.REGION_SIZE);
            int localY = Math.floorMod(chunk.getChunkY(), RegionManager.REGION_SIZE);
            byte[] data = ChunkIO.serialize(chunk);
            try
            {
                regionFile.writeChunk(localX, localY, data);
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        // Alle Mobs speichern
        Map<String, List<Mob>> mobsByChunk = groupMobsByChunk(Globals.mobs.values());
        for (Map.Entry<String, List<Mob>> entry : mobsByChunk.entrySet())
        {
            String[] parts = entry.getKey().split(":");
            int chunkX = Integer.parseInt(parts[0]);
            int chunkY = Integer.parseInt(parts[1]);
            List<Mob> chunkMobs = entry.getValue();

            if (!chunkMobs.isEmpty())
            {
                saveMobsForChunk(chunkX, chunkY, chunkMobs);
            }
        }
    }
}
