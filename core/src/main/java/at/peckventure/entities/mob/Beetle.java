package at.peckventure.entities.mob;

import at.peckventure.Textures;
import at.peckventure.world.Box2DOperationManager;
import at.peckventure.world.block.Block;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;

public class Beetle extends Mob {
    private final Texture texture;
    private Body body;
    private float direction;
    private float speed = 1.5f;
    private float checkTime;
    private float lastX;
    private boolean facingRight;

    public Beetle(World world, float x, float y) {
        super(world, x, y);
        texture = Textures.BEETLE.getTexture();
        setSize(32, 32);

        // Zufällige Start-Richtung (links oder rechts)
        direction = Math.random() < 0.5 ? 1 : -1;
        facingRight = direction > 0;

        Box2DOperationManager.queueOperation(() -> {
            BodyDef bodyDef = new BodyDef();
            bodyDef.type = BodyDef.BodyType.DynamicBody;
            bodyDef.position.set((x + getWidth() / 2f) / Block.BLOCK_SIZE, (y + getHeight() / 2f) / Block.BLOCK_SIZE);
            body = world.createBody(bodyDef);

            // Kreisförmige Kollisionsform
            CircleShape shape = new CircleShape();
            shape.setRadius(getWidth() / 2f / Block.BLOCK_SIZE);

            FixtureDef fixtureDef = new FixtureDef();
            fixtureDef.shape = shape;
            fixtureDef.density = 1f;
            fixtureDef.friction = 1f;  // etwas höherer Reibungswert für bessere Bodenhaftung
            fixtureDef.restitution = 0f;
            body.createFixture(fixtureDef);
            shape.dispose();

            body.setUserData(this);
        });
    }

    @Override
    public void act(float delta) {
        super.act(delta);

        if (body != null) {
            float s = Block.BLOCK_SIZE;
            // Position aus der Box2D-Welt auf Pixelebene umrechnen
            setPosition(body.getPosition().x * s - getWidth() / 2f,
                body.getPosition().y * s - getHeight() / 2f);

            // Aktuelle Geschwindigkeit abrufen
            Vector2 v = body.getLinearVelocity();

            // Setze die horizontale Geschwindigkeit entsprechend der Richtung
            body.setLinearVelocity(direction * speed, v.y);

            // Nach einer Sekunde prüfen, ob sich der Käfer bewegt hat;
            // wenn nicht, Richtung umkehren
            checkTime += delta;
            if (checkTime > 1f) {
                if (Math.abs(getX() - lastX) < 2f) {
                    direction *= -1;
                }
                lastX = getX();
                checkTime = 0;
            }

            // Richtung → Ausrichtung des Sprites
            facingRight = direction > 0;
        }
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        // Falls er nach rechts schaut, normal zeichnen
        // Falls nach links, umgedreht zeichnen
        if (facingRight) {
            batch.draw(texture, getX() + getWidth(), getY(), -getWidth(), getHeight());
        } else {
            batch.draw(texture, getX(), getY(), getWidth(), getHeight());
        }
    }

    @Override
    public void dispose() {
        if (body != null) {
            Box2DOperationManager.queueOperation(() -> {
                if (body.getWorld() != null) {
                    body.getWorld().destroyBody(body);
                }
            });
        }
        super.dispose();
    }
}
