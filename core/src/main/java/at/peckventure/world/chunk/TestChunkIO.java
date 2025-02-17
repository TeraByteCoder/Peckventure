package at.peckventure.world.chunk;

import at.peckventure.world.chunk.Chunk;
import at.peckventure.world.chunk.ChunkIO;
import at.peckventure.world.block.Block;
import at.peckventure.world.block.BlockFactory;

public class TestChunkIO {
    public static void main(String[] args) {
        // Wir übergeben für den Test einfach null anstelle einer echten Box2D-World,
        // da für die reine Serialisierung/Deserialisierung keine physikalischen Funktionen benötigt werden.
        Object world = null; // Wir nutzen null, da die Blöcke in diesem Test keine Methoden der World aufrufen

        // Erstelle einen Chunk an Koordinate (0,0)
        Chunk chunk = new Chunk(0, 0);

        // Fülle den Chunk vollständig mit DirtBlocks
        for (int x = 0; x < Chunk.CHUNK_SIZE; x++) {
            for (int y = 0; y < Chunk.CHUNK_SIZE; y++) {
                // Berechne die Weltkoordinaten (auch wenn sie hier nur für den Test dienen)
                int worldX = 0 * Chunk.CHUNK_SIZE + x;
                int worldY = 0 * Chunk.CHUNK_SIZE + y;
                // Erzeuge einen DirtBlock über die Factory – dabei wird null als World übergeben
                Block block = BlockFactory.createBlock(BlockFactory.DIRT_ID, null, worldX, worldY);
                chunk.setBlock(x, y, block);
            }
        }

        // Serialisiere den Chunk in ein Byte-Array
        byte[] serializedData = ChunkIO.serialize(chunk);
        System.out.println("Serialized data length: " + serializedData.length);

        // Deserialisiere den Chunk wieder, wiederum mit null als World
        Chunk deserializedChunk = ChunkIO.deserialize(serializedData, null);
        if (deserializedChunk == null) {
            System.out.println("Deserialized chunk is null!");
        } else {
            // Zähle die Anzahl der nicht-leeren Blöcke im deserialisierten Chunk
            int blockCount = 0;
            for (int x = 0; x < Chunk.CHUNK_SIZE; x++) {
                for (int y = 0; y < Chunk.CHUNK_SIZE; y++) {
                    if (deserializedChunk.getBlock(x, y) != null) {
                        blockCount++;
                    }
                }
            }
            System.out.println("Block count in deserialized chunk: " + blockCount);
            // Erwartet werden Chunk.CHUNK_SIZE * Chunk.CHUNK_SIZE Blöcke
            if (blockCount == Chunk.CHUNK_SIZE * Chunk.CHUNK_SIZE) {
                System.out.println("Test successful: All blocks were correctly deserialized.");
            } else {
                System.out.println("Test failed: Some blocks are missing.");
            }
        }
    }
}
