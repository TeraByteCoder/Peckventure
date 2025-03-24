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
        float rotation;
        long time;

        PositionUpdate(float x, float y, float rotation, long time) {
            this.x = x;
            this.y = y;
            this.rotation = rotation;
            this.time = time;
        }
    }

    private final Queue<PositionUpdate> positionBuffer = new Queue<>();

    public RemotePlayer(World world, float x, float y) {
        super(world, x, y);
        this.sprite = new Sprite(new Texture("textures/woodpecker/woodpecker_idle.png"));
        long now = System.currentTimeMillis();
        float initialRotation = 0; // Standard: nach rechts (0°)
        positionBuffer.addLast(new PositionUpdate(x, y, initialRotation, now));
    }

    @Override
    protected void handleInput(float delta) {
        // RemotePlayer verarbeitet keine Eingaben
    }

    public void updateFromPacket(NetworkPackets.PlayerUpdatePacket packet) {
        // Wir wandeln den boolean in einen Rotationswert um:
        // true = 0° (nach rechts), false = 180° (nach links)
        float newRotation = packet.rotation ? 0 : 180;
        positionBuffer.addLast(new PositionUpdate(packet.x, packet.y, newRotation, packet.time));

        // Buffer klein halten (maximal 20 Einträge)
        if (positionBuffer.size > 20) {
            positionBuffer.removeFirst();
        }
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        long renderTime = System.currentTimeMillis() - BUFFER_MS;

        // Entferne alte Einträge, die vor dem aktuellen RenderTime liegen
        while (positionBuffer.size >= 2 && positionBuffer.get(1).time <= renderTime) {
            positionBuffer.removeFirst();
        }

        if (positionBuffer.size >= 2) {
            PositionUpdate a = positionBuffer.get(0);
            PositionUpdate b = positionBuffer.get(1);
            float t = (float) (renderTime - a.time) / (b.time - a.time);
            t = Math.max(0f, Math.min(1f, t)); // clamp auf [0, 1]

            float interpX = a.x + (b.x - a.x) * t;
            float interpY = a.y + (b.y - a.y) * t;
            float interpRotation = a.rotation + (b.rotation - a.rotation) * t;

            setPosition(interpX, interpY);
            setRotation(interpRotation);
        } else if (positionBuffer.size == 1) {
            PositionUpdate a = positionBuffer.get(0);
            setPosition(a.x, a.y);
            setRotation(a.rotation);
        }
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        sprite.setPosition(getX(), getY());
        sprite.setRotation(getRotation());
        sprite.draw(batch);
    }
}
