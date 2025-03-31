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

    protected boolean direction = false;

    private boolean disposed = false;
    protected boolean movementDisabled = false;

    // --- Neue Felder für Interpolation ---
    protected float targetX, targetY;          // Zielposition (Server-Update)
    protected float interpolationTime = 0f;
    protected final float interpolationDuration = 0.1f; // 100ms Interpolationsdauer

    public Mob(World world, float x, float y)
    {
        this.world = world;
        setPosition(x, y);
        this.targetX = x;
        this.targetY = y;
    }

    @Override
    public abstract void draw(Batch batch, float parentAlpha);

    public Body getBody()
    {
        return body;
    }

    public void setMovementDisabled(boolean disabled) {
        if(disabled)
        {
            Box2DOperationManager.queueOperation(() ->
            {
                world.destroyBody(body);
                this.body = null;
            });
        }
        this.movementDisabled = disabled;
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

    public void setTargetPosition(float x, float y) {
        this.targetX = x;
        this.targetY = y;
        // Reset der Zeit, damit die Interpolation neu startet
        this.interpolationTime = 0f;
    }

    public void setDirection(boolean direction)
    {
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        // Wenn ein physikalischer Body existiert und movement nicht disabled ist, benutze ihn:
        if (body != null && !movementDisabled) {
            float s = Block.BLOCK_SIZE;
            setPosition(body.getPosition().x * s - getWidth() / 2f,
                body.getPosition().y * s - getHeight() / 2f);
        } else {
            // Falls kein Body vorhanden ist (Remote-Mobs) – benutze Interpolation:
            interpolationTime += delta;
            float alpha = Math.min(interpolationTime / interpolationDuration, 1f);
            float newX = getX() + (targetX - getX()) * alpha;
            float newY = getY() + (targetY - getY()) * alpha;
            setPosition(newX, newY);
        }
    }


    public int getChunkX()
    {
        return (int) this.getX() / Block.BLOCK_SIZE / Chunk.CHUNK_SIZE;
    }

    public int getChunkY()
    {
        return (int) this.getY() / Block.BLOCK_SIZE / Chunk.CHUNK_SIZE;
    }

    public boolean isDirection()
    {
        return direction;
    }
}
