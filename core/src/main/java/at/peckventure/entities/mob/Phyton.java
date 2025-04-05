package at.peckventure.entities.mob;

import at.peckventure.Textures;
import at.peckventure.world.Box2DOperationManager;
import at.peckventure.world.block.Block;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;

public class Phyton extends Mob {
    private final Texture texture;
    private float direction;
    private float speed = 1.0f;  // Baumphyton bewegt sich etwas langsamer
    private float checkTime;
    private float lastX;
    private boolean facingRight;

    private float maxHealth;
    private float currentHealth;

    public Phyton(World world, float x, float y, float maxHealth) {
        super(world, x, y);
        // Verwende die Textur, die zu einem Baumphyton passt
        texture = Textures.PHYTON.getTexture();
        // Angepasstes, länglicheres Format für einen Baumphyton
        setSize(64, 32);
        this.maxHealth = maxHealth;
        this.currentHealth = maxHealth;

        // Zufällige Start-Richtung (links oder rechts)
        direction = Math.random() < 0.5 ? 1f : -1f;
        facingRight = direction > 0;

        Box2DOperationManager.queueOperation(() -> {
            BodyDef bodyDef = new BodyDef();
            bodyDef.type = BodyDef.BodyType.DynamicBody;
            bodyDef.position.set((x + getWidth() / 2f) / Block.BLOCK_SIZE,
                (y + getHeight() / 2f) / Block.BLOCK_SIZE);
            body = world.createBody(bodyDef);

            // Rechteckige Kollisionsform passend zur langgestreckten Form
            PolygonShape shape = new PolygonShape();
            shape.setAsBox(getWidth() / 2f / Block.BLOCK_SIZE, getHeight() / 2f / Block.BLOCK_SIZE);

            FixtureDef fixtureDef = new FixtureDef();
            fixtureDef.shape = shape;
            fixtureDef.density = 1f;
            fixtureDef.friction = 1f;  // erhöhter Reibungswert für bessere Stabilität
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
            // Position aus der Box2D-Welt in Pixel umrechnen
            setPosition(body.getPosition().x * s - getWidth() / 2f,
                body.getPosition().y * s - getHeight() / 2f);

            // Aktuelle Geschwindigkeit abrufen und horizontale Bewegung setzen
            Vector2 v = body.getLinearVelocity();
            body.setLinearVelocity(direction * speed, v.y);

            // Überprüfen, ob sich der Phyton in der letzten Sekunde bewegt hat;
            // falls nicht, Richtung umkehren
            checkTime += delta;
            if (checkTime > 1f) {
                if (Math.abs(getX() - lastX) < 2f) {
                    direction *= -1;
                }
                lastX = getX();
                checkTime = 0;
            }

            // Sprite-Ausrichtung basierend auf der Bewegungsrichtung
            facingRight = direction > 0;
        }
        super.direction = facingRight;
    }

    @Override
    public void setDirection(boolean facingRight) {
        // Interner Richtungswert basierend auf dem Boolean: true → rechts (1), false → links (-1)
        this.direction = facingRight ? 1f : -1f;
        this.facingRight = facingRight;

        // Falls der Body bereits existiert, sofort die Geschwindigkeit aktualisieren
        if (body != null) {
            Vector2 v = body.getLinearVelocity();
            body.setLinearVelocity(this.direction * speed, v.y);
        }
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        // Zeichne den Phyton: Bei Blick nach rechts normal, andernfalls horizontal gespiegelt
        if (facingRight) {
            batch.draw(texture, getX() + getWidth(), getY(), -getWidth(), getHeight());
        } else {
            batch.draw(texture, getX(), getY(), getWidth(), getHeight());
        }
    }

    public void takeDamage(float damage) {
        currentHealth -= damage;
        if (currentHealth < 0) {
            currentHealth = 0;
        }
    }

    public void heal(float amount) {
        currentHealth += amount;
        if (currentHealth > maxHealth) {
            currentHealth = maxHealth;
        }
    }

    public boolean isDead() {
        return currentHealth <= 0;
    }

    public float getCurrentHealth() {
        return currentHealth;
    }

    public float getMaxHealth() {
        return maxHealth;
    }


}
