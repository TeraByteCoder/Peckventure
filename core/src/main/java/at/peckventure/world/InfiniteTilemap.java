package at.peckventure.world;

import at.peckventure.entities.Player;
import at.peckventure.entities.mob.Mob;
import at.peckventure.entities.mob.MobIO;
import at.peckventure.world.chunk.Chunk;
import at.peckventure.world.chunk.ChunkIO;
import at.peckventure.world.generator.WorldGenerator;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.physics.box2d.World;
import static at.peckventure.Globals.mobs;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class InfiniteTilemap {
    private final World physicsWorld;
    public static final int RENDER_DISTANCE = 3;
    public static final int MOB_DISTANCE = RENDER_DISTANCE - 2;
    private final WorldGenerator worldGenerator;
    private final Set<Chunk> loadedChunks = ConcurrentHashMap.newKeySet();
    private final RegionManager regionManager;
    private final MobRegionManager mobRegionManager;
    private Thread chunkUpdateThread;
    private volatile boolean running = false;

    public InfiniteTilemap(World world, WorldGenerator generator, Set<Chunk> preLoadedChunks, RegionManager regionManager, MobRegionManager mobRegionManager) {
        this.physicsWorld = world;
        this.worldGenerator = generator;
        if (preLoadedChunks != null) {
            loadedChunks.addAll(preLoadedChunks);
        }
        this.regionManager = regionManager;
        this.mobRegionManager = mobRegionManager;
    }

    public void render(Batch batch) {
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
                        worldGenerator.generateChunk(chunk);
                    }
                    loadedChunks.add(chunk);
                }
            }
        }
    }

    public void loadChunksAroundPlayers(List<Player> players) {
        for (Player player : players) {
            loadChunksAroundPlayer(player);
        }
    }

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

    public void unloadChunksOutsideRenderDistance(List<Player> players) {
        Iterator<Chunk> iterator = loadedChunks.iterator();
        while (iterator.hasNext()) {
            Chunk chunk = iterator.next();
            boolean keep = false;
            for (Player player : players) {
                if (Math.abs(chunk.getChunkX() - player.getChunkX()) <= RENDER_DISTANCE + 2 &&
                    Math.abs(chunk.getChunkY() - player.getChunkY()) <= RENDER_DISTANCE + 2) {
                    keep = true;
                    break;
                }
            }
            if (!keep) {
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

    public byte[] getChunkData(int chunkX, int chunkY) {
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
        return data;
    }


    public void loadMobsAroundPlayer(Player player) {
        for (int x_offset = -MOB_DISTANCE - 1; x_offset <= MOB_DISTANCE; x_offset++) {
            for (int y_offset = -MOB_DISTANCE; y_offset <= MOB_DISTANCE; y_offset++) {
                int targetChunkX = player.getChunkX() + x_offset;
                int targetChunkY = player.getChunkY() + y_offset;
                boolean mobExists = false;
                for (Mob m : mobs) {
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
                        mobs.add(mob);
                    }
                }
            }
        }
    }

    public void loadMobsAroundPlayers(List<Player> players) {
        for (Player player : players) {
            loadMobsAroundPlayer(player);
        }
    }

    public void unloadMobsOutsideRenderDistance(Player player) {
        Iterator<Mob> iterator = mobs.iterator();
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

    public void unloadMobsOutsideRenderDistance(List<Player> players) {
        Iterator<Mob> iterator = mobs.iterator();
        while (iterator.hasNext()) {
            Mob mob = iterator.next();
            boolean keep = false;
            for (Player player : players) {
                if (Math.abs(mob.getChunkX() - player.getChunkX()) <= MOB_DISTANCE + 2 &&
                    Math.abs(mob.getChunkY() - player.getChunkY()) <= MOB_DISTANCE + 2) {
                    keep = true;
                    break;
                }
            }
            if (!keep) {
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

    public void updateChunks(Player player) {
        loadChunksAroundPlayer(player);
        unloadChunksOutsideRenderDistance(player);
        loadMobsAroundPlayer(player);
        unloadMobsOutsideRenderDistance(player);
    }

    public void updateChunks(List<Player> players) {
        loadChunksAroundPlayers(players);
        unloadChunksOutsideRenderDistance(players);
        loadMobsAroundPlayers(players);
        unloadMobsOutsideRenderDistance(players);
    }

    public void startChunkUpdateThread(Player player) {
        if (chunkUpdateThread != null && chunkUpdateThread.isAlive()) return;
        running = true;
        chunkUpdateThread = new Thread(() -> {
            while (running) {
                updateChunks(player);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        chunkUpdateThread.setDaemon(true);
        chunkUpdateThread.start();
    }

    public void startChunkUpdateThread(List<Player> players) {
        if (chunkUpdateThread != null && chunkUpdateThread.isAlive()) return;
        running = true;
        chunkUpdateThread = new Thread(() -> {
            while (running) {
                updateChunks(players);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        chunkUpdateThread.setDaemon(true);
        chunkUpdateThread.start();
    }

    public void stopChunkUpdateThread() {
        running = false;
        if (chunkUpdateThread != null) {
            try {
                chunkUpdateThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

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
}
