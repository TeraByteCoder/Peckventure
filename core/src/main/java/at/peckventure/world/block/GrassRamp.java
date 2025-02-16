package at.peckventure.world.block;

import at.peckventure.Textures;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;

public class GrassRamp extends Block {

    // Gibt an, ob es sich um eine linke Rampe handelt (true) oder um eine rechte Rampe (false)
    private boolean leftRamp;

    /**
     * Konstruktor für eine geneigte Grasoberfläche (Rampe).
     *
     * @param world Die Box2D-World
     * @param gridX X-Position im Blockraster (in Einheiten)
     * @param gridY Y-Position im Blockraster (in Einheiten)
     * @param left  true, wenn es sich um eine linke Rampe handeln soll, false für eine rechte Rampe
     */
    public GrassRamp(World world, int gridX, int gridY, boolean left) {
        // Wähle das entsprechende Texture abhängig vom Boolean.
        super(world,
            left ? Textures.GRASSRAMPLEFT.getTexture()
                : Textures.GRASSRAMPRIGHT.getTexture(),
            gridX,
            gridY);
        this.leftRamp = left;
    }

    /**
     * Überschreibt die Kollisionsform, um eine dreieckige Form (45° Rampe) zu erstellen.
     *
     * Für eine linke Rampe (leftRamp == true) gilt (CCW-Reihenfolge):
     *   - (-halfWidth, -halfHeight)  = linker unterer Punkt
     *   - (-halfWidth,  halfHeight)  = linker oberer Punkt
     *   - ( halfWidth, -halfHeight)  = rechter unterer Punkt
     *
     * Für eine rechte Rampe (leftRamp == false) soll gelten:
     *   - (-halfWidth, -halfHeight)  = linker unterer Punkt
     *   - ( halfWidth, -halfHeight)  = rechter unterer Punkt
     *   - ( halfWidth,  halfHeight)  = rechter oberer Punkt
     *
     * @return Die erzeugte PolygonShape.
     */
    @Override
    protected PolygonShape createShape() {
        PolygonShape shape = new PolygonShape();
        float halfWidth  = getWidth()  / 2f / BLOCK_SIZE;
        float halfHeight = getHeight() / 2f / BLOCK_SIZE;

        Vector2[] vertices = new Vector2[3];
        if (leftRamp) {
            vertices[0] = new Vector2(-halfWidth, -halfHeight); // linker unterer Punkt
            vertices[1] = new Vector2(-halfWidth,  halfHeight); // linker oberer Punkt
            vertices[2] = new Vector2( halfWidth, -halfHeight); // rechter unterer Punkt
        } else {
            vertices[0] = new Vector2(-halfWidth, -halfHeight); // linker unterer Punkt
            vertices[1] = new Vector2( halfWidth, -halfHeight); // rechter unterer Punkt
            vertices[2] = new Vector2( halfWidth,  halfHeight); // rechter oberer Punkt
        }

        shape.set(vertices);
        return shape;
    }
}
