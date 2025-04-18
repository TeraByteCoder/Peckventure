package at.peckventure.entities;

import at.peckventure.ClientGlobal;
import at.peckventure.Globals;
import at.peckventure.InputManager;
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

public class ControlledPlayer extends Player {
    private static ControlledPlayer instance;
    private boolean facingRight = true;

    // Felder für Flugmodi
    private boolean diveInitiated = false;
    private long lastLeftTapTime = 0;
    private long lastRightTapTime = 0;

    private boolean wasLeftPressed = false;
    private boolean wasRightPressed = false;

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

    // Maximale Beschleunigungszeiten:
    private static final float MAX_ACCEL_TIME_FLY = 1.0f;
    private static final float MAX_ACCEL_TIME_GROUND = 0.5f;

    // Energiekosten (pro Sekunde bzw. pro Aktion)
    private static final float FLUEGELSCHLAG_ENERGY_COST = 8.0f; // pro Sekunde
    private static final float HOVER_ENERGY_COST = 10.0f;         // pro Sekunde
    private static final float STURZFUG_ENERGY_COST = 20.0f;        // pro Aktion
    private static final float AIRROLL_ENERGY_COST = 15.0f;         // pro Aktion
    private static final float REGENERATION_ENERGY = 5.0f;          // pro Sekunde
    private static final float WALK_ENERGY_COST = 3.0f;             // pro Sekunde
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

    private ControlledPlayer(World world, float x, float y) {
        super(world, x, y);
        this.sprite = new Sprite(new Texture("textures/woodpecker/woodpecker_idle.png"));

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
    public static ControlledPlayer getInstance(World world, float x, float y) {
        if (instance == null) {
            instance = new ControlledPlayer(world, x, y);
        } else {
            // Wenn eine Instanz bereits existiert, aber neu positioniert werden soll
            if (instance.getBody() != null) {
                instance.getBody().setTransform(x / Block.BLOCK_SIZE, y / Block.BLOCK_SIZE, instance.getBody().getAngle());
            }
        }
        Globals.controlledPlayer = instance;
        return instance;
    }

    // Methode zum Behandeln des Peckens
    private void handlePecking(float delta) {
        // Cooldown verringern, falls aktiv
        if (peckCooldown > 0) {
            peckCooldown -= delta;
        }

        // Zungenanimation aktualisieren, wenn sie läuft
        if (isPecking) {
            updateTongueAnimation(delta);
            return;
        }

        // Wenn F gedrückt wird und kein Cooldown aktiv ist und genug Energie vorhanden ist
        if (InputManager.getInstance().isPeckPressed() && peckCooldown <= 0 &&
            this.getEnergyStatus().getCurrent() >= PECK_ENERGY_COST) {

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
     * Sends a peck notification to the server in multiplayer mode
     * This method is called after a successful peck is initiated
     */
    private void sendPeckToServer(float targetX, float targetY) {
        try {
            // Check if NetworkClient exists and is connected (multiplayer mode)
            if (at.peckventure.NetworkClient.hasInstance() &&
                at.peckventure.NetworkClient.getInstance().isConnected()) {

                // Create and send the peck request packet
                NetworkPackets.PeckRequestPacket packet = new NetworkPackets.PeckRequestPacket();
                packet.uuid = Globals.uuid;
                packet.targetX = targetX;
                packet.targetY = targetY;
                at.peckventure.NetworkClient.getInstance().sendTCP(packet);
                System.out.println("Sending peck request to server: target(" + targetX + "," + targetY + ")");
            }
        } catch (Exception e) {
            // Silently handle any errors to ensure pecking still works in singleplayer
            System.err.println("Error sending peck packet: " + e.getMessage());
        }
    }

    // Sucht nach Mobs in Peck-Reichweite und startet die Animation nur, wenn ein Mob gefunden wird
    private void searchForMobsToAttack(float peckX, float peckY) {
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
        final float maxY = (peckY + Block.BLOCK_SIZE/2) / Block.BLOCK_SIZE;

        // Zurücksetzen, falls es von einem früheren Versuch noch gesetzt ist
        targetMob = null;

        Box2DOperationManager.queueOperation(() -> {
            // Suche nach Mobs im definierten Bereich
            world.QueryAABB(fixture -> {
                if (fixture.getBody().getUserData() instanceof Mob && !( fixture.getBody().getUserData() instanceof ItemActor)) {
                    Mob mob = (Mob) fixture.getBody().getUserData();

                    // Überprüfe, ob der Mob in der richtigen Richtung liegt
                    boolean isInRightDirection = facingRight ?
                        mob.getX() > getX() :
                        mob.getX() < getX();

                    if (isInRightDirection) {
                        // Setze den Mob als Ziel
                        targetMob = mob;

                        // Setze das Ziel für die Zunge
                        final float mobCenterX = mob.getX() + mob.getWidth() / 2;
                        final float mobCenterY = mob.getY() + mob.getHeight() / 2;

                        // Starte die Zungenanimation (wird im nächsten Frame ausgeführt)
                        Box2DOperationManager.queueOperation(() -> {
                            if (targetMob != null) {
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
    private void updateTongueAnimation(float delta) {
        if (!isPecking) return;

        // Wenn das Ziel nicht mehr existiert, breche ab
        if (targetMob == null) {
            isPecking = false;
            tongueLength = 0f;
            peckCooldown = PECK_COOLDOWN;
            return;
        }

        if (isExtending) {
            // Zunge ausstrecken
            tongueLength += TONGUE_EXTEND_SPEED * delta;

            // Prüfen, ob die Zunge ihr Ziel erreicht hat
            float distanceToTarget = calculateDistanceToTarget();
            if (tongueLength >= distanceToTarget) {
                tongueLength = distanceToTarget;
                isExtending = false; // Beginne mit dem Zurückziehen
            }
        } else {
            // Zunge zurückziehen
            tongueLength -= TONGUE_RETRACT_SPEED * delta;

            // Wenn die Zunge vollständig zurückgezogen ist
            if (tongueLength <= 0) {
                tongueLength = 0;
                isPecking = false;
                peckCooldown = PECK_COOLDOWN;

                // Mob töten, wenn er noch existiert
                if (targetMob != null) {
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
    private float calculateDistanceToTarget() {
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
    public static ControlledPlayer getInstance() {
        if (instance == null) {
            throw new IllegalStateException("ControlledPlayer not initialized. Call getInstance(world, x, y) first.");
        }
        return instance;
    }

    /**
     * Prüft, ob bereits eine Singleton-Instanz existiert.
     */
    public static boolean hasInstance() {
        return instance != null;
    }

    /**
     * Setzt die Singleton-Instanz zurück und gibt alle zugehörigen Ressourcen frei.
     * Diese Methode sollte aufgerufen werden, wenn man zum Hauptmenü zurückkehrt.
     */
    public static void reset() {
        if (instance != null) {
            // Den Body aus der Physics-Welt entfernen, falls er noch existiert
            if (instance.getBody() != null && instance.getBody().getWorld() != null) {
                // Einfacherer Ansatz ohne getBodyList() und getNext() zu verwenden
                final Body bodyToDestroy = instance.getBody();
                Box2DOperationManager.queueOperation(() -> {
                    try {
                        if (bodyToDestroy != null && bodyToDestroy.getWorld() != null) {
                            bodyToDestroy.getWorld().destroyBody(bodyToDestroy);
                        }
                    } catch (Exception e) {
                        // Fehler beim Zerstören des Körpers abfangen
                        System.err.println("Error destroying player body: " + e.getMessage());
                    }
                });
            }

            // Textur freigeben, falls vorhanden
            if (instance.sprite != null && instance.sprite.getTexture() != null) {
                instance.sprite.getTexture().dispose();
            }

            // Pixeltextur für die Zunge freigeben
            if (instance.pixelTexture != null) {
                instance.pixelTexture.dispose();
            }

            // Die Instanz auf null setzen
            instance = null;
        }
    }

    @Override
    protected void handleInput(float delta) {
        int direction = 0; // -1: links, 1: rechts

        // Bestimme, ob der Spieler gerade geht (links oder rechts gedrückt)
        boolean isWalking = InputManager.getInstance().isLeftPressed() || InputManager.getInstance().isRightPressed();

        // Berechne den zu regenerierenden Energie-Betrag pro Frame:
        // - Steht der Spieler, wird der normale Regenerationswert verwendet.
        // - Geht der Spieler, soll er so viel regenerieren, dass nach Abzug des Gehverbrauchs (WALK_ENERGY_COST)
        //   noch ein Netto-Gewinn von 0,5 Energie pro Sekunde verbleibt.
        float regenerationEnergy;
        if (isWalking) {
            regenerationEnergy = (WALK_ENERGY_COST + 0.5f) * delta;
        } else {
            regenerationEnergy = REGENERATION_ENERGY * delta;
        }

        // Horizontale Eingabe inkl. Luftrolle (Doppeltippen)
        // Linke Taste
        if (InputManager.getInstance().isLeftPressed()) {
            if (!wasLeftPressed) { // Taste wurde gerade gedrückt
                long now = System.currentTimeMillis();
                if (now - lastLeftTapTime < DOUBLE_TAP_THRESHOLD) {
                    if (this.getEnergyStatus().getCurrent() >= AIRROLL_ENERGY_COST) {
                        this.getEnergyStatus().consume(AIRROLL_ENERGY_COST);
                        body.setLinearVelocity(-AIRROLL_IMPULSE, body.getLinearVelocity().y);
                    }
                }
                lastLeftTapTime = now;
            }
            wasLeftPressed = true;
            direction = -1;
            facingRight = false;
            sprite.setFlip(false, false);
        } else {
            wasLeftPressed = false;
        }

        // Handling für das Pecken
        handlePecking(delta);

        // Rechte Taste
        if (InputManager.getInstance().isRightPressed()) {
            if (!wasRightPressed) { // Taste wurde gerade gedrückt
                long now = System.currentTimeMillis();
                if (now - lastRightTapTime < DOUBLE_TAP_THRESHOLD) {
                    if (this.getEnergyStatus().getCurrent() >= AIRROLL_ENERGY_COST) {
                        this.getEnergyStatus().consume(AIRROLL_ENERGY_COST);
                        body.setLinearVelocity(AIRROLL_IMPULSE, body.getLinearVelocity().y);
                    }
                }
                lastRightTapTime = now;
            }
            wasRightPressed = true;
            direction = 1;
            facingRight = true;
            sprite.setFlip(true, false);
        } else {
            wasRightPressed = false;
        }

        // Horizontale Beschleunigung (unabhängig von Flug oder Boden)
        Vector2 vel = body.getLinearVelocity();
        boolean isFlying = InputManager.getInstance().isJumpPressed() && !onGround;
        float maxSpeed;
        float maxAccelTime;
        if (isFlying) {
            maxSpeed = BASE_HORIZONTAL_SPEED * HORIZONTAL_SPEED_MULTIPLIER_FLY;
            maxAccelTime = MAX_ACCEL_TIME_FLY;
        } else {
            maxSpeed = BASE_HORIZONTAL_SPEED * HORIZONTAL_SPEED_MULTIPLIER_GROUND;
            maxAccelTime = MAX_ACCEL_TIME_GROUND;
        }
        if (direction != 0) {
            if (direction != lastDirection) {
                accelerationTime = 0f;
            }
            lastDirection = direction;
            accelerationTime += delta;
            if (accelerationTime > maxAccelTime) {
                accelerationTime = maxAccelTime;
            }
            float currentHorizontalSpeed = maxSpeed * (float) Math.sqrt(accelerationTime / maxAccelTime);
            body.setLinearVelocity(direction * currentHorizontalSpeed / Block.BLOCK_SIZE, vel.y);

            // Ziehe den Gehenergieverbrauch ab (3 Energie pro Sekunde)
            this.getEnergyStatus().consume(WALK_ENERGY_COST * delta);
        } else {
            accelerationTime = 0f;
            lastDirection = 0;
            body.setLinearVelocity(0, vel.y);
        }

        // SPACE-Taste: Je nach Situation wird entweder gehupft oder der Flugmodus aktiviert.
        if (InputManager.getInstance().isJumpPressed()) {
            regenerationEnergy = 0;
            if (onGround) {
                if (!groundJumpUsed) {
                    body.setLinearVelocity(body.getLinearVelocity().x, GROUND_HOP_FORCE / Block.BLOCK_SIZE);
                    groundJumpUsed = true;
                }
            } else if (direction != 0) {
                if (InputManager.getInstance().isWPressed()) {
                    // Präzisionsflug (Schweben)
                    if (this.getEnergyStatus().getCurrent() >= HOVER_ENERGY_COST * delta) {
                        this.getEnergyStatus().consume(HOVER_ENERGY_COST * delta);
                        body.setLinearVelocity(body.getLinearVelocity().x, HOVER_FORCE / Block.BLOCK_SIZE);
                    }
                } else if (InputManager.getInstance().isSPressed()) {
                    // Steiler Sturzflug & explosiver Auftrieb
                    if (!diveInitiated) {
                        body.setLinearVelocity(body.getLinearVelocity().x, DIVE_FORCE / Block.BLOCK_SIZE);
                        diveInitiated = true;
                    } else {
                        if (this.getEnergyStatus().getCurrent() >= STURZFUG_ENERGY_COST) {
                            this.getEnergyStatus().consume(STURZFUG_ENERGY_COST);
                            body.setLinearVelocity(body.getLinearVelocity().x, HIGH_UP_FORCE / Block.BLOCK_SIZE);
                        }
                        diveInitiated = false;
                    }
                } else {
                    // Normaler Flügelschlag (Auftrieb)
                    if (getY() < maxHeight) {
                        if (this.getEnergyStatus().getCurrent() >= FLUEGELSCHLAG_ENERGY_COST * delta) {
                            this.getEnergyStatus().consume(FLUEGELSCHLAG_ENERGY_COST * delta);
                            body.setLinearVelocity(body.getLinearVelocity().x, FLY_FORCE / Block.BLOCK_SIZE);
                        }
                    } else {
                        body.setLinearVelocity(body.getLinearVelocity().x, hoverDampening / Block.BLOCK_SIZE);
                    }
                }
            }
        }
        this.getEnergyStatus().regenerate(regenerationEnergy);
        setRotation(facingRight ? 0 : 180);
    }

    @Override
    public void draw(Batch batch) {
        // Zuerst die Zunge zeichnen, wenn wir am Pecken sind (damit sie hinter dem Spieler erscheint)
        if (isPecking && tongueLength > 0) {
            drawTongue(batch);
        }

        // Dann den Spieler zeichnen (im Vordergrund)
        super.draw(batch);
    }

    /**
     * Zeichnet die Zunge als dehnbares Rechteck
     */
    private void drawTongue(Batch batch) {
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

    public void setOnGround(boolean onGround) {
        this.onGround = onGround;
        if (onGround) {
            groundJumpUsed = false;
        }
    }

    protected boolean isOnGround() {
        return onGround;
    }

    public boolean isFacingRight() {
        return facingRight;
    }

    @Override
    public void dropItemOutside(Item item, int amount) {
        System.out.println("Dropped " + amount + "x " + item.getName() + " outside inventory.");
        Mob mob = MobRegistry.createMob("item", Globals.physicsWorld, this.getX(), this.getY() + 40, item);
        ClientGlobal.stage.addActor(mob);
        float dropSpeed = 20f;
        float angle = this.getRotation();
        float vx = com.badlogic.gdx.math.MathUtils.cosDeg(angle) * dropSpeed;
        float vy = com.badlogic.gdx.math.MathUtils.sinDeg(angle) * dropSpeed;
        Box2DOperationManager.queueOperation(() -> {
            if (mob.getBody() != null)
                mob.getBody().setLinearVelocity(vx, vy);
        });
    }

    @Override
    public void setSpeed(float speed) {
        BASE_HORIZONTAL_SPEED = speed;
    }

    @Override
    public float getSpeed() {
        return BASE_HORIZONTAL_SPEED;
    }

    @Override
    public void dispose() {
        super.dispose();
        if (pixelTexture != null) {
            pixelTexture.dispose();
        }
    }
}
