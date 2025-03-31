package at.peckventure;

import at.peckventure.entities.Player;
import at.peckventure.entities.mob.ItemActor;
import com.badlogic.gdx.physics.box2d.*;

public class GameContactListener implements ContactListener {

    @Override
    public void beginContact(Contact contact) {
        Object userDataA = contact.getFixtureA().getBody().getUserData();
        Object userDataB = contact.getFixtureB().getBody().getUserData();

        if (userDataA instanceof Player && userDataB instanceof ItemActor) {
            ((ItemActor) userDataB).onPlayerContact((Player) userDataA);
        } else if (userDataA instanceof ItemActor && userDataB instanceof Player) {
            ((ItemActor) userDataA).onPlayerContact((Player) userDataB);
        }
    }

    @Override
    public void endContact(Contact contact) {
        // Hier kannst du ggf. Logik implementieren, wenn die Kollision endet.
    }

    @Override
    public void preSolve(Contact contact, Manifold oldManifold) {
    }

    @Override
    public void postSolve(Contact contact, ContactImpulse impulse) {
    }
}
