package at.peckventure.world.block;

import com.badlogic.gdx.physics.box2d.World;

@FunctionalInterface
public interface BlockCreator
{
    /**
     * Erzeugt einen neuen Block.
     *
     * @param world  Die Box2D-World.
     * @param worldX Die X-Position in Weltkoordinaten.
     * @param worldY Die Y-Position in Weltkoordinaten.
     * @param args   Optionale weitere Parameter (z.B. spezielle Eigenschaften).
     * @return Den erzeugten Block.
     */
    Block create(World world, int worldX, int worldY, Object... args);
}
