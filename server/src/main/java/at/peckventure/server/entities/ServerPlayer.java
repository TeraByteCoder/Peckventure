package at.peckventure.server.entities;

import at.peckventure.Globals;
import at.peckventure.entities.Player;
import at.peckventure.entities.mob.ItemActor;
import at.peckventure.entities.mob.Mob;
import at.peckventure.entities.mob.MobRegistry;
import at.peckventure.inventory.item.Item;
import at.peckventure.multiplayer.NetworkPackets;
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

    public ServerPlayer(World world, float x, float y, String uuid, Connection connection, String username, int energy, int health, boolean operator)
    {
        super(world, x, y);
        this.uuid = uuid;
        this.connection = connection;
        this.username = username;
        this.operator = operator;
        this.getEnergyStatus().setCurrent(energy);
        this.getHealthStatus().setCurrent(health);
    }

    @Override
    protected void handleInput(float delta)
    {
    }

    @Override
    public void dropItemOutside(Item item, int amount)
    {

        Mob mob = MobRegistry.createMob("item", Globals.physicsWorld, this.getX(), this.getY() + 40, item, amount);
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

    public void peck(float targetX, float targetY)
    {
        // Check if the target is near any other player
        String targetUuid = "";
        float finalTargetX = targetX;
        float finalTargetY = targetY;

        // Search for players that might be near the target position
        for (ServerPlayer p : GameServer.instance.players)
        {
            if (!p.getUuid().equals(this.getUuid()))  // Don't target self
            {
                float playerCenterX = p.getX() + 32; // Assuming player width is 64
                float playerCenterY = p.getY() + 32; // Assuming player height is 64

                // Calculate distance to target
                float distance = (float) Math.sqrt(
                    Math.pow(playerCenterX - targetX, 2) +
                        Math.pow(playerCenterY - targetY, 2)
                );

                // If close enough, consider this player the target
                if (distance < 100)  // Adjust threshold as needed
                {
                    targetUuid = p.getUuid();
                    finalTargetX = playerCenterX;
                    finalTargetY = playerCenterY;
                    break;
                }
            }
        }

        // Determine if the target is a mob
        Mob targetMob = null;
        if (targetUuid.isEmpty()) {
            // Search for mobs near the target position, similar to the client's searchForMobsToAttack method
            final float searchX = targetX;
            final float searchY = targetY;
            final float searchRadius = 100; // Adjust as needed

            Box2DOperationManager.queueOperation(() -> {
                // Find mobs in the area using Box2D query
                // This is simplified, actual implementation will depend on your mob tracking system
                for (Mob mob : Globals.mobs.values()) {
                    if (mob != null && !(mob instanceof ItemActor)) {
                        float mobCenterX = mob.getX() + mob.getWidth()/2;
                        float mobCenterY = mob.getY() + mob.getHeight()/2;
                        float distance = (float) Math.sqrt(
                            Math.pow(mobCenterX - searchX, 2) +
                                Math.pow(mobCenterY - searchY, 2)
                        );

                        if (distance < searchRadius) {
                            // Found a target mob
                            // If you want to apply damage to the mob, do it here
                            mob.onDeath(); // Or apply damage instead of killing
                        }
                    }
                }
            });
        }

        // Create and send the response packet to all clients
        NetworkPackets.PeckResponsePacket response = new NetworkPackets.PeckResponsePacket();
        response.uuid = this.getUuid();
        response.targetUuid = targetUuid;
        response.targetX = finalTargetX;
        response.targetY = finalTargetY;

        GameServer.instance.getServer().sendToAllTCP(response);
    }

    public String getUsername()
    {
        return username;
    }
}
