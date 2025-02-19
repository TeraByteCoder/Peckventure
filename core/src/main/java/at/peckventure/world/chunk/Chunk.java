package at.peckventure.world.chunk;

import at.peckventure.entities.Player;
import at.peckventure.entities.mob.Mob;
import at.peckventure.world.block.Block;
import com.badlogic.gdx.graphics.g2d.Batch;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class Chunk {
    public static final int CHUNK_SIZE = 16;
    private Block[][] blocks;
    private int chunkX, chunkY;
    private List<Mob> mobs;

    public Chunk(int chunkX, int chunkY) {
        this.blocks = new Block[CHUNK_SIZE][CHUNK_SIZE];
        this.chunkX = chunkX;
        this.chunkY = chunkY;
        this.mobs = new LinkedList<>();
    }

    public void setBlock(int x, int y, Block block) {
        if (x >= 0 && x < CHUNK_SIZE && y >= 0 && y < CHUNK_SIZE) {
            blocks[x][y] = block;
        }
    }

    public Block getBlock(int x, int y) {
        if (x >= 0 && x < CHUNK_SIZE && y >= 0 && y < CHUNK_SIZE) {
            return blocks[x][y];
        }
        return null;
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkY() {
        return chunkY;
    }

    public void render(Batch batch) {
        for (int i = 0; i < CHUNK_SIZE; i++) {
            for (int j = 0; j < CHUNK_SIZE; j++) {
                if (blocks[i][j] != null) {
                    blocks[i][j].draw(batch);
                }
            }
        }
        // Rendere alle Mobs, die in diesem Chunk liegen:
        for (Mob mob : mobs) {
            if (mob != null) {
                mob.draw(batch, 1);
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Chunk chunk = (Chunk) o;
        return chunkX == chunk.chunkX && chunkY == chunk.chunkY;
    }

    @Override
    public int hashCode() {
        return Objects.hash(chunkX, chunkY);
    }

    /**
     * Gibt alle Ressourcen der Blocks und Mobs in diesem Chunk frei.
     */
    public void dispose() {
        for (int i = 0; i < CHUNK_SIZE; i++) {
            for (int j = 0; j < CHUNK_SIZE; j++) {
                if (blocks[i][j] != null) {
                    blocks[i][j].dispose();
                    blocks[i][j] = null;
                }
            }
        }
        for (Mob mob : mobs) {
            mob.dispose();
        }
        mobs.clear();
    }

    // Methoden für Mobs:
    public void addMob(Mob mob) {
        mobs.add(mob);
    }

    public List<Mob> getMobs() {
        return mobs;
    }

    /**
     * Prüft, ob mindestens ein GrassBlock in diesem Chunk vorhanden ist.
     */
    public boolean containsGrass() {
        for (int i = 0; i < CHUNK_SIZE; i++) {
            for (int j = 0; j < CHUNK_SIZE; j++) {
                Block block = blocks[i][j];
                if (block != null && block.getClass().getSimpleName().equals("GrassBlock")) {
                    return true;
                }
            }
        }
        return false;
    }
}
