package at.peckventure.entities.mob;

import at.peckventure.Textures;
import at.peckventure.world.Box2DOperationManager;
import at.peckventure.world.block.Block;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.physics.box2d.*;

public class Beetle extends Mob
{
    private final Texture texture;
    private Body body;

    public Beetle(World world, float x, float y)
    {
        super(world, x, y);
        // Lade die Textur über dein Textur-Management (hier ein Beispiel)
        texture = Textures.BEETLE.getTexture();
        setSize(32, 32);

        // Erstelle den Box2D-Körper über die Operation-Queue
        Box2DOperationManager.queueOperation(() ->
        {
            BodyDef bodyDef = new BodyDef();
            bodyDef.type = BodyDef.BodyType.DynamicBody;
            // Positioniere den Body in der Mitte des Mobs (Umrechnung in Meter: Pixel / BLOCK_SIZE)
            bodyDef.position.set((x + getWidth() / 2f) / Block.BLOCK_SIZE,
                (y + getHeight() / 2f) / Block.BLOCK_SIZE);
            body = world.createBody(bodyDef);

            // Erstelle eine kreisförmige Kollisionsform
            CircleShape shape = new CircleShape();
            shape.setRadius(getWidth() / 2f / Block.BLOCK_SIZE);
            FixtureDef fixtureDef = new FixtureDef();
            fixtureDef.shape = shape;
            fixtureDef.density = 1f;
            fixtureDef.friction = 0.5f;
            fixtureDef.restitution = 0f;
            body.createFixture(fixtureDef);
            shape.dispose();

            body.setUserData(this);
        });
    }

    @Override
    public void draw(Batch batch, float parentAlpha)
    {
        batch.draw(texture, getX(), getY(), getWidth(), getHeight());
    }

    @Override
    public void update(float delta)
    {
        if (body != null)
        {
            // Aktualisiere die Position anhand des Box2D-Körpers (Umrechnung Meter->Pixel)
            float newX = body.getPosition().x * Block.BLOCK_SIZE - getWidth() / 2f;
            float newY = body.getPosition().y * Block.BLOCK_SIZE - getHeight() / 2f;
            setPosition(newX, newY);
        }
    }

    @Override
    public void dispose()
    {
        //texture.dispose();
        if (body != null)
        {
            Box2DOperationManager.queueOperation(() ->
            {
                if (body.getWorld() != null)
                {
                    body.getWorld().destroyBody(body);
                }
            });
        }
        super.dispose();
    }
}
