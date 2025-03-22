package at.peckventure.entities;

import at.peckventure.multiplayer.NetworkPackets;
import at.peckventure.world.block.Block;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.physics.box2d.World;

public class RemotePlayer extends Player
{
    private Sprite sprite;
    private World world;

    public RemotePlayer(World world, float x, float y)
    {
        super(world, x, y);
        this.sprite = new Sprite(new Texture("textures/woodpecker/woodpecker_idle.png"));
    }

    @Override
    protected void handleInput(float delta)
    {

    }

    // Wird vom PlayerManager aufgerufen, wenn ein Update-Paket eintrifft.
    public void updateFromPacket(NetworkPackets.PlayerUpdatePacket packet)
    {
        if (body != null)
        {
            setPosition(packet.x, packet.y);
            body.setTransform(packet.x / Block.BLOCK_SIZE, packet.y / Block.BLOCK_SIZE, body.getAngle());
        }
    }

    @Override
    public void draw(Batch batch, float parentAlpha)
    {
        sprite.setPosition(getX(), getY());
        sprite.draw(batch);
    }

}
