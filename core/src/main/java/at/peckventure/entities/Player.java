package at.peckventure.entities;

import at.peckventure.Const;
import at.peckventure.inventory.Inventory;
import at.peckventure.inventory.item.Item;
import at.peckventure.multiplayer.NetworkPackets;
import at.peckventure.status.AbstractStatusEffect;
import at.peckventure.status.EffectRegistry;
import at.peckventure.status.Status;
import at.peckventure.status.StatusEffect;
import at.peckventure.world.Box2DOperationManager;
import at.peckventure.world.block.Block;
import at.peckventure.world.chunk.Chunk;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.scenes.scene2d.Actor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class Player extends Actor {
    protected World world;

    private Status health;
    private Status energy;

    protected Body body;
    protected Sprite sprite;
    protected final float speed = 400;
    protected final float flyForce = 700;
    protected final float maxHeight = 10000;
    protected final float hoverDampening = 200;
    protected final float startY;
    protected boolean operator;
    Inventory inventory;
    protected boolean rotation;

    // Tree climbing variables
    protected boolean attachedToTree = false;
    protected Block attachedTreeBlock = null;
    protected static final float TREE_CLIMB_SPEED = 150f;
    private static final boolean DEBUG = true;

    private List<StatusEffect> effects = new ArrayList<>();

    private void debugLog(String message) {
        if (DEBUG) {
            System.out.println("[PLAYER_TREE] " + message);
        }
    }

    public boolean isOperator()
    {
        return operator;
    }

    public void setOperator(boolean operator)
    {
        this.operator = operator;
    }

    public void addEffect(StatusEffect effect) {
        effect.apply(this);
        effects.add(effect);
    }

    public void setSpeed(float speed) {
        // Implementierung nach Bedarf
    }

    public float getSpeed() {
        return speed;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public Player(World world, float x, float y) {
        this.inventory = new Inventory();
        this.world = world;
        if (Gdx.gl != null) {
            this.sprite = new Sprite(new Texture("textures/woodpecker/woodpecker_idle.png"));
        }
        this.startY = y;

        health = new Status("Health", Const.MAXHEALTH);
        energy = new Status("Energy", Const.MAXENERGY);

        setSize(64, 64);
        Box2DOperationManager.queueOperation(() -> {
            BodyDef bodyDef = new BodyDef();
            bodyDef.type = BodyDef.BodyType.DynamicBody;
            bodyDef.position.set((x + getWidth() / 2f) / Block.BLOCK_SIZE, (y + getHeight() / 2f) / Block.BLOCK_SIZE);
            body = world.createBody(bodyDef);
            float widthMeters = getWidth() / Block.BLOCK_SIZE;
            float heightMeters = getHeight() / Block.BLOCK_SIZE;
            float radius = widthMeters / 2f;
            float rectHeight = heightMeters - 2 * radius;
            if (rectHeight < 0) rectHeight = 0;
            FixtureDef fixtureDef = new FixtureDef();
            fixtureDef.density = 1f;
            fixtureDef.friction = 0.5f;
            fixtureDef.restitution = 0f;
            if (rectHeight > 0) {
                PolygonShape rectShape = new PolygonShape();
                rectShape.setAsBox(radius, rectHeight / 2f, new Vector2(0, 0), 0);
                fixtureDef.shape = rectShape;
                body.createFixture(fixtureDef);
                rectShape.dispose();
            }
            CircleShape topCircle = new CircleShape();
            topCircle.setRadius(radius);
            topCircle.setPosition(new Vector2(0, rectHeight / 2f));
            fixtureDef.shape = topCircle;
            body.createFixture(fixtureDef);
            topCircle.dispose();
            CircleShape bottomCircle = new CircleShape();
            bottomCircle.setRadius(radius);
            bottomCircle.setPosition(new Vector2(0, -rectHeight / 2f));
            fixtureDef.shape = bottomCircle;
            body.createFixture(fixtureDef);
            bottomCircle.dispose();
            body.setUserData(this);
        });
    }

    protected abstract void handleInput(float delta);

    @Override
    public void act(float delta) {
        Iterator<StatusEffect> it = effects.iterator();
        while (it.hasNext()) {
            StatusEffect e = it.next();
            e.update(delta);
            if (e.isExpired()) {
                e.remove(this);
                it.remove();
            }
        }
        handleInput(delta);
        Vector2 bodyPos = body.getPosition();
        setPosition(bodyPos.x * Block.BLOCK_SIZE - getWidth() / 2, bodyPos.y * Block.BLOCK_SIZE - getHeight() / 2);
    }

    public void draw(Batch batch) {
        if (sprite != null)
            batch.draw(sprite, getX(), getY(), getWidth(), getHeight());
    }

    // Wird vom PlayerManager aufgerufen, wenn ein Update-Paket eintrifft.
    public void updateFromPacket(NetworkPackets.PlayerUpdatePacket packet) {
        setPosition(packet.x, packet.y);
        this.getEnergyStatus().setCurrent(packet.energy);
        body.setTransform(packet.x / Block.BLOCK_SIZE, packet.y / Block.BLOCK_SIZE, body.getAngle());
    }

    public Body getBody() {
        return body;
    }

    public int getChunkX() {
        return (int) getX() / Block.BLOCK_SIZE / Chunk.CHUNK_SIZE;
    }

    public int getChunkY() {
        return (int) getY() / Block.BLOCK_SIZE / Chunk.CHUNK_SIZE;
    }

    public abstract void dropItemOutside(Item item, int amount);

    public Status getHealthStatus() {
        return health;
    }

    public Status getEnergyStatus() {
        return energy;
    }

    // Tree climbing methods

    /**
     * Attaches the player to a tree block
     * @param treeBlock The tree block to attach to
     */
    public void attachToTree(Block treeBlock) {
        if (!attachedToTree) {
            attachedToTree = true;
            attachedTreeBlock = treeBlock;

            // Physics changes will be made by the contact listener
            debugLog("Player " + this + " attached to tree " + treeBlock);
        }
    }

    /**
     * Detaches the player from the tree
     */
    public void detachFromTree() {
        if (attachedToTree) {
            debugLog("Player " + this + " detaching from tree");
            attachedToTree = false;
            attachedTreeBlock = null;

            // Re-enable gravity
            Box2DOperationManager.queueOperation(() -> {
                if (body != null) {
                    body.setGravityScale(1);
                    debugLog("Gravity restored for player");
                }
            });
        }
    }

    /**
     * Checks if the player is attached to a tree
     * @return true if attached to a tree, false otherwise
     */
    public boolean isAttachedToTree() {
        return attachedToTree;
    }

    /**
     * Handles tree climbing movement
     * @param upPressed Whether the up button is pressed
     * @param downPressed Whether the down button is pressed
     * @param delta The time delta
     */
    protected void handleTreeClimbing(boolean upPressed, boolean downPressed, float delta) {
        if (attachedToTree && body != null) {
            float currentVelocityX = body.getLinearVelocity().x;
            float climbVelocity = 0;

            if (upPressed) {
                climbVelocity = TREE_CLIMB_SPEED / Block.BLOCK_SIZE;
                debugLog("Climbing up tree at velocity " + climbVelocity);
            } else if (downPressed) {
                climbVelocity = -TREE_CLIMB_SPEED / Block.BLOCK_SIZE;
                debugLog("Climbing down tree at velocity " + climbVelocity);
            }

            // Apply the climbing velocity
            final float finalVelocity = climbVelocity;
            Box2DOperationManager.queueOperation(() -> {
                if (body != null) {
                    body.setLinearVelocity(currentVelocityX, finalVelocity);
                }
            });
        }
    }

    public String serializeEffects() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < effects.size(); i++) {
            // Wir gehen davon aus, dass der Effekt aus AbstractStatusEffect stammt
            AbstractStatusEffect effect = (AbstractStatusEffect) effects.get(i);
            sb.append(effect.getId())
                .append(":")
                .append(effect.getLevel())
                .append(":")
                .append(effect.getRemainingDuration());
            if (i < effects.size() - 1) {
                sb.append(",");
            }
        }
        return sb.toString();
    }

    public void deserializeEffects(String data) {
        if (data == null || data.isEmpty()) return;
        String[] entries = data.split(",");
        for (String entry : entries) {
            String[] parts = entry.split(":");
            if (parts.length == 3) {
                String id = parts[0].trim();
                try {
                    int level = Integer.parseInt(parts[1].trim());
                    float duration = Float.parseFloat(parts[2].trim());
                    StatusEffect effect = EffectRegistry.createEffect(id, level, duration);
                    if (effect != null) {
                        effects.add(effect);
                        effect.apply(this);
                    }
                } catch (NumberFormatException e) {
                    // Fehlerhafte Einträge überspringen
                }
            }
        }
    }

    public void dispose() {
        // Ressourcen freigeben, falls vorhanden
        if (sprite != null && sprite.getTexture() != null) {
            sprite.getTexture().dispose();
        }
    }
}
