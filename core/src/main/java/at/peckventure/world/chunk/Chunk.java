package at.peckventure.world.chunk;

import at.peckventure.entities.Player;
import at.peckventure.world.block.Block;
import com.badlogic.gdx.graphics.g2d.Batch;

import java.util.Objects;

public class Chunk
{
    public static final int CHUNK_SIZE = 16;
    private Block[][] blocks;

    private int chunkX, chunkY;

    public Chunk(int chunkX, int chunkY)
    {
        blocks = new Block[CHUNK_SIZE][CHUNK_SIZE];
        this.chunkX = chunkX;
        this.chunkY = chunkY;
    }

    public void setBlock(int x, int y, Block block)
    {
        if (x >= 0 && x < CHUNK_SIZE && y >= 0 && y < CHUNK_SIZE)
        {
            blocks[x][y] = block;
        }
    }

    public Block getBlock(int x, int y)
    {
        if (x >= 0 && x < CHUNK_SIZE && y >= 0 && y < CHUNK_SIZE)
        {
            return blocks[x][y];
        }
        return null;
    }

    public int getChunkY()
    {
        return chunkY;
    }

    public int getChunkX()
    {
        return chunkX;
    }


    public void render(Batch batch)
    {
        for (int i = 0; i < CHUNK_SIZE; i++)
        {
            for (int j = 0; j < CHUNK_SIZE; j++)
            {
                if (blocks[i][j] != null)
                {
                    getBlock(i, j).draw(batch);
                }
            }
        }
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == null || getClass() != o.getClass()) return false;
        Chunk chunk = (Chunk) o;
        return chunkX == chunk.chunkX && chunkY == chunk.chunkY;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(chunkX, chunkY);
    }

    /**
     * Entlädt alle Ressourcen der Blöcke in diesem Chunk.
     * Diese Methode sollte aufgerufen werden, bevor der Chunk entladen wird.
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
    }
}
