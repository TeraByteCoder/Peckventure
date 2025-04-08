package at.peckventure.world.block;

import at.peckventure.Textures;
import at.peckventure.world.Box2DOperationManager;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.physics.box2d.World;

public class SpruceLogBlock extends Block
{
    public SpruceLogBlock(World world, int x, int y)
    {
        super(
            world,
            (Gdx.gl != null) ? Textures.SPRUCE_LOG.getTexture() : null,
            x,
            y
        );
        this.isCollisionEnabled =false;
    }
}
