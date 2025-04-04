package at.peckventure.menu;

import at.peckventure.Globals;
import at.peckventure.NetworkClient;
import at.peckventure.entities.ControlledPlayer;
import at.peckventure.entities.Player;
import at.peckventure.entities.RemotePlayer;
import at.peckventure.inventory.InventoryUI;
import at.peckventure.inventory.MultiplayerInventoryManager;
import at.peckventure.multiplayer.NetworkPackets;
import at.peckventure.world.Box2DOperationManager;
import at.peckventure.world.MultiPlayerMap;
import at.peckventure.world.block.Block;
import at.peckventure.world.chunk.ChunkIO;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.FrameworkMessage;
import com.esotericsoftware.kryonet.Listener;

import java.util.HashMap;
import java.util.Map;

public class MultiPlayerGameScreen extends GameScreen
{
    private Map<String, RemotePlayer> players = new HashMap<>();
    private MultiPlayerMap tilemap;


    private String serverHost;
    private int serverPort;
    private boolean chunksLoaded = false;
    private static final int DEFAULT_PORT = 4242;

    public MultiPlayerGameScreen(Game game, String serverAddress)
    {
        super(game);
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
    }

    @Override
    public void show()
    {
        super.show();
        tilemap = new MultiPlayerMap(physicsWorld);
        stage.addActor(player);


        inventoryUI = new InventoryUI(uiStage, new MultiplayerInventoryManager());


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
                        ControlledPlayer.getInstance().deserializeEffects(packet.effects.effects);


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
                } else if (object instanceof NetworkPackets.PlayerStatusUpdatePacket)
                {
                    ControlledPlayer.getInstance().getHealthStatus().setCurrent(((NetworkPackets.PlayerStatusUpdatePacket) object).health);
                    ControlledPlayer.getInstance().getEnergyStatus().setCurrent(((NetworkPackets.PlayerStatusUpdatePacket) object).energy);
                } else if (object instanceof NetworkPackets.ServerPositionChangePacket)
                {
                    NetworkPackets.ServerPositionChangePacket packet = (NetworkPackets.ServerPositionChangePacket) object;

                    Box2DOperationManager.queueOperation(() ->
                    {
                        float angle = ControlledPlayer.getInstance().getBody().getAngle();
                        ControlledPlayer.getInstance().getBody().setTransform((packet.x / Block.BLOCK_SIZE), (packet.y / Block.BLOCK_SIZE), angle);
                    });
                } else if (object instanceof NetworkPackets.EffectUpdatePacket)
                {
                    NetworkPackets.EffectUpdatePacket packet = (NetworkPackets.EffectUpdatePacket) object;

                    Gdx.app.postRunnable(() ->
                    {
                        ControlledPlayer.getInstance().deserializeEffects(packet.effects);
                    });
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
        backgroundStage.draw();
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
        packet.energy = player.getEnergyStatus().getCurrent();
        NetworkClient.getInstance().sendUDP(packet);
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

}
