package at.peckventure.world.block;

import com.badlogic.gdx.physics.box2d.World;

import javax.swing.*;

public class BlockFactory {
    public static final int DIRT_ID = 1;
    public static final int GRASS_ID = 2;
    public static final int GRASSRAMPLEFT_ID = 3;
    public static final int GRASSRAMPRIGHT_ID = 4;

    public static Block createBlock(int blockId, World world, int worldX, int worldY) {
        switch(blockId) {
            case DIRT_ID:
                return new DirtBlock(world, worldX, worldY);
            case GRASS_ID:
                return new GrassBlock(world, worldX, worldY);
            case GRASSRAMPLEFT_ID:
                // Standardmäßig mit "true" als Rampenparameter; erweiterbar, wenn du weitere Daten speichern möchtest
                return new GrassRamp(world, worldX, worldY, true);
            case GRASSRAMPRIGHT_ID:
                // Standardmäßig mit "true" als Rampenparameter; erweiterbar, wenn du weitere Daten speichern möchtest
                return new GrassRamp(world, worldX, worldY, false);
            default:
                return null;
        }
    }

    public static int getBlockId(Block block) {
        if (block instanceof DirtBlock) {
            return DIRT_ID;
        } else if (block instanceof GrassBlock) {
            return GRASS_ID;
        } else if (block instanceof GrassRamp) {
            if (((GrassRamp) block).isLeftRamp())
            {
                return GRASSRAMPLEFT_ID;
            }
            else {
                return GRASSRAMPRIGHT_ID;
            }
        }
        return 0;
    }
}
