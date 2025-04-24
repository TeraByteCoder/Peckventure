package at.peckventure.world.generator;

import at.peckventure.Globals;
import at.peckventure.world.block.*;
import at.peckventure.world.chunk.Chunk;
import com.badlogic.gdx.physics.box2d.World;

import java.util.Random;

public class WorldGenerator
{

    private final NoiseGenerator noise;
    private final World world;
    private final Random random;

    private int lastTreeX;


    public WorldGenerator(long seed, World world)
    {
        noise = new NoiseGenerator(seed);
        this.random = new Random(seed);
        this.world = world;

        this.lastTreeX = 5;

    }

    /**
     * Erzeugt einen Chunk anhand seiner Chunk-Koordinaten.
     * Die Blockpositionen werden anhand der absoluten Weltkoordinaten bestimmt.
     */
    public Chunk generateChunk(Chunk chunk, boolean generateTrees) {
        for (int x = 0; x < Chunk.CHUNK_SIZE; x++) {
            int worldX = chunk.getChunkX() * Chunk.CHUNK_SIZE + x;
            int currentHeight = getHeight(worldX);
            int leftHeight = getHeight(worldX - 1);
            int rightHeight = getHeight(worldX + 1);
            for (int y = 0; y < Chunk.CHUNK_SIZE; y++) {
                int worldY = chunk.getChunkY() * Chunk.CHUNK_SIZE + y;
                if (worldY < currentHeight - 1 && chunk.getBlock(x, y) == null) {
                    chunk.setBlock(x, y, new DirtBlock(world, worldX, worldY));
                } else if (worldY == currentHeight - 1) {
                    boolean leftLower = leftHeight < currentHeight;
                    boolean rightLower = rightHeight < currentHeight;
                    if (leftLower && rightLower) {
                        if (y - 1 >= 0) {
                            chunk.setBlock(x, y - 1, new GrassBlock(world, worldX, worldY - 1));
                        }
                    } else if (leftLower || rightLower) {
                        boolean leftRamp = (leftLower && !rightLower) ? false : true;
                        chunk.setBlock(x, y, new GrassRamp(world, worldX, worldY, leftRamp));
                        chunk.setBlock(x, y-1, new GrassPatch(world, worldX, worldY-1, leftRamp));
                    } else {
                        chunk.setBlock(x, y, new GrassBlock(world, worldX, worldY));
                    }
                    // Baumgenerierung nur, wenn Flag aktiviert ist:
                    if (generateTrees && Math.abs(worldX - lastTreeX) >= 10 && random.nextDouble() < 0.2) {
                        generateTree(chunk, x, y);
                        lastTreeX = worldX; // Speichere die absolute Welt-X-Position
                    }
                }
            }
        }
        return chunk;
    }



    /**
     * Berechnet die Terrain-Höhe an der horizontalen Position worldX.
     * Hier: octaveNoise(worldX * 0.01, 10, 0.5) * 50 + 50
     */
    public int getHeight(int worldX)
    {
        return (int) (noise.octaveNoise(worldX * 0.01, 10, 0.5) * 30 + 50);
    }

    public boolean hasBlock(int worldX, int worldY)
    {
        return worldY < getHeight(worldX);
    }

    private void generateTree(Chunk chunk, int x, int y) {
        int treeHeight = random.nextInt(21) + 30; // 30-50 Blöcke hoch
        int trunkThickness = treeHeight < 40 ? 2 : 3;

        // Stamm generieren
        for (int i = 0; i < treeHeight; i++) {
            for (int j = 0; j < trunkThickness; j++) {
                int worldX = chunk.getChunkX() * Chunk.CHUNK_SIZE + x + j;
                int worldY = chunk.getChunkY() * Chunk.CHUNK_SIZE + y + i;
                placeBlock(worldX, worldY, new SpruceLogBlock(world, worldX, worldY));
            }
        }

        // Kronen-Parameter
        int crownStartY = y + treeHeight;
        int trunkCenterX = x + trunkThickness / 2;
        int numberOfLayers = 5;
        int baseLayerWidth = trunkThickness + 12;
        int layerHeight = 3;
        int widthDecrement = 4;

        // Mehrstufige Krone
        for (int layer = 0; layer < numberOfLayers; layer++) {
            int layerWidth = Math.max(trunkThickness, baseLayerWidth - (layer * widthDecrement));
            int layerYStart = crownStartY + (layer * layerHeight);
            int xOffset = trunkCenterX - (layerWidth / 2);

            for (int dy = 0; dy < layerHeight; dy++) {
                int currentY = layerYStart + dy;
                for (int dx = 0; dx < layerWidth; dx++) {
                    int worldX = chunk.getChunkX() * Chunk.CHUNK_SIZE + xOffset + dx;
                    int worldYValue = chunk.getChunkY() * Chunk.CHUNK_SIZE + currentY;

                    // Natürlichere Verteilung der Blätter
                    if (dx == 0 || dx == layerWidth - 1) {
                        if (random.nextFloat() < 0.6f) continue;
                    }
                    placeBlock(worldX, worldYValue, new SpruceLeavesBlock(world, worldX, worldYValue));
                }
            }
        }

        // Baumspitze
        int topY = crownStartY + (numberOfLayers * layerHeight);
        int worldX = chunk.getChunkX() * Chunk.CHUNK_SIZE + trunkCenterX;
        int worldY = chunk.getChunkY() * Chunk.CHUNK_SIZE + topY;
        placeBlock(worldX, worldY, new SpruceLeavesBlock(world, worldX, worldY));
    }


    // getHeight, hasBlock, placeBlock und getChunk bleiben wie im Original

    /**
     * Hilfsmethode, um einen Block an absoluten Weltkoordinaten zu platzieren.
     * Berücksichtigt dabei, ob der Block im aktuellen oder in einem angrenzenden Chunk liegt.
     */
    private void placeBlock(int worldX, int worldY, Object block) {
        int chunkX = Math.floorDiv(worldX, Chunk.CHUNK_SIZE);
        int chunkY = Math.floorDiv(worldY, Chunk.CHUNK_SIZE);
        Chunk targetChunk = null;

        for (Chunk c : Globals.tileMap.getLoadedChunks()) {
            if (c.getChunkX() == chunkX && c.getChunkY() == chunkY) {
                targetChunk = c;
                break;
            }
        }
        if (targetChunk == null) {
            targetChunk = Globals.tileMap.loadChunk(chunkX, chunkY, false);
        }
        int blockX = worldX - targetChunk.getChunkX() * Chunk.CHUNK_SIZE;
        int blockY = worldY - targetChunk.getChunkY() * Chunk.CHUNK_SIZE;

        Block existingBlock = targetChunk.getBlock(blockX, blockY);
        if (existingBlock != null && !(existingBlock instanceof GrassRamp)) {
            return; // Nicht überschreiben, wenn ein anderer Block vorhanden ist
        }

        if (block instanceof SpruceLogBlock) {
            targetChunk.setBlock(blockX, blockY, (SpruceLogBlock) block);
        } else if (block instanceof SpruceLeavesBlock) {
            targetChunk.setBlock(blockX, blockY, (SpruceLeavesBlock) block);
        }
    }



    // Hilfsmethode, um einen Chunk aus den geladenen Chunks zu holen
    private Chunk getChunk(int chunkX, int chunkY) {
        for (Chunk c : Globals.tileMap.getLoadedChunks()) {
            if (c.getChunkX() == chunkX && c.getChunkY() == chunkY) {
                return c;
            }
        }
        return null;
    }




}
