package at.peckventure.entities;

import at.peckventure.Const;
import at.peckventure.Textures;
import at.peckventure.inventory.item.Item;
import at.peckventure.multiplayer.NetworkPackets;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Queue;

public class RemotePlayer extends Player {
    private Sprite sprite;
    private static final int BUFFER_MS = 100;

    private boolean isPecking = false;
    private float tongueLength = 0f;
    private boolean isExtending = false;
    private Vector2 tongueTarget = new Vector2();
    private static final float TONGUE_EXTEND_SPEED = 800f;
    private static final float TONGUE_RETRACT_SPEED = 1000f;
    private Color tongueColor = new Color(1f, 0.5f, 0.5f, 1f);
    private Texture pixelTexture;
    private boolean facingRight = true;

    // Buffer-Eintrag, der nun auch die Rotation speichert
    private static class PositionUpdate {
        float x, y;
        boolean facingRight;
        long time;

        PositionUpdate(float x, float y, boolean facingRight, long time) {
            this.x = x;
            this.y = y;
            this.facingRight = facingRight;
            this.time = time;
        }

    }


    private final Queue<PositionUpdate> positionBuffer = new Queue<>();

    public RemotePlayer(World world, float x, float y) {
        super(world, x, y, Textures.WOODPECKER_IDLE);
        long now = System.currentTimeMillis();
        positionBuffer.addLast(new PositionUpdate(x, y, true, now));

        // Initialize pixel texture for tongue
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        pixelTexture = new Texture(pixmap);
        pixmap.dispose();
    }

    @Override
    protected void handleInput(float delta) {
        // RemotePlayer verarbeitet keine Eingaben
    }

    public void updateFromPacket(NetworkPackets.PlayerUpdatePacket packet) {
        positionBuffer.addLast(new PositionUpdate(packet.x, packet.y, packet.rotation, packet.time));
        if (positionBuffer.size > 20) {
            positionBuffer.removeFirst();
        }
    }

    @Override
    public void dropItemOutside(Item item, int amount)
    {

    }

    @Override
    public void act(float delta) {
        super.act(delta);

        // Update existing position interpolation code
        long renderTime = System.currentTimeMillis() - BUFFER_MS;

        // Remove old updates from buffer
        while (positionBuffer.size >= 2 && positionBuffer.get(1).time <= renderTime) {
            positionBuffer.removeFirst();
        }

        if (positionBuffer.size >= 2) {
            PositionUpdate a = positionBuffer.get(0);
            PositionUpdate b = positionBuffer.get(1);

            float t = (float) (renderTime - a.time) / (b.time - a.time);
            t = Math.max(0f, Math.min(1f, t));

            // Smoothstep function for interpolation
            t = t * t * (3 - 2 * t);

            float interpX = a.x + (b.x - a.x) * t;
            float interpY = a.y + (b.y - a.y) * t;

            setPosition(interpX, interpY);

            // Update facing direction
            facingRight = (t < 0.5f) ? a.facingRight : b.facingRight;
            sprite.setFlip(facingRight, false);
        } else if (positionBuffer.size == 1) {
            PositionUpdate a = positionBuffer.get(0);
            setPosition(a.x, a.y);
            facingRight = a.facingRight;
            sprite.setFlip(facingRight, false);
        }

        // Update tongue animation if active
        if (isPecking) {
            updateTongueAnimation(delta);
        }
    }

    public void startPeckAnimation(float targetX, float targetY) {
        isPecking = true;
        isExtending = true;
        tongueLength = 0f;
        tongueTarget.set(targetX, targetY);
    }




    // Override draw method to include tongue
    @Override
    public void draw(Batch batch) {
        // Draw tongue if pecking
        if (isPecking && tongueLength > 0) {
            drawTongue(batch);
        }

        // Draw the player sprite
        sprite.setPosition(getX(), getY());
        sprite.setRotation(getRotation());
        sprite.draw(batch);
    }
    // Add method for updating tongue animation
    private void updateTongueAnimation(float delta) {
        if (!isPecking) return;

        if (isExtending) {
            // Extend tongue
            tongueLength += TONGUE_EXTEND_SPEED * delta;

            // Check if the tongue has reached its target
            float distanceToTarget = calculateDistanceToTarget();
            if (tongueLength >= distanceToTarget) {
                tongueLength = distanceToTarget;
                isExtending = false; // Start retracting
            }
        } else {
            // Retract tongue
            tongueLength -= TONGUE_RETRACT_SPEED * delta;

            // If the tongue is fully retracted
            if (tongueLength <= 0) {
                tongueLength = 0;
                isPecking = false;
            }
        }
    }

    // Add method to calculate distance to target
    private float calculateDistanceToTarget() {
        // Beak position (origin of the tongue)
        float startX = facingRight ?
            getX() + getWidth() * 0.8f : // Further in the woodpecker for the beak
            getX() + getWidth() * 0.2f;  // Further in the woodpecker for the beak
        float startY = getY() + getHeight() * 0.75f; // Adjusted for beak height

        float dx = tongueTarget.x - startX;
        float dy = tongueTarget.y - startY;

        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    // Add method to draw the tongue
    private void drawTongue(Batch batch) {
        // Beak position (origin of the tongue)
        float startX = facingRight ?
            getX() + getWidth() * 0.8f : // Further in the woodpecker for the beak
            getX() + getWidth() * 0.2f;  // Further in the woodpecker for the beak
        float startY = getY() + getHeight() * 0.75f; // Adjusted for beak height

        // Calculate direction to target
        float dx = tongueTarget.x - startX;
        float dy = tongueTarget.y - startY;
        float angle = (float) Math.atan2(dy, dx) * MathUtils.radiansToDegrees;

        // Save original batch color
        Color originalColor = batch.getColor().cpy();

        // Set color for tongue
        batch.setColor(tongueColor);

        // Draw tongue as a stretchable rectangle
        batch.draw(
            pixelTexture,
            startX,
            startY - 3, // 3 pixels from center to make tongue 6 pixels wide
            0,
            3,
            tongueLength,
            6,
            1,
            1,
            angle,
            0,
            0,
            1,
            1,
            false,
            false
        );

        // Reset batch color
        batch.setColor(originalColor);
    }
}
