package at.peckventure.entities;

import at.peckventure.ClientGlobal;
import at.peckventure.Globals;
import at.peckventure.entities.mob.Mob;
import at.peckventure.entities.mob.MobRegistry;
import at.peckventure.inventory.item.Item;
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

    // Variable für horizontale Beschleunigung
    private float accelerationTime = 0f;
    private int lastDirection = 0; // -1 für links, 1 für rechts

    private ControlledPlayer(World world, float x, float y)
    {
        super(world, x, y);
        this.sprite = new Sprite(new Texture("textures/woodpecker/woodpecker_idle.png"));
    }

    public static ControlledPlayer getInstance(World world, float x, float y)
    {
        if (instance == null)
        {
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
            float currentHorizontalSpeed = maxSpeed * (float)Math.sqrt(accelerationTime / maxAccelTime);
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
            regenerationEnergy=0;
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
    public void setSpeed(float speed)
    {
        BASE_HORIZONTAL_SPEED = speed;
    }

    @Override
    public float getSpeed()
    {
        return BASE_HORIZONTAL_SPEED;
    }
}
