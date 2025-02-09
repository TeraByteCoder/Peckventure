package at.peckventure.world.generator;

import at.peckventure.world.block.DirtBlock;
import at.peckventure.world.block.GrassBlock;
import at.peckventure.world.chunk.Chunk;
import com.badlogic.gdx.physics.box2d.World;

public class WorldGenerator {


    private NoiseGenerator noise;
    private World world;


    public WorldGenerator(long seed, World world) {
        noise = new NoiseGenerator(seed);
        this.world = world;
    }

    /**
     * Erzeugt einen Chunk anhand seiner Chunk-Koordinaten.
     * Die Blockpositionen werden anhand der absoluten Weltkoordinaten bestimmt.
     */
    public Chunk generateChunk(Chunk chunk) {
        for (int x = 0; x < Chunk.CHUNK_SIZE; x++) {
            int worldX = chunk.getChunkX() * Chunk.CHUNK_SIZE + x;
            int height = getHeight(worldX); // Terrain-Höhe in Blockeinheiten
            for (int y = 0; y < Chunk.CHUNK_SIZE; y++) {
                int worldY = chunk.getChunkY() * Chunk.CHUNK_SIZE + y;
                if (worldY < height - 1) {
                    chunk.setBlock(x, y, new DirtBlock(world, worldX, worldY));
                } else if (worldY < height) {
                    chunk.setBlock(x, y, new GrassBlock(world, worldX, worldY));
                }
                // oberhalb des Terrains bleibt null (Luft)
            }
        }
        return chunk;
    }

    /**
     * Berechnet die Terrain-Höhe an der horizontalen Position worldX.
     * Hier: noise.noise(worldX * 0.1) * 15 + 20
     */
    public int getHeight(int worldX) {
        return (int) noise.getTerrainHeight(worldX / 10.0);
    }
}
