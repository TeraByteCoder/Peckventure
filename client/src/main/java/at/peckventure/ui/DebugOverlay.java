package at.peckventure.ui;

import at.peckventure.Globals;
import at.peckventure.entities.ControlledPlayer;
import at.peckventure.world.block.Block;
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
        // Optional: Schriftgröße und andere Einstellungen anpassen
        stage.addActor(this);
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        // Hier können weitere Debug-Daten aktualisiert werden, falls benötigt.
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        String debugText = getDebugInfo();
        font.draw(batch, debugText, 10, Gdx.graphics.getHeight() - 10);
    }

    /**
     * Erzeugt den Debug-Text, der angezeigt wird.
     * Diese Methode kann in abgeleiteten Klassen überschrieben werden, um zusätzliche Informationen einzufügen.
     */
    protected String getDebugInfo() {
        int fps = Gdx.graphics.getFramesPerSecond();
        int mobcount = Globals.mobs.size();

        // Berechnung der Position in Block-Koordinaten
        double x = ControlledPlayer.getInstance().getX() / Block.BLOCK_SIZE;
        double y = ControlledPlayer.getInstance().getY() / Block.BLOCK_SIZE;

        // Formatierung: immer 3 Nachkommastellen (auch bei 0,000)
        String posX = String.format("%.3f", x);
        String posY = String.format("%.3f", y);

        StringBuilder sb = new StringBuilder();
        sb.append("FPS: ").append(fps).append("\n")
                .append(mobcount).append(" mobs\n")
                .append("Position: ").append(posX).append(" ").append(posY);
        return sb.toString();
    }
}
