package at.peckventure;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.Array;

/**
 * Ein Actor, der mehrere Parallax-Layer lädt und
 * den gesamten Bildschirm in der Höhe füllt.
 * Horizontal wird die Textur wiederholt (Loop).
 *
 * Die Position orientiert sich an der Kamera.
 */
public class ParallaxBackgroundActor extends Actor {

    private final OrthographicCamera camera;
    private final Array<ParallaxLayer> layers;

    public ParallaxBackgroundActor(OrthographicCamera camera) {
        this.camera = camera;
        this.layers = new Array<>();
    }

    /**
     * Fügt eine Parallax-Ebene hinzu.
     *
     * @param texturePath   Pfad zur Textur (intern).
     * @param parallaxRatio Verhältnis, wie stark diese Ebene scrollt.
     */
    public void addLayer(String texturePath, float parallaxRatio) {
        Texture texture = new Texture(Gdx.files.internal(texturePath));
        texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.ClampToEdge);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        TextureRegion region = new TextureRegion(texture);
        layers.add(new ParallaxLayer(region, parallaxRatio));
    }


    @Override
    public void act(float delta) {
        super.act(delta);
        if (getStage() != null) {
            float viewportWidth = getStage().getViewport().getWorldWidth();
            float viewportHeight = getStage().getViewport().getWorldHeight();
            // Setze Position und Größe so, dass der gesamte virtuellen Bereich abgedeckt wird
            setPosition(
                camera.position.x - viewportWidth / 2f,
                camera.position.y - viewportHeight / 2f
            );
            setSize(viewportWidth, viewportHeight);
        }
    }


    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (getStage() == null) return;

        float viewportWidth = getStage().getViewport().getWorldWidth();
        float viewportHeight = getStage().getViewport().getWorldHeight();
        // Basis-Höhe deines Bildes (native Höhe, z.B. 180)
        float baseHeight = 180f;
        // Einheitlicher Skalierungsfaktor
        float scale = viewportHeight / baseHeight;

        for (ParallaxLayer layer : layers) {
            TextureRegion region = layer.region;
            // Berechne die skalierten Maße
            float drawWidth = region.getRegionWidth() * scale;
            float drawHeight = region.getRegionHeight() * scale;

            // Horizontaler Offset: berechnet anhand der Kameraposition, Parallax-Faktor und Skalierung
            float offsetX = (camera.position.x * layer.parallaxRatio * scale * 0.1f) % drawWidth;
            if (offsetX < 0) offsetX += drawWidth;

            // Vertikaler Offset: Wird berechnet, aber nicht gemodt – also kein Tiling
            float offsetY = -camera.position.y * layer.parallaxRatio * 0.2f;


            // Für horizontalen Loop: Zeichne mehrere Kopien in x-Richtung
            for (float x = getX() - offsetX; x < getX() + viewportWidth; x += drawWidth) {
                // Vertikal wird nur einmal gezeichnet: getY() + offsetY
                batch.draw(region, x, getY() + offsetY, drawWidth, drawHeight);
            }
        }
    }


    /**
     * Innere Hilfsklasse für eine Parallax-Ebene.
     */
    private static class ParallaxLayer {
        final TextureRegion region;
        final float parallaxRatio;

        public ParallaxLayer(TextureRegion region, float parallaxRatio) {
            this.region = region;
            this.parallaxRatio = parallaxRatio;
        }
    }
}
