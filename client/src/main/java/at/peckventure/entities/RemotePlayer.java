package at.peckventure.entities;

import at.peckventure.multiplayer.NetworkPackets;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Queue;

public class RemotePlayer extends Player {
    private Sprite sprite;
    private static final int BUFFER_MS = 100;

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
        super(world, x, y);
        this.sprite = new Sprite(new Texture("textures/woodpecker/woodpecker_idle.png"));
        long now = System.currentTimeMillis();
        positionBuffer.addLast(new PositionUpdate(x, y, true, now));
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
    public void act(float delta) {
        super.act(delta);
        long renderTime = System.currentTimeMillis() - BUFFER_MS;

        while (positionBuffer.size >= 2 && positionBuffer.get(1).time <= renderTime) {
            positionBuffer.removeFirst();
        }

        if (positionBuffer.size >= 2) {
            PositionUpdate a = positionBuffer.get(0);
            PositionUpdate b = positionBuffer.get(1);

            float t = (float) (renderTime - a.time) / (b.time - a.time);
            t = Math.max(0f, Math.min(1f, t));

            float interpX = a.x + (b.x - a.x) * t;
            float interpY = a.y + (b.y - a.y) * t;

            setPosition(interpX, interpY);

            // Hier Flippen wir den Sprite basierend auf der Blickrichtung
            if (t < 0.5f) {
                sprite.setFlip(a.facingRight, false); // Spiegeln, wenn Richtung 'facingRight' ist
            } else {
                sprite.setFlip(b.facingRight, false); // Spiegeln, wenn Richtung 'facingRight' ist
            }
        } else if (positionBuffer.size == 1) {
            PositionUpdate a = positionBuffer.get(0);
            setPosition(a.x, a.y);
            sprite.setFlip(a.facingRight, false); // Spiegeln, wenn Richtung 'facingRight' ist
        }
    }



    @Override
    public void draw(Batch batch, float parentAlpha) {
        sprite.setPosition(getX(), getY());
        sprite.setRotation(getRotation());
        sprite.draw(batch);
    }
}
