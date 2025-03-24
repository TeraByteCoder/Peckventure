package at.peckventure.entities;

import at.peckventure.world.block.Block;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import at.peckventure.InputManager;

public class ControlledPlayer extends Player {
    private static ControlledPlayer instance;

    private boolean facingRight = true;


    private ControlledPlayer(World world, float x, float y) {
        super(world, x, y);
        this.sprite = new Sprite(new Texture("textures/woodpecker/woodpecker_idle.png"));
    }

    public static ControlledPlayer getInstance(World world, float x, float y) {
        if (instance == null) {
            instance = new ControlledPlayer(world, x, y);
        }
        return instance;
    }

    public static ControlledPlayer getInstance() {
        if (instance == null) {
            throw new IllegalStateException("ControlledPlayer not initialized. Call getInstance(world, x, y) first.");
        }
        return instance;
    }

    @Override
    protected void handleInput(float delta) {
        float direction = 0;
        if (InputManager.getInstance().isLeftPressed()) {
            direction = -1;
            facingRight = false;
            sprite.setFlip(false, false);
        }
        if (InputManager.getInstance().isRightPressed()) {
            direction = 1;
            facingRight = true;
            sprite.setFlip(true, false);
        }

        setRotation(facingRight ? 0 : 180);

        Vector2 vel = body.getLinearVelocity();
        body.setLinearVelocity(direction * speed / Block.BLOCK_SIZE, vel.y);

        float bodyYPixels = body.getPosition().y * Block.BLOCK_SIZE;
        float relativeHeight = startY - bodyYPixels;
        if (InputManager.getInstance().isJumpPressed()) {
            if (relativeHeight < maxHeight) {
                body.setLinearVelocity(body.getLinearVelocity().x, flyForce / Block.BLOCK_SIZE);
            } else {
                body.setLinearVelocity(body.getLinearVelocity().x, hoverDampening / Block.BLOCK_SIZE);
            }
        }
    }

    public boolean isFacingRight() {
        return facingRight;
    }

}
