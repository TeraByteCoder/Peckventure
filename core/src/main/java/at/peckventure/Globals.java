package at.peckventure;

import at.peckventure.entities.Player;
import at.peckventure.entities.mob.Mob;
import at.peckventure.world.block.Block;
import at.peckventure.world.chunk.Chunk;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class Globals
{
    public static World physicsWorld;
    public static List<Mob> mobs = Collections.synchronizedList(new LinkedList<>());

    public static String uuid;

    public static int toChunkCoords(float x)
    {
        return (int) x / Block.BLOCK_SIZE / Chunk.CHUNK_SIZE;
    }

}
