package at.peckventure.world.generator;

import at.peckventure.world.block.DirtBlock;
import at.peckventure.world.block.GrassBlock;
import at.peckventure.world.block.GrassRamp;
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
            int currentHeight = getHeight(worldX);    // Terrainhöhe an der aktuellen Spalte
            int leftHeight = getHeight(worldX - 1);     // Terrainhöhe in der linken Nachbarspalte
            int rightHeight = getHeight(worldX + 1);    // Terrainhöhe in der rechten Nachbarspalte

            for (int y = 0; y < Chunk.CHUNK_SIZE; y++) {
                int worldY = chunk.getChunkY() * Chunk.CHUNK_SIZE + y;

                // Unterhalb der Oberfläche als DirtBlock
                if (worldY < currentHeight - 1) {
                    chunk.setBlock(x, y, new DirtBlock(world, worldX, worldY));
                }
                // Oberster Block der Spalte (sichtbare Oberfläche)
                else if (worldY == currentHeight - 1) {
                    boolean leftLower = leftHeight < currentHeight;
                    boolean rightLower = rightHeight < currentHeight;

                    // Wenn beide Seiten "Luft" haben:
                    if (leftLower && rightLower) {
                        // Wir generieren **keinen** obersten Block,
                        // sondern ersetzen den darunterliegenden Block (normalerweise Dirt) durch Grass.
                        if (y - 1 >= 0) {
                            chunk.setBlock(x, y - 1, new GrassBlock(world, worldX, worldY - 1));
                        }
                        // Der aktuelle Block (an y == currentHeight - 1) bleibt leer (Luft).
                    }
                    // Falls nur eine Seite niedriger ist, wird ein GrassTilted-Block gesetzt:
                    else if (leftLower || rightLower) {
                        boolean leftRamp;
                        if (leftLower && !rightLower) {
                            leftRamp = false;
                        } else if (rightLower && !leftLower) {
                            leftRamp = true;
                        } else {
                            // Beide Nachbarn sind niedriger – hier wird der stärkere Höhenunterschied berücksichtigt.
                            leftRamp = (currentHeight - leftHeight) < (currentHeight - rightHeight);
                        }
                        chunk.setBlock(x, y, new GrassRamp(world, worldX, worldY, leftRamp));
                    }
                    // Ansonsten wird ein normaler GrassBlock gesetzt.
                    else {
                        chunk.setBlock(x, y, new GrassBlock(world, worldX, worldY));
                    }
                }
                // Oberhalb der Oberfläche bleibt der Block leer (Luft)
            }
        }
        return chunk;
    }

    /**
     * Berechnet die Terrain-Höhe an der horizontalen Position worldX.
     * Hier: octaveNoise(worldX * 0.01, 10, 0.5) * 50 + 50
     */
    public int getHeight(int worldX) {
        return (int) (noise.octaveNoise(worldX * 0.01, 10, 0.5) * 30 + 50);
    }

    public boolean hasBlock(int worldX, int worldY) {
        return worldY < getHeight(worldX);
    }
}
