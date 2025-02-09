package at.peckventure.world.block;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.physics.box2d.World;

public class GrassBlock extends Block
{
    public GrassBlock(World world, int x, int y)
    {
        super(world, new Texture("textures/blocks/grass_block.png"), x, y);
    }
}
