package at.peckventure;

import at.peckventure.entities.mob.Mob;
import at.peckventure.world.block.Block;
import at.peckventure.world.chunk.Chunk;
import com.badlogic.gdx.physics.box2d.World;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Globals
{
    public static World physicsWorld;
    public static Map<Integer, Mob> mobs;

    public static String uuid;

    public static String username;

    public static int toChunkCoords(float x)
    {
        return (int) x / Block.BLOCK_SIZE / Chunk.CHUNK_SIZE;
    }

}
