package at.peckventure.entities;

import com.badlogic.gdx.physics.box2d.World;

public class ServerPlayer extends Player
{
    public ServerPlayer(World world, float x, float y) {
        super(world, x, y);
    }

    @Override
    protected void handleInput(float delta) {
    }


}
