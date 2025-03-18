package at.peckventure.world;

import at.peckventure.NetworkClient;
import at.peckventure.entities.Player;
import at.peckventure.entities.mob.Mob;
import at.peckventure.multiplayer.NetworkPackets;
import at.peckventure.world.chunk.Chunk;
import com.badlogic.gdx.physics.box2d.World;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class MultiPlayerMap extends AbstractTileMap {
    public MultiPlayerMap(World world) {
        super(world);
    }

    @Override
    public void loadChunksAroundPlayer(Player player)
    {

    }

    @Override
    public void unloadChunksOutsideRenderDistance(Player player) {
        Iterator<Chunk> iterator = loadedChunks.iterator();
        while (iterator.hasNext()) {
            Chunk chunk = iterator.next();
            if (Math.abs(chunk.getChunkX() - player.getChunkX()) > RENDER_DISTANCE + 2 ||
                Math.abs(chunk.getChunkY() - player.getChunkY()) > RENDER_DISTANCE + 2) {
                chunk.dispose();
                iterator.remove();
            }
        }
    }

    @Override
    public void loadMobsAroundPlayer(Player player) {
        for (int x_offset = -MOB_DISTANCE - 1; x_offset <= MOB_DISTANCE; x_offset++) {
            for (int y_offset = -MOB_DISTANCE; y_offset <= MOB_DISTANCE; y_offset++) {
                int targetChunkX = player.getChunkX() + x_offset;
                int targetChunkY = player.getChunkY() + y_offset;
                boolean mobExists = false;
                for (Mob m : at.peckventure.Globals.mobs) {
                    if (m.getChunkX() == targetChunkX && m.getChunkY() == targetChunkY) {
                        mobExists = true;
                        break;
                    }
                }
                if (!mobExists) return;
            }
        }
    }

    @Override
    public void unloadMobsOutsideRenderDistance(Player player) {
        Iterator<Mob> iterator = at.peckventure.Globals.mobs.iterator();
        while (iterator.hasNext()) {
            Mob mob = iterator.next();
            if (Math.abs(mob.getChunkX() - player.getChunkX()) > MOB_DISTANCE + 2 ||
                Math.abs(mob.getChunkY() - player.getChunkY()) > MOB_DISTANCE + 2) {
                mob.dispose();
                mob.remove();
                iterator.remove();
            }
        }
    }

    @Override
    public void updateChunks(Player player) {
        loadChunksAroundPlayer(player);
        unloadChunksOutsideRenderDistance(player);
        loadMobsAroundPlayer(player);
        unloadMobsOutsideRenderDistance(player);
    }

    @Override
    public void dispose() {
        stopChunkUpdateThread();
        for (Chunk chunk : loadedChunks) {
            chunk.dispose();
        }
        loadedChunks.clear();
    }

    public void loadChunksAroundPlayers(List<Player> players) {
        for (Player player : players) {
            loadChunksAroundPlayer(player);
        }
    }

    public void unloadChunksOutsideRenderDistance(List<Player> players) {
        Iterator<Chunk> iterator = loadedChunks.iterator();
        while (iterator.hasNext()) {
            Chunk chunk = iterator.next();
            boolean keep = false;
            for (Player player : players) {
                if (Math.abs(chunk.getChunkX() - player.getChunkX()) <= RENDER_DISTANCE + 2 &&
                    Math.abs(chunk.getChunkY() - player.getChunkY()) <= RENDER_DISTANCE + 2) {
                    keep = true;
                    break;
                }
            }
            if (!keep) {
                chunk.dispose();
                iterator.remove();
            }
        }
    }


    public void addLoadedChunk(Chunk chunk) {
        loadedChunks.add(chunk);
    }
}
