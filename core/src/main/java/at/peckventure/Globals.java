package at.peckventure;

import at.peckventure.entities.MobManager;
import at.peckventure.entities.Player;
import at.peckventure.inventory.InventoryUI;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.scenes.scene2d.Stage;

public class Globals
{
    public static InventoryUI inventoryUI;
    public static Player player;
    public static World physicsWorld;
    public static Stage gamestage;

    public static MobManager mobManager;
}
