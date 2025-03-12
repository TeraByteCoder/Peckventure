package at.peckventure;

import at.peckventure.entities.Player;
import at.peckventure.entities.mob.Mob;
import at.peckventure.world.InfiniteTilemap;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class Globals
{
    public static World physicsWorld;

    public static InfiniteTilemap infiniteTilemap;

    public static List<Mob> mobs = Collections.synchronizedList(new LinkedList<>());

}
