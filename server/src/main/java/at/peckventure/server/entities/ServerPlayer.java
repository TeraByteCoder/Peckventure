package at.peckventure.server.entities;

import at.peckventure.entities.Player;
import at.peckventure.server.GameServer;
import com.badlogic.gdx.physics.box2d.World;
import com.esotericsoftware.kryonet.Connection;

import java.util.Set;

public class ServerPlayer extends Player
{
    private String uuid;

    private Connection connection;
    public ServerPlayer(World world, float x, float y, String uuid, Connection connection) {
        super(world, x, y);
        this.uuid = uuid;
        this.connection = connection;
    }

    @Override
    protected void handleInput(float delta) {
    }

    public String getUuid()
    {
        return uuid;
    }

    public Connection getConnection()
    {
        return connection;
    }

    public static ServerPlayer findPlayer(String uuid)
    {
        for(ServerPlayer player : GameServer.instance.players)
        {
            if (player.getUuid().equals(uuid))
            {
                return player;
            }
        }
        return null;
    }

    public static ServerPlayer findPlayer(Connection connection)
    {
        for(ServerPlayer player : GameServer.instance.players)
        {
            if (player.getConnection().equals(connection))
            {
                return player;
            }
        }
        return null;
    }
}
