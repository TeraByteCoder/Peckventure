package at.peckventure.world.block;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.physics.box2d.World;

public class DirtBlock extends Block
{
    public DirtBlock(World world, int x, int y)
    {
        super(world, new Texture("textures/blocks/dirt.png"), x, y);
    }
}
