package at.peckventure.world;

import at.peckventure.Globals;
import at.peckventure.NetworkClient;
import at.peckventure.entities.ControlledPlayer;
import at.peckventure.entities.Player;
import at.peckventure.entities.RemotePlayer;
import at.peckventure.entities.ServerPlayer;
import at.peckventure.inventory.InventoryUI;
import at.peckventure.menu.MultiPlayer;
import at.peckventure.multiplayer.NetworkPackets;
import at.peckventure.world.block.Block;
import at.peckventure.world.generator.WorldGenerator;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import at.peckventure.chat.ChatUI;
import at.peckventure.InputManager;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

import java.util.*;

public class MultiPlayerGameScreen implements Screen
{
    private ChatUI chatUI;

    private Map<String, RemotePlayer> players = new HashMap<>();
    private final Game game;
    private final String worldName;
    private OrthographicCamera camera;
    private SpriteBatch batch;
    private Player player;
    private Stage stage;
    private Stage uiStage;
    private InfiniteTilemap tilemap;
    private final World physicsWorld;
    private WorldConfig worldConfig;
    private InventoryUI inventoryUI;
    private String serverHost;
    private int serverPort;

    private String uuid = UUID.randomUUID().toString();
    private static final int DEFAULT_PORT = 4242;

    public MultiPlayerGameScreen(Game game, String worldName, String serverAddress)
    {
        this.game = game;
        this.worldName = worldName;
        if (serverAddress.contains(":"))
        {
            int index = serverAddress.indexOf(":");
            serverHost = serverAddress.substring(0, index);
            serverPort = Integer.parseInt(serverAddress.substring(index + 1));
        } else
        {
            serverHost = serverAddress;
            serverPort = DEFAULT_PORT;
        }
        physicsWorld = new World(new Vector2(0, -19.81f), true);
    }

    @Override
    public void show()
    {
        batch = new SpriteBatch();
        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth() / 2f, Gdx.graphics.getHeight() / 2f);
        stage = new Stage(new StretchViewport(Gdx.graphics.getWidth() / 2f, Gdx.graphics.getHeight() / 2f, camera));
        uiStage = new Stage(new ScreenViewport());
        FileHandle worldDir = Gdx.files.absolute(at.peckventure.Const.savesDir + "/" + worldName);
        chatUI = new ChatUI(uiStage);
        InputManager.getInstance().setChatToggle(new InputManager.ChatToggle()
        {
            public void toggleChat()
            {
                chatUI.toggleChat();
            }

            public void cancelChat()
            {
                chatUI.cancelChat();
            }

            public boolean isChatActive()
            {
                return chatUI.isChatActive();
            }
        });
        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(InputManager.getInstance());
        multiplexer.addProcessor(uiStage);
        multiplexer.addProcessor(stage);
        Gdx.input.setInputProcessor(multiplexer);

        WorldIO.LoadedWorld loaded = WorldIO.loadWorld(worldDir, physicsWorld);
        worldConfig = loaded.getConfig();
        WorldGenerator generator = new WorldGenerator(worldConfig.getSeed(), physicsWorld);
        RegionManager regionManager = new RegionManager(worldDir);
        MobRegionManager mobRegionManager = new MobRegionManager(worldDir);
        tilemap = new InfiniteTilemap(physicsWorld, generator, loaded.getLoadedChunks(), regionManager, mobRegionManager);
        float spawnX = worldConfig.getPlayerX();
        float spawnY = worldConfig.getPlayerY();
        if (spawnX == 0 && spawnY == 0)
        {
            spawnX = 0;
            int terrainHeight = generator.getHeight((int) spawnX);
            spawnY = terrainHeight * Block.BLOCK_SIZE + 400;
        }
        player = ControlledPlayer.getInstance(physicsWorld, spawnX, spawnY);
        stage.addActor(player);
        inventoryUI = new InventoryUI(uiStage);
        if (!worldConfig.getInventoryHotbar().isEmpty() && !worldConfig.getInventoryMain().isEmpty())
        {
            ControlledPlayer.getInstance().getInventory().deserialize(worldConfig.getInventoryHotbar(), worldConfig.getInventoryMain());
        }
        Globals.physicsWorld = physicsWorld;
        tilemap.startChunkUpdateThread(player);

        // Initialisiere den NetworkClient als Singleton
        // Hier gehen wir davon aus, dass TCP und UDP den gleichen Port verwenden.
        NetworkClient.init(serverHost, serverPort, serverPort + 222);
        NetworkClient.getInstance().addListener(new Listener()
        {
            @Override
            public void connected(Connection connection)
            {
                NetworkPackets.ServerConnectPacket packet = new NetworkPackets.ServerConnectPacket();
                packet.uuid = uuid;
                NetworkClient.getInstance().sendTCP(packet);
            }

            @Override
            public void disconnected(Connection connection)
            {
                game.setScreen(new MultiPlayer(game));
            }

            @Override
            public void received(Connection connection, Object object)
            {
                if (object instanceof NetworkPackets.PlayerListPacket)
                {
                    final NetworkPackets.PlayerListPacket listPacket = (NetworkPackets.PlayerListPacket) object;
                    Gdx.app.postRunnable(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            System.out.println("Received PlayerListPacket with " + listPacket.players.size() + " players.");
                            for (NetworkPackets.PlayerUpdatePacket updatePacket : listPacket.players)
                            {
                                System.out.println("ListPacket: player uuid=" + updatePacket.uuid + ", x=" + updatePacket.x + ", y=" + updatePacket.y);
                                if (!updatePacket.uuid.equals(uuid))
                                {
                                    if (!players.containsKey(updatePacket.uuid))
                                    {
                                        System.out.println("Creating new remote player for uuid: " + updatePacket.uuid);
                                        RemotePlayer remotePlayer = new RemotePlayer(physicsWorld, updatePacket.x, updatePacket.y);
                                        players.put(updatePacket.uuid, remotePlayer);
                                        stage.addActor(remotePlayer);
                                    } else
                                    {
                                        System.out.println("Updating existing remote player for uuid: " + updatePacket.uuid);
                                        Player remotePlayer = players.get(updatePacket.uuid);
                                        remotePlayer.setX(updatePacket.x);
                                        remotePlayer.setY(updatePacket.y);
                                    }
                                } else
                                {
                                    System.out.println("Ignoring own update in list for uuid: " + updatePacket.uuid);
                                }
                            }
                        }
                    });
                } else if (object instanceof NetworkPackets.PlayerUpdatePacket)
                {
                    final NetworkPackets.PlayerUpdatePacket packet = (NetworkPackets.PlayerUpdatePacket) object;
                    Gdx.app.postRunnable(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            final NetworkPackets.PlayerUpdatePacket packet = (NetworkPackets.PlayerUpdatePacket) object;
                            if (!packet.uuid.equals(uuid))
                            {
                                if (!players.containsKey(packet.uuid))
                                {
                                    System.out.println("Creating new remote player for uuid: " + packet.uuid);
                                    RemotePlayer remotePlayer = new RemotePlayer(physicsWorld, packet.x, packet.y);
                                    players.put(packet.uuid, remotePlayer);
                                    stage.addActor(remotePlayer);
                                } else
                                {
                                    System.out.println("Updating existing remote player for uuid: " + packet.uuid + "   " + packet.x);
                                    players.get(packet.uuid).updateFromPacket(packet);
                                }
                            }
                        }
                    });
                } else if (object instanceof NetworkPackets.ClientConnectPacket)
                {
                    final NetworkPackets.ClientConnectPacket packet = (NetworkPackets.ClientConnectPacket) object;
                    Gdx.app.postRunnable(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            // Aktualisiere den lokalen Spieler, falls nötig
                            player.setX(packet.posx);
                            player.setY(packet.posy);
                        }
                    });
                }
                else if (object instanceof NetworkPackets.ClientDisconnectPacket)
                {
                    NetworkPackets.ClientDisconnectPacket packet = (NetworkPackets.ClientDisconnectPacket) object;
                    Gdx.app.postRunnable(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            Player  player = players.get(packet.uuid);
                            player.remove();
                            players.remove(packet.uuid);
                        }
                    });
                }
            }

            @Override
            public void idle(Connection connection)
            {

            }
        });

        NetworkClient.getInstance().connect(5000); // Timeout z. B. 5000 ms
    }

    @Override
    public void render(float delta)
    {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Box2DOperationManager.processOperations();
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        physicsWorld.step(delta, 6, 2);
        camera.position.set(player.getX() + player.getWidth() / 2, player.getY() + player.getHeight() / 2, 0);
        camera.zoom = 2.0f;
        camera.update();
        stage.act(delta);
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        tilemap.render(batch);
        player.draw(batch);
        batch.end();
        stage.draw();
        uiStage.act(delta);
        uiStage.draw();

        NetworkPackets.PlayerUpdatePacket packet = new NetworkPackets.PlayerUpdatePacket();
        packet.uuid = uuid;
        packet.x = player.getX();
        packet.y = player.getY();
        NetworkClient.getInstance().sendTCP(packet);
    }

    @Override
    public void resize(int width, int height)
    {
        stage.getViewport().update(width, height, true);
        uiStage.getViewport().update(width, height, true);
    }

    @Override
    public void pause()
    {
        WorldIO.saveWorld(worldName, worldConfig, tilemap.getLoadedChunks(), player, ControlledPlayer.getInstance().getInventory());
    }

    @Override
    public void resume()
    {
    }

    @Override
    public void hide()
    {
    }

    @Override
    public void dispose()
    {
        batch.dispose();
        stage.dispose();
        uiStage.dispose();
        physicsWorld.dispose();
        WorldIO.saveWorld(worldName, worldConfig, tilemap.getLoadedChunks(), player, ControlledPlayer.getInstance().getInventory());
        tilemap.dispose();
    }
}
