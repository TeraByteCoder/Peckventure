package at.peckventure.menu;

import at.peckventure.Globals;
import at.peckventure.InputManager;
import at.peckventure.NetworkClient;
import at.peckventure.chat.ChatUI;
import at.peckventure.chat.MultiPlayerChatExecutor;
import at.peckventure.entities.ControlledPlayer;
import at.peckventure.entities.Player;
import at.peckventure.entities.RemotePlayer;
import at.peckventure.entities.mob.MobMap;
import at.peckventure.inventory.InventoryUI;
import at.peckventure.inventory.MultiplayerInventoryManager;
import at.peckventure.multiplayer.NetworkPackets;
import at.peckventure.ui.EnergyUI;
import at.peckventure.ui.HealthUI;
import at.peckventure.world.Box2DOperationManager;
import at.peckventure.world.MultiPlayerMap;
import at.peckventure.world.WorldConfig;
import at.peckventure.world.block.Block;
import at.peckventure.world.chunk.ChunkIO;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.FrameworkMessage;
import com.esotericsoftware.kryonet.Listener;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MultiPlayerGameScreen implements Screen
{
    private ChatUI chatUI;
    private Map<String, RemotePlayer> players = new HashMap<>();
    private final Game game;
    private OrthographicCamera camera;
    private SpriteBatch batch;
    private Player player;
    private Stage stage;
    private Stage uiStage;
    private MultiPlayerMap tilemap;
    private final World physicsWorld;
    private WorldConfig worldConfig;
    private InventoryUI inventoryUI;

    private HealthUI healthUI;
    private EnergyUI energyUI;
    private String serverHost;
    private int serverPort;
    private boolean chunksLoaded = false;
    private static final int DEFAULT_PORT = 4242;

    public MultiPlayerGameScreen(Game game, String serverAddress)
    {
        this.game = game;
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
        Globals.mobs = Collections.synchronizedMap(new MobMap(stage));
        uiStage = new Stage(new ScreenViewport());
        chatUI = new ChatUI(uiStage, new MultiPlayerChatExecutor());
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
        tilemap = new MultiPlayerMap(physicsWorld);
        player = ControlledPlayer.getInstance(physicsWorld, 0, 0);
        Box2DOperationManager.processOperations();
        stage.addActor(player);

        healthUI = new HealthUI(uiStage, ControlledPlayer.getInstance().getHealthStatus());
        energyUI = new EnergyUI(uiStage, ControlledPlayer.getInstance().getEnergyStatus());

        inventoryUI = new InventoryUI(uiStage, new MultiplayerInventoryManager());
        Globals.physicsWorld = physicsWorld;
        NetworkClient.init(serverHost, serverPort, serverPort + 222);

        NetworkClient.getInstance().addListener(new Listener()
        {
            @Override
            public void connected(Connection connection)
            {
                NetworkPackets.ServerConnectPacket packet = new NetworkPackets.ServerConnectPacket();
                packet.uuid = Globals.uuid;
                packet.username = Globals.username;
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
                if (object instanceof NetworkPackets.PlayerUpdatePacket)
                {
                    final NetworkPackets.PlayerUpdatePacket packet = (NetworkPackets.PlayerUpdatePacket) object;
                    Gdx.app.postRunnable(() ->
                    {
                        if (!packet.uuid.equals(Globals.uuid))
                        {
                            if (!players.containsKey(packet.uuid))
                            {
                                RemotePlayer remotePlayer = new RemotePlayer(physicsWorld, packet.x, packet.y);
                                players.put(packet.uuid, remotePlayer);
                                stage.addActor(remotePlayer);
                            } else
                            {
                                players.get(packet.uuid).updateFromPacket(packet);
                            }
                        }
                    });
                } else if (object instanceof NetworkPackets.ClientConnectPacket)
                {
                    final NetworkPackets.ClientConnectPacket packet = (NetworkPackets.ClientConnectPacket) object;
                    final NetworkPackets.PlayerListPacket listPacket = packet.playerList;
                    Gdx.app.postRunnable(() ->
                    {
                        ControlledPlayer.getInstance().setX(packet.posx);
                        ControlledPlayer.getInstance().setY(packet.posy);
                        ControlledPlayer.getInstance().getBody().setTransform((float) packet.posx / Block.BLOCK_SIZE, (float) packet.posy / Block.BLOCK_SIZE, 0);
                        ControlledPlayer.getInstance().getInventory().deserialize(packet.inventoryHotbar, packet.inventoryMain);
                        ControlledPlayer.getInstance().getHealthStatus().setCurrent(packet.playerStatus.health);
                        ControlledPlayer.getInstance().getEnergyStatus().setCurrent(packet.playerStatus.energy);


                        for (NetworkPackets.PlayerUpdatePacket updatePacket : listPacket.players)
                        {
                            if (!updatePacket.uuid.equals(Globals.uuid))
                            {
                                if (!players.containsKey(updatePacket.uuid))
                                {
                                    RemotePlayer remotePlayer = new RemotePlayer(physicsWorld, updatePacket.x, updatePacket.y);
                                    players.put(updatePacket.uuid, remotePlayer);
                                    stage.addActor(remotePlayer);
                                } else
                                {
                                    players.get(updatePacket.uuid).setX(updatePacket.x);
                                    players.get(updatePacket.uuid).setY(updatePacket.y);
                                }
                            }
                        }
                    });
                } else if (object instanceof NetworkPackets.ClientDisconnectPacket)
                {
                    NetworkPackets.ClientDisconnectPacket packet = (NetworkPackets.ClientDisconnectPacket) object;
                    Gdx.app.postRunnable(() ->
                    {
                        Player p = players.get(packet.uuid);
                        p.remove();
                        players.remove(packet.uuid);
                    });
                } else if (object instanceof NetworkPackets.ChunkDataPacket)
                {
                    NetworkPackets.ChunkDataPacket packet = (NetworkPackets.ChunkDataPacket) object;
                    Gdx.app.postRunnable(() ->
                    {
                        tilemap.addLoadedChunk(ChunkIO.deserialize(packet.data, physicsWorld));
                    });
                } else if (object instanceof NetworkPackets.ChatMessagePacket)
                {
                    NetworkPackets.ChatMessagePacket packet = (NetworkPackets.ChatMessagePacket) object;
                    Gdx.app.postRunnable(() ->
                        {
                            chatUI.addMessage(packet.message);
                        }
                    );
                } else if (object instanceof NetworkPackets.InventoryUpdatePacket)
                {
                    NetworkPackets.InventoryUpdatePacket packet = (NetworkPackets.InventoryUpdatePacket) object;
                    Gdx.app.postRunnable(() ->
                        {
                            ControlledPlayer.getInstance().getInventory().deserialize(packet.hotbarData, packet.mainInventoryData);
                        }

                    );
                } else if (object instanceof NetworkPackets.MobUpdatePacket)
                {
                    tilemap.updateMobs((NetworkPackets.MobUpdatePacket) object);
                } else if (object instanceof NetworkPackets.PlayerStatusPacket)
                {
                    ControlledPlayer.getInstance().getHealthStatus().setCurrent(((NetworkPackets.PlayerStatusPacket) object).health);
                    ControlledPlayer.getInstance().getEnergyStatus().setCurrent(((NetworkPackets.PlayerStatusPacket) object).energy);
                }


                // Unbekannte Pakete anzeigen
                else if (!(object instanceof FrameworkMessage.KeepAlive))
                {

                    System.out.println("Unknown packet type: " + object.getClass().getSimpleName());
                }
            }

            @Override
            public void idle(Connection connection)
            {
            }
        });
        NetworkClient.getInstance().connect(5000);

        tilemap.startChunkUpdateThread(player);
        Box2DOperationManager.processOperations();
    }

    @Override
    public void render(float delta)
    {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Box2DOperationManager.processOperations();
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
        packet.uuid = Globals.uuid;
        packet.x = player.getX();
        packet.y = player.getY();
        packet.rotation = ControlledPlayer.getInstance().isFacingRight();
        packet.time = System.currentTimeMillis();
        NetworkClient.getInstance().sendUDP(packet);
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
        tilemap.dispose();
    }
}
