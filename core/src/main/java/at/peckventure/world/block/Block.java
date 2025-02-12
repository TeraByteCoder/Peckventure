package at.peckventure.world.block;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.scenes.scene2d.Actor;

public abstract class Block extends Actor {
    public static final int BLOCK_SIZE = 32; // Blockgröße in Pixel

    private World world;
    private Body body;
    private Texture texture;

    /**
     * Konstruktor
     *
     * @param world   Die Box2D-World
     * @param texture Das Texture des Blocks
     * @param gridX   X-Position im Blockraster (in Einheiten)
     * @param gridY   Y-Position im Blockraster (in Einheiten)
     */
    public Block(World world, Texture texture, int gridX, int gridY) {
        this.world = world;
        this.texture = texture;
        setSize(BLOCK_SIZE, BLOCK_SIZE);

        // Berechne die Position in Pixeln
        float x = gridX * BLOCK_SIZE;
        float y = gridY * BLOCK_SIZE;
        setPosition(x, y);

        // --- Body erstellen ---
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.StaticBody;
        // Positioniere den Body in der Mitte des Blocks (Umrechnung in Meter: Pixel / BLOCK_SIZE)
        bodyDef.position.set((x + getWidth() / 2f) / BLOCK_SIZE, (y + getHeight() / 2f) / BLOCK_SIZE);
        body = world.createBody(bodyDef);

        // Erzeuge die Kollisionsform. Der Default ist ein Rechteck.
        PolygonShape shape = createShape();

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.friction = 0.5f;
        fixtureDef.restitution = 0f;

        body.createFixture(fixtureDef);
        shape.dispose();

        // Setze UserData, damit der Body im ContactListener identifiziert werden kann
        body.setUserData(this);
    }

    /**
     * Erzeugt die Kollisionsform des Blocks.
     *
     * Default: Rechteckige Kollisionsbox, so wie vorher.
     *
     * Diese Methode kann in abgeleiteten Klassen überschrieben werden,
     * um beispielsweise eine dreieckige oder andere Form zu definieren.
     *
     * @return Die erzeugte PolygonShape.
     */
    protected PolygonShape createShape() {
        PolygonShape shape = new PolygonShape();
        // Erzeugt ein Rechteck: setAsBox erwartet Halbmaße in Meter.
        shape.setAsBox(getWidth() / 2f / BLOCK_SIZE, getHeight() / 2f / BLOCK_SIZE);
        return shape;
    }

    public void draw(Batch batch) {
        // Da sich der Block nicht bewegt, genügt es, den Actor an der
        // in der Konstruktor gesetzten Position zu zeichnen.
        batch.draw(texture, getX(), getY(), getWidth(), getHeight());
    }

    public Body getBody() {
        return body;
    }
}
