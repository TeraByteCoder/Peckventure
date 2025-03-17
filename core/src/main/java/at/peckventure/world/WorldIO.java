package at.peckventure.world;

import at.peckventure.entities.Player;
import at.peckventure.entities.mob.Mob;
import at.peckventure.entities.mob.MobIO;
import at.peckventure.inventory.Inventory;
import at.peckventure.world.chunk.Chunk;
import at.peckventure.world.chunk.ChunkIO;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.physics.box2d.World;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static at.peckventure.Const.savesDir;
import static at.peckventure.Globals.mobs;

public class WorldIO {
    public static void saveWorld(String worldName, WorldConfig config, Set<Chunk> loadedChunks, Player player, Inventory inventory) {
        FileHandle worldDir = Gdx.files.absolute(at.peckventure.Const.savesDir + "/" + worldName);
        if (!worldDir.exists()) {
            worldDir.mkdirs();
        }
        config.setPlayerX(player.getX());
        config.setPlayerY(player.getY());
        config.setInventoryHotbar(inventory.serializeHotbar());
        config.setInventoryMain(inventory.serializeMain());
        FileHandle configFile = worldDir.child("worldconfig.txt");
        config.save(configFile);
        RegionManager regionManager = new RegionManager(worldDir);
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
        }
        regionManager.closeAll();
        MobRegionManager mobRegionManager = new MobRegionManager(worldDir);
        for (Mob mob : mobs) {
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
        }
        mobRegionManager.closeAll();
    }

    public static LoadedWorld loadWorld(FileHandle worldDir, World physicsWorld) {
        if (!worldDir.exists()) {
            createWorld(worldDir,new Random().nextInt());
        }
        FileHandle configFile = worldDir.child("worldconfig.txt");
        WorldConfig config = WorldConfig.load(configFile);
        int playerChunkX = (int) config.getPlayerX();
        int playerChunkY = (int) config.getPlayerY();
        Set<Chunk> loadedChunks = new HashSet<>();
        FileHandle regionsDir = worldDir.child("regions");
        if (regionsDir.exists()) {
            for (FileHandle regionFileHandle : regionsDir.list()) {
                try {
                    RegionFile regionFile = new RegionFile(regionFileHandle.file());
                    String[] tokens = regionFileHandle.nameWithoutExtension().split("\\.");
                    if (tokens.length >= 3) {
                        int regionX = Integer.parseInt(tokens[1]);
                        int regionY = Integer.parseInt(tokens[2]);
                        for (int localX = 0; localX < RegionFile.CHUNKS_PER_REGION; localX++) {
                            for (int localY = 0; localY < RegionFile.CHUNKS_PER_REGION; localY++) {
                                int chunkX = regionX * RegionManager.REGION_SIZE + localX;
                                int chunkY = regionY * RegionManager.REGION_SIZE + localY;
                                if (Math.abs(chunkX - playerChunkX) <= InfiniteTilemap.RENDER_DISTANCE && Math.abs(chunkY - playerChunkY) <= InfiniteTilemap.RENDER_DISTANCE) {
                                    byte[] data = regionFile.readChunk(localX, localY);
                                    if (data != null) {
                                        Chunk chunk = ChunkIO.deserialize(data, physicsWorld);
                                        loadedChunks.add(chunk);
                                    }
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
        Set<Mob> loadedMobs = new HashSet<>();
        FileHandle mobRegionsDir = worldDir.child("mob_regions");
        if (mobRegionsDir.exists()) {
            for (FileHandle mobRegionFileHandle : mobRegionsDir.list()) {
                try {
                    MobRegionFile mobRegionFile = new MobRegionFile(mobRegionFileHandle.file());
                    String[] tokens = mobRegionFileHandle.nameWithoutExtension().split("\\.");
                    if (tokens.length >= 3) {
                        int regionX = Integer.parseInt(tokens[1]);
                        int regionY = Integer.parseInt(tokens[2]);
                        for (int localX = 0; localX < MobRegionManager.REGION_SIZE; localX++) {
                            for (int localY = 0; localY < MobRegionManager.REGION_SIZE; localY++) {
                                int mobChunkX = regionX * MobRegionManager.REGION_SIZE + localX;
                                int mobChunkY = regionY * MobRegionManager.REGION_SIZE + localY;
                                if (Math.abs(mobChunkX - playerChunkX) <= InfiniteTilemap.MOB_DISTANCE && Math.abs(mobChunkY - playerChunkY) <= InfiniteTilemap.MOB_DISTANCE) {
                                    byte[] mobData = mobRegionFile.readMobs(localX, localY);
                                    if (mobData != null) {
                                        String mobJson = new String(mobData, StandardCharsets.UTF_8);
                                        Mob mob = MobIO.deserializeFromJson(mobJson, physicsWorld);
                                        loadedMobs.add(mob);
                                    }
                                }
                            }
                        }
                    }
                    mobRegionFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        mobs.addAll(loadedMobs);
        return new LoadedWorld(config, loadedChunks, loadedMobs);
    }

    public static void createWorld(String worldName, FileHandle path, long seed)
    {
        if (worldName == null || worldName.trim().isEmpty()) {
            worldName = "New World";
        }
        FileHandle newWorldDir = Gdx.files.absolute(path + "/" + worldName);
        while (newWorldDir.exists()) {
            worldName += "_";
            newWorldDir = Gdx.files.absolute(path + "/" + worldName);
        }
        newWorldDir.mkdirs();
        WorldConfig config = new WorldConfig(seed);
        FileHandle configFile = newWorldDir.child("worldconfig.txt");
        config.save(configFile);
    }

    public static void createWorld(FileHandle path, long seed)
    {

        path.mkdirs();
        WorldConfig config = new WorldConfig(seed);
        FileHandle configFile = path.child("worldconfig.txt");
        config.save(configFile);
    }

    public static class LoadedWorld {
        private final WorldConfig config;
        private final Set<Chunk> loadedChunks;
        private final Set<Mob> loadedMobs;

        public LoadedWorld(WorldConfig config, Set<Chunk> loadedChunks, Set<Mob> loadedMobs) {
            this.config = config;
            this.loadedChunks = loadedChunks;
            this.loadedMobs = loadedMobs;
        }

        public WorldConfig getConfig() {
            return config;
        }

        public Set<Chunk> getLoadedChunks() {
            return loadedChunks;
        }

        public Set<Mob> getLoadedMobs() {
            return loadedMobs;
        }
    }
}
