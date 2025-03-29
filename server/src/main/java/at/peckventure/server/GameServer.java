package at.peckventure.server;

import at.peckventure.Globals;
import at.peckventure.entities.mob.Mob;
import at.peckventure.entities.mob.MobMap;
import at.peckventure.inventory.Inventory;
import at.peckventure.inventory.InventorySlot;
import at.peckventure.inventory.item.Item;
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
import java.util.Collections;
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
        Globals.physicsWorld = physicsWorld;
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
        Globals.mobs = Collections.synchronizedMap(new MobMap());
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
                if(player != null)
                {
                    PlayerData playerData = new PlayerData(player.getUuid(), player.getX(), player.getY(), player.getInventory().serializeHotbar(), player.getInventory().serializeMain(), false);
                    playerData.save(worldFolder);
                    players.remove(player);

                    NetworkPackets.ChatMessagePacket leavemessage = new NetworkPackets.ChatMessagePacket();
                    leavemessage.message = player.getUsername() + "left the Game";
                    server.sendToAllTCP(leavemessage);

                    NetworkPackets.ClientDisconnectPacket packet = new NetworkPackets.ClientDisconnectPacket();
                    packet.uuid = player.getUuid();
                    server.sendToAllUDP(packet);

                }

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

                    if (playerData.getPlayerX() == 0.0 && playerData.getPlayerY() == 0.0 && playerData.getInventoryHotbar().isEmpty() && playerData.getInventoryMain().isEmpty())
                    {

                        players.add(new ServerPlayer(physicsWorld, spawnX, spawnY, packet.uuid, connection, packet.username));
                        connectPacket.posx = spawnX;
                        connectPacket.posy = spawnY;
                        connectPacket.inventoryHotbar = "";
                        connectPacket.inventoryMain = "";
                    } else
                    {
                        ServerPlayer player = new ServerPlayer(physicsWorld, playerData.getPlayerX(), playerData.getPlayerY(), packet.uuid, connection, packet.username);
                        player.getInventory().deserialize(playerData.getInventoryHotbar(), playerData.getInventoryMain());
                        players.add(player);
                        connectPacket.posx = (int) playerData.getPlayerX();
                        connectPacket.posy = (int) playerData.getPlayerY();
                        connectPacket.inventoryHotbar = playerData.getInventoryHotbar();
                        connectPacket.inventoryMain = playerData.getInventoryMain();

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
                    chatMessagePacket.message = packet.username + " Joined";
                    server.sendToAllTCP(chatMessagePacket);
                } else if (object instanceof NetworkPackets.ChatMessagePacket)
                {
                    NetworkPackets.ChatMessagePacket packet = (NetworkPackets.ChatMessagePacket) object;
                    executor.processChatInput(packet.message, ServerPlayer.findPlayer(connection));

                    ServerPlayer player = ServerPlayer.findPlayer(connection);
                    NetworkPackets.InventoryUpdatePacket updatePacket = new NetworkPackets.InventoryUpdatePacket();
                    updatePacket.hotbarData = player.getInventory().serializeHotbar();
                    updatePacket.mainInventoryData = player.getInventory().serializeMain();
                    connection.sendTCP(updatePacket);
                } else if (object instanceof NetworkPackets.InventoryMovePacket)
                {
                    NetworkPackets.InventoryMovePacket movePacket = (NetworkPackets.InventoryMovePacket) object;
                    ServerPlayer player = ServerPlayer.findPlayer(connection);

                    Inventory inventory = player.getInventory();
                    InventorySlot sourceSlot = inventory.getSlotByIndex(movePacket.fromSlot);
                    InventorySlot targetSlot = inventory.getSlotByIndex(movePacket.toSlot);
                    if (sourceSlot != null || targetSlot != null)
                    {
                        if (sourceSlot.getItem() != null)
                        {
                            Item sourceItem = sourceSlot.getItem();
                            // Wenn der Zielslot leer ist, wird das Item (oder ein Teil davon) verschoben.
                            if (targetSlot.getItem() == null)
                            {
                                if (sourceItem.getStackSize() > movePacket.count)
                                {
                                    // Klone das Item für den Zielslot und reduziere die Anzahl im Quellslot.
                                    Item movedItem = inventory.cloneItem(sourceItem);
                                    movedItem.setStackSize(movePacket.count);
                                    targetSlot.setItem(movedItem);
                                    sourceItem.setStackSize(sourceItem.getStackSize() - movePacket.count);
                                } else
                                {
                                    // Falls der ganze Stack verschoben wird.
                                    targetSlot.setItem(sourceItem);
                                    sourceSlot.setItem(null);
                                }
                                ;
                            } else
                            {
                                // Falls im Zielslot bereits ein Item desselben Typs liegt, versuche die Stacks zu mergen.
                                if (targetSlot.getItem().getId().equals(sourceItem.getId()))
                                {
                                    int availableSpace = Item.MAX_STACK_SIZE - targetSlot.getItem().getStackSize();
                                    if (availableSpace >= 0)
                                    {
                                        int toMove = Math.min(movePacket.count, Math.min(sourceItem.getStackSize(), availableSpace));
                                        targetSlot.getItem().setStackSize(targetSlot.getItem().getStackSize() + toMove);
                                        sourceItem.setStackSize(sourceItem.getStackSize() - toMove);
                                        if (sourceItem.getStackSize() <= 0)
                                        {
                                            sourceSlot.setItem(null);
                                        }
                                    }
                                } else
                                {
                                    // Ansonsten tauschen.
                                    Item temp = targetSlot.getItem();
                                    targetSlot.setItem(sourceItem);
                                    sourceSlot.setItem(temp);
                                }
                            }
                        }
                    }
                    NetworkPackets.InventoryUpdatePacket updatePacket = new NetworkPackets.InventoryUpdatePacket();
                    updatePacket.hotbarData = player.getInventory().serializeHotbar();
                    updatePacket.mainInventoryData = player.getInventory().serializeMain();
                    connection.sendTCP(updatePacket);

                } else if (object instanceof NetworkPackets.ItemDropPacket)
                {
                    NetworkPackets.ItemDropPacket packet = (NetworkPackets.ItemDropPacket) object;
                    ServerPlayer player = ServerPlayer.findPlayer(connection);
                    assert player != null;
                    Inventory inventory = player.getInventory();

                    InventorySlot inventorySlot = inventory.getSlotByIndex(packet.slot);
                    if (inventorySlot != null && inventorySlot.getItem() != null)
                    {
                        Item item = inventorySlot.getItem();
                        if (inventorySlot.getItem() != null && inventorySlot.getItem().getStackSize() >= packet.count)
                        {
                            inventorySlot.setItem(null);
                            player.dropItemOutside(item, packet.count);
                        }
                    }

                    NetworkPackets.InventoryUpdatePacket updatePacket = new NetworkPackets.InventoryUpdatePacket();
                    updatePacket.hotbarData = player.getInventory().serializeHotbar();
                    updatePacket.mainInventoryData = player.getInventory().serializeMain();
                    connection.sendTCP(updatePacket);
                } else if (object instanceof NetworkPackets.PingRequestPacket)
                {
                    connection.sendTCP(new NetworkPackets.PingResponsePacket());
                }

            }

            @Override
            public void idle(Connection connection)
            {
            }
        });
        gameLoopThread = new Thread(() -> {
            long lastTime = System.nanoTime();
            while (running) {
                long now = System.nanoTime();
                float delta = (now - lastTime) / 1_000_000_000f; // delta in Sekunden
                lastTime = now;

                Box2DOperationManager.processOperations();
                physicsWorld.step(1f / 60f, 6, 2);
                tilemap.updateChunksForAllPlayers();
                for (Mob mob : at.peckventure.Globals.mobs.values()) {
                    mob.act(delta);
                }

                // Update der Mobs für alle Spieler
                for (ServerPlayer player : players) {
                    tilemap.updateMobsForPlayer(player);
                }

                try {
                    Thread.sleep(16);
                } catch (InterruptedException e) {
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
