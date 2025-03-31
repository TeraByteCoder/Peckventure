package at.peckventure.world;

import at.peckventure.Globals;
import at.peckventure.entities.Player;
import at.peckventure.entities.mob.Mob;
import at.peckventure.entities.mob.MobIO;
import at.peckventure.entities.mob.MobMap;
import at.peckventure.world.chunk.Chunk;
import at.peckventure.world.chunk.ChunkIO;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.physics.box2d.World;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static at.peckventure.Const.savesDir;
import static at.peckventure.Globals.mobs;
import static at.peckventure.Globals.uuid;

public class WorldIO
{
    public static void saveWorld(String worldName, WorldConfig config, Set<Chunk> loadedChunks, HashMap<String, Player> players)
    {
        FileHandle worldDir = Gdx.files.absolute(savesDir + "/" + worldName);
        if (!worldDir.exists())
        {
            worldDir.mkdirs();
        }
        Set<String> uuids = players.keySet();
        for (String uuid : uuids)
        {
            Player player = players.get(uuid);
            PlayerData playerData = new PlayerData(uuid);

            playerData.setPlayerX(player.getX());
            playerData.setPlayerY(player.getY());
            playerData.setInventoryHotbar(player.getInventory().serializeHotbar());
            playerData.setInventoryMain((player.getInventory().serializeMain()));
            playerData.setOperator(false);
        }
        FileHandle configFile = worldDir.child("worldconfig.txt");
        config.save(configFile);
        RegionManager regionManager = new RegionManager(worldDir);
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
        regionManager.closeAll();
        MobRegionManager mobRegionManager = new MobRegionManager(worldDir);
        for (Mob mob : mobs.values())
        {
            int regionX = Math.floorDiv(mob.getChunkX(), MobRegionManager.REGION_SIZE);
            int regionY = Math.floorDiv(mob.getChunkY(), MobRegionManager.REGION_SIZE);
            MobRegionFile mobRegionFile = mobRegionManager.getMobRegionFile(regionX, regionY);
            int localX = Math.floorMod(mob.getChunkX(), MobRegionManager.REGION_SIZE);
            int localY = Math.floorMod(mob.getChunkY(), MobRegionManager.REGION_SIZE);
            String mobJson = MobIO.serializeToJson(mob);
            byte[] mobData = mobJson.getBytes(StandardCharsets.UTF_8);
            try
            {
                mobRegionFile.writeMobs(localX, localY, mobData);
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        mobRegionManager.closeAll();
    }

    public static void saveWorld(String worldName, WorldConfig config, Set<Chunk> loadedChunks, Player player)
    {
        FileHandle worldDir = Gdx.files.absolute(savesDir + "/" + worldName);
        if (!worldDir.exists())
        {
            worldDir.mkdirs();
        }

        PlayerData playerData = new PlayerData(uuid, player.getX(), player.getY(), player.getInventory().serializeHotbar(), player.getInventory().serializeMain(), false, player.getEnergyStatus().getCurrent(), player.getHealthStatus().getCurrent());
        playerData.save(worldDir);

        FileHandle configFile = worldDir.child("worldconfig.txt");
        config.save(configFile);
        RegionManager regionManager = new RegionManager(worldDir);
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
        regionManager.closeAll();
        MobRegionManager mobRegionManager = new MobRegionManager(worldDir);
        for (Mob mob : mobs.values())
        {
            int regionX = Math.floorDiv(mob.getChunkX(), MobRegionManager.REGION_SIZE);
            int regionY = Math.floorDiv(mob.getChunkY(), MobRegionManager.REGION_SIZE);
            MobRegionFile mobRegionFile = mobRegionManager.getMobRegionFile(regionX, regionY);
            int localX = Math.floorMod(mob.getChunkX(), MobRegionManager.REGION_SIZE);
            int localY = Math.floorMod(mob.getChunkY(), MobRegionManager.REGION_SIZE);
            String mobJson = MobIO.serializeToJson(mob);
            byte[] mobData = mobJson.getBytes(StandardCharsets.UTF_8);
            try
            {
                mobRegionFile.writeMobs(localX, localY, mobData);
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        mobRegionManager.closeAll();
    }

    public static LoadedWorld loadWorld(FileHandle worldDir, World physicsWorld)
    {
        PlayerData singlePlayerData = null;
        if (!worldDir.exists())
        {
            createWorld(worldDir, new Random().nextInt());
        }
        FileHandle configFile = worldDir.child("worldconfig.txt");

        FileHandle playerdataFolder = worldDir.child("playerData");
        HashSet<String> uuids = PlayerData.getPlayerUUIDs(playerdataFolder);
        Set<PlayerData> players = new HashSet<>();
        for (String uuid : uuids)
        {
            PlayerData playerData = new PlayerData(uuid);
            players.add(playerData);
            if (uuid == Globals.uuid && Globals.uuid != null)
            {
                singlePlayerData = playerData;
            }
        }


        WorldConfig config = WorldConfig.load(configFile);
        Set<Chunk> loadedChunks = new HashSet<>();
        FileHandle regionsDir = worldDir.child("regions");
        if (regionsDir.exists())
        {
            for (FileHandle regionFileHandle : regionsDir.list())
            {
                try
                {
                    RegionFile regionFile = new RegionFile(regionFileHandle.file());
                    String[] tokens = regionFileHandle.nameWithoutExtension().split("\\.");
                    if (tokens.length >= 3)
                    {
                        int regionX = Integer.parseInt(tokens[1]);
                        int regionY = Integer.parseInt(tokens[2]);
                        for (int localX = 0; localX < RegionFile.CHUNKS_PER_REGION; localX++)
                        {
                            for (int localY = 0; localY < RegionFile.CHUNKS_PER_REGION; localY++)
                            {
                                int chunkX = regionX * RegionManager.REGION_SIZE + localX;
                                int chunkY = regionY * RegionManager.REGION_SIZE + localY;
                                if (singlePlayerData != null)
                                {
                                    if (Math.abs(chunkX - Globals.toChunkCoords(singlePlayerData.getPlayerX())) <= AbstractTileMap.RENDER_DISTANCE && Math.abs(chunkY - Globals.toChunkCoords(singlePlayerData.getPlayerY())) <= AbstractTileMap.RENDER_DISTANCE)
                                    {
                                        byte[] data = regionFile.readChunk(localX, localY);
                                        if (data != null)
                                        {
                                            Chunk chunk = ChunkIO.deserialize(data, physicsWorld);
                                            loadedChunks.add(chunk);
                                        }
                                    }
                                } else
                                {
                                    if (Math.abs(chunkX) <= AbstractTileMap.RENDER_DISTANCE && Math.abs(chunkY) <= AbstractTileMap.RENDER_DISTANCE)
                                    {
                                        byte[] data = regionFile.readChunk(localX, localY);
                                        if (data != null)
                                        {
                                            Chunk chunk = ChunkIO.deserialize(data, physicsWorld);
                                            loadedChunks.add(chunk);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    regionFile.close();
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
        Set<Mob> loadedMobs = new HashSet<>();
        FileHandle mobRegionsDir = worldDir.child("mob_regions");
        if (mobRegionsDir.exists())
        {
            for (FileHandle mobRegionFileHandle : mobRegionsDir.list())
            {
                try
                {
                    MobRegionFile mobRegionFile = new MobRegionFile(mobRegionFileHandle.file());
                    String[] tokens = mobRegionFileHandle.nameWithoutExtension().split("\\.");
                    if (tokens.length >= 3)
                    {
                        int regionX = Integer.parseInt(tokens[1]);
                        int regionY = Integer.parseInt(tokens[2]);
                        for (int localX = 0; localX < MobRegionManager.REGION_SIZE; localX++)
                        {
                            for (int localY = 0; localY < MobRegionManager.REGION_SIZE; localY++)
                            {
                                if (singlePlayerData != null)
                                {
                                    int mobChunkX = regionX * MobRegionManager.REGION_SIZE + localX;
                                    int mobChunkY = regionY * MobRegionManager.REGION_SIZE + localY;
                                    if (Math.abs(mobChunkX - Globals.toChunkCoords(singlePlayerData.getPlayerX())) <= AbstractTileMap.MOB_DISTANCE && Math.abs(mobChunkY - Globals.toChunkCoords(singlePlayerData.getPlayerY())) <= AbstractTileMap.MOB_DISTANCE)
                                    {
                                        byte[] mobData = mobRegionFile.readMobs(localX, localY);
                                        if (mobData != null)
                                        {
                                            String mobJson = new String(mobData, StandardCharsets.UTF_8);
                                            Mob mob = MobIO.deserializeFromJson(mobJson, physicsWorld);
                                            loadedMobs.add(mob);
                                        }
                                    }
                                }
                                int mobChunkX = regionX * MobRegionManager.REGION_SIZE + localX;
                                int mobChunkY = regionY * MobRegionManager.REGION_SIZE + localY;
                                if (Math.abs(mobChunkX) <= AbstractTileMap.MOB_DISTANCE && Math.abs(mobChunkY) <= AbstractTileMap.MOB_DISTANCE)
                                {
                                    byte[] mobData = mobRegionFile.readMobs(localX, localY);
                                    if (mobData != null)
                                    {
                                        String mobJson = new String(mobData, StandardCharsets.UTF_8);
                                        Mob mob = MobIO.deserializeFromJson(mobJson, physicsWorld);
                                        loadedMobs.add(mob);
                                    }
                                }
                            }
                        }
                    }
                    mobRegionFile.close();
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
        for (Mob mob : loadedMobs) {
            int newId = MobMap.getNextId();
            mobs.put(newId, mob);
        }
        return new LoadedWorld(config, loadedChunks, loadedMobs, players);
    }

    public static void createWorld(String worldName, FileHandle path, long seed)
    {
        if (worldName == null || worldName.trim().isEmpty())
        {
            worldName = "New World";
        }
        FileHandle newWorldDir = Gdx.files.absolute(path + "/" + worldName);
        while (newWorldDir.exists())
        {
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

    public static class LoadedWorld
    {
        private final WorldConfig config;
        private final Set<Chunk> loadedChunks;
        private final Set<Mob> loadedMobs;

        private final Set<PlayerData> players;

        public LoadedWorld(WorldConfig config, Set<Chunk> loadedChunks, Set<Mob> loadedMobs, Set<PlayerData> players)
        {
            this.config = config;
            this.loadedChunks = loadedChunks;
            this.loadedMobs = loadedMobs;
            this.players = players;
        }

        public LoadedWorld(WorldConfig config, Set<Chunk> loadedChunks, Set<Mob> loadedMobs, PlayerData player)
        {
            this.config = config;
            this.loadedChunks = loadedChunks;
            this.loadedMobs = loadedMobs;
            this.players = new HashSet<>();
            players.add(player);
        }


        public WorldConfig getConfig()
        {
            return config;
        }

        public Set<Chunk> getLoadedChunks()
        {
            return loadedChunks;
        }

        public Set<Mob> getLoadedMobs()
        {
            return loadedMobs;
        }

        public Set<PlayerData> players()
        {
            return players;
        }
    }
}
