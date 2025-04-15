package at.peckventure.light;

import at.peckventure.world.block.Block;
import box2dLight.DirectionalLight;
import box2dLight.PointLight;
import box2dLight.RayHandler;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.graphics.OrthographicCamera;
import at.peckventure.entities.Player;

/**
 * Ein einfaches Box2DLights System mit Directional Light
 */
public class LightSystem implements Disposable {
    private RayHandler rayHandler;
    private DirectionalLight sunLight;
    private PointLight playerLight;
    private Player player;

    // WICHTIG: Dies muss exakt dem BLOCK_SIZE entsprechen, der in der Box2D-Physik verwendet wird
    private static final float PPM = Block.BLOCK_SIZE;

    /**
     * Erstellt ein neues Lichtsystem
     * @param world Die Box2D-Welt
     * @param player Der Spieler (für spielerzentriertes Licht)
     */
    public LightSystem(World world, Player player) {
        this.player = player;

        // Erstelle RayHandler mit aktueller API
        rayHandler = new RayHandler(world);

        // Konfiguration direkt am rayHandler-Objekt statt über statische Methoden
        rayHandler.setGammaCorrection(true);
        rayHandler.useDiffuseLight(true);

        // Ambient Light sehr niedrig setzen, damit der Kontrast stärker ist
        rayHandler.setAmbientLight(0.1f, 0.1f, 0.1f, 0.3f);

        // Anzahl der Blur-Schritte - höher = weichere Schatten, aber langsamer
        rayHandler.setBlurNum(2);

        // Schatten aktivieren - WICHTIG für Block-Abschattung
        rayHandler.setShadows(true);

        // Debug-Ausgabe
        System.out.println("LightSystem: RayHandler erstellt mit PPM=" + PPM);
        System.out.println("LightSystem: Block.BLOCK_SIZE=" + Block.BLOCK_SIZE);

        createLights();
    }

    /**
     * Erstellt die Lichter
     */
    private void createLights() {
        // Sonnenlicht von oben - DirectionalLight mit Richtung 270° (nach unten)
        Color sunColor = new Color(1.0f, 0.9f, 0.8f, 0.8f);
        sunLight = new DirectionalLight(rayHandler, 512, sunColor, 270);

        // Spielerlicht
        Color playerLightColor = new Color(0.9f, 0.8f, 0.6f, 0.7f);
        playerLight = new PointLight(rayHandler, 128, playerLightColor, 8f,
            player.getX() / PPM, player.getY() / PPM);

        // Debug-Info
        System.out.println("LightSystem: Lichter erstellt");
        System.out.println("LightSystem: Spieler-Koordinaten: x=" + player.getX() + ", y=" + player.getY());
        System.out.println("LightSystem: Spielerlicht-Koordinaten: x=" +
            (player.getX() / PPM) + ", y=" + (player.getY() / PPM));

        // An Spieler anhängen wenn möglich
        if (player.getBody() != null) {
            playerLight.attachToBody(player.getBody());
            System.out.println("LightSystem: Spielerlicht an Body angehängt");
        } else {
            System.out.println("LightSystem: Warnung - Spieler hat keinen Body!");
        }
    }

    /**
     * Aktualisiert und rendert das Lichtsystem
     * WICHTIG: Diese Methode muss nach dem Zeichnen der Welt und Spieler aufgerufen werden
     */
    public void updateAndRender(OrthographicCamera camera) {
        if (rayHandler == null) return;

        // Rendering-Setup
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_DST_COLOR, GL20.GL_SRC_ALPHA);

        try {
            // Verwende die neuere API-Methode für die Kamera-Matrix
            // Die aktuelle Methode benötigt: Matrix, Position X, Position Y, Viewport-Breite, Viewport-Höhe
            rayHandler.setCombinedMatrix(
                camera.combined,
                camera.position.x / PPM,
                camera.position.y / PPM,
                camera.viewportWidth / PPM,
                camera.viewportHeight / PPM
            );

            // Spielerlicht-Koordinaten aktualisieren, falls nicht am Body angehängt
            if (player.getBody() == null && playerLight != null) {
                playerLight.setPosition(player.getX() / PPM, player.getY() / PPM);
            }

            // Licht-Rendering - wird alle Box2D-Bodies beachten
            rayHandler.updateAndRender();

        } catch (Exception e) {
            System.err.println("LightSystem: Fehler beim Rendern: " + e.getMessage());
            e.printStackTrace();
        } finally {
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }
    }

    /**
     * Schaltet zwischen Tag und Nacht um
     */
    public boolean toggleDarkMode() {
        // Umschaltlogik: Wenn Sonnenlicht hell ist, dann dunkel setzen, sonst hell
        float alpha = sunLight.getColor().a;
        boolean isDarkMode = alpha > 0.5f;

        if (isDarkMode) {
            // Tag: Helles Sonnenlicht
            sunLight.setColor(1.0f, 0.9f, 0.8f, 0.8f);
            rayHandler.setAmbientLight(0.1f, 0.1f, 0.1f, 0.3f);
            System.out.println("LightSystem: Tag-Modus aktiviert");
            return false;
        } else {
            // Nacht: Dunkles, blaues Mondlicht
            sunLight.setColor(0.2f, 0.3f, 0.8f, 0.3f);
            rayHandler.setAmbientLight(0.05f, 0.05f, 0.1f, 0.7f);
            System.out.println("LightSystem: Nacht-Modus aktiviert");
            return true;
        }
    }

    /**
     * Direkter Zugriff auf den RayHandler für fortgeschrittene Konfiguration
     */
    public RayHandler getRayHandler() {
        return rayHandler;
    }

    /**
     * Setzt die Umgebungslichtfarbe
     */
    public void setAmbientLight(float r, float g, float b, float a) {
        if (rayHandler != null) {
            rayHandler.setAmbientLight(r, g, b, a);
        }
    }

    @Override
    public void dispose() {
        if (rayHandler != null) {
            rayHandler.dispose();
            rayHandler = null;
        }
    }
}
