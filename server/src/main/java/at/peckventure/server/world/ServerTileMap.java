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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ServerTileMap extends AbstractTileMap {
    private final WorldGenerator worldGenerator;
    private final RegionManager regionManager;
    private final MobRegionManager mobRegionManager;

    public static final float MOB_UPDATE_RADIUS = 1000f; // z. B. 500 Pixel oder passe den Wert an
    public static final int MOB_UNLOAD_DISTANCE = MOB_DISTANCE + 2;

    private static final int LOAD_DISTANCE = 6;  // Größerer Radius zum Vorladen
    private static final int SEND_DISTANCE = 3;  // Kleinerer Radius zum Senden
    private static final int MOB_LOAD_DISTANCE = 6;
    private static final int MOB_SEND_DISTANCE = 4;



    // Globaler Chunk-Pool (wird z. B. zum Caching genutzt – stammt aus der Oberklasse)
    // protected Set<Chunk> loadedChunks;

    // Map zum per-Spieler-Tracking der aktuell geladenen Chunks
    private Map<ServerPlayer, Set<Chunk>> playerLoadedChunks = new HashMap<>();

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
        return null;
    }



    public void updateMobsForPlayer(ServerPlayer player) {
        // Erstelle ein neues MobUpdatePacket
        NetworkPackets.MobUpdatePacket mobPacket = new NetworkPackets.MobUpdatePacket();
        mobPacket.mobUpdates = new ArrayList<>();

        // Hole die Position des Spielers
        float playerX = player.getX();
        float playerY = player.getY();

        // Iteriere über alle Mobs in der Globals-Mob-Map
        for (Map.Entry<Integer, Mob> entry : at.peckventure.Globals.mobs.entrySet()) {
            Mob mob = entry.getValue();
            // Berechne den Abstand zum Spieler
            float dx = mob.getX() - playerX;
            float dy = mob.getY() - playerY;
            if (dx * dx + dy * dy <= MOB_UPDATE_RADIUS * MOB_UPDATE_RADIUS) {
                // Erstelle ein Update-Paket für diesen Mob
                NetworkPackets.SingleMobUpdatePacket update = new NetworkPackets.SingleMobUpdatePacket();
                update.umid = entry.getKey();  // Stelle sicher, dass diese Methode in der Mob-Klasse vorhanden ist.
                update.mobid = MobRegistry.getMobId(mob);     // Ebenso: Implementiere diese Methode, falls noch nicht vorhanden.
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
        GameServer.instance.getServer().sendToUDP(player.getConnection().getID(), mobPacket);

        // Sende das Mob-Update-Paket an den Spieler
        try {
            player.getConnection().sendTCP(mobPacket);
        } catch (Exception e) {
            System.err.println("Fehler beim Senden der Mob-Updates an " + player.getUsername());
            e.printStackTrace();
        }
    }

    public void loadMobsAroundPlayer(Player player) {
        for (int x_offset = -MOB_DISTANCE - 1; x_offset <= MOB_DISTANCE; x_offset++) {
            for (int y_offset = -MOB_DISTANCE; y_offset <= MOB_DISTANCE; y_offset++) {
                int targetChunkX = player.getChunkX() + x_offset;
                int targetChunkY = player.getChunkY() + y_offset;
                boolean mobExists = false;
                for (Mob m : Globals.mobs.values()) {
                    if (m.getChunkX() == targetChunkX && m.getChunkY() == targetChunkY) {
                        mobExists = true;
                        break;
                    }
                }
                if (!mobExists) {
                    int regionX = Math.floorDiv(targetChunkX, MobRegionManager.REGION_SIZE);
                    int regionY = Math.floorDiv(targetChunkY, MobRegionManager.REGION_SIZE);
                    MobRegionFile mobRegionFile = mobRegionManager.getMobRegionFile(regionX, regionY);
                    int localX = Math.floorMod(targetChunkX, MobRegionManager.REGION_SIZE);
                    int localY = Math.floorMod(targetChunkY, MobRegionManager.REGION_SIZE);
                    byte[] mobData = null;
                    try {
                        mobData = mobRegionFile.readMobs(localX, localY);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (mobData != null) {
                        System.out.println("Lade Mob-Daten aus Region (" + localX + ", " + localY + ")");
                        String mobJson = new String(mobData, StandardCharsets.UTF_8);
                        Mob mob = MobIO.deserializeFromJson(mobJson, physicsWorld);
                        int newId = MobMap.getNextId();
                        Globals.mobs.put(newId, mob);
                    }
                }
            }
        }
    }


    public void unloadMobsOutsideRenderDistance() {
        Iterator<Map.Entry<Integer, Mob>> iterator = Globals.mobs.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Mob> entry = iterator.next();
            Mob mob = entry.getValue();
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
                // Speichern und Entfernen des Mobs
                int regionX = Math.floorDiv(mob.getChunkX(), MobRegionManager.REGION_SIZE);
                int regionY = Math.floorDiv(mob.getChunkY(), MobRegionManager.REGION_SIZE);
                MobRegionFile mobRegionFile = mobRegionManager.getMobRegionFile(regionX, regionY);
                int localX = Math.floorMod(mob.getChunkX(), MobRegionManager.REGION_SIZE);
                int localY = Math.floorMod(mob.getChunkY(), MobRegionManager.REGION_SIZE);
                String mobJson = MobIO.serializeToJson(mob);
                byte[] mobData = mobJson.getBytes(StandardCharsets.UTF_8);
                try {
                    mobRegionFile.writeMobs(localX, localY, mobData);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mob.dispose();
                mob.remove();
                iterator.remove();
            }
        }
    }

    @Override
    public void updateChunks(Player player) {
        if (player instanceof ServerPlayer) {
            updateChunksForPlayer((ServerPlayer) player);
        }
        loadMobsAroundPlayer(player);
    }


    @Override
    public void dispose() {
        stopChunkUpdateThread();
        // Speichere alle noch geladenen Mobs
        for (Mob mob : at.peckventure.Globals.mobs.values()) {
            int regionX = Math.floorDiv(mob.getChunkX(), MobRegionManager.REGION_SIZE);
            int regionY = Math.floorDiv(mob.getChunkY(), MobRegionManager.REGION_SIZE);
            MobRegionFile mobRegionFile = mobRegionManager.getMobRegionFile(regionX, regionY);
            int localX = Math.floorMod(mob.getChunkX(), MobRegionManager.REGION_SIZE);
            int localY = Math.floorMod(mob.getChunkY(), MobRegionManager.REGION_SIZE);
            String mobJson = MobIO.serializeToJson(mob);
            byte[] mobData = mobJson.getBytes(StandardCharsets.UTF_8);
            try {
                mobRegionFile.writeMobs(localX, localY, mobData);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mob.dispose();
            mob.remove(); // Entfernt den Mob aus der Stage, falls vorhanden
        }
        at.peckventure.Globals.mobs.clear();

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
        }
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
