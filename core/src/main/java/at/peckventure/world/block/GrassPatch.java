package at.peckventure.world.block;

import at.peckventure.Textures;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;

public class GrassPatch extends Block
{

    // Gibt an, ob es sich um eine linke Rampe handelt (true) oder um eine rechte Rampe (false)
    private final boolean leftPatch;

    public boolean isLeftRamp()
    {
        return leftPatch;
    }

    /**
     * Konstruktor für eine geneigte Grasoberfläche (Rampe).
     *
     * @param world Die Box2D-World
     * @param gridX X-Position im Blockraster (in Einheiten)
     * @param gridY Y-Position im Blockraster (in Einheiten)
     * @param left  true, wenn es sich um eine linke Rampe handeln soll, false für eine rechte Rampe
     */
    public GrassPatch(World world, int gridX, int gridY, boolean left) {
        super(
            world,
            Gdx.gl != null
                ? (left ? Textures.GRASSPATCHLEFT.getTexture() : Textures.GRASSPATCHRIGHT.getTexture())
                : null,
            gridX,
            gridY
        );
        this.leftPatch = left;
    }
}
