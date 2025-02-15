package at.peckventure.world.block;

import at.peckventure.Textures;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.physics.box2d.World;

public class GrassBlock extends Block
{
    public GrassBlock(World world, int x, int y)
    {
        super(world, Textures.GRASS_BLOCK.getTexture(), x, y);
    }
}
