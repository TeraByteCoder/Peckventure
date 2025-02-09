package at.peckventure.world;

import at.peckventure.entities.Player;
import at.peckventure.world.chunk.Chunk;

import at.peckventure.world.generator.WorldGenerator;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.physics.box2d.World;


import java.util.HashSet;
import java.util.Set;

public class InfiniteTilemap {
    public static final int RENDER_DISTANCE = 2;
    private WorldGenerator worldGenerator;
    private Set<Chunk> loadetChunks = new HashSet<>();

    public InfiniteTilemap(World world, long seed) {
        worldGenerator = new WorldGenerator(seed, world);
    }

    public void render(Batch batch, Player player) {
        loadChunksAroundPlayer(player);
        unloadChunksAroundPlayer(player);
        //getCollision(player);
        for (Chunk chunk : loadetChunks) {
            chunk.render(batch);
        }
    }

    public WorldGenerator getWorldGenerator()
    {
        return worldGenerator;
    }

    /**
     * Liefert alle Blöcke, die mit der gegebenen Region kollidieren.
     *     public List<Block> getBlocksInRegion(Rectangle region) {
     *         return chunkManager.getBlocksInRegion(region);
     *     }
     */

    public void loadChunksAroundPlayer(Player player)
    {
        for (int x_offset = -RENDER_DISTANCE; x_offset <= RENDER_DISTANCE; x_offset++)
        {
            for(int y_offset = -RENDER_DISTANCE; y_offset <= RENDER_DISTANCE; y_offset++)
            {
                Chunk chunk = new Chunk(player.getChunkX() + x_offset, player.getChunkY() + y_offset);
                if(!loadetChunks.contains(chunk))
                {
                    worldGenerator.generateChunk(chunk);
                    loadetChunks.add(chunk);
                }
            }
        }
    }

    public void unloadChunksAroundPlayer(Player player)
    {
        loadetChunks.removeIf(chunk -> Math.sqrt(Math.pow(chunk.getChunkX() - player.getChunkX(), 2)) > RENDER_DISTANCE+1 || Math.sqrt(Math.pow(chunk.getChunkY() - player.getChunkY(), 2)) > RENDER_DISTANCE+1);
    }
/*
    public void getCollision(Player player)
    {
        for(Chunk chunk : loadetChunks)
        {
            chunk.checkCollision(player    );
        }
    }

 */
}
