package at.peckventure.world.block;

import at.peckventure.Textures;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.physics.box2d.World;

public class DirtBlock extends Block {
    public DirtBlock(World world, int x, int y) {
        super(
            world,
            (Gdx.gl != null) ? Textures.DIRT.getTexture() : null,
            x,
            y
        );
    }

}
