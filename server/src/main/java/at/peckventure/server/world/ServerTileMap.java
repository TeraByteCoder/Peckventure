package at.peckventure.server.world;

import at.peckventure.entities.Player;
import at.peckventure.entities.mob.ItemActor;
import at.peckventure.entities.mob.Mob;
import at.peckventure.entities.mob.MobIO;
import at.peckventure.entities.mob.MobRegistry;
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

    public static final float MOB_UPDATE_RADIUS = 5000f; // z. B. 500 Pixel oder passe den Wert an


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
                    update.extraItem = item.getInventoryItem().getName();
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

    @Override
    public void updateChunks(Player player) {
        if (player instanceof ServerPlayer) {
            updateChunksForPlayer((ServerPlayer) player);
        }
    }

    @Override
    public void dispose() {
        stopChunkUpdateThread();
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
        // Hole oder initialisiere das Set für diesen Spieler
        Set<Chunk> loadedForPlayer = playerLoadedChunks.computeIfAbsent(player, k -> new HashSet<>());

        // Berechne alle Chunks, die der Spieler aktuell im Sichtbereich (RENDER_DISTANCE) benötigt
        Set<Chunk> requiredChunks = new HashSet<>();
        for (int xOffset = -RENDER_DISTANCE - 1; xOffset <= RENDER_DISTANCE; xOffset++) {
            for (int yOffset = -RENDER_DISTANCE; yOffset <= RENDER_DISTANCE; yOffset++) {
                int targetChunkX = player.getChunkX() + xOffset;
                int targetChunkY = player.getChunkY() + yOffset;
                // Erstelle einen Dummy-Chuck, der nur die Koordinaten repräsentiert
                requiredChunks.add(new Chunk(targetChunkX, targetChunkY));
            }
        }

        // Lade alle benötigten Chunks, die im Spieler-Set noch fehlen
        for (Chunk required : requiredChunks) {
            if (!loadedForPlayer.contains(required)) {
                Chunk actualChunk = getOrLoadChunk(required.getChunkX(), required.getChunkY());
                loadedForPlayer.add(actualChunk);
                // Sende den Chunk an den Spieler
                NetworkPackets.ChunkDataPacket dataPacket = new NetworkPackets.ChunkDataPacket();
                dataPacket.data = ChunkIO.serialize(actualChunk);
                player.getConnection().sendTCP(dataPacket);
            }
        }

        // Entferne alle Chunks, die nicht mehr im Sichtbereich des Spielers liegen
        Iterator<Chunk> it = loadedForPlayer.iterator();
        while (it.hasNext()) {
            Chunk chunk = it.next();
            if (!requiredChunks.contains(chunk)) {
                it.remove();
            }
        }
    }

    public void updateMobsForAllPlayers(Set<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            updateMobsForPlayer(player);
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
            worldGenerator.generateChunk(chunk);
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
}
