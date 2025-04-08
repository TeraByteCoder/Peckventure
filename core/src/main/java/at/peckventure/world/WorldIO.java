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
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static at.peckventure.Const.savesDir;
import static at.peckventure.Globals.mobs;
import static at.peckventure.Globals.uuid;

public class WorldIO
{
    private static final Gson gson = new Gson();
    private static final Type MOB_LIST_TYPE = new TypeToken<List<MobIO.MobData>>(){}.getType();

    /**
     * Gruppiert die Mobs nach ihrer Chunk-Position
     */
    private static Map<String, List<Mob>> groupMobsByChunk(Collection<Mob> mobCollection) {
        Map<String, List<Mob>> mobsByChunk = new HashMap<>();

        for (Mob mob : mobCollection) {
            String chunkKey = getChunkKey(mob.getChunkX(), mob.getChunkY());

            if (!mobsByChunk.containsKey(chunkKey)) {
                mobsByChunk.put(chunkKey, new ArrayList<>());
            }
            mobsByChunk.get(chunkKey).add(mob);
        }

        return mobsByChunk;
    }

    /**
     * Erzeugt einen eindeutigen Schlüssel für die Chunk-Position
     */
    private static String getChunkKey(int chunkX, int chunkY) {
        return chunkX + ":" + chunkY;
    }

    /**
     * Speichert alle Mobs für einen bestimmten Chunk in der entsprechenden Regionsdatei
     */
    private static void saveMobsForChunk(MobRegionManager mobRegionManager, int chunkX, int chunkY, List<Mob> mobsToSave) {
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
                // Prüfe, ob es sich um ein JSON-Array handelt
                if (existingJson.startsWith("[") && existingJson.endsWith("]")) {
                    allMobData = gson.fromJson(existingJson, MOB_LIST_TYPE);
                } else {
                    // Es handelt sich um einen einzelnen Mob
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

        // Gruppiere Mobs nach Chunk für besseres Speichern
        MobRegionManager mobRegionManager = new MobRegionManager(worldDir);
        Map<String, List<Mob>> mobsByChunk = groupMobsByChunk(mobs.values());

        // Speichere alle Mobs für jeden Chunk
        for (Map.Entry<String, List<Mob>> entry : mobsByChunk.entrySet()) {
            String[] parts = entry.getKey().split(":");
            int chunkX = Integer.parseInt(parts[0]);
            int chunkY = Integer.parseInt(parts[1]);
            List<Mob> chunkMobs = entry.getValue();

            if (!chunkMobs.isEmpty()) {
                saveMobsForChunk(mobRegionManager, chunkX, chunkY, chunkMobs);
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
        PlayerData playerData = new PlayerData(uuid, player.getX(), player.getY(), player.getInventory().serializeHotbar(), player.getInventory().serializeMain(), false, (int) player.getEnergyStatus().getCurrent(), (int) player.getHealthStatus().getCurrent(), player.getHealthStatus().getMax(), player.getEnergyStatus().getMax(), player.serializeEffects());
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

        // Gruppiere Mobs nach Chunk für besseres Speichern
        MobRegionManager mobRegionManager = new MobRegionManager(worldDir);
        Map<String, List<Mob>> mobsByChunk = groupMobsByChunk(mobs.values());

        // Speichere alle Mobs für jeden Chunk
        for (Map.Entry<String, List<Mob>> entry : mobsByChunk.entrySet()) {
            String[] parts = entry.getKey().split(":");
            int chunkX = Integer.parseInt(parts[0]);
            int chunkY = Integer.parseInt(parts[1]);
            List<Mob> chunkMobs = entry.getValue();

            if (!chunkMobs.isEmpty()) {
                saveMobsForChunk(mobRegionManager, chunkX, chunkY, chunkMobs);
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
                                int mobChunkX = regionX * MobRegionManager.REGION_SIZE + localX;
                                int mobChunkY = regionY * MobRegionManager.REGION_SIZE + localY;
                                boolean inRangeOfPlayer = false;

                                if (singlePlayerData != null)
                                {
                                    inRangeOfPlayer = Math.abs(mobChunkX - Globals.toChunkCoords(singlePlayerData.getPlayerX())) <= AbstractTileMap.MOB_DISTANCE &&
                                        Math.abs(mobChunkY - Globals.toChunkCoords(singlePlayerData.getPlayerY())) <= AbstractTileMap.MOB_DISTANCE;
                                }
                                else
                                {
                                    inRangeOfPlayer = Math.abs(mobChunkX) <= AbstractTileMap.MOB_DISTANCE &&
                                        Math.abs(mobChunkY) <= AbstractTileMap.MOB_DISTANCE;
                                }

                                if (inRangeOfPlayer)
                                {
                                    byte[] mobData = mobRegionFile.readMobs(localX, localY);
                                    if (mobData != null)
                                    {
                                        String mobJson = new String(mobData, StandardCharsets.UTF_8);

                                        // Prüfe, ob es sich um ein JSON-Array handelt
                                        if (mobJson.startsWith("[") && mobJson.endsWith("]")) {
                                            // Es handelt sich um eine Liste von Mobs
                                            List<MobIO.MobData> mobDataList = gson.fromJson(mobJson, MOB_LIST_TYPE);

                                            // Erstelle alle Mobs in der Liste
                                            for (int i = 0; i < mobDataList.size(); i++) {
                                                String singleMobJson = gson.toJson(mobDataList.get(i));
                                                Mob mob = MobIO.deserializeFromJson(singleMobJson, physicsWorld);
                                                loadedMobs.add(mob);
                                            }
                                        } else {
                                            // Es handelt sich um einen einzelnen Mob
                                            Mob mob = MobIO.deserializeFromJson(mobJson, physicsWorld);
                                            loadedMobs.add(mob);
                                        }
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
