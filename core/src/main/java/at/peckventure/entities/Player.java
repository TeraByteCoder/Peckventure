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

public class Player extends Actor {

    private World world;
    private Body body;
    private Sprite sprite;

    // Parameter in Pixel (für die Spielmechanik)
    private float speed = 400;       // Horizontale Geschwindigkeit in Pixel/s
    private float flyForce = 700;    // Vertikale "Flugkraft" in Pixel/s
    private float maxHeight = 1000;  // Maximale Flughöhe (relativ zum Start) in Pixel
    private float hoverDampening = 200; // Dämpfungswert, wenn oberhalb der Maximalhöhe

    private float startY;            // Startposition in Pixel

    public Player(World world, float x, float y) {
        this.world = world;
        this.sprite = new Sprite(new Texture("textures/woodpecker/woodpecker_idle.png"));
        this.startY = y;
        setSize(64, 64); // Spielergröße in Pixel

        // --- Body erstellen ---
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        // Bei Box2D entspricht die Position des Bodies dem Schwerpunkt.
        // Deshalb setzen wir die Position so, dass der Body in der Mitte des Actors liegt.
        bodyDef.position.set((x + getWidth() / 2) / Block.BLOCK_SIZE, (y + getHeight() / 2) / Block.BLOCK_SIZE);
        body = world.createBody(bodyDef);

        // Erstelle eine Box-Shape, die dem Actor entspricht (Halbmaße in Meter)
        PolygonShape shape = new PolygonShape();
        shape.setAsBox(getWidth() / 2 / Block.BLOCK_SIZE, getHeight() / 2 / Block.BLOCK_SIZE);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.density = 1f;
        fixtureDef.friction = 0.5f;
        fixtureDef.restitution = 0f; // Keine Rückprallkraft

        body.createFixture(fixtureDef);
        shape.dispose();

        // Setze UserData, um den Body später leichter identifizieren zu können
        body.setUserData(this);
    }

    @Override
    public void act(float delta) {
        // --- Steuerung horizontal (A/D) ---
        float direction = 0;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            direction = -1;
            sprite.setFlip(false, false);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            direction = 1;
            sprite.setFlip(true, false);
        }

        // Aktuelle Geschwindigkeiten ermitteln
        Vector2 vel = body.getLinearVelocity();
        // Horizontal setzen (Umrechnung: Pixel/s -> m/s)
        body.setLinearVelocity(direction * speed / Block.BLOCK_SIZE, vel.y);

        // --- Vertikale Steuerung (Space) ---
        // Berechne die aktuelle Y-Position des Bodies in Pixeln
        float bodyYPixels = body.getPosition().y * Block.BLOCK_SIZE;
        float relativeHeight = startY - bodyYPixels;
        if (Gdx.input.isKeyPressed(Input.Keys.SPACE)) {
            if (relativeHeight < maxHeight) {
                // Setze die vertikale Geschwindigkeit (in m/s)
                body.setLinearVelocity(body.getLinearVelocity().x, flyForce / Block.BLOCK_SIZE);
            } else {
                body.setLinearVelocity(body.getLinearVelocity().x, hoverDampening / Block.BLOCK_SIZE);
            }
        }

        // --- Synchronisiere den Actor mit der Body-Position ---
        // Da der Body in Box2D seinen Schwerpunkt angibt, müssen wir zum Zeichnen
        // die Position so setzen, dass der Actor mittig zum Body liegt.
        Vector2 bodyPos = body.getPosition();
        setPosition(bodyPos.x * Block.BLOCK_SIZE - getWidth() / 2, bodyPos.y * Block.BLOCK_SIZE - getHeight() / 2);
        
    }

    public int getChunkX() {
        return (int) this.getX() / Block.BLOCK_SIZE / Chunk.CHUNK_SIZE;
    }

    public int getChunkY() {
        return (int) this.getY() / Block.BLOCK_SIZE / Chunk.CHUNK_SIZE;
    }


    public void draw(Batch batch) {
        // Zeichne das Texture an der Position des Actors
        batch.draw(sprite, getX(), getY(), getWidth(), getHeight());
    }

    public Body getBody() {
        return body;
    }
}
