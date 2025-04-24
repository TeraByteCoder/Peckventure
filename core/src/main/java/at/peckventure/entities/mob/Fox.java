package at.peckventure.entities.mob;

import at.peckventure.Globals;
import at.peckventure.Textures;
import at.peckventure.entities.Player;
import at.peckventure.inventory.ItemRegistry;
import at.peckventure.world.Box2DOperationManager;
import at.peckventure.world.block.Block;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static at.peckventure.Textures.*;

public class Fox extends Mob {
    private float direction;
    private final float speed = 1.5f;
    private float checkTime;
    private float lastX;
    private boolean facingRight;

    private enum State {
        IDLE,
        MOVING,
        ATTACKING,
        DYING
    }

    private final Map<Fox.State, Animation<TextureRegion>> animations = new HashMap<>();
    private Fox.State currentState = Fox.State.IDLE;
    private float stateTime = 0f;


    public Fox(World world, float x, float y) {
        super(world, x, y);
        setSize(46, 46);

        // Zufällige Start-Richtung (links oder rechts)
        direction = Math.random() < 0.5 ? 1 : -1;
        facingRight = direction > 0;

        // Richtige Fox-Animationen verwenden (statt Python)
        animations.put(Fox.State.IDLE, FOX_IDLE.getAnimation());
        animations.put(Fox.State.MOVING, FOX_MOVING.getAnimation());
        animations.put(Fox.State.ATTACKING, FOX_ATTACKING.getAnimation());
        animations.put(Fox.State.DYING, FOX_DYING.getAnimation());

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

    private void setState(Fox.State newState) {
        if (this.currentState != newState) {
            this.currentState = newState;
            this.stateTime = 0f;
        }
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        stateTime += delta; // Aktualisiere die Animationszeit

        if (body != null) {
            float s = Block.BLOCK_SIZE;
            // Position aus der Box2D-Welt auf Pixelebene umrechnen
            setPosition(body.getPosition().x * s - getWidth() / 2f,
                body.getPosition().y * s - getHeight() / 2f);

            // Aktuelle Geschwindigkeit abrufen
            Vector2 v = body.getLinearVelocity();

            // Setze die horizontale Geschwindigkeit entsprechend der Richtung
            body.setLinearVelocity(direction * speed, v.y);

            // Nach einer Sekunde prüfen, ob sich der Fuchs bewegt hat;
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

            // Aktualisiere den Zustand basierend auf der Bewegung
            if (Math.abs(v.x) > 0.1f) {
                setState(Fox.State.MOVING);
            } else {
                setState(Fox.State.IDLE);
            }
        }
        super.direction = facingRight;
    }

    @Override
    public void setDirection(boolean facingRight) {
        // Setze den internen Richtungswert basierend auf dem Boolean:
        // true → nach rechts (1), false → nach links (-1)
        this.direction = facingRight ? 1f : -1f;
        this.facingRight = facingRight;

        // Optional: Falls der Body bereits existiert, aktualisiere sofort auch seine Geschwindigkeit
        if (body != null) {
            Vector2 v = body.getLinearVelocity();
            body.setLinearVelocity(this.direction * speed, v.y);
        }
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        // Hol das aktuelle Frame der Animation
        TextureRegion currentFrame = animations.get(currentState).getKeyFrame(stateTime, true);

        // Zeichne das aktuelle Frame in die richtige Richtung
        if (facingRight) {
            batch.draw(currentFrame, getX(), getY(), getWidth(), getHeight());
        } else {
            batch.draw(currentFrame, getX() + getWidth(), getY(), -getWidth(), getHeight());
        }
    }

    public void die() {
        setState(Fox.State.DYING);
        // Optional: Beende die Animation, wenn sie abgeschlossen ist
        if (animations.get(Fox.State.DYING).isAnimationFinished(stateTime)) {
            onDeath();
        }
    }

    @Override
    public void onDeath() {
        Random rand = new Random();
        int randNum = rand.nextInt();
        if (randNum % 100 == 0) {
            MobRegistry.createMob(MobRegistration.ITEMACTOR_ID, world, this.getX(), this.getY(), ItemRegistry.createItem("speed_potion"), 1);
        }
        if (randNum % 4 == 0) {
            MobRegistry.createMob(MobRegistration.ITEMACTOR_ID, world, this.getX(), this.getY(), ItemRegistry.createItem("wood"), 1);
        }
        dispose();
    }

    @Override
    public void onPeck(Player player) {
        setState(Fox.State.ATTACKING);
        // Optional: Warte, bis die Angriffs-Animation abgeschlossen ist
        if (animations.get(Fox.State.ATTACKING).isAnimationFinished(stateTime)) {
            onDeath();
        }
    }
}
