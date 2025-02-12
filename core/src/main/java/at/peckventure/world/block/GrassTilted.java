package at.peckventure.world.block;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;

public class GrassTilted extends Block {

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
    public GrassTilted(World world, int gridX, int gridY, boolean left) {
        // Wähle das entsprechende Texture abhängig vom Boolean.
        // Dabei gehen wir davon aus, dass du für beide Richtungen ein eigenes Bild hast.
        super(world, new Texture(left
            ? "textures/blocks/grass_ramp_left.png"
            : "textures/blocks/grass_ramp_right.png"), gridX, gridY);
        this.leftRamp = left;
    }

    /**
     * Überschreibt die Kollisionsform, um eine dreieckige Form (45° Rampe) zu erstellen.
     * Hier wird zusätzlich die Hitbox gespiegelt, wenn leftRamp true ist.
     *
     * Für eine rechte Rampe (leftRamp == false) gilt:
     *   - linker unterer Punkt: (-halfWidth, -halfHeight)
     *   - rechter unterer Punkt: (halfWidth, -halfHeight)
     *   - rechter oberer Punkt: (halfWidth, halfHeight)
     *
     * Für eine linke Rampe (leftRamp == true) wird die Hitbox horizontal gespiegelt:
     *   - rechter unterer Punkt: (halfWidth, -halfHeight)
     *   - linker unterer Punkt: (-halfWidth, -halfHeight)
     *   - linker oberer Punkt: (-halfWidth, halfHeight)
     *
     * @return Die erzeugte, ggf. gespiegelte PolygonShape.
     */
    @Override
    protected PolygonShape createShape() {
        PolygonShape shape = new PolygonShape();
        float halfWidth = getWidth() / 2f / BLOCK_SIZE;
        float halfHeight = getHeight() / 2f / BLOCK_SIZE;

        Vector2[] vertices = new Vector2[3];
// Definiere zunächst die Standard-Vertices (entsprechend leftRamp = true)
        Vector2[] baseVertices = new Vector2[3];
        baseVertices[0] = new Vector2(-halfWidth, -halfHeight);  // linker unterer Punkt
        baseVertices[1] = new Vector2( halfWidth, -halfHeight);  // rechter unterer Punkt
        baseVertices[2] = new Vector2( halfWidth,  halfHeight);  // rechter oberer Punkt

        if (leftRamp) {
            // Standard: direkt übernehmen
            for (int i = 0; i < baseVertices.length; i++) {
                vertices[i] = new Vector2(baseVertices[i].x, baseVertices[i].y);
            }
        } else {
            // Spiegeln: x-Werte negieren
            for (int i = 0; i < baseVertices.length; i++) {
                vertices[i] = new Vector2(-baseVertices[i].x, baseVertices[i].y);
            }
        }

        shape.set(vertices);
        return shape;
    }
}
