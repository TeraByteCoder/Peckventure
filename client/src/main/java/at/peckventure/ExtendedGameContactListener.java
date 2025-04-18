package at.peckventure;

import at.peckventure.entities.ControlledPlayer;
import at.peckventure.entities.Player;
import at.peckventure.world.block.Block;
import at.peckventure.world.Box2DOperationManager;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;

/**
 * A completely rewritten contact listener focused on direct tree landing
 */
public class ExtendedGameContactListener extends GameContactListener {

    // Debug flag - set to true for verbose logging
    private static final boolean DEBUG = true;

    private void debugLog(String message) {
        if (DEBUG) {
            System.out.println("[TREE_DEBUG] " + message);
        }
    }

    /**
     * Check if a block is a tree (has collision disabled)
     */
    private boolean isTreeBlock(Block block) {
        boolean isTree = !block.isCollisionEnabled();
        if (isTree) {
            debugLog("Found a tree block: " + block);
        }
        return isTree;
    }

    @Override
    public void beginContact(Contact contact) {
        // Don't call super.beginContact yet - we need to handle tree landing first

        Object userDataA = contact.getFixtureA().getBody().getUserData();
        Object userDataB = contact.getFixtureB().getBody().getUserData();

        debugLog("Contact between: " + userDataA + " and " + userDataB);

        // Handle tree landing logic
        boolean handled = false;

        if (userDataA instanceof ControlledPlayer && userDataB instanceof Block) {
            handled = handleTreeLanding((ControlledPlayer)userDataA, (Block)userDataB, contact);
        }
        else if (userDataB instanceof ControlledPlayer && userDataA instanceof Block) {
            handled = handleTreeLanding((ControlledPlayer)userDataB, (Block)userDataA, contact);
        }

        // Only call super.beginContact if we didn't handle it ourselves
        if (!handled) {
            super.beginContact(contact);
        }
    }

    /**
     * Direct tree landing handler - returns true if handled, false otherwise
     */
    private boolean handleTreeLanding(ControlledPlayer player, Block block, Contact contact) {
        // Skip if not a tree
        if (!isTreeBlock(block)) {
            // For non-tree blocks, set ground contact if appropriate
            if (!player.isAttachedToTree()) {
                player.setOnGround(true);
            }
            return false;
        }

        debugLog("Processing tree landing for player at pos: " + player.getX() + "," + player.getY());

        // Check landing conditions
        boolean isJumping = InputManager.getInstance().isJumpPressed();
        boolean isMovingHorizontally = InputManager.getInstance().isLeftPressed() ||
            InputManager.getInstance().isRightPressed();
        boolean isOnGround = player.isOnGround();
        boolean isAttachedToTree = player.isAttachedToTree();

        // Calculate player position relative to tree
        float playerCenterX = player.getX() + player.getWidth() / 2;
        float blockCenterX = block.getX() + block.getWidth() / 2;
        float distanceFromCenter = Math.abs(playerCenterX - blockCenterX);
        boolean isNearEdge = distanceFromCenter > block.getWidth() / 4;

        debugLog("Tree Landing Conditions - Jumping: " + isJumping +
            ", Moving: " + isMovingHorizontally +
            ", OnGround: " + isOnGround +
            ", AttachedToTree: " + isAttachedToTree +
            ", NearEdge: " + isNearEdge +
            ", DistFromCenter: " + distanceFromCenter);

        // Now for the actual landing logic

        // CASE 1: Already attached to a tree
        if (isAttachedToTree) {
            debugLog("Already attached to tree - disabling collision");
            contact.setEnabled(false);
            return true;
        }

        // CASE 2: On the ground - should pass through trees
        if (isOnGround) {
            debugLog("On ground - disabling tree collision");
            contact.setEnabled(false);
            return true;
        }

        // CASE 3: In the middle of a tree - should pass through
        if (!isNearEdge) {
            debugLog("In middle of tree - disabling collision");
            contact.setEnabled(false);
            return true;
        }

        // CASE 4: Flying - should pass through trees
        if (isJumping) {
            debugLog("Flying through tree - disabling collision");
            contact.setEnabled(false);
            return true;
        }

        // CASE 5: THE LANDING CASE - In air, not flying, moving horizontally, near edge
        if (!isOnGround && !isJumping && isMovingHorizontally && isNearEdge) {
            debugLog("LANDING ON TREE DETECTED!");

            // Directly modify player's body - this is critical for responsive landing
            Box2DOperationManager.queueOperation(() -> {
                // First stop any vertical movement
                player.getBody().setLinearVelocity(player.getBody().getLinearVelocity().x, 0);

                // Disable gravity
                player.getBody().setGravityScale(0);

                // Adjust position to visually attach to tree edge
                float offsetX = (playerCenterX < blockCenterX) ?
                    block.getX() - player.getWidth() * 0.3f :
                    block.getX() + block.getWidth() - player.getWidth() * 0.7f;

                // Set new position
                player.getBody().setTransform(
                    offsetX / Block.BLOCK_SIZE,
                    player.getBody().getPosition().y,
                    player.getBody().getAngle()
                );

                debugLog("Physics adjusted for tree landing");
            });

            // Set player as attached to tree
            player.attachToTree(block);

            // Disable normal physics collision response
            contact.setEnabled(false);

            debugLog("Tree landing complete");
            return true;
        }

        // Default: let regular physics handle it
        debugLog("No tree landing condition met - proceeding with normal physics");
        return false;
    }

    @Override
    public void endContact(Contact contact) {
        Object userDataA = contact.getFixtureA().getBody().getUserData();
        Object userDataB = contact.getFixtureB().getBody().getUserData();

        boolean handled = false;

        // Only handle end contact for ControlledPlayer and Block
        if (userDataA instanceof ControlledPlayer && userDataB instanceof Block) {
            handled = handleTreeEndContact((ControlledPlayer)userDataA, (Block)userDataB);
        }
        else if (userDataB instanceof ControlledPlayer && userDataA instanceof Block) {
            handled = handleTreeEndContact((ControlledPlayer)userDataB, (Block)userDataA);
        }

        // Only call super.endContact if we didn't handle it
        if (!handled) {
            super.endContact(contact);
        }
    }

    /**
     * Handle the end of contact with a tree
     */
    private boolean handleTreeEndContact(ControlledPlayer player, Block block) {
        // Only handle tree blocks
        if (!isTreeBlock(block)) {
            // For non-tree blocks, update ground contact if needed
            if (!player.isAttachedToTree()) {
                player.setOnGround(false);
            }
            return false;
        }

        // Don't modify ground state if player is attached to a tree
        // This prevents falling when moving from one tree block to another
        if (player.isAttachedToTree()) {
            debugLog("End contact with tree while attached - keeping attachment");
            return true;
        }

        return false;
    }

    @Override
    public void preSolve(Contact contact, Manifold oldManifold) {
        Object userDataA = contact.getFixtureA().getBody().getUserData();
        Object userDataB = contact.getFixtureB().getBody().getUserData();

        boolean handled = false;

        // Handle tree physics for preSolve
        if (userDataA instanceof ControlledPlayer && userDataB instanceof Block) {
            handled = handleTreePreSolve((ControlledPlayer)userDataA, (Block)userDataB, contact);
        }
        else if (userDataB instanceof ControlledPlayer && userDataA instanceof Block) {
            handled = handleTreePreSolve((ControlledPlayer)userDataB, (Block)userDataA, contact);
        }

        // Only call super.preSolve if we didn't handle it
        if (!handled) {
            super.preSolve(contact, oldManifold);
        }
    }

    /**
     * Handle tree physics before collision resolution
     */
    private boolean handleTreePreSolve(ControlledPlayer player, Block block, Contact contact) {
        // Only handle tree blocks
        if (!isTreeBlock(block)) {
            return false;
        }

        // Always disable physics collision for tree blocks
        contact.setEnabled(false);

        // Now check if we should be attaching to the tree
        boolean isJumping = InputManager.getInstance().isJumpPressed();
        boolean isMovingHorizontally = InputManager.getInstance().isLeftPressed() ||
            InputManager.getInstance().isRightPressed();
        boolean isOnGround = player.isOnGround();

        // Check player position relative to tree edge
        float playerCenterX = player.getX() + player.getWidth() / 2;
        float blockCenterX = block.getX() + block.getWidth() / 2;
        float distanceFromCenter = Math.abs(playerCenterX - blockCenterX);
        boolean isNearEdge = distanceFromCenter > block.getWidth() / 4;

        // If in the air, not flying, moving horizontally, and near the edge
        // And not already attached to a tree
        if (!isOnGround && !isJumping && isMovingHorizontally && isNearEdge && !player.isAttachedToTree()) {
            debugLog("PreSolve detects tree landing condition!");

            // Try attaching to the tree
            Box2DOperationManager.queueOperation(() -> {
                // Only proceed if the player isn't already attached
                if (!player.isAttachedToTree()) {
                    // Get the player's velocity
                    Vector2 vel = player.getBody().getLinearVelocity();

                    // Stop vertical movement
                    player.getBody().setLinearVelocity(vel.x, 0);

                    // Disable gravity
                    player.getBody().setGravityScale(0);

                    debugLog("PreSolve adjusted physics for potential tree landing");
                }
            });
        }

        return true;
    }
}
