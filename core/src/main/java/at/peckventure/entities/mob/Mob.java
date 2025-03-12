package at.peckventure.entities.mob;

import at.peckventure.Globals;
import at.peckventure.world.Box2DOperationManager;
import at.peckventure.world.block.Block;
import at.peckventure.world.chunk.Chunk;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.physics.box2d.World;

public abstract class Mob extends Actor
{
    protected World world;

    protected Body body;

    private boolean disposed = false;

    public Mob(World world, float x, float y)
    {
        this.world = world;
        setPosition(x, y);
    }

    @Override
    public abstract void draw(Batch batch, float parentAlpha);

    public Body getBody()
    {
        return body;
    }

    public void dispose() {
        if (!disposed && body != null) {
            disposed = true;
            Box2DOperationManager.queueOperation(() -> {
                if (body != null && body.getWorld() != null) {
                    body.getWorld().destroyBody(body);
                }
            });
            body = null;
        }
    }

    @Override
    public void act(float delta) {
        super.act(delta);
    }

    public int getChunkX()
    {
        return (int) this.getX() / Block.BLOCK_SIZE / Chunk.CHUNK_SIZE;
    }

    public int getChunkY()
    {
        return (int) this.getY() / Block.BLOCK_SIZE / Chunk.CHUNK_SIZE;
    }
}
