package at.peckventure;

import at.peckventure.entities.ControlledPlayer;
import at.peckventure.world.Box2DOperationManager;
import at.peckventure.world.block.Block;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;

/**
 * Enhanced contact listener with improved ground handling for slopes, ramps, and tree landing
 */
public class ExtendedGameContactListener extends GameContactListener
{

    // Debug flag - set to true for verbose logging
    private static final boolean DEBUG = true;

    // Constants for slope detection
    private static final float MAX_SLOPE_ANGLE = 45f; // Maximum angle for walkable slope in degrees
    private static final float GROUND_STICK_FORCE = 10.5f; // Force for ground sticking
    private static final float EDGE_TOLERANCE = 1.1f; // Tolerance for edge transitions

    // Track if ground sensor is in contact with ground
    private boolean groundSensorContact = false;

    private void debugLog(String message)
    {
        if (DEBUG)
        {
            System.out.println("[PHYSICS_DEBUG] " + message);
        }
    }

    /**
     * Check if a block is a tree (has collision disabled)
     */
    private boolean isTreeBlock(Block block)
    {
        boolean isTree = !block.isCollisionEnabled();
        if (isTree)
        {
            debugLog("Found a tree block: " + block);
        }
        return isTree;
    }

    @Override
    public void beginContact(Contact contact)
    {
        Object userDataA = contact.getFixtureA().getBody().getUserData();
        Object userDataB = contact.getFixtureB().getBody().getUserData();

        // First check for ground sensor contact
        if (contact.getFixtureA().getUserData() != null &&
            contact.getFixtureA().getUserData().equals("groundSensor"))
        {
            if (userDataA instanceof ControlledPlayer)
            {
                handleGroundSensorContact((ControlledPlayer) userDataA, true);
            }
        } else if (contact.getFixtureB().getUserData() != null &&
            contact.getFixtureB().getUserData().equals("groundSensor"))
        {
            if (userDataB instanceof ControlledPlayer)
            {
                handleGroundSensorContact((ControlledPlayer) userDataB, true);
            }
        }

        boolean handled = false;

        // Then proceed with tree handling
        if (userDataA instanceof ControlledPlayer && userDataB instanceof Block)
        {
            handled = handleTreeLanding((ControlledPlayer) userDataA, (Block) userDataB, contact);
            if (!handled)
            {
                // If not a tree, handle slope contact
                handleSlopeContact((ControlledPlayer) userDataA, contact);
            }
        } else if (userDataB instanceof ControlledPlayer && userDataA instanceof Block)
        {
            handled = handleTreeLanding((ControlledPlayer) userDataB, (Block) userDataA, contact);
            if (!handled)
            {
                // If not a tree, handle slope contact
                handleSlopeContact((ControlledPlayer) userDataB, contact);
            }
        }

        // Call super method if not handled
        if (!handled)
        {
            super.beginContact(contact);
        }
    }

    /**
     * Handles ground sensor contact to distinguish between ground and air
     */
    private void handleGroundSensorContact(ControlledPlayer player, boolean isContact)
    {
        groundSensorContact = isContact;
        if (isContact && !player.isAttachedToTree())
        {
            player.setOnGround(true);
            debugLog("Ground sensor contact detected, player is on ground");
        }
    }

    /**
     * Handles contact with slopes
     */
    private void handleSlopeContact(ControlledPlayer player, Contact contact)
    {
        if (player.isAttachedToTree()) return; // Ignore slopes when on tree

        // For Box2D we need to get the normal from getWorldManifold()
        WorldManifold worldManifold = contact.getWorldManifold();

        // The normal points away from the contact point
        Vector2 normal = worldManifold.getNormal();

        // Determine the slope angle (0 degrees is horizontal, 90 degrees is vertical)
        float angle = Math.abs((float) Math.toDegrees(Math.atan2(normal.y, normal.x)) - 90f);

        debugLog("Slope contact angle: " + angle + " degrees");

        // If the angle is within walkable range and player is moving
        if (angle <= MAX_SLOPE_ANGLE && (InputManager.getInstance().isLeftPressed() ||
            InputManager.getInstance().isRightPressed()))
        {

            // Set player as on ground
            player.setOnGround(true);

            // Apply downward force to keep player on the slope
            Box2DOperationManager.queueOperation(() ->
            {
                if (player.getBody() != null && !player.isAttachedToTree())
                {
                    player.getBody().applyForceToCenter(0, -GROUND_STICK_FORCE, true);
                    debugLog("Applied stick force to player on slope");
                }
            });
        }
    }

    /**
     * Special method for moving on uneven ground
     * This should be called in ControlledPlayer
     */
    public void handleGroundMovement(ControlledPlayer player, float delta)
    {
        if (player.isAttachedToTree() || !player.isOnGround())
        {
            return; // Only apply on ground
        }

        // Determine if we might be on a slope
        final boolean movingHorizontally = InputManager.getInstance().isLeftPressed() ||
            InputManager.getInstance().isRightPressed();

        if (movingHorizontally && groundSensorContact)
        {
            // We're moving horizontally and on ground - slope handling
            Box2DOperationManager.queueOperation(() ->
            {
                if (player.getBody() != null)
                {
                    // Apply slight downward pressure to stay on ground
                    player.getBody().applyForceToCenter(0, -GROUND_STICK_FORCE, true);

                    // Prevent jumping when we're "sticking" to ground
                    if (!InputManager.getInstance().isJumpPressed())
                    {
                        // Limit upward movement when moving horizontally
                        Vector2 vel = player.getBody().getLinearVelocity();
                        if (vel.y > 0 && !InputManager.getInstance().isJumpPressed())
                        {
                            // Reduce positive Y velocity if not jumping
                            player.getBody().setLinearVelocity(vel.x, vel.y * 0.8f);
                        }
                    }
                }
            });
        }

        Body body = player.getBody();
// 1) Halbe Höhe in Box2D-Einheiten
        float halfHeight = (player.getHeight() / Block.BLOCK_SIZE) * 0.5f;

// 2) Start- und Endpunkt für den Raycast (wie gehabt)
        Vector2 start = body.getPosition().cpy().add(0, halfHeight + 0.02f);
        Vector2 end   = body.getPosition().cpy().add(0, -halfHeight - 0.05f);
        final float[] closestFrac = {1};
        Vector2 closestPoint = new Vector2();
        Globals.physicsWorld.rayCast(new RayCastCallback()
        {
            @Override
            public float reportRayFixture(Fixture fixture, Vector2 point,
                                          Vector2 normal, float fraction)
            {
                if (fixture.getBody().getUserData() instanceof Block && !fixture.isSensor() && ((Block) fixture.getBody().getUserData()).isCollisionEnabled())
                {
                    if (fraction < closestFrac[0])
                    {
                        closestFrac[0] = fraction;
                        closestPoint.set(point);
                    }
                    if (closestFrac[0] < 1) {
                        float groundY        = closestPoint.y;
                        float currentCenterY = body.getPosition().y;
                        float footY          = currentCenterY - halfHeight;
                        float penetration    = groundY - footY;

                        // 3) Nur wenn Fuß unter Boden ist, um Penetration anheben
                        if (penetration > 0f) {
                            float newCenterY = currentCenterY + penetration;
                            Box2DOperationManager.queueOperation(() ->
                                body.setTransform(body.getPosition().x, newCenterY, body.getAngle())
                            );
                        }

                        // Immer Y-Geschwindigkeit nullen
                        Box2DOperationManager.queueOperation(() ->
                            body.setLinearVelocity(body.getLinearVelocity().x, 0f)
                        );
                    }

                }
                return 1;
            }
        }, start, end);

        if (closestFrac[0] < 1 && player.isOnGround())
        {
            // korrekte halbe Höhe in World-Units:
            float halfHeightWorld = (player.getHeight() / Block.BLOCK_SIZE) * 0.5f;
            Box2DOperationManager.queueOperation(() -> {
                // exakt auf Bodenhöhe setzen
                body.setTransform(
                    body.getPosition().x,
                    closestPoint.y + halfHeightWorld,
                    body.getAngle()
                );
                body.setLinearVelocity(body.getLinearVelocity().x, 0f);
            });
        }

    }

    /**
     * Improved tree landing handler - returns true if handled, false otherwise
     */
    private boolean handleTreeLanding(ControlledPlayer player, Block block, Contact contact)
    {
        // Skip if not a tree
        if (!isTreeBlock(block))
        {
            // For non-tree blocks, set ground contact if appropriate
            if (!player.isAttachedToTree())
            {
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
        boolean isInLandingMode = player.isInLandingMode(); // NEW: Check if player wants to land

        // Calculate player position relative to tree
        float playerCenterX = player.getX() + player.getWidth() / 2;
        float blockCenterX = block.getX() + block.getWidth() / 2;
        float distanceFromCenter = Math.abs(playerCenterX - blockCenterX);
        boolean isNearEdge = distanceFromCenter > block.getWidth() / 4;

        debugLog("Tree Landing Conditions - Jumping: " + isJumping +
            ", Moving: " + isMovingHorizontally +
            ", OnGround: " + isOnGround +
            ", AttachedToTree: " + isAttachedToTree +
            ", LandingMode: " + isInLandingMode + // NEW: Log landing mode
            ", NearEdge: " + isNearEdge +
            ", DistFromCenter: " + distanceFromCenter);

        // Now for the actual landing logic

        // CASE 1: Already attached to a tree
        if (isAttachedToTree)
        {
            debugLog("Already attached to tree - disabling collision");
            contact.setEnabled(false);
            return true;
        }

        // CASE 2: On the ground - should pass through trees
        if (isOnGround)
        {
            debugLog("On ground - disabling tree collision");
            contact.setEnabled(false);
            return true;
        }

        // CASE 3: In the middle of a tree - should pass through
        if (!isNearEdge)
        {
            debugLog("In middle of tree - disabling collision");
            contact.setEnabled(false);
            return true;
        }

        // CASE 4: Flying and not in landing mode - should pass through trees
        if (isJumping && !isInLandingMode)
        {
            debugLog("Flying through tree - disabling collision");
            contact.setEnabled(false);
            return true;
        }

        // CASE 5: THE LANDING CASE - In air, not flying, near edge, and either
        // in landing mode or moving horizontally
        if (!isOnGround && !isJumping && isNearEdge &&
            (isInLandingMode || isMovingHorizontally))
        {
            debugLog("LANDING ON TREE DETECTED!");

            // Directly modify player's body - this is critical for responsive landing
            Box2DOperationManager.queueOperation(() ->
            {
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

            // Reset landing mode after successfully landing
            player.setLandingMode(false);

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
    public void endContact(Contact contact)
    {
        Object userDataA = contact.getFixtureA().getBody().getUserData();
        Object userDataB = contact.getFixtureB().getBody().getUserData();

        // First check for ground sensor contact
        if (contact.getFixtureA().getUserData() != null &&
            contact.getFixtureA().getUserData().equals("groundSensor"))
        {
            if (userDataA instanceof ControlledPlayer)
            {
                handleGroundSensorContact((ControlledPlayer) userDataA, false);
                if (!((ControlledPlayer) userDataA).isAttachedToTree())
                {
                    // Only set if not attached to a tree
                    ((ControlledPlayer) userDataA).setOnGround(false);
                }
            }
        } else if (contact.getFixtureB().getUserData() != null &&
            contact.getFixtureB().getUserData().equals("groundSensor"))
        {
            if (userDataB instanceof ControlledPlayer)
            {
                handleGroundSensorContact((ControlledPlayer) userDataB, false);
                if (!((ControlledPlayer) userDataB).isAttachedToTree())
                {
                    // Only set if not attached to a tree
                    ((ControlledPlayer) userDataB).setOnGround(false);
                }
            }
        }

        boolean handled = false;

        // Only handle end contact for ControlledPlayer and Block
        if (userDataA instanceof ControlledPlayer && userDataB instanceof Block)
        {
            handled = handleTreeEndContact((ControlledPlayer) userDataA, (Block) userDataB);
        } else if (userDataB instanceof ControlledPlayer && userDataA instanceof Block)
        {
            handled = handleTreeEndContact((ControlledPlayer) userDataB, (Block) userDataA);
        }

        // Only call super.endContact if we didn't handle it
        if (!handled)
        {
            super.endContact(contact);
        }
    }

    /**
     * Handle the end of contact with a tree
     */
    private boolean handleTreeEndContact(ControlledPlayer player, Block block)
    {
        // Only handle tree blocks
        if (!isTreeBlock(block))
        {
            return false;
        }

        // Don't modify ground state if player is attached to a tree
        // This prevents falling when moving from one tree block to another
        if (player.isAttachedToTree())
        {
            debugLog("End contact with tree while attached - keeping attachment");
            return true;
        }

        return false;
    }

    @Override
    public void preSolve(Contact contact, Manifold oldManifold)
    {
        Object userDataA = contact.getFixtureA().getBody().getUserData();
        Object userDataB = contact.getFixtureB().getBody().getUserData();

        boolean handled = false;

        // Handle tree physics for preSolve
        if (userDataA instanceof ControlledPlayer && userDataB instanceof Block)
        {
            handled = handleTreePreSolve((ControlledPlayer) userDataA, (Block) userDataB, contact);
            if (!handled)
            {
                // If not a tree, handle slope
                handleSlopePreSolve((ControlledPlayer) userDataA, contact);
            }
        } else if (userDataB instanceof ControlledPlayer && userDataA instanceof Block)
        {
            handled = handleTreePreSolve((ControlledPlayer) userDataB, (Block) userDataA, contact);
            if (!handled)
            {
                // If not a tree, handle slope
                handleSlopePreSolve((ControlledPlayer) userDataB, contact);
            }
        }

        // Only call super.preSolve if we didn't handle it
        if (!handled)
        {
            super.preSolve(contact, oldManifold);
        }


    }

    /**
     * Improved slope handling in preSolve
     */
    private void handleSlopePreSolve(ControlledPlayer player, Contact contact)
    {
        if (player.isAttachedToTree()) return;

        // The world manifold contains information about the contact
        WorldManifold worldManifold = contact.getWorldManifold();

        // Determine contact normal
        Vector2 normal = worldManifold.getNormal();

        // If the normal is predominantly upward, it might be a slope
        if (normal.y > 0.2f)
        { // Y value is positive for upward-facing normals
            // Player is moving horizontally
            boolean movingHorizontally = InputManager.getInstance().isLeftPressed() ||
                InputManager.getInstance().isRightPressed();

            if (movingHorizontally)
            {
                // We assume we're on a slope
                player.setOnGround(true);

                // For sharp edges we can reduce friction
                if (Math.abs(normal.x) > 0.8f)
                { // Very steep normal => sharp edge
                    // Reduce friction for sharper edges
                    contact.setFriction(0.1f);
                    debugLog("Reduced friction for sharp edge");
                } else
                {
                    // Increase friction for slopes for better control
                    contact.setFriction(0.8f);
                }
            }
        }

    }

    /**
     * Handle tree physics before collision resolution
     */
    private boolean handleTreePreSolve(ControlledPlayer player, Block block, Contact contact)
    {
        // Only handle tree blocks
        if (!isTreeBlock(block))
        {
            return false;
        }

        // Always disable physics collision for tree blocks
        contact.setEnabled(false);

        // Now check if we should be attaching to the tree
        boolean isJumping = InputManager.getInstance().isJumpPressed();
        boolean isMovingHorizontally = InputManager.getInstance().isLeftPressed() ||
            InputManager.getInstance().isRightPressed();
        boolean isOnGround = player.isOnGround();
        boolean isInLandingMode = player.isInLandingMode(); // NEW: Check landing mode

        // Check player position relative to tree edge
        float playerCenterX = player.getX() + player.getWidth() / 2;
        float blockCenterX = block.getX() + block.getWidth() / 2;
        float distanceFromCenter = Math.abs(playerCenterX - blockCenterX);
        boolean isNearEdge = distanceFromCenter > block.getWidth() / 4;

        // If in the air, not flying, near the edge, and either in landing mode or moving horizontally
        // And not already attached to a tree
        if (!isOnGround && !isJumping && isNearEdge &&
            (isInLandingMode || isMovingHorizontally) &&
            !player.isAttachedToTree())
        {
            debugLog("PreSolve detects tree landing condition!");

            // Try attaching to the tree
            Box2DOperationManager.queueOperation(() ->
            {
                // Only proceed if the player isn't already attached
                if (!player.isAttachedToTree())
                {
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
