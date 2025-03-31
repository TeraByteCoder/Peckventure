package at.peckventure;

import at.peckventure.entities.ControlledPlayer;
import at.peckventure.entities.Player;
import at.peckventure.world.block.Block;
import com.badlogic.gdx.physics.box2d.*;

public class ExtendedGameContactListener extends GameContactListener {

    @Override
    public void beginContact(Contact contact) {
        // Zuerst den Basis-Listener aufrufen (z.B. für ItemActor-Interaktionen)
        super.beginContact(contact);

        // Dann den Bodenkontakt prüfen
        Object userDataA = contact.getFixtureA().getBody().getUserData();
        Object userDataB = contact.getFixtureB().getBody().getUserData();

        // Prüfe, ob der Kontakt zwischen einem Player und einem Block (Boden) besteht
        if (userDataA instanceof ControlledPlayer && userDataB instanceof Block) {
            ((ControlledPlayer) userDataA).setOnGround(true);
        } else if (userDataB instanceof ControlledPlayer && userDataA instanceof Block) {
            ((ControlledPlayer) userDataB).setOnGround(true);
        }
    }

    @Override
    public void endContact(Contact contact) {
        // Basis-Listener aufrufen (falls benötigt)
        super.endContact(contact);

        // Beim Verlassen der Kollision den Bodenkontakt zurücksetzen
        Object userDataA = contact.getFixtureA().getBody().getUserData();
        Object userDataB = contact.getFixtureB().getBody().getUserData();

        if (userDataA instanceof Player && userDataB instanceof Block) {
            ((ControlledPlayer) userDataA).setOnGround(false);
        } else if (userDataB instanceof Player && userDataA instanceof Block) {
            ((ControlledPlayer) userDataB).setOnGround(false);
        }
    }

    @Override
    public void preSolve(Contact contact, Manifold oldManifold) {
        super.preSolve(contact, oldManifold);
    }

    @Override
    public void postSolve(Contact contact, ContactImpulse impulse) {
        super.postSolve(contact, impulse);
    }
}
