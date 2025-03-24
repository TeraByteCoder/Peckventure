package at.peckventure.entities;

import at.peckventure.multiplayer.NetworkPackets;
import at.peckventure.world.block.Block;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.physics.box2d.World;

public class RemotePlayer extends Player {
    private Sprite sprite;
    private float prevX, prevY;
    private float targetX, targetY;
    private long prevTime, targetTime;

    public RemotePlayer(World world, float x, float y) {
        super(world, x, y);
        this.sprite = new Sprite(new Texture("textures/woodpecker/woodpecker_idle.png"));
        this.prevX = x;
        this.prevY = y;
        this.targetX = x;
        this.targetY = y;
        this.prevTime = System.currentTimeMillis();
        this.targetTime = this.prevTime;
    }

    @Override
    protected void handleInput(float delta) {
        // RemotePlayer empfängt keine Eingaben.
    }

    public void updateFromPacket(NetworkPackets.PlayerUpdatePacket packet) {
        // Alte Ziel-Position wird zum neuen Startpunkt
        this.prevX = this.targetX;
        this.prevY = this.targetY;
        this.prevTime = this.targetTime;

        // Neue Zielposition + Zeit
        this.targetX = packet.x;
        this.targetY = packet.y;
        this.targetTime = packet.time;
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        long now = System.currentTimeMillis();
        float t = (targetTime - prevTime) == 0 ? 1f : (float)(now - prevTime) / (targetTime - prevTime);
        t = Math.min(Math.max(t, 0f), 1f); // clamp to [0,1]
        float interpolatedX = prevX + (targetX - prevX) * t;
        float interpolatedY = prevY + (targetY - prevY) * t;

        setPosition(interpolatedX, interpolatedY);
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        sprite.setPosition(getX(), getY());
        sprite.draw(batch);
    }
}
