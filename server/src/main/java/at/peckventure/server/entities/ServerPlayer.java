package at.peckventure.server.entities;

import at.peckventure.Globals;
import at.peckventure.entities.Player;
import at.peckventure.entities.mob.Mob;
import at.peckventure.entities.mob.MobRegistry;
import at.peckventure.inventory.item.Item;
import at.peckventure.server.GameServer;
import at.peckventure.world.Box2DOperationManager;
import com.badlogic.gdx.physics.box2d.World;
import com.esotericsoftware.kryonet.Connection;

public class ServerPlayer extends Player
{
    private String uuid;

    private Connection connection;

    private String username;

    public ServerPlayer(World world, float x, float y, String uuid, Connection connection, String username)
    {
        super(world, x, y);
        this.uuid = uuid;
        this.connection = connection;
        this.username = username;
    }

    @Override
    protected void handleInput(float delta)
    {
    }

    @Override
    public void dropItemOutside(Item item, int amount)
    {

        Mob mob = MobRegistry.createMob("item", Globals.physicsWorld, this.getX(), this.getY() + 40, item);
        float dropSpeed = 20f;
        float angle = this.getRotation();
        float vx = com.badlogic.gdx.math.MathUtils.cosDeg(angle) * dropSpeed;
        float vy = com.badlogic.gdx.math.MathUtils.sinDeg(angle) * dropSpeed;
        Box2DOperationManager.queueOperation(() -> {
            if (mob.getBody() != null)
                mob.getBody().setLinearVelocity(vx, vy);
        });

    }

    public String getUuid()
    {
        return uuid;
    }

    public Connection getConnection()
    {
        return connection;
    }

    public static ServerPlayer findPlayerUUID(String uuid)
    {
        for (ServerPlayer player : GameServer.instance.players)
        {

            if (player.getUuid().equals(uuid))
            {
                return player;
            }
        }
        return null;
    }

    public static ServerPlayer findPlayerName(String name)
    {
        for (ServerPlayer player : GameServer.instance.players)
        {
            if (player.getName().equals(name))
            {
                return player;
            }
        }
        return null;
    }

    public static ServerPlayer findPlayer(Connection connection)
    {
        for (ServerPlayer player : GameServer.instance.players)
        {
            if (player.getConnection().equals(connection))
            {
                return player;
            }
        }
        return null;
    }

    public String getUsername()
    {
        return username;
    }
}
