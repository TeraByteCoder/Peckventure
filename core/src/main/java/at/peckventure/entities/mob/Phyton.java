package at.peckventure.entities.mob;

import at.peckventure.Globals;
import at.peckventure.entities.Player;
import at.peckventure.inventory.ItemRegistry;
import at.peckventure.status.Status;
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
import java.util.Random;

import static at.peckventure.Textures.*;

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
    private final float speed = 10.0f;
    private float checkTime;
    private float lastX;
    private boolean facingRight;

    private Status health;

    // New AI-related fields
    private Player targetPlayer;
    private float detectionRange = 100f; // Blocks
    private float attackRange = 3.5f; // Blocks
    private final float attackCooldown = 0.8f; // Seconds between attacks
    private float currentAttackCooldown = 0f;
    private float attackDamage = 10f;

    // Aggression levels
    private AggressionLevel aggressionLevel = AggressionLevel.NEUTRAL;

    // Aggression levels enum
    public enum AggressionLevel {
        PASSIVE,   // Will only move away from player
        NEUTRAL,   // Will move randomly, attack if directly threatened
        AGGRESSIVE // Will actively hunt and attack player
    }

    public Phyton(World world, float x, float y, float maxHealth) {
        super(world, x, y);
        setSize(128, 128);
        this.health = new Status("Health", 30);
        this.targetPlayer = Globals.controlledPlayer;

        direction = Math.random() < 0.5 ? 1f : -1f;
        facingRight = direction > 0;

        animations.put(State.IDLE, PHYTON_IDLE.getAnimation());
        animations.put(State.MOVING, PHYTON_MOVING.getAnimation());
        animations.put(State.ATTACKING, PHYTON_ATTACKING.getAnimation());
        animations.put(State.DYING, PHYTON_DYING.getAnimation());

        Box2DOperationManager.queueOperation(() -> {
            BodyDef bodyDef = new BodyDef();
            bodyDef.type = BodyDef.BodyType.DynamicBody;
            bodyDef.position.set((x + getWidth() / 2f) / Block.BLOCK_SIZE,
                (y + getHeight() / 2f) / Block.BLOCK_SIZE);
            body = world.createBody(bodyDef);

            // Create a 64x64 collision box
            PolygonShape shape = new PolygonShape();
            shape.setAsBox(64f / 2f / Block.BLOCK_SIZE, 64f / 2f / Block.BLOCK_SIZE);

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

    private boolean isPlayerInRange() {
        if (targetPlayer == null || body == null || targetPlayer.getBody() == null) return false;

        Vector2 phytonPos = body.getPosition();
        Vector2 playerPos = targetPlayer.getBody().getPosition();

        float distanceToPlayer = Math.abs(phytonPos.x - playerPos.x);
        return distanceToPlayer <= detectionRange;
    }

    private boolean canAttackPlayer() {
        if (targetPlayer == null || body == null || targetPlayer.getBody() == null) return false;

        Vector2 phytonPos = body.getPosition();
        Vector2 playerPos = targetPlayer.getBody().getPosition();

        float distanceToPlayer = Math.abs(phytonPos.x - playerPos.x);
        return distanceToPlayer <= attackRange;
    }

    private void moveTowardsPlayer() {
        if (targetPlayer == null || body == null) return;

        Vector2 phytonPos = body.getPosition();
        Vector2 playerPos = targetPlayer.getBody().getPosition();

        // Determine direction to player
        direction = phytonPos.x < playerPos.x ? 1f : -1f;
        facingRight = direction > 0;
    }

    private void moveAwayFromPlayer() {
        if (targetPlayer == null || body == null) return;

        Vector2 phytonPos = body.getPosition();
        Vector2 playerPos = targetPlayer.getBody().getPosition();

        // Move in opposite direction of player
        direction = phytonPos.x > playerPos.x ? 1f : -1f;
        facingRight = direction > 0;
    }

    @Override
    public void act(float delta) {
        super.act(delta);

        if (body != null) {
            float s = Block.BLOCK_SIZE;
            setPosition(body.getPosition().x * s - getWidth() / 2f,
                body.getPosition().y * s - getHeight() / 2f);

            // Update attack cooldown
            if (currentAttackCooldown > 0) {
                currentAttackCooldown -= delta;
            }

            // Intelligent movement and attack logic based on aggression level
            if (isDead()) {
                setState(State.DYING);
                body.setLinearVelocity(0, body.getLinearVelocity().y);
            } else {
                switch (aggressionLevel) {
                    case PASSIVE:
                        handleAggressiveBehavior();
                        break;
                    case NEUTRAL:
                        handleAggressiveBehavior();
                        break;
                    case AGGRESSIVE:
                        handleAggressiveBehavior();
                        break;
                }
            }

            facingRight = direction > 0;
            super.direction = facingRight;
        }
    }

    private void handlePassiveBehavior() {
        if (isPlayerInRange()) {
            // Move away from player
            moveAwayFromPlayer();
            setState(State.MOVING);
            Vector2 v = body.getLinearVelocity();
            body.setLinearVelocity(direction * speed, v.y);
        } else {
            // Wander randomly
            handleWanderingBehavior();
        }
    }

    private void handleNeutralBehavior() {
        if (isPlayerInRange()) {
            // If player is very close, prepare to defend
            if (canAttackPlayer() && Math.random() < 0.5) {
                setState(State.ATTACKING);
                body.setLinearVelocity(0, body.getLinearVelocity().y);

                // Perform attack if cooldown is ready
                if (currentAttackCooldown <= 0) {
                    performAttack();
                }
            } else {
                // Otherwise, just move away or wander
                moveAwayFromPlayer();
                setState(State.MOVING);
                Vector2 v = body.getLinearVelocity();
                body.setLinearVelocity(direction * speed, v.y);
            }
        } else {
            // Wander randomly
            handleWanderingBehavior();
        }
    }

    private void handleAggressiveBehavior() {
        if (isPlayerInRange()) {
            if (canAttackPlayer()) {
                // Player is in attack range
                setState(State.ATTACKING);
                body.setLinearVelocity(0, body.getLinearVelocity().y);

                // Perform attack if cooldown is ready
                if (currentAttackCooldown <= 0) {
                    performAttack();
                }
            } else {
                // Move towards player
                moveTowardsPlayer();
                setState(State.MOVING);
                Vector2 v = body.getLinearVelocity();
                body.setLinearVelocity(direction * speed, v.y);
            }
        } else {
            // Wander randomly
            handleWanderingBehavior();
        }
    }

    private void handleWanderingBehavior() {
        checkTime += Gdx.graphics.getDeltaTime();
        if (checkTime > 1f) {
            if (Math.abs(getX() - lastX) < 2f) {
                direction *= -1;
            }
            lastX = getX();
            checkTime = 0;
        }

        Vector2 v = body.getLinearVelocity();
        body.setLinearVelocity(direction * speed, v.y);

        if (Math.abs(direction * speed) > 0.1f) {
            setState(State.MOVING);
        } else {
            setState(State.IDLE);
        }
    }

    private void performAttack() {
        if (targetPlayer != null) {
            // Damage the player's health status
            float currentHealth = targetPlayer.getHealthStatus().getCurrent();
            targetPlayer.getHealthStatus().setCurrent(currentHealth - attackDamage);
            currentAttackCooldown = attackCooldown;
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

    public boolean isDead() {
        return this.health.getCurrent() <= 0;
    }

    public Status getHealth()
    {
        return health;
    }

    // Setter for target player (in case it changes)
    public void setTargetPlayer(Player player) {
        this.targetPlayer = player;
    }

    // Adjust detection and attack parameters
    public void setDetectionRange(float range) {
        this.detectionRange = range;
    }

    public void setAttackRange(float range) {
        this.attackRange = range;
    }

    public void setAttackDamage(float damage) {
        this.attackDamage = damage;
    }

    // Set aggression level
    public void setAggressionLevel(AggressionLevel level) {
        this.aggressionLevel = level;
    }

    public AggressionLevel getAggressionLevel() {
        return this.aggressionLevel;
    }

    @Override
    public void onDeath()
    {
        Random rand = new Random();
        int randNum = rand.nextInt();
        if (randNum % 2 == 0)
        {
            MobRegistry.createMob(MobRegistration.ITEMACTOR_ID, world, this.getX(), this.getY(), ItemRegistry.createItem("speed_potion"));
        }
        dispose();
    }

    @Override
    public void onPeck(Player player)
    {
        health.damage(4);
        if (aggressionLevel == AggressionLevel.NEUTRAL) {
            aggressionLevel = AggressionLevel.AGGRESSIVE;
        }
        if(isDead()) onDeath();
    }
}
