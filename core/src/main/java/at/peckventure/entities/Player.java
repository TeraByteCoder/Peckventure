package at.peckventure.entities;

import at.peckventure.inventory.Inventory;
import at.peckventure.multiplayer.NetworkPackets;
import at.peckventure.world.block.Block;
import at.peckventure.world.chunk.Chunk;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.scenes.scene2d.Actor;
import java.util.UUID;

public abstract class Player extends Actor {
    protected World world;

    protected Body body;
    protected Sprite sprite;
    protected final float speed = 400;
    protected final float flyForce = 700;
    protected final float maxHeight = 1000;
    protected final float hoverDampening = 200;
    protected final float startY;
    Inventory inventory;

    public Inventory getInventory() {
        return inventory;
    }


    public Player(World world, float x, float y) {
        this.inventory = new Inventory();
        this.world = world;
        // Generiere eine eindeutige ID, z. B. mit UUID
        if (Gdx.gl != null) {
            this.sprite = new Sprite(new Texture("textures/woodpecker/woodpecker_idle.png"));
        }
        this.startY = y;
        setSize(64, 64);
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
    }

    protected abstract void handleInput(float delta);

    @Override
    public void act(float delta) {
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
}
