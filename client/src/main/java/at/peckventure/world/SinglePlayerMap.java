package at.peckventure.world;

import at.peckventure.entities.Player;
import at.peckventure.entities.mob.Mob;
import at.peckventure.entities.mob.MobIO;
import at.peckventure.world.chunk.Chunk;
import at.peckventure.world.chunk.ChunkIO;
import at.peckventure.world.generator.WorldGenerator;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.physics.box2d.World;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;
import static at.peckventure.Globals.mobs;

public class SinglePlayerMap extends AbstractTileMap {
    private final WorldGenerator worldGenerator;
    private final RegionManager regionManager;
    private final MobRegionManager mobRegionManager;

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
                        worldGenerator.generateChunk(chunk);
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

    @Override
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

    @Override
    public void updateChunks(Player player) {
        loadChunksAroundPlayer(player);
        unloadChunksOutsideRenderDistance(player);
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
}
