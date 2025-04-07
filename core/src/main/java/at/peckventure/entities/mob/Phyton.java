package at.peckventure.entities.mob;

import at.peckventure.SpriteSheetLoader;
import at.peckventure.world.Box2DOperationManager;
import at.peckventure.world.block.Block;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;

import java.util.HashMap;
import java.util.Map;

public class Phyton extends Mob {

    private enum State {
        IDLE,
        MOVING,
        ATTACKING,
        DYING
    }

    private final Map<State, Animation<TextureRegion>> animations = new HashMap<>();
    private State currentState = State.IDLE;
    private float stateTime = 0f;

    private float direction;
    private float speed = 1.0f;
    private float checkTime;
    private float lastX;
    private boolean facingRight;

    private float maxHealth;
    private float currentHealth;

    public Phyton(World world, float x, float y, float maxHealth) {
        super(world, x, y);
        setSize(128, 512);
        this.maxHealth = maxHealth;
        this.currentHealth = maxHealth;

        direction = Math.random() < 0.5 ? 1f : -1f;
        facingRight = direction > 0;

        int cols = 6; // Anzahl der Frames pro Zeile
        int rows = 4; // Anzahl der Animationsarten (z. B. idle, walk, attack, death)

        animations.put(State.IDLE, SpriteSheetLoader.loadRow("textures/mobs/cobra.png", cols, rows, 0, 0.2f));
        animations.put(State.MOVING, SpriteSheetLoader.loadRow("textures/mobs/cobra.png", cols, rows, 1, 0.1f));
        animations.put(State.ATTACKING, SpriteSheetLoader.loadRow("textures/mobs/cobra.png", cols, rows, 2, 0.08f));
        animations.put(State.DYING, SpriteSheetLoader.loadRow("textures/mobs/cobra.png", cols, rows, 3, 0.12f));

        Box2DOperationManager.queueOperation(() -> {
            BodyDef bodyDef = new BodyDef();
            bodyDef.type = BodyDef.BodyType.DynamicBody;
            bodyDef.position.set((x + getWidth() / 2f) / Block.BLOCK_SIZE,
                (y + getHeight() / 2f) / Block.BLOCK_SIZE);
            body = world.createBody(bodyDef);

            PolygonShape shape = new PolygonShape();
            shape.setAsBox(getWidth() / 2f / Block.BLOCK_SIZE, getHeight() / 2f / Block.BLOCK_SIZE);

            FixtureDef fixtureDef = new FixtureDef();
            fixtureDef.shape = shape;
            fixtureDef.density = 1f;
            fixtureDef.friction = 1f;
            fixtureDef.restitution = 0f;
            body.createFixture(fixtureDef);
            shape.dispose();

            body.setUserData(this);
        });
    }

    private void setState(State newState) {
        if (this.currentState != newState) {
            this.currentState = newState;
            this.stateTime = 0f;
        }
    }

    @Override
    public void act(float delta) {
        super.act(delta);

        if (body != null) {
            float s = Block.BLOCK_SIZE;
            setPosition(body.getPosition().x * s - getWidth() / 2f,
                body.getPosition().y * s - getHeight() / 2f);

            Vector2 v = body.getLinearVelocity();
            body.setLinearVelocity(direction * speed, v.y);

            checkTime += delta;
            if (checkTime > 1f) {
                if (Math.abs(getX() - lastX) < 2f) {
                    direction *= -1;
                }
                lastX = getX();
                checkTime = 0;
            }

            facingRight = direction > 0;

            if (isDead()) {
                setState(State.DYING);
            } else if (Math.abs(direction * speed) > 0.1f) {
                setState(State.MOVING);
            } else {
                setState(State.IDLE);
            }
        }

        super.direction = facingRight;
    }

    @Override
    public void setDirection(boolean facingRight) {
        this.direction = facingRight ? 1f : -1f;
        this.facingRight = facingRight;

        if (body != null) {
            Vector2 v = body.getLinearVelocity();
            body.setLinearVelocity(this.direction * speed, v.y);
        }
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        stateTime += Gdx.graphics.getDeltaTime();
        Animation<TextureRegion> currentAnim = animations.get(currentState);
        TextureRegion currentFrame = currentAnim.getKeyFrame(stateTime, currentState != State.DYING);

        // Nur flippen, wenn nötig – verhindert Flacker-Fehler!
        if (!facingRight && !currentFrame.isFlipX()) {
            currentFrame.flip(true, false);
        } else if (facingRight && currentFrame.isFlipX()) {
            currentFrame.flip(true, false);
        }

        batch.draw(currentFrame, getX(), getY(), getWidth(), getHeight());
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
