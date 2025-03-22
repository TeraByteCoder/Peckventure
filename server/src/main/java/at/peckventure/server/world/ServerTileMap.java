package at.peckventure.server.world;

import at.peckventure.entities.Player;
import at.peckventure.entities.mob.Mob;
import at.peckventure.entities.mob.MobIO;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class ServerTileMap extends AbstractTileMap {
    private final WorldGenerator worldGenerator;
    private final RegionManager regionManager;
    private final MobRegionManager mobRegionManager;

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
    public void loadMobsAroundPlayer(Player player) {
        for (int x_offset = -MOB_DISTANCE - 1; x_offset <= MOB_DISTANCE; x_offset++) {
            for (int y_offset = -MOB_DISTANCE; y_offset <= MOB_DISTANCE; y_offset++) {
                int targetChunkX = player.getChunkX() + x_offset;
                int targetChunkY = player.getChunkY() + y_offset;
                boolean mobExists = false;
                for (Mob m : at.peckventure.Globals.mobs) {
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
                        String mobJson = new String(mobData, StandardCharsets.UTF_8);
                        Mob mob = MobIO.deserializeFromJson(mobJson, physicsWorld);
                        at.peckventure.Globals.mobs.add(mob);
                    }
                }
            }
        }
    }

    @Override
    public void unloadMobsOutsideRenderDistance(Player player) {
        Iterator<Mob> iterator = at.peckventure.Globals.mobs.iterator();
        while (iterator.hasNext()) {
            Mob mob = iterator.next();
            if (Math.abs(mob.getChunkX() - player.getChunkX()) > MOB_DISTANCE + 2 ||
                Math.abs(mob.getChunkY() - player.getChunkY()) > MOB_DISTANCE + 2) {
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
        unloadMobsOutsideRenderDistance(player);
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
