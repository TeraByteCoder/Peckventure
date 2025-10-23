package at.peckventure.entities;

import at.peckventure.*;
import at.peckventure.entities.mob.ItemActor;
import at.peckventure.entities.mob.Mob;
import at.peckventure.entities.mob.MobRegistry;
import at.peckventure.inventory.item.Item;
import at.peckventure.multiplayer.NetworkPackets;
import at.peckventure.world.Box2DOperationManager;
import at.peckventure.world.block.Block;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.World;

public class ControlledPlayer extends Player
{
    private static ControlledPlayer instance;
    private boolean facingRight = true;

    // Felder für Flugmodi
    private boolean diveInitiated = false;
    private long lastLeftTapTime = 0;
    private long lastRightTapTime = 0;

    private boolean wasLeftPressed = false;
    private boolean wasRightPressed = false;

    // New fields to add to the ControlledPlayer class:
    private boolean landingMode = false; // Tracks if 'C' was pressed to initiate landing
    private static final boolean DEBUG = true;

    private static final long DOUBLE_TAP_THRESHOLD = 300; // in Millisekunden

    // Felder für Ground-Hop
    private boolean groundJumpUsed = false;
    private static final float GROUND_HOP_FORCE = 300f;

    // Bodenkontakt
    private boolean onGround = false;

    // Angepasste Kräfte:
    private static final float FLY_FORCE = 450f;
    private static final float HIGH_UP_FORCE = 550f;
    private static final float DIVE_FORCE = -150f;
    private static final float HOVER_FORCE = 10f;
    private static final float AIRROLL_IMPULSE = 50f;

    // Basisgeschwindigkeit
    private static float BASE_HORIZONTAL_SPEED = 300f;

    // Geschwindigkeitsmultiplikatoren:
    private static final float HORIZONTAL_SPEED_MULTIPLIER_FLY = 1.5f;
    private static final float HORIZONTAL_SPEED_MULTIPLIER_GROUND = 2.0f;

    // Tree climbing speed multiplier
    private static final float HORIZONTAL_SPEED_MULTIPLIER_TREE = 0.5f;

    // Maximale Beschleunigungszeiten:
    private static final float MAX_ACCEL_TIME_FLY = 1.0f;
    private static final float MAX_ACCEL_TIME_GROUND = 0.5f;
    private static final float MAX_ACCEL_TIME_TREE = 0.3f;

    // Energiekosten (pro Sekunde bzw. pro Aktion)
    private static final float FLUEGELSCHLAG_ENERGY_COST = 8.0f; // pro Sekunde
    private static final float HOVER_ENERGY_COST = 10.0f;         // pro Sekunde
    private static final float STURZFUG_ENERGY_COST = 20.0f;        // pro Aktion
    private static final float AIRROLL_ENERGY_COST = 15.0f;         // pro Aktion
    private static final float REGENERATION_ENERGY = 5.0f;          // pro Sekunde
    private static final float WALK_ENERGY_COST = 3.0f;             // pro Sekunde
    private static final float TREE_CLIMB_ENERGY_COST = 2.0f;       // pro Sekunde
    private static final float PECK_ENERGY_COST = 5.0f;             // Kosten pro Peck

    // Peck-Einstellungen
    private static final float HORIZONTAL_PECK_RANGE = 3 * Block.BLOCK_SIZE; // 3 Blöcke horizontale Reichweite
    private static final float VERTICAL_PECK_RANGE = 4 * Block.BLOCK_SIZE;   // 4 Blöcke vertikale Reichweite
    private static final float PECK_COOLDOWN = 0.5f; // 0.5 Sekunden Abklingzeit

    // Zungen-Animation
    private float peckCooldown = 0f; // Aktueller Cooldown-Timer
    private boolean isPecking = false; // Ist der Specht gerade beim Pecken?
    private float tongueLength = 0f;
    private boolean isExtending = false;
    public Vector2 tongueTarget = new Vector2();
    private volatile Mob targetMob = null; // Volatile für Thread-Sicherheit
    private static final float TONGUE_EXTEND_SPEED = 800f; // Pixel pro Sekunde
    private static final float TONGUE_RETRACT_SPEED = 1000f; // Pixel pro Sekunde
    private Color tongueColor = new Color(1f, 0.5f, 0.5f, 1f); // Rosa Farbe für die Zunge
    private Texture pixelTexture; // Textur für die Zunge

    // Variable für horizontale Beschleunigung
    private float accelerationTime = 0f;
    private int lastDirection = 0; // -1 für links, 1 für rechts

    private ControlledPlayer(World world, float x, float y)
    {
        super(world, x, y, Textures.WOODPECKER_FLYING);

        // Erzeuge eine 1x1 weiße Textur für die Zunge
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        pixelTexture = new Texture(pixmap);
        pixmap.dispose();
    }

    /**
     * Gibt die Singleton-Instanz zurück oder erstellt eine neue, falls keine existiert.
     * Diese Methode sollte zuerst aufgerufen werden, bevor getInstance() ohne Parameter verwendet wird.
     */
    public static ControlledPlayer getInstance(World world, float x, float y)
    {
        if (instance == null)
        {
            instance = new ControlledPlayer(world, x, y);
        } else
        {
            // Wenn eine Instanz bereits existiert, aber neu positioniert werden soll
            if (instance.getBody() != null)
            {
                instance.getBody().setTransform(x / Block.BLOCK_SIZE, y / Block.BLOCK_SIZE, instance.getBody().getAngle());
            }
        }
        Globals.controlledPlayer = instance;
        return instance;
    }

    // Methode zum Behandeln des Peckens
    private void handlePecking(float delta)
    {
        // Cooldown verringern, falls aktiv
        if (peckCooldown > 0)
        {
            peckCooldown -= delta;
        }

        // Zungenanimation aktualisieren, wenn sie läuft
        if (isPecking)
        {
            updateTongueAnimation(delta);
            return;
        }

        // Wenn F gedrückt wird und kein Cooldown aktiv ist und genug Energie vorhanden ist
        if (InputManager.getInstance().isPeckPressed() && peckCooldown <= 0 &&
            this.getEnergyStatus().getCurrent() >= PECK_ENERGY_COST)
        {

            // Bestimme die Peck-Position basierend auf der Blickrichtung des Spechts
            float peckX = facingRight ?
                getX() + getWidth() * 0.8f : // Etwas weiter im Specht für den Schnabel
                getX() + getWidth() * 0.2f;  // Etwas weiter im Specht für den Schnabel

            // Exakte Schnabelposition
            float peckY = getY() + getHeight() * 0.75f; // Angepasst für Schnabelhöhe

            // Suche zuerst nach Mobs, bevor wir die Animation starten
            searchForMobsToAttack(peckX, peckY);
        }
    }

    /**
     * Improved tree climbing handling
     */
    @Override
    protected void handleTreeClimbing(boolean upPressed, boolean downPressed, float delta)
    {
        if (attachedToTree && body != null)
        {
            float currentVelocityX = body.getLinearVelocity().x;
            float climbVelocity = 0;

            if (upPressed)
            {
                climbVelocity = TREE_CLIMB_SPEED / Block.BLOCK_SIZE;
                debugLog("Climbing up tree at velocity " + climbVelocity);

                // Consume energy for climbing
                this.getEnergyStatus().consume(TREE_CLIMB_ENERGY_COST * delta);
            } else if (downPressed)
            {
                climbVelocity = -TREE_CLIMB_SPEED / Block.BLOCK_SIZE;
                debugLog("Climbing down tree at velocity " + climbVelocity);

                // Consume energy for climbing
                this.getEnergyStatus().consume(TREE_CLIMB_ENERGY_COST * delta);
            }

            // Apply the climbing velocity
            final float finalVelocity = climbVelocity;
            Box2DOperationManager.queueOperation(() ->
            {
                if (body != null)
                {
                    body.setLinearVelocity(currentVelocityX, finalVelocity);
                }
            });
        }
    }

    /**
     * Sends a peck notification to the server in multiplayer mode
     * This method is called after a successful peck is initiated
     */
    private void sendPeckToServer(float targetX, float targetY)
    {
        try
        {
            // Check if NetworkClient exists and is connected (multiplayer mode)
            if (at.peckventure.NetworkClient.hasInstance() &&
                at.peckventure.NetworkClient.getInstance().isConnected())
            {

                // Create and send the peck request packet
                NetworkPackets.PeckRequestPacket packet = new NetworkPackets.PeckRequestPacket();
                packet.uuid = Globals.uuid;
                packet.targetX = targetX;
                packet.targetY = targetY;
                at.peckventure.NetworkClient.getInstance().sendTCP(packet);
                System.out.println("Sending peck request to server: target(" + targetX + "," + targetY + ")");
            }
        } catch (Exception e)
        {
            // Silently handle any errors to ensure pecking still works in singleplayer
            System.err.println("Error sending peck packet: " + e.getMessage());
        }
    }

    // Sucht nach Mobs in Peck-Reichweite und startet die Animation nur, wenn ein Mob gefunden wird
    private void searchForMobsToAttack(float peckX, float peckY)
    {
        // Suchrichtung basierend auf Blickrichtung
        float searchOffsetX = facingRight ? HORIZONTAL_PECK_RANGE : -HORIZONTAL_PECK_RANGE;

        // Erstelle einen Suchbereich, der auch nach unten geht
        final float searchStartX = peckX;
        final float searchEndX = peckX + searchOffsetX;

        // Horizontaler Bereich in Box2D-Koordinaten
        final float minX = Math.min(searchStartX, searchEndX) / Block.BLOCK_SIZE;
        final float maxX = Math.max(searchStartX, searchEndX) / Block.BLOCK_SIZE;

        // Vertikaler Bereich - gehe 4 Blöcke nach unten und ein bisschen nach oben
        final float minY = (peckY - VERTICAL_PECK_RANGE) / Block.BLOCK_SIZE;
        final float maxY = (peckY + Block.BLOCK_SIZE / 2) / Block.BLOCK_SIZE;

        // Zurücksetzen, falls es von einem früheren Versuch noch gesetzt ist
        targetMob = null;

        Box2DOperationManager.queueOperation(() ->
        {
            // Suche nach Mobs im definierten Bereich
            world.QueryAABB(fixture ->
            {
                if (fixture.getBody().getUserData() instanceof Mob && !(fixture.getBody().getUserData() instanceof ItemActor))
                {
                    Mob mob = (Mob) fixture.getBody().getUserData();

                    // Überprüfe, ob der Mob in der richtigen Richtung liegt
                    boolean isInRightDirection = facingRight ?
                        mob.getX() > getX() :
                        mob.getX() < getX();

                    if (isInRightDirection)
                    {
                        // Setze den Mob als Ziel
                        targetMob = mob;

                        // Setze das Ziel für die Zunge
                        final float mobCenterX = mob.getX() + mob.getWidth() / 2;
                        final float mobCenterY = mob.getY() + mob.getHeight() / 2;

                        // Starte die Zungenanimation (wird im nächsten Frame ausgeführt)
                        Box2DOperationManager.queueOperation(() ->
                        {
                            if (targetMob != null)
                            {
                                tongueTarget.set(mobCenterX, mobCenterY);
                                isPecking = true;
                                isExtending = true;
                                tongueLength = 0f;

                                // Energiekosten abziehen
                                getEnergyStatus().consume(PECK_ENERGY_COST);
                                sendPeckToServer(mobCenterX, mobCenterY);

                                System.out.println("Pecking at mob: " + mob);
                            }
                        });

                        // Stoppe die Suche nach dem ersten gefundenen Mob
                        return false;
                    }
                }
                // Weitersuchen
                return true;
            }, minX, minY, maxX, maxY);
        });
    }

    /**
     * Aktualisiert die Animation der Zunge
     */
    private void updateTongueAnimation(float delta)
    {
        if (!isPecking) return;

        // Wenn das Ziel nicht mehr existiert, breche ab
        if (targetMob == null)
        {
            isPecking = false;
            tongueLength = 0f;
            peckCooldown = PECK_COOLDOWN;
            return;
        }

        if (isExtending)
        {
            // Zunge ausstrecken
            tongueLength += TONGUE_EXTEND_SPEED * delta;

            // Prüfen, ob die Zunge ihr Ziel erreicht hat
            float distanceToTarget = calculateDistanceToTarget();
            if (tongueLength >= distanceToTarget)
            {
                tongueLength = distanceToTarget;
                isExtending = false; // Beginne mit dem Zurückziehen
            }
        } else
        {
            // Zunge zurückziehen
            tongueLength -= TONGUE_RETRACT_SPEED * delta;

            // Wenn die Zunge vollständig zurückgezogen ist
            if (tongueLength <= 0)
            {
                tongueLength = 0;
                isPecking = false;
                peckCooldown = PECK_COOLDOWN;

                // Mob töten, wenn er noch existiert
                if (targetMob != null)
                {
                    targetMob.onPeck(this);

                    // Spieler bekommt etwas Energie zurück fürs Pecken
                    getEnergyStatus().regenerate(5.0f);

                    targetMob = null;
                }
            }
        }
    }

    /**
     * Berechnet die Distanz zum Ziel
     */
    private float calculateDistanceToTarget()
    {
        // Schnabelposition (Ursprung der Zunge)
        float startX = facingRight ?
            getX() + getWidth() * 0.8f : // Etwas weiter im Specht für den Schnabel
            getX() + getWidth() * 0.2f;  // Etwas weiter im Specht für den Schnabel
        float startY = getY() + getHeight() * 0.75f; // Angepasst für Schnabelhöhe

        float dx = tongueTarget.x - startX;
        float dy = tongueTarget.y - startY;

        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Gibt die existierende Singleton-Instanz zurück.
     * Wirft eine Exception, wenn die Instanz nicht initialisiert wurde.
     */
    public static ControlledPlayer getInstance()
    {
        if (instance == null)
        {
            throw new IllegalStateException("ControlledPlayer not initialized. Call getInstance(world, x, y) first.");
        }
        return instance;
    }

    /**
     * Prüft, ob bereits eine Singleton-Instanz existiert.
     */
    public static boolean hasInstance()
    {
        return instance != null;
    }

    /**
     * Setzt die Singleton-Instanz zurück und gibt alle zugehörigen Ressourcen frei.
     * Diese Methode sollte aufgerufen werden, wenn man zum Hauptmenü zurückkehrt.
     */
    public static void reset()
    {
        if (instance != null)
        {
            // Den Body aus der Physics-Welt entfernen, falls er noch existiert
            if (instance.getBody() != null && instance.getBody().getWorld() != null)
            {
                // Einfacherer Ansatz ohne getBodyList() und getNext() zu verwenden
                final Body bodyToDestroy = instance.getBody();
                Box2DOperationManager.queueOperation(() ->
                {
                    try
                    {
                        if (bodyToDestroy != null && bodyToDestroy.getWorld() != null)
                        {
                            bodyToDestroy.getWorld().destroyBody(bodyToDestroy);
                        }
                    } catch (Exception e)
                    {
                        // Fehler beim Zerstören des Körpers abfangen
                        System.err.println("Error destroying player body: " + e.getMessage());
                    }
                });
            }

            // Pixeltextur für die Zunge freigeben
            if (instance.pixelTexture != null)
            {
                instance.pixelTexture.dispose();
            }

            // Die Instanz auf null setzen
            instance = null;
        }
    }

    @Override
    protected void handleInput(float delta) {
        int direction = 0; // -1: links, 1: rechts

        // Tree‐Landing Input (C‐Key)
        handleTreeLandingInput();

        // Bewegung links/rechts
        boolean isWalking = InputManager.getInstance().isLeftPressed() || InputManager.getInstance().isRightPressed();
        boolean isFlying = InputManager.getInstance().isJumpPressed();

        // Energie‐Regeneration
        float regenerationEnergy = isWalking
            ? (WALK_ENERGY_COST + 0.5f) * delta
            : REGENERATION_ENERGY * delta;

        // Double-Tap‐Air-Roll
        if (InputManager.getInstance().isLeftPressed()) {
            if (!wasLeftPressed && System.currentTimeMillis() - lastLeftTapTime < DOUBLE_TAP_THRESHOLD) {
                if (getEnergyStatus().getCurrent() >= AIRROLL_ENERGY_COST) {
                    getEnergyStatus().consume(AIRROLL_ENERGY_COST);
                    body.setLinearVelocity(-AIRROLL_IMPULSE, body.getLinearVelocity().y);
                }
            }
            lastLeftTapTime = wasLeftPressed ? lastLeftTapTime : System.currentTimeMillis();
            wasLeftPressed = true;
            direction = -1;
            facingRight = false;
            //todo sprite.setFlip(false, false);
        } else {
            wasLeftPressed = false;
        }

        if (InputManager.getInstance().isRightPressed()) {
            if (!wasRightPressed && System.currentTimeMillis() - lastRightTapTime < DOUBLE_TAP_THRESHOLD) {
                if (getEnergyStatus().getCurrent() >= AIRROLL_ENERGY_COST) {
                    getEnergyStatus().consume(AIRROLL_ENERGY_COST);
                    body.setLinearVelocity(AIRROLL_IMPULSE, body.getLinearVelocity().y);
                }
            }
            lastRightTapTime = wasRightPressed ? lastRightTapTime : System.currentTimeMillis();
            wasRightPressed = true;
            direction = 1;
            facingRight = true;
            //todo sprite.setFlip(true, false);
        } else {
            wasRightPressed = false;
        }

        // Pecking & Tree Climbing
        handlePecking(delta);
        if (attachedToTree) {
            // (unverändert) Baumkletter‐Logik hier …
            // bei Jump detachFromTree() + initialer Flug‐Impulse
            if (isFlying) {
                detachFromTree();
                getEnergyStatus().consume(FLUEGELSCHLAG_ENERGY_COST * delta);
            }
            // horizontaler Baum‐Speed …
            regenerationEnergy = 0.5f * delta;
        } else {
            // Horizontalbeschleunigung (Boden & Luft)
            Vector2 vel = body.getLinearVelocity();
            boolean inAirFlightMode = isFlying && !onGround;
            float maxSpeed = BASE_HORIZONTAL_SPEED * (inAirFlightMode
                ? HORIZONTAL_SPEED_MULTIPLIER_FLY
                : HORIZONTAL_SPEED_MULTIPLIER_GROUND);
            float maxAccelTime = inAirFlightMode
                ? MAX_ACCEL_TIME_FLY
                : MAX_ACCEL_TIME_GROUND;

            if (direction != 0) {
                if (direction != lastDirection) accelerationTime = 0f;
                lastDirection = direction;
                accelerationTime = Math.min(accelerationTime + delta, maxAccelTime);
                float currentSpeed = maxSpeed * (float)Math.sqrt(accelerationTime / maxAccelTime);
                body.setLinearVelocity(direction * currentSpeed / Block.BLOCK_SIZE, vel.y);
                if (!inAirFlightMode) getEnergyStatus().consume(WALK_ENERGY_COST * delta);
            } else {
                accelerationTime = 0f;
                lastDirection = 0;
                body.setLinearVelocity(0, vel.y);
            }

            // ─────────────── JUMP / FLY ───────────────
            boolean jumpHeld        = InputManager.getInstance().isJumpPressed();

            if (jumpHeld) {
                // während gehalten: verschiedene Flugmodi
                regenerationEnergy = 0;
                if (InputManager.getInstance().isWPressed()) {
                    // HOVER
                    if (getEnergyStatus().getCurrent() >= HOVER_ENERGY_COST * delta) {
                        getEnergyStatus().consume(HOVER_ENERGY_COST * delta);
                        body.setLinearVelocity(body.getLinearVelocity().x, HOVER_FORCE / Block.BLOCK_SIZE);
                    }
                } else if (InputManager.getInstance().isSPressed()) {
                    // DIVE / HIGH-UP
                    if (!diveInitiated) {
                        body.setLinearVelocity(body.getLinearVelocity().x, DIVE_FORCE / Block.BLOCK_SIZE);
                        diveInitiated = true;
                    } else if (getEnergyStatus().getCurrent() >= STURZFUG_ENERGY_COST) {
                        getEnergyStatus().consume(STURZFUG_ENERGY_COST);
                        body.setLinearVelocity(body.getLinearVelocity().x, HIGH_UP_FORCE / Block.BLOCK_SIZE);
                        diveInitiated = false;
                    }
                } else {
                    // normaler Flügelschlag
                    if (getY() < maxHeight && getEnergyStatus().getCurrent() >= FLUEGELSCHLAG_ENERGY_COST * delta) {
                        getEnergyStatus().consume(FLUEGELSCHLAG_ENERGY_COST * delta);
                        body.setLinearVelocity(body.getLinearVelocity().x, FLY_FORCE / Block.BLOCK_SIZE);
                    }
                }
            } else {
                // Reset des Dive-Flags, wenn Jump losgelassen
                diveInitiated = false;
            }
        }

        // Energie zurückgeben & Bodenhaftung
        getEnergyStatus().regenerate(regenerationEnergy);
        setRotation(facingRight ? 0 : 180);

        // Ramp-Handling
        if (ClientGlobal.contactListener instanceof ExtendedGameContactListener) {
            ((ExtendedGameContactListener)ClientGlobal.contactListener).handleGroundMovement(this, delta);
        }

        // Sanftes Abbremsen nach oben, wenn onGround & Y-Velocity > 0
        Vector2 vel = body.getLinearVelocity();
        if (onGround && vel.y > 0 && !InputManager.getInstance().isJumpPressed()) {
            body.setLinearVelocity(vel.x, vel.y * 0.5f);
        }
    }

    @Override
    public void draw(Batch batch)
    {
        // Zuerst die Zunge zeichnen, wenn wir am Pecken sind (damit sie hinter dem Spieler erscheint)
        if (isPecking && tongueLength > 0)
        {
            drawTongue(batch);
        }

        // Dann den Spieler zeichnen (im Vordergrund)
        super.draw(batch);
    }

    /**
     * Zeichnet die Zunge als dehnbares Rechteck
     */
    private void drawTongue(Batch batch)
    {
        // Schnabelposition (Ursprung der Zunge)
        float startX = facingRight ?
            getX() + getWidth() * 0.8f : // Etwas weiter im Specht für den Schnabel
            getX() + getWidth() * 0.2f;  // Etwas weiter im Specht für den Schnabel
        float startY = getY() + getHeight() * 0.75f; // Angepasst für Schnabelhöhe

        // Richtung zum Ziel berechnen
        float dx = tongueTarget.x - startX;
        float dy = tongueTarget.y - startY;
        float angle = (float) Math.atan2(dy, dx) * MathUtils.radiansToDegrees;

        // Originale Batch-Farbe speichern
        Color originalColor = batch.getColor().cpy();

        // Farbe für die Zunge setzen
        batch.setColor(tongueColor);

        // Zunge zeichnen (ein dehnbares Rechteck)
        batch.draw(
            pixelTexture,
            startX,
            startY - 3, // 3 Pixel vom Mittelpunkt, damit die Zunge 6 Pixel breit ist
            0,
            3,
            tongueLength,
            6,
            1,
            1,
            angle,
            0,
            0,
            1,
            1,
            false,
            false
        );

        // Batch-Farbe zurücksetzen
        batch.setColor(originalColor);
    }

    // Update in setOnGround method to handle ground contact
    public void setOnGround(boolean onGround)
    {
        this.onGround = onGround;
        if (onGround)
        {
            groundJumpUsed = false;

            // Reset landing mode when touching ground
            landingMode = false;

            // If we touch the ground while attached to a tree, detach
            if (isAttachedToTree())
            {
                detachFromTree();
            }
        }
    }


    /**
     * Improved tree attachment
     */
    @Override
    public void attachToTree(Block treeBlock)
    {
        if (!attachedToTree)
        {
            attachedToTree = true;
            attachedTreeBlock = treeBlock;

            // Stop falling when attached to the tree and disable gravity
            Box2DOperationManager.queueOperation(() ->
            {
                if (body != null)
                {
                    // First stabilize the player by stopping any vertical movement
                    body.setLinearVelocity(body.getLinearVelocity().x, 0);
                    body.setGravityScale(0); // Disable gravity when on tree

                    // Move the player slightly to visually attach to the tree edge
                    // Determine which side of the tree we're on
                    float playerCenterX = getX() + getWidth() / 2;
                    float blockCenterX = treeBlock.getX() + treeBlock.getWidth() / 2;

                    // If player is to the left of tree, attach to left edge
                    // If player is to the right of tree, attach to right edge
                    float offsetX = (playerCenterX < blockCenterX) ?
                        treeBlock.getX() - getWidth() * 0.3f :
                        treeBlock.getX() + treeBlock.getWidth() - getWidth() * 0.7f;

                    // Use the current Y position
                    body.setTransform(
                        offsetX / Block.BLOCK_SIZE,
                        body.getPosition().y,
                        body.getAngle()
                    );
                }
            });

            // Update sprite to climbing animation (if you have one)
            // sprite.setTexture(climbingTexture);

            debugLog("Successfully attached to tree at position: " + getX() + "," + getY());
        }
    }

    /**
     * Improved handling for 'C' key for tree landing
     * Call this from handleInput
     */
    private void handleTreeLandingInput()
    {
        // Check for C key press (land key)
        if (InputManager.getInstance().isLandPressed())
        {
            // Toggle landing mode if in the air
            if (!onGround)
            {
                // If we're already attached to a tree, detach
                if (isAttachedToTree())
                {
                    debugLog("Land key pressed while on tree - detaching");
                    detachFromTree();
                } else
                {
                    // Otherwise, enable landing mode
                    debugLog("Land key pressed while in air - enabling landing mode");
                    setLandingMode(true);
                }
            }
        }
    }

    /**
     * Debug logging for tree climbing
     */
    private void debugLog(String message)
    {
        if (DEBUG)
        {
            System.out.println("[PLAYER_TREE] " + message);
        }
    }

    /**
     * Set landing mode status
     *
     * @param mode true to enable landing mode, false to disable
     */
    public void setLandingMode(boolean mode)
    {
        if (mode != landingMode)
        {
            landingMode = mode;
            debugLog("Landing mode " + (mode ? "enabled" : "disabled"));

            // If we're disabling landing mode while attached to a tree, detach
            if (!mode && isAttachedToTree())
            {
                detachFromTree();
            }
        }
    }

    /**
     * Check if player is in landing mode (ready to land on a tree)
     *
     * @return true if player wants to land on a tree
     */
    public boolean isInLandingMode()
    {
        return landingMode;
    }

    /**
     * Overriding the detachFromTree method for more robust handling
     */
    /**
     * Improved tree detachment
     */
    @Override
    public void detachFromTree()
    {
        if (attachedToTree)
        {
            debugLog("Detaching from tree");
            attachedToTree = false;
            attachedTreeBlock = null;

            // Reset landing mode
            landingMode = false;

            // Re-enable gravity and apply a small impulse to ensure the player moves away from the tree
            Box2DOperationManager.queueOperation(() ->
            {
                if (body != null)
                {
                    body.setGravityScale(1);

                    // If player is actively trying to fly, add a small upward boost when detaching
                    if (InputManager.getInstance().isJumpPressed())
                    {
                        body.setLinearVelocity(
                            body.getLinearVelocity().x,
                            FLY_FORCE / Block.BLOCK_SIZE
                        );
                    }
                }
            });

            // Update sprite back to normal (if you have specific animations)
            // sprite.setTexture(normalTexture);
        }
    }

    public boolean isOnGround()
    {
        return onGround;
    }

    public boolean isFacingRight()
    {
        return facingRight;
    }

    @Override
    public void dropItemOutside(Item item, int amount)
    {
        System.out.println("Dropped " + amount + "x " + item.getName() + " outside inventory.");
        Mob mob = MobRegistry.createMob("item", Globals.physicsWorld, this.getX(), this.getY() + 40, item, amount);
        ClientGlobal.stage.addActor(mob);
        float dropSpeed = 20f;
        float angle = this.getRotation();
        float vx = com.badlogic.gdx.math.MathUtils.cosDeg(angle) * dropSpeed;
        float vy = com.badlogic.gdx.math.MathUtils.sinDeg(angle) * dropSpeed;
        Box2DOperationManager.queueOperation(() ->
        {
            if (mob.getBody() != null)
                mob.getBody().setLinearVelocity(vx, vy);
        });
    }

    @Override
    public void setSpeed(float speed)
    {
        BASE_HORIZONTAL_SPEED = speed;
    }

    @Override
    public float getSpeed()
    {
        return BASE_HORIZONTAL_SPEED;
    }

    @Override
    public void dispose()
    {
        super.dispose();
        if (pixelTexture != null)
        {
            pixelTexture.dispose();
        }
    }
}
