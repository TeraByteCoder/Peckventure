package at.peckventure.world;

import at.peckventure.Globals;
import at.peckventure.entities.Player;
import at.peckventure.entities.mob.Mob;
import at.peckventure.entities.mob.MobRegistry;
import at.peckventure.inventory.ItemRegistry;
import at.peckventure.multiplayer.NetworkPackets;
import at.peckventure.world.chunk.Chunk;
import com.badlogic.gdx.physics.box2d.World;

import java.util.*;

public class MultiPlayerMap extends AbstractTileMap
{
    public MultiPlayerMap(World world)
    {
        super(world);
    }

    @Override
    public void loadChunksAroundPlayer(Player player)
    {

    }

    @Override
    public void unloadChunksOutsideRenderDistance(Player player)
    {
        Iterator<Chunk> iterator = loadedChunks.iterator();
        while (iterator.hasNext())
        {
            Chunk chunk = iterator.next();
            if (Math.abs(chunk.getChunkX() - player.getChunkX()) > RENDER_DISTANCE + 2 ||
                Math.abs(chunk.getChunkY() - player.getChunkY()) > RENDER_DISTANCE + 2)
            {
                chunk.dispose();
                iterator.remove();
            }
        }
    }

    public void updateMobs(NetworkPackets.MobUpdatePacket packet)
    {
        // Erstelle ein Set, um die IDs der im Paket vorhandenen Mobs zu verfolgen
        Set<Integer> updatedMobIds = new HashSet<>();
        ArrayList<NetworkPackets.SingleMobUpdatePacket> mobUpdates = packet.mobUpdates;

        for (NetworkPackets.SingleMobUpdatePacket mobUpdate : mobUpdates)
        {
            updatedMobIds.add(mobUpdate.umid);
            if (Globals.mobs.containsKey(mobUpdate.umid))
            {
                // Mob existiert bereits, daher aktualisiere seine Position
                Mob mob = Globals.mobs.get(mobUpdate.umid);
                Box2DOperationManager.queueOperation(() ->
                {
                    mob.setTargetPosition(mobUpdate.x, mobUpdate.y);
                    mob.setDirection(mobUpdate.direction);
                });

            } else
            {
                // Codeabschnitt 1: Mob ist noch nicht im Set, also erstellen und hinzufügen
                Mob mob = MobRegistry.createMobObject(mobUpdate.mobid, Globals.physicsWorld, mobUpdate.x, mobUpdate.y, ItemRegistry.createItem(mobUpdate.extraItem));
                if (mob != null)
                {
                    mob.setMovementDisabled(true);
                    mob.setPosition(mobUpdate.x, mobUpdate.y);
                    mob.setTargetPosition(mobUpdate.x, mobUpdate.y);
                    mob.setDirection(mobUpdate.direction);
                }

                Globals.mobs.put(mobUpdate.umid, mob);
            }
        }

        // Codeabschnitt 2: Mobs, die im Set sind, aber nicht im Paket enthalten waren
        // Beispiel: Entferne Mobs, die nicht aktualisiert wurden
        Iterator<Map.Entry<Integer, Mob>> iterator = Globals.mobs.entrySet().iterator();
        while (iterator.hasNext())
        {
            Map.Entry<Integer, Mob> entry = iterator.next();
            if (!updatedMobIds.contains(entry.getKey()))
            {
                entry.getValue().remove();
                entry.getValue().dispose();
                iterator.remove();
            }
        }
    }


    @Override
    public void updateChunks(Player player)
    {
        loadChunksAroundPlayer(player);
        unloadChunksOutsideRenderDistance(player);
    }

    @Override
    public void dispose()
    {
        stopChunkUpdateThread();
        for (Chunk chunk : loadedChunks)
        {
            chunk.dispose();
        }
        loadedChunks.clear();
    }

    public void addLoadedChunk(Chunk chunk)
    {
        loadedChunks.add(chunk);
    }
}
