package at.peckventure;

import at.peckventure.entities.Player;
import at.peckventure.entities.mob.ItemActor;
import at.peckventure.entities.mob.Mob;
import at.peckventure.world.block.Block;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Manifold;

public class GameContactListener implements ContactListener
{

    @Override
    public void beginContact(Contact contact)
    {
        Object userDataA = contact.getFixtureA().getBody().getUserData();
        Object userDataB = contact.getFixtureB().getBody().getUserData();

        if (userDataA instanceof Player && userDataB instanceof ItemActor)
        {
            ((ItemActor) userDataB).onPlayerContact((Player) userDataA);
        } else if (userDataA instanceof ItemActor && userDataB instanceof Player)
        {
            ((ItemActor) userDataA).onPlayerContact((Player) userDataB);
        }

        // Checke, ob es sich um Player und Block handelt
        if ((userDataA instanceof Player && userDataB instanceof Block)
            || (userDataA instanceof Block && userDataB instanceof Player)
            || (userDataA instanceof Mob && userDataB instanceof Mob)
            || (userDataA instanceof Block && userDataB instanceof Mob))
        {

            Block block = null;
            if (userDataA instanceof Block) block = (Block) userDataA;
            else if (userDataB instanceof Block) block = (Block) userDataB;

            // Wenn der Block Kollisionen deaktiviert hat: Unterdrücke die Physik-Reaktion
            if (block != null && !block.isCollisionEnabled())
            {
                contact.setEnabled(false); // Kollision wird ignoriert
            }
        }
    }

    @Override
    public void endContact(Contact contact)
    {
        // Hier kannst du ggf. Logik implementieren, wenn die Kollision endet.
    }

    @Override
    public void preSolve(Contact contact, Manifold oldManifold)
    {
        // Kollision zwischen Player und Block prüfen
        Object userDataA = contact.getFixtureA().getBody().getUserData();
        Object userDataB = contact.getFixtureB().getBody().getUserData();

        if ((userDataA instanceof Player && userDataB instanceof Block)
            || (userDataA instanceof Block && userDataB instanceof Player)
            || (userDataA instanceof Mob && userDataB instanceof Mob)
            || (userDataA instanceof Block && userDataB instanceof Mob))
        {

            Block block = null;
            if (userDataA instanceof Block) block = (Block) userDataA;
            else if (userDataB instanceof Block) block = (Block) userDataB;

            // Wenn der Block Kollisionen deaktiviert hat: Unterdrücke die Physik-Reaktion
            if (block != null && !block.isCollisionEnabled())
            {
                contact.setEnabled(false); // Kollision wird ignoriert
            }
        }
    }

    @Override
    public void postSolve(Contact contact, ContactImpulse impulse)
    {
    }
}
