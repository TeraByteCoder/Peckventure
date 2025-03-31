package at.peckventure.ui;

import at.peckventure.Globals;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;

public class DebugOverlay extends Actor {
    private final BitmapFont font;

    public DebugOverlay(Stage stage) {
        font = new BitmapFont(); // Default-Schriftart
        font.setColor(Color.WHITE);
        // Optional: Schriftgröße, etc. anpassen
        stage.addActor(this);
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        // Hier kannst du weitere Debug-Daten aktualisieren, falls benötigt.
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        int fps = Gdx.graphics.getFramesPerSecond();
        int mobcount = Globals.mobs.size();
        // Erstelle den Debug-Text. Hier kannst du weitere Infos wie Speicher oder Koordinaten anhängen.
        String debugText = "FPS: " + fps + "\n" + mobcount + " mobs\n";

        // Zeichne den Text oben links (10px Abstand von links und oben)
        font.draw(batch, debugText, 10, Gdx.graphics.getHeight() - 10);
    }
}
