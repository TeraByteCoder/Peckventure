package at.peckventure.world;

import at.peckventure.entities.Player;
import at.peckventure.entities.mob.Mob;
import at.peckventure.world.chunk.Chunk;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.physics.box2d.World;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractTileMap {
    protected final World physicsWorld;
    protected final Set<Chunk> loadedChunks = ConcurrentHashMap.newKeySet();
    public static final int RENDER_DISTANCE = 3;
    public static final int MOB_DISTANCE = RENDER_DISTANCE - 2;
    protected Thread chunkUpdateThread;
    protected volatile boolean running = false;

    public AbstractTileMap(World world) {
        this.physicsWorld = world;
    }

    public void render(Batch batch) {
        for (Chunk chunk : loadedChunks) {
            chunk.render(batch);
        }
    }

    public Set<Chunk> getLoadedChunks() {
        return loadedChunks;
    }

    public abstract void loadChunksAroundPlayer(Player player);
    public abstract void unloadChunksOutsideRenderDistance(Player player);
    public abstract void loadMobsAroundPlayer(Player player);
    public abstract void unloadMobsOutsideRenderDistance(Player player);
    public abstract void updateChunks(Player player);
    public abstract void dispose();

    public void startChunkUpdateThread(Player player) {
        if (chunkUpdateThread != null && chunkUpdateThread.isAlive()) return;
        running = true;
        chunkUpdateThread = new Thread(() -> {
            while (running) {
                updateChunks(player);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        chunkUpdateThread.setDaemon(true);
        chunkUpdateThread.start();
    }

    public void stopChunkUpdateThread() {
        running = false;
        if (chunkUpdateThread != null) {
            try {
                chunkUpdateThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
