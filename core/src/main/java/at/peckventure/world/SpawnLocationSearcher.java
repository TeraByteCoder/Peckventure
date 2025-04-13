package at.peckventure.world;

import at.peckventure.Globals;
import at.peckventure.world.block.GrassBlock;
import at.peckventure.world.chunk.Chunk;

public class SpawnLocationSearcher
{
    public static int getValidY(int x)
    {
        int chunkX = x / Chunk.CHUNK_SIZE;
        int inchunkX = Math.abs(x % Chunk.CHUNK_SIZE); // Handle negative x values

        System.out.println("Searching for spawn at block X: " + x + " (Chunk: " + chunkX + ", InChunk: " + inchunkX + ")");

        for(Chunk chunk: Globals.tileMap.getLoadedChunks())
        {
            if(chunkX == chunk.getChunkX())
            {
                // Start from the top of the chunk and work down to find first valid position
                for(int i = Chunk.CHUNK_SIZE - 1; i > 0; i--) // Start from top, stop at 1 to avoid index -1
                {
                    // Check if current block is empty AND block below is a GrassBlock
                    if(chunk.getBlock(inchunkX, i) == null &&
                        i > 0 && chunk.getBlock(inchunkX, i-1) instanceof GrassBlock)
                    {
                        int worldY = chunk.getChunkY() * Chunk.CHUNK_SIZE + i;
                        System.out.println("Found valid spawn at Y: " + worldY + " (Chunk Y: " + chunk.getChunkY() + ", InChunk Y: " + i + ")");
                        return worldY;
                    }
                }
            }
        }

        System.out.println("Didn't find valid spawn Y at block X: " + x);
        return 300; // Default return value when no valid position is found
    }
}
