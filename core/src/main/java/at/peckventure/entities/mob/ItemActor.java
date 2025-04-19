package at.peckventure.entities.mob;

import at.peckventure.Globals;
import at.peckventure.entities.Player;
import at.peckventure.inventory.ItemRegistry;
import at.peckventure.inventory.item.Item;
import at.peckventure.multiplayer.NetworkManager;
import at.peckventure.multiplayer.NetworkPackets;
import at.peckventure.world.Box2DOperationManager;
import at.peckventure.world.block.Block;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.World;

public class ItemActor extends Mob
{
    private final Texture texture;
    private final Item inventoryItem;
    private float existenceTime = 0f;

    private int amount;
    private boolean destroyed = false;
    private Player contactingPlayer = null; // Track the player currently in contact


    public ItemActor(World world, float x, float y, Item inventoryItem, int amount)
    {
        super(world, x, y);
        this.inventoryItem = inventoryItem;
        this.texture = inventoryItem.getTexture();
        this.amount = amount;
        setSize(32, 32);
        Box2DOperationManager.queueOperation(() ->
        {
            BodyDef bodyDef = new BodyDef();
            bodyDef.type = BodyDef.BodyType.DynamicBody;
            bodyDef.position.set((x + getWidth() / 2f) / Block.BLOCK_SIZE, (y + getHeight() / 2f) / Block.BLOCK_SIZE);
            body = world.createBody(bodyDef);

            CircleShape shape = new CircleShape();
            shape.setRadius(getWidth() / 2f / Block.BLOCK_SIZE);

            // Erstelle zwei Fixtures: Eine für normale Kollisionen und eine Sensor-Fixture nur für Player

            // Erstelle normale Kollisions-Fixture für andere Objekte (Blöcke, etc.)
            FixtureDef normalFixtureDef = new FixtureDef();
            normalFixtureDef.shape = shape;
            normalFixtureDef.density = 1f;
            normalFixtureDef.friction = 1f;
            normalFixtureDef.restitution = 0f;
            normalFixtureDef.filter.categoryBits = 0x0002; // Item Kategorie
            normalFixtureDef.filter.maskBits = 0x0001;     // Kollidiert nur mit der Welt (nicht mit Spielern)
            body.createFixture(normalFixtureDef);

            // Erstelle Sensor-Fixture für Kollisionserkennung mit Spielern
            FixtureDef sensorFixtureDef = new FixtureDef();
            sensorFixtureDef.shape = shape;
            sensorFixtureDef.isSensor = true;
            sensorFixtureDef.filter.categoryBits = 0x0004; // Sensor Kategorie
            sensorFixtureDef.filter.maskBits = 0x0008;     // Kollidiert nur mit Spielern
            body.createFixture(sensorFixtureDef);

            shape.dispose();

            body.setUserData(this);
        });
    }

    // Optionaler Fallback-Konstruktor für Abwärtskompatibilität:
    public ItemActor(World world, float x, float y)
    {
        this(world, x, y, ItemRegistry.createItem("sword"),1);
        System.out.println("fallback construcktor used");
    }

    public Item getInventoryItem()
    {
        return inventoryItem;
    }

    @Override
    public void act(float delta)
    {
        super.act(delta);

        existenceTime += delta;

        if (body != null)
        {
            float s = Block.BLOCK_SIZE;
            setPosition(body.getPosition().x * s - getWidth() / 2f,
                body.getPosition().y * s - getHeight() / 2f);
            Vector2 v = body.getLinearVelocity();
            if (Math.abs(v.y) < 0.1f && Math.abs(v.x) > 0.01f)
            {
                body.setLinearVelocity(v.x * 0.8f, v.y);
                if (Math.abs(v.x) < 0.05f)
                {
                    body.setLinearVelocity(0, 0);
                }
            }
        }

        // Check if we should pick up the item when the existence timer expires
        // while the player is still in contact
        if (existenceTime >= 2f && contactingPlayer != null && !destroyed) {
            pickupItem(contactingPlayer);
        }
    }

    @Override
    public void draw(Batch batch, float parentAlpha)
    {
        batch.draw(texture, getX(), getY(), getWidth(), getHeight());
    }

    public void onPlayerContact(Player player)
    {
        contactingPlayer = player;

        if (existenceTime < 2f)
        {
            return; // Still in cooldown period
        }

        pickupItem(player);
    }

    public int getAmount()
    {
        return amount;
    }

    public void onPlayerEndContact(Player player)
    {
        if (contactingPlayer == player) {
            contactingPlayer = null;
        }
    }

    private void pickupItem(Player player)
    {
        if (!destroyed)
        {
            destroyed = true;
            player.getInventory().addItem(inventoryItem, amount);  // Use the amount when adding to inventory
            System.out.println("picking uo");
            try{
                NetworkPackets.InventoryUpdatePacket updatePacket = new NetworkPackets.InventoryUpdatePacket();
                updatePacket.hotbarData = player.getInventory().serializeHotbar();
                updatePacket.mainInventoryData = player.getInventory().serializeMain();
                NetworkManager.getInstance().sendToPlayerTCP(updatePacket, player);
            }
            catch (IllegalStateException e)
            {
                // Silent catch
            }

            Box2DOperationManager.queueOperation(() ->
            {
                world.destroyBody(body);
                Globals.mobs.removeMob(this);
            });
        }
    }
}

