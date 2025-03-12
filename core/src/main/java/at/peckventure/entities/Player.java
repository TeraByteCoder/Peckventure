package at.peckventure.entities;

import at.peckventure.world.block.Block;
import at.peckventure.world.chunk.Chunk;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.scenes.scene2d.Actor;
import at.peckventure.InputManager;

public class Player extends Actor
{

    private final World world;
    private final Body body;
    private final Sprite sprite;

    // Parameter in Pixel (für die Spielmechanik)
    private final float speed = 400;       // Horizontale Geschwindigkeit in Pixel/s
    private final float flyForce = 700;    // Vertikale "Flugkraft" in Pixel/s
    private final float maxHeight = 1000;  // Maximale Flughöhe (relativ zum Start) in Pixel
    private final float hoverDampening = 200; // Dämpfungswert, wenn oberhalb der Maximalhöhe

    private final float startY;            // Startposition in Pixel

    public Player(World world, float x, float y)
    {
        this.world = world;
        this.sprite = new Sprite(new Texture("textures/woodpecker/woodpecker_idle.png"));
        this.startY = y;
        setSize(64, 64); // Spielergröße in Pixel

        // --- Body erstellen ---
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        // Positioniere den Body so, dass der Schwerpunkt in der Mitte liegt.
        bodyDef.position.set((x + getWidth() / 2f) / Block.BLOCK_SIZE, (y + getHeight() / 2f) / Block.BLOCK_SIZE);
        body = world.createBody(bodyDef);

        /*
         * Erstelle eine Capsule-Hitbox, die aus einem zentralen Rechteck und zwei Kreisen (oben & unten) besteht.
         *
         * Berechnungen:
         * - Breite in Meter: getWidth() / Block.BLOCK_SIZE
         * - Höhe in Meter: getHeight() / Block.BLOCK_SIZE
         * - Radius der Capsule: halbe Breite (in Meter)
         * - Höhe des Rechtecks: Gesamt-Höhe - 2 * Radius
         */
        float widthMeters = getWidth() / Block.BLOCK_SIZE;      // z.B. 64 / 32 = 2 m
        float heightMeters = getHeight() / Block.BLOCK_SIZE;      // z.B. 64 / 32 = 2 m
        float radius = widthMeters / 2f;                          // Radius = 1 m (bei 64x64)
        float rectHeight = heightMeters - 2 * radius;             // Mittlerer Teil; kann 0 oder negativ werden, falls Höhe <= Breite
        if (rectHeight < 0)
        {
            rectHeight = 0;
        }

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.density = 1f;
        fixtureDef.friction = 0.5f;
        fixtureDef.restitution = 0f;

        // Falls noch ein mittlerer Teil vorhanden ist, füge ein Rechteck hinzu.
        if (rectHeight > 0)
        {
            PolygonShape rectShape = new PolygonShape();
            // Erstelle ein Rechteck, das zentriert ist: halbe Breite = radius, halbe Höhe = rectHeight/2.
            rectShape.setAsBox(radius, rectHeight / 2f, new Vector2(0, 0), 0);
            fixtureDef.shape = rectShape;
            body.createFixture(fixtureDef);
            rectShape.dispose();
        }

        // Erstelle den oberen Kreis.
        CircleShape topCircle = new CircleShape();
        topCircle.setRadius(radius);
        // Positioniere den Kreis oben: die Mitte des Kreises liegt auf halber Höhe des Rechtecks.
        topCircle.setPosition(new Vector2(0, rectHeight / 2f));
        fixtureDef.shape = topCircle;
        body.createFixture(fixtureDef);
        topCircle.dispose();

        // Erstelle den unteren Kreis.
        CircleShape bottomCircle = new CircleShape();
        bottomCircle.setRadius(radius);
        // Positioniere den Kreis unten.
        bottomCircle.setPosition(new Vector2(0, -rectHeight / 2f));
        fixtureDef.shape = bottomCircle;
        body.createFixture(fixtureDef);
        bottomCircle.dispose();

        // Setze UserData, um den Body später leichter identifizieren zu können
        body.setUserData(this);
    }


    @Override
    public void act(float delta)
    {
        // --- Steuerung horizontal (A/D) ---
        float direction = 0;
        if (InputManager.getInstance().isLeftPressed()) {
            direction = -1;
            sprite.setFlip(false, false);
        }
        if (InputManager.getInstance().isRightPressed()) {
            direction = 1;
            sprite.setFlip(true, false);
        }

        // Rotation passend setzen: Rechts = 0°, Links = 180°
        if (direction != 0) {
            setRotation(direction == 1 ? 0 : 180);
        }

        // Aktuelle Geschwindigkeiten ermitteln
        Vector2 vel = body.getLinearVelocity();
        // Horizontal setzen (Umrechnung: Pixel/s -> m/s)
        body.setLinearVelocity(direction * speed / Block.BLOCK_SIZE, vel.y);

        // --- Vertikale Steuerung (Space) ---
        float bodyYPixels = body.getPosition().y * Block.BLOCK_SIZE;
        float relativeHeight = startY - bodyYPixels;
        if (InputManager.getInstance().isJumpPressed())
        {
            if (relativeHeight < maxHeight)
            {
                body.setLinearVelocity(body.getLinearVelocity().x, flyForce / Block.BLOCK_SIZE);
            } else
            {
                body.setLinearVelocity(body.getLinearVelocity().x, hoverDampening / Block.BLOCK_SIZE);
            }
        }

        // Synchronisiere den Actor mit der Body-Position
        Vector2 bodyPos = body.getPosition();
        setPosition(bodyPos.x * Block.BLOCK_SIZE - getWidth() / 2,
            bodyPos.y * Block.BLOCK_SIZE - getHeight() / 2);
    }


    public int getChunkX()
    {
        return (int) this.getX() / Block.BLOCK_SIZE / Chunk.CHUNK_SIZE;
    }

    public int getChunkY()
    {
        return (int) this.getY() / Block.BLOCK_SIZE / Chunk.CHUNK_SIZE;
    }


    public void draw(Batch batch)
    {
        // Zeichne das Texture an der Position des Actors
        batch.draw(sprite, getX(), getY(), getWidth(), getHeight());
    }

    public Body getBody()
    {
        return body;
    }
}
