package at.peckventure.world.block;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.scenes.scene2d.Actor;

public abstract class Block extends Actor {
    public static final int BLOCK_SIZE = 32; // Blockgröße in Pixel

    private World world;
    private Body body;
    private Texture texture;

    /**
     * @param world   Die Box2D-World
     * @param texture Das Texture des Blocks
     * @param gridX   X-Position im Blockraster (in Einheiten)
     * @param gridY   Y-Position im Blockraster (in Einheiten)
     */
    public Block(World world, Texture texture, int gridX, int gridY) {
        this.world = world;
        this.texture = texture;
        setSize(BLOCK_SIZE, BLOCK_SIZE); // Blockgröße in Pixel

        // Berechne die Pixelposition (unten links) anhand des Rasters
        float x = gridX * BLOCK_SIZE;
        float y = gridY * BLOCK_SIZE;
        setPosition(x, y);

        // --- Body erstellen ---
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.StaticBody;
        // Setze die Body-Position auf die Mitte des Blocks (in Meter)
        bodyDef.position.set((x + getWidth() / 2) / BLOCK_SIZE, (y + getHeight() / 2) / BLOCK_SIZE);
        body = world.createBody(bodyDef);

        // Erstelle eine Box-Shape, die der Blockgröße entspricht (Halbmaße in Meter)
        PolygonShape shape = new PolygonShape();
        shape.setAsBox(getWidth() / 2 / BLOCK_SIZE, getHeight() / 2 / BLOCK_SIZE);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.friction = 0.5f;
        fixtureDef.restitution = 0f;

        body.createFixture(fixtureDef);
        shape.dispose();

        // Setze UserData, um den Body später im ContactListener zu identifizieren
        body.setUserData(this);
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
