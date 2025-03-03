package at.peckventure.entities.mob;

import com.badlogic.gdx.physics.box2d.World;

@FunctionalInterface
public interface MobCreator
{
    Mob create(World world, float x, float y, Object... args);
}
