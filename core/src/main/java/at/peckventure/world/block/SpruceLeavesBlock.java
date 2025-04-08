package at.peckventure.world.block;

import at.peckventure.Textures;
import at.peckventure.world.Box2DOperationManager;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.physics.box2d.World;

public class SpruceLeavesBlock extends Block
{
    public SpruceLeavesBlock(World world, int x, int y)
    {
        super(
            world,//todo
            (Gdx.gl != null) ? Textures.SPRUCE_LEAVES.getTexture() : null,
            x,
            y
        );
        this.isCollisionEnabled =false;
    }
}
