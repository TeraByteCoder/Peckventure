package at.peckventure.entities.mob;

import at.peckventure.Globals;
import at.peckventure.entities.Player;
import at.peckventure.world.Box2DOperationManager;
import at.peckventure.world.block.Block;
import at.peckventure.inventory.item.Item;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.World;

public class ItemActor extends Mob {
    private final Texture texture;
    private final Item inventoryItem;

    public ItemActor(World world, float x, float y, Item inventoryItem) {
        super(world, x, y);
        this.inventoryItem = inventoryItem;
        texture = inventoryItem.getTexture();
        setSize(32, 32);

        Box2DOperationManager.queueOperation(() -> {
            BodyDef bodyDef = new BodyDef();
            bodyDef.type = BodyDef.BodyType.DynamicBody;
            bodyDef.position.set((x + getWidth() / 2f) / Block.BLOCK_SIZE, (y + getHeight() / 2f) / Block.BLOCK_SIZE);
            body = world.createBody(bodyDef);

            CircleShape shape = new CircleShape();
            shape.setRadius(getWidth() / 2f / Block.BLOCK_SIZE);

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

    @Override
    public void act(float delta) {
        super.act(delta);
        if (body != null) {
            float s = Block.BLOCK_SIZE;
            setPosition(body.getPosition().x * s - getWidth() / 2f,
                body.getPosition().y * s - getHeight() / 2f);
            Vector2 v = body.getLinearVelocity();
            if (Math.abs(v.y) < 0.1f && Math.abs(v.x) > 0.01f) {
                body.setLinearVelocity(v.x * 0.8f, v.y);
                if (Math.abs(v.x) < 0.05f) {
                    body.setLinearVelocity(0, 0);
                }
            }
        }
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        batch.draw(texture, getX(), getY(), getWidth(), getHeight());
    }

    public void onPlayerContact(Player player) {
        Globals.inventoryUI.addItem(inventoryItem, 1);
        remove();
    }
}
