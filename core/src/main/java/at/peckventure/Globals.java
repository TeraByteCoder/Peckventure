package at.peckventure;

import at.peckventure.entities.Player;
import at.peckventure.entities.mob.MobMap;
import at.peckventure.world.AbstractTileMap;
import at.peckventure.world.block.Block;
import at.peckventure.world.chunk.Chunk;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.physics.box2d.World;


public class Globals
{
    public static World physicsWorld;
    public static MobMap mobs;

    public static String uuid;
    public static Player controlledPlayer; // todo anders lösen

    public static String username;
    public static AbstractTileMap tileMap;

    public static int toChunkCoords(float x)
    {
        return (int) x / Block.BLOCK_SIZE / Chunk.CHUNK_SIZE;
    }

}
