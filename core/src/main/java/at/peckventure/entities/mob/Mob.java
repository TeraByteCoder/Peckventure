package at.peckventure.entities.mob;

import at.peckventure.Globals;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.physics.box2d.World;

public abstract class Mob extends Actor
{
    protected World world;

    public Mob(World world, float x, float y)
    {
        this.world = world;
        setPosition(x, y);
    }

    @Override
    public abstract void draw(Batch batch, float parentAlpha);


    public void dispose()
    {
        // Ressourcen freigeben, z. B. Box2D-Körper zerstören, falls vorhanden.
    }

    @Override
    public void act(float delta) {
        super.act(delta);
    }

}
