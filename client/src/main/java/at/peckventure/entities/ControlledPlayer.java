package at.peckventure.entities;

import at.peckventure.ClientGlobal;
import at.peckventure.Globals;
import at.peckventure.entities.mob.Mob;
import at.peckventure.entities.mob.MobRegistry;
import at.peckventure.inventory.item.Item;
import at.peckventure.status.Status;
import at.peckventure.world.Box2DOperationManager;
import at.peckventure.world.block.Block;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import at.peckventure.InputManager;

public class ControlledPlayer extends Player {
    private static ControlledPlayer instance;
    private boolean facingRight = true;

    // Felder für Flugmodi
    private boolean diveInitiated = false; // Kennzeichnet, ob ein steiler Sturzflug begonnen hat
    private long lastLeftTapTime = 0;
    private long lastRightTapTime = 0;
    private static final long DOUBLE_TAP_THRESHOLD = 300; // in Millisekunden

    // Felder für Ground-Hop (einmaliges Hüpfen vom Boden)
    private boolean groundJumpUsed = false;           // Wurde bereits gehupft, bis zum nächsten Bodenkontakt
    private static final float GROUND_HOP_FORCE = 300f;  // Auftriebskraft beim Ground-Hop

    // Neues Feld, das den Bodenkontakt speichert (wird über den ContactListener gesetzt)
    private boolean onGround = false;

    // Annahme: Der Energie-Status wird in der Elternklasse gesetzt (z. B. new Status("Energie", 50))

    // Angepasste Kräfte für beeindruckendes Flugverhalten:
    private static final float FLY_FORCE = 450f;        // Normaler Flügelschlag: 5× mehr Auftrieb
    private static final float HIGH_UP_FORCE = 550f;      // Explosiver Auftrieb nach steilem Sturzflug
    private static final float DIVE_FORCE = -150f;        // Steiler, schneller Sturzflug
    private static final float HOVER_FORCE = 10f;         // Für Präzisionsflug (W + SPACE)
    private static final float AIRROLL_IMPULSE = 50f;       // Stärkerer horizontaler Impuls beim Airroll

    // Basisgeschwindigkeit (hier als Konstante definiert)
    private static final float BASE_HORIZONTAL_SPEED = 300f;

    // Horizontale Geschwindigkeitsmultiplikatoren:
    private static final float HORIZONTAL_SPEED_MULTIPLIER_FLY = 1.5f;    // Im Flug
    private static final float HORIZONTAL_SPEED_MULTIPLIER_GROUND = 2.0f; // Am Boden

    // Maximale Beschleunigungszeiten (bis Maximum erreicht wird)
    private static final float MAX_ACCEL_TIME_FLY = 1.0f;   // langsamer Anstieg im Flug
    private static final float MAX_ACCEL_TIME_GROUND = 0.5f; // schneller Anstieg am Boden

    // Energiekosten (Beispielwerte)
    private static final float FLUEGELSCHLAG_ENERGY_COST = 8.0f; // pro Sekunde
    private static final float HOVER_ENERGY_COST = 10.0f;         // pro Sekunde
    private static final float STURZFUG_ENERGY_COST = 20.0f;        // pro Aktion
    private static final float AIRROLL_ENERGY_COST = 15.0f;         // pro Aktion

    // Variable für horizontale Beschleunigung (über Zeit)
    private float accelerationTime = 0f;
    private int lastDirection = 0; // -1 für links, 1 für rechts, 0 wenn keine Eingabe

    private ControlledPlayer(World world, float x, float y) {
        super(world, x, y);
        this.sprite = new Sprite(new Texture("textures/woodpecker/woodpecker_idle.png"));
    }

    public static ControlledPlayer getInstance(World world, float x, float y) {
        if (instance == null) {
            instance = new ControlledPlayer(world, x, y);
        }
        return instance;
    }

    public static ControlledPlayer getInstance() {
        if (instance == null) {
            throw new IllegalStateException("ControlledPlayer not initialized. Call getInstance(world, x, y) first.");
        }
        return instance;
    }

    @Override
    protected void handleInput(float delta) {
        int direction = 0; // -1: links, 1: rechts

        // Horizontale Eingabe inkl. Luftrolle (Doppeltippen)
        if (InputManager.getInstance().isLeftPressed()) {
            long now = System.currentTimeMillis();
            if (now - lastLeftTapTime < DOUBLE_TAP_THRESHOLD) {
                if (this.getEnergyStatus().getCurrent() >= AIRROLL_ENERGY_COST) {
                    this.getEnergyStatus().consume((int) AIRROLL_ENERGY_COST);
                    body.setLinearVelocity(-AIRROLL_IMPULSE, body.getLinearVelocity().y);
                }
            }
            lastLeftTapTime = now;
            direction = -1;
            facingRight = false;
            sprite.setFlip(false, false);
        }
        if (InputManager.getInstance().isRightPressed()) {
            long now = System.currentTimeMillis();
            if (now - lastRightTapTime < DOUBLE_TAP_THRESHOLD) {
                if (this.getEnergyStatus().getCurrent() >= AIRROLL_ENERGY_COST) {
                    this.getEnergyStatus().consume((int) AIRROLL_ENERGY_COST);
                    body.setLinearVelocity(AIRROLL_IMPULSE, body.getLinearVelocity().y);
                }
            }
            lastRightTapTime = now;
            direction = 1;
            facingRight = true;
            sprite.setFlip(true, false);
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
            float currentHorizontalSpeed = maxSpeed * (float)Math.sqrt(accelerationTime / maxAccelTime);
            body.setLinearVelocity(direction * currentHorizontalSpeed / Block.BLOCK_SIZE, vel.y);
        } else {
            accelerationTime = 0f;
            lastDirection = 0;
            body.setLinearVelocity(0, vel.y);
        }

        // SPACE-Taste: Je nach Situation wird entweder gehupft oder der Flugmodus aktiviert.
        if (InputManager.getInstance().isJumpPressed()) {
            if (onGround) {
                // Vom Boden einmalig hüpfen, falls noch nicht gehupft
                if (!groundJumpUsed) {
                    body.setLinearVelocity(body.getLinearVelocity().x, GROUND_HOP_FORCE / Block.BLOCK_SIZE);
                    groundJumpUsed = true;
                }
            } else if (direction != 0) {
                // In der Luft: Flugmodi aktivieren
                if (InputManager.getInstance().isWPressed()) {
                    // Präzisionsflug (Schweben)
                    float energyCost = HOVER_ENERGY_COST * delta;
                    if (this.getEnergyStatus().getCurrent() >= energyCost) {
                        this.getEnergyStatus().consume((int) energyCost);
                        body.setLinearVelocity(body.getLinearVelocity().x, HOVER_FORCE / Block.BLOCK_SIZE);
                    }
                } else if (InputManager.getInstance().isSPressed()) {
                    // Steiler Sturzflug & explosiver Auftrieb
                    if (!diveInitiated) {
                        body.setLinearVelocity(body.getLinearVelocity().x, DIVE_FORCE / Block.BLOCK_SIZE);
                        diveInitiated = true;
                    } else {
                        if (this.getEnergyStatus().getCurrent() >= STURZFUG_ENERGY_COST) {
                            this.getEnergyStatus().consume((int) STURZFUG_ENERGY_COST);
                            body.setLinearVelocity(body.getLinearVelocity().x, HIGH_UP_FORCE / Block.BLOCK_SIZE);
                        }
                        diveInitiated = false;
                    }
                } else {
                    // Normaler Flügelschlag (Auftrieb)
                    if (getY() < maxHeight) {
                        float energyCost = FLUEGELSCHLAG_ENERGY_COST * delta;
                        if (this.getEnergyStatus().getCurrent() >= energyCost) {
                            this.getEnergyStatus().consume((int) energyCost);
                            body.setLinearVelocity(body.getLinearVelocity().x, FLY_FORCE / Block.BLOCK_SIZE);
                        }
                    } else {
                        body.setLinearVelocity(body.getLinearVelocity().x, hoverDampening / Block.BLOCK_SIZE);
                    }
                }
            }
        }
        setRotation(facingRight ? 0 : 180);
    }

    // Methode zum Setzen des Bodenkontakts – wird z. B. vom ContactListener aufgerufen
    public void setOnGround(boolean onGround) {
        this.onGround = onGround;
        if (onGround) {
            // Sobald der Spieler den Boden berührt, wird der Ground-Hop zurückgesetzt
            groundJumpUsed = false;
        }
    }

    // Überschreibe isOnGround() nun, um den Kontakt aus dem Listener zu verwenden
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
}
