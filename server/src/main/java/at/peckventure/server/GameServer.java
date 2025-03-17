package at.peckventure.server;
import at.peckventure.entities.Player;
import at.peckventure.entities.ServerPlayer;
import at.peckventure.multiplayer.Network;
import at.peckventure.world.InfiniteTilemap;
import at.peckventure.world.RegionManager;
import at.peckventure.world.MobRegionManager;
import at.peckventure.world.WorldConfig;
import at.peckventure.world.generator.WorldGenerator;
import at.peckventure.world.WorldIO;
import at.peckventure.world.block.Block;
import at.peckventure.multiplayer.NetworkPackets;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public class GameServer {

    public static GameServer instance;

    private static final String SERVER_FOLDER = "C:\\Users\\Lukas\\Desktop\\peckserver";
    private Server server;

    public Map<String, Player> players = new HashMap<>();
    public Map<Connection, String> connectionuuid = new HashMap<>();
    private World physicsWorld;
    private InfiniteTilemap tilemap;
    private WorldConfig worldConfig;
    private boolean running;
    private Thread gameLoopThread;
    public GameServer()
    {
        instance = this;
        physicsWorld = new World(new Vector2(0, -19.81f), true);
        FileHandle worldFolder = new FileHandle(new File(SERVER_FOLDER + "/world"));
        WorldIO.LoadedWorld loaded = WorldIO.loadWorld(worldFolder, physicsWorld);
        worldConfig = loaded.getConfig();

        WorldGenerator generator = new WorldGenerator(worldConfig.getSeed(), physicsWorld);
        RegionManager regionManager = new RegionManager(worldFolder);
        MobRegionManager mobRegionManager = new MobRegionManager(worldFolder);

        tilemap = new InfiniteTilemap(physicsWorld, generator, loaded.getLoadedChunks(), regionManager, mobRegionManager);

        //Player
        float spawnX = worldConfig.getPlayerX();
        float spawnY = worldConfig.getPlayerY();
        if(spawnX == 0 && spawnY == 0) {
            spawnX = 0;
            int terrainHeight = generator.getHeight((int) spawnX);
            spawnY = terrainHeight * Block.BLOCK_SIZE + 400;
        }
        String ip = null;
        try
        {
            ip = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e)
        {
            e.printStackTrace();
        }
        System.out.println("Server running at " + ip + ":54555");
    }
    public void start() throws IOException {
        running = true;

        server = new Server();
        Network.register(server.getKryo());
        server.start();
        server.bind(54555, 54777);

        //listener
        server.addListener(new Listener() {
            @Override
            public void connected(Connection connection) {
            }


            @Override
            public void disconnected(Connection connection)
            {
                players.remove(connectionuuid.get(connection));
                NetworkPackets.ClientDisconnectPacket packet = new NetworkPackets.ClientDisconnectPacket();
                packet.uuid = connectionuuid.get(connection);
                connectionuuid.remove(connection);
                server.sendToAllUDP(packet);
            }

            public void received(Connection connection, Object object) {
                /*
                if(object instanceof NetworkPackets.ChunkRequestPacket) {
                    NetworkPackets.ChunkRequestPacket packet = (NetworkPackets.ChunkRequestPacket) object;
                    NetworkPackets.ChunkDataPacket dataPacket = new NetworkPackets.ChunkDataPacket();
                    dataPacket.chunkX = packet.chunkX;
                    dataPacket.chunkY = packet.chunkY;
                    dataPacket.data = tilemap.getChunkData(packet.chunkX, packet.chunkY);
                    connection.sendTCP(dataPacket);

                } else if(object instanceof NetworkPackets.ChatMessagePacket) {
                    NetworkPackets.ChatMessagePacket packet = (NetworkPackets.ChatMessagePacket) object;
                    server.sendToAllExceptTCP(connection.getID(), packet);

                } else if(object instanceof NetworkPackets.InventoryUpdatePacket) {
                    NetworkPackets.InventoryUpdatePacket packet = (NetworkPackets.InventoryUpdatePacket) object;
                    server.sendToAllTCP(packet);
                }*/
                if (object instanceof NetworkPackets.PlayerUpdatePacket) {
                    NetworkPackets.PlayerUpdatePacket packet = (NetworkPackets.PlayerUpdatePacket) object;
                    if(players.containsKey(packet.uuid))
                    {
                        GameServer.instance.players.get(packet.uuid).setX(packet.x);
                        GameServer.instance.players.get(packet.uuid).setY(packet.y);

                        server.sendToAllUDP(object);
                    }
                }
                else if (object instanceof NetworkPackets.ServerConnectPacket)
                {
                    NetworkPackets.ServerConnectPacket packet = (NetworkPackets.ServerConnectPacket) object;
                    players.put(packet.uuid, new ServerPlayer(physicsWorld, 0, 10000));
                    connectionuuid.put(connection, packet.uuid);
                    System.out.println(packet.uuid+ "connected");
                    NetworkPackets.ClientConnectPacket connectPacket = new NetworkPackets.ClientConnectPacket();

                    connectPacket.posx = 0;
                    connectPacket.posy = 10000;
                    connection.sendTCP(connectPacket);


                    // Erstelle ein PlayerListPacket und fülle es mit allen aktuellen Spieler-Updates
                    NetworkPackets.PlayerListPacket listPacket = new NetworkPackets.PlayerListPacket();

                    Iterator<Map.Entry<String, Player>> iterator = players.entrySet().iterator();
                    while (iterator.hasNext()) {
                        Map.Entry<String, Player> entry = iterator.next();
                        NetworkPackets.PlayerUpdatePacket updatePacket = new NetworkPackets.PlayerUpdatePacket();
                        updatePacket.uuid = entry.getKey();
                        Player player = entry.getValue();
                        updatePacket.x = player.getX();
                        updatePacket.y = player.getY();
                        listPacket.players.add(updatePacket);
                    }

                    // Sende die Liste via TCP an den neu verbundenen Client
                    connection.sendTCP(listPacket);
                }
            }

            @Override
            public void idle(Connection connection)
            {

            }
        });
        //tilemap.startChunkUpdateThread(player);
        gameLoopThread = new Thread(() -> {
            while(running) {
                physicsWorld.step(1f/60f,6,2);
                try {
                    Thread.sleep(16);
                } catch(InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        gameLoopThread.setDaemon(true);
        gameLoopThread.start();
    }
    public void stopServer() {
        running = false;
        if(gameLoopThread != null) {
            try {
                gameLoopThread.join();
            } catch(InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        tilemap.dispose();
        physicsWorld.dispose();
        if(server != null) {
            server.stop();
        }
    }
}
