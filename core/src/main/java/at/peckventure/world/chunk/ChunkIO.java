package at.peckventure.world.chunk;

import at.peckventure.world.block.Block;
import at.peckventure.world.block.BlockRegistry;
import com.badlogic.gdx.physics.box2d.World;

import java.io.*;

public class ChunkIO
{

    // Serialisiert einen Chunk in ein Byte-Array
    public static byte[] serialize(Chunk chunk)
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DataOutputStream dos = new DataOutputStream(baos))
        {
            dos.writeInt(chunk.getChunkX());
            dos.writeInt(chunk.getChunkY());
            // Speichere alle Blockdaten (Zeilenweise)
            for (int x = 0; x < Chunk.CHUNK_SIZE; x++)
            {
                for (int y = 0; y < Chunk.CHUNK_SIZE; y++)
                {
                    Block block = chunk.getBlock(x, y);
                    if (block != null)
                    {
                        dos.writeBoolean(true);
                        int blockId = BlockRegistry.getBlockId(block);
                        dos.writeInt(blockId);
                    } else
                    {
                        dos.writeBoolean(false);
                    }
                }
            }

        } catch (IOException e)
        {
            e.printStackTrace();
        }
        return baos.toByteArray();
    }

    // Deserialisiert einen Chunk aus einem Byte-Array
    public static Chunk deserialize(byte[] data, World world)
    {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        try (DataInputStream dis = new DataInputStream(bais))
        {
            int chunkX = dis.readInt();
            int chunkY = dis.readInt();
            Chunk chunk = new Chunk(chunkX, chunkY);
            for (int x = 0; x < Chunk.CHUNK_SIZE; x++)
            {
                for (int y = 0; y < Chunk.CHUNK_SIZE; y++)
                {
                    boolean hasBlock = dis.readBoolean();
                    if (hasBlock)
                    {
                        int blockId = dis.readInt();
                        // Umrechnung von Chunk- zu Weltkoordinaten:
                        int worldX = chunkX * Chunk.CHUNK_SIZE + x;
                        int worldY = chunkY * Chunk.CHUNK_SIZE + y;
                        Block block = BlockRegistry.createBlock(blockId, world, worldX, worldY);
                        chunk.setBlock(x, y, block);
                    }
                }
            }
            return chunk;
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        return null;
    }

}
