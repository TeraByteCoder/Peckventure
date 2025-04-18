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

        // Handle player and item contact
        if (userDataA instanceof Player && userDataB instanceof ItemActor)
        {
            ((ItemActor) userDataB).onPlayerContact((Player) userDataA);
        }
        else if (userDataA instanceof ItemActor && userDataB instanceof Player)
        {
            ((ItemActor) userDataA).onPlayerContact((Player) userDataB);
        }

        // Check if it's Player and Block contact
        if ((userDataA instanceof Player && userDataB instanceof Block)
            || (userDataA instanceof Block && userDataB instanceof Player)
            || (userDataA instanceof Mob && userDataB instanceof Mob)
            || (userDataA instanceof Block && userDataB instanceof Mob))
        {
            Block block = null;
            if (userDataA instanceof Block) block = (Block) userDataA;
            else if (userDataB instanceof Block) block = (Block) userDataB;

            // If the block has disabled collisions: suppress the physics reaction
            if (block != null && !block.isCollisionEnabled())
            {
                contact.setEnabled(false); // Collision is ignored
            }
        }
    }

    @Override
    public void endContact(Contact contact)
    {
        Object userDataA = contact.getFixtureA().getBody().getUserData();
        Object userDataB = contact.getFixtureB().getBody().getUserData();

        // Handle player leaving item contact
        if (userDataA instanceof Player && userDataB instanceof ItemActor)
        {
            ((ItemActor) userDataB).onPlayerEndContact((Player) userDataA);
        }
        else if (userDataA instanceof ItemActor && userDataB instanceof Player)
        {
            ((ItemActor) userDataA).onPlayerEndContact((Player) userDataB);
        }
    }


    @Override
    public void preSolve(Contact contact, Manifold oldManifold)
    {
        // Check collision between Player and Block
        Object userDataA = contact.getFixtureA().getBody().getUserData();
        Object userDataB = contact.getFixtureB().getBody().getUserData();

        // Prüfe, ob es eine Kollision zwischen Player und ItemActor ist
        if ((userDataA instanceof Player && userDataB instanceof ItemActor) ||
            (userDataA instanceof ItemActor && userDataB instanceof Player)) {

            // Für normale Kollisionen (nicht Sensor) zwischen Player und ItemActor
            // deaktivieren wir die Kollision
            if (!contact.getFixtureA().isSensor() && !contact.getFixtureB().isSensor()) {
                contact.setEnabled(false);
            }
        }

        // Originaler Code für Block-Kollisionen
        if ((userDataA instanceof Player && userDataB instanceof Block)
            || (userDataA instanceof Block && userDataB instanceof Player)
            || (userDataA instanceof Mob && userDataB instanceof Mob)
            || (userDataA instanceof Block && userDataB instanceof Mob))
        {
            Block block = null;
            if (userDataA instanceof Block) block = (Block) userDataA;
            else if (userDataB instanceof Block) block = (Block) userDataB;

            // If the block has disabled collisions: suppress the physics reaction
            if (block != null && !block.isCollisionEnabled())
            {
                contact.setEnabled(false); // Collision is ignored
            }
        }
    }

    @Override
    public void postSolve(Contact contact, ContactImpulse impulse)
    {
        // No implementation needed
    }
}
