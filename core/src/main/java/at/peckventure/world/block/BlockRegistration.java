package at.peckventure.world.block;

import com.badlogic.gdx.physics.box2d.World;

public class BlockRegistration
{
    public static final int DIRT_ID = 1;
    public static final int GRASS_ID = 2;
    public static final int GRASSRAMPLEFT_ID = 3;
    public static final int GRASSRAMPRIGHT_ID = 4;

    static
    {
        BlockRegistry.registerBlock(DIRT_ID, DirtBlock.class, (world, worldX, worldY, args) -> new DirtBlock(world, worldX, worldY));
        BlockRegistry.registerBlock(GRASS_ID, GrassBlock.class, (world, worldX, worldY, args) -> new GrassBlock(world, worldX, worldY));
        BlockRegistry.registerBlock(GRASSRAMPLEFT_ID, GrassRamp.class, (world, worldX, worldY, args) -> new GrassRamp(world, worldX, worldY, true));
        BlockRegistry.registerBlock(GRASSRAMPRIGHT_ID, GrassRamp.class, (world, worldX, worldY, args) -> new GrassRamp(world, worldX, worldY, false));
    }

    // Optional: Diese Methode aufrufen, damit die statische Initialisierung ausgelöst wird.
    public static void init()
    {
        // Leerer Aufruf – die statische Initialisierung passiert automatisch beim ersten Zugriff.
    }
}
