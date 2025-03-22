package at.peckventure.server;

import at.peckventure.multiplayer.NetworkPackets;
import at.peckventure.server.chat.ChatExecutor;
import at.peckventure.server.entities.ServerPlayer;
import at.peckventure.server.world.ServerTileMap;
import at.peckventure.world.Box2DOperationManager;
import at.peckventure.world.PlayerData;
import at.peckventure.world.WorldConfig;
import at.peckventure.world.WorldIO;
import at.peckventure.world.block.Block;
import at.peckventure.world.generator.WorldGenerator;
import com.badlogic.gdx.Gdx;
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
import java.util.HashSet;
import java.util.Set;

public class GameServer
{
    public static GameServer instance;
    private static final String SERVER_FOLDER = "C:\\Users\\Lukas\\Desktop\\peckserver";
    Server server;
    public Set<ServerPlayer> players = new HashSet<>();
    private World physicsWorld;
    private ServerTileMap tilemap;
    private WorldConfig worldConfig;
    private boolean running;
    private Thread gameLoopThread;
    private int spawnX;
    private ChatExecutor executor;
    private int spawnY;
    FileHandle worldFolder;

    public GameServer()
    {
        instance = this;
        this.executor = new ChatExecutor();
        physicsWorld = new World(new Vector2(0, -19.81f), true);
        worldFolder = new FileHandle(new File(SERVER_FOLDER + "/world"));
        WorldIO.LoadedWorld loaded = WorldIO.loadWorld(worldFolder, physicsWorld);
        worldConfig = loaded.getConfig();
        WorldGenerator generator = new WorldGenerator(worldConfig.getSeed(), physicsWorld);
        at.peckventure.world.RegionManager regionManager = new at.peckventure.world.RegionManager(worldFolder);
        at.peckventure.world.MobRegionManager mobRegionManager = new at.peckventure.world.MobRegionManager(worldFolder);
        tilemap = new ServerTileMap(physicsWorld, generator, loaded.getLoadedChunks(), regionManager, mobRegionManager);
        spawnX = 0;
        int terrainHeight = generator.getHeight((int) spawnX);
        spawnY = terrainHeight * Block.BLOCK_SIZE + 400;
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

    public void start() throws IOException
    {
        running = true;
        server = new Server(65536, 65536);
        at.peckventure.multiplayer.Network.register(server.getKryo());
        server.start();
        server.bind(54555, 54777);
        server.addListener(new Listener()
        {
            @Override
            public void connected(Connection connection)
            {
            }

            @Override
            public void disconnected(Connection connection)
            {

                ServerPlayer player = ServerPlayer.findPlayer(connection);
                PlayerData playerData = new PlayerData(player.getUuid(), player.getX(), player.getY(), "", "", false);
                playerData.save(worldFolder);
                players.remove(player);

                NetworkPackets.ChatMessagePacket leavemessage = new NetworkPackets.ChatMessagePacket();
                leavemessage.message = player.getUsername() + "left the Game";
                server.sendToAllTCP(leavemessage);

                NetworkPackets.ClientDisconnectPacket packet = new NetworkPackets.ClientDisconnectPacket();
                packet.uuid = player.getUuid();
                server.sendToAllUDP(packet);

            }

            public void received(Connection connection, Object object)
            {

                if (object instanceof NetworkPackets.PlayerUpdatePacket)
                {
                    NetworkPackets.PlayerUpdatePacket packet = (NetworkPackets.PlayerUpdatePacket) object;

                        ServerPlayer player = ServerPlayer.findPlayerUUID(packet.uuid);
                        if (player != null)
                        {
                            Box2DOperationManager.queueOperation(() ->
                            {
                                player.updateFromPacket(packet);
                            });
                            server.sendToAllExceptUDP(connection.getID(), packet);
                        } else
                        {
                            connection.close();
                        }
                } else if (object instanceof NetworkPackets.ServerConnectPacket)
                {

                    NetworkPackets.ServerConnectPacket packet = (NetworkPackets.ServerConnectPacket) object;


                    PlayerData playerData = PlayerData.load(worldFolder, packet.uuid);

                    //connectpacket erstellen
                    NetworkPackets.ClientConnectPacket connectPacket = new NetworkPackets.ClientConnectPacket();

                        if (playerData.getPlayerX() == 0.0 && playerData.getPlayerY() == 0.0)
                        {

                            players.add(new ServerPlayer(physicsWorld, spawnX, spawnY, packet.uuid, connection, packet.username));
                            connectPacket.posx = spawnX;
                            connectPacket.posy = spawnY;

                        } else
                        {

                            players.add(new ServerPlayer(physicsWorld, playerData.getPlayerX(), playerData.getPlayerY(), packet.uuid, connection, packet.username));
                            connectPacket.posx = (int) playerData.getPlayerX();
                            connectPacket.posy = (int) playerData.getPlayerY();

                        }
                        connection.sendTCP(connectPacket);
                        NetworkPackets.PlayerListPacket listPacket = new NetworkPackets.PlayerListPacket();
                        for (ServerPlayer player : players)
                        {
                            NetworkPackets.PlayerUpdatePacket updatePacket = new NetworkPackets.PlayerUpdatePacket();
                            updatePacket.uuid = player.getUuid();
                            updatePacket.x = player.getX();
                            updatePacket.y = player.getY();
                            listPacket.players.add(updatePacket);
                        }
                        connection.sendTCP(listPacket);


                        NetworkPackets.ChatMessagePacket chatMessagePacket = new NetworkPackets.ChatMessagePacket();
                        chatMessagePacket.message = packet.username+" Joined";
                        server.sendToAllTCP(chatMessagePacket);
                } else if (object instanceof NetworkPackets.ChatMessagePacket)
                {
                    NetworkPackets.ChatMessagePacket packet = (NetworkPackets.ChatMessagePacket) object;
                    executor.processChatInput(packet.message, ServerPlayer.findPlayer(connection));
                }

            }

            @Override
            public void idle(Connection connection)
            {
            }
        });
        gameLoopThread = new Thread(() ->
        {
            while (running)
            {
                Box2DOperationManager.processOperations();
                physicsWorld.step(1f / 60f, 6, 2);
                tilemap.updateChunksForAllPlayers();
                try
                {
                    Thread.sleep(16);
                } catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                }
            }
        });
        gameLoopThread.setDaemon(true);
        gameLoopThread.start();
    }

    public void stopServer()
    {
        running = false;
        if (gameLoopThread != null)
        {
            try
            {
                gameLoopThread.join();
            } catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
        }
        tilemap.dispose();
        physicsWorld.dispose();
        if (server != null)
        {
            server.stop();
        }
    }

    public Server getServer()
    {
        return server;
    }
}
