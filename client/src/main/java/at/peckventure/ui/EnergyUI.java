package at.peckventure.ui;

import at.peckventure.status.Status;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;

public class EnergyUI extends Actor {
    private final Status status;
    // Damit die EnergyUI genauso breit ist wie die HealthUI, verwenden wir dieselbe Breitenberechnung.
    // Hier nehmen wir an, dass bei der HealthUI numHearts = Math.ceil(healthMax/10) gilt.
    // Für diesen Actor hardcoden wir deshalb denselben Wert:
    private final int numHearts =  (int) Math.ceil(100 / 10.0); // z.B. wenn max Health immer 100 ist
    private final int heartSize = 16;
    private final int spacing = 2;
    private final int barWidth = (heartSize + spacing) * numHearts;
    private final int barHeight = 10;

    private static final TextureRegion WHITE_PIXEL;
    static {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        WHITE_PIXEL = new TextureRegion(new Texture(pixmap));
        pixmap.dispose();
    }

    // Konstruktor erhält nur das Status-Objekt; Position und Größe werden intern festgelegt.
    public EnergyUI(Stage stage , Status status) {
        this.status = status;
        stage.addActor(this);
        setSize(barWidth, barHeight);
    }

    // Positioniert sich automatisch unten (unterhalb der HealthUI) oben rechts.
    // Hier nehmen wir an, dass die HealthUI eine Höhe von heartSize (16px) hat und wir einen Abstand von 10px einhalten.
    @Override
    public void act(float delta) {
        super.act(delta);
        if(getStage() != null) {
            float stageWidth = getStage().getWidth();
            float stageHeight = getStage().getHeight();
            // y-Position: oberer Rand - HealthUI-Höhe - 10px Abstand - EnergyUI-Höhe - 20px Rand
            setPosition(stageWidth - getWidth() - 20, stageHeight - heartSize - 20 - 10 - getHeight());
        }
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        // Hintergrund
        batch.setColor(Color.DARK_GRAY);
        batch.draw(WHITE_PIXEL, getX(), getY(), barWidth, barHeight);

        // Gefüllte Leiste – Füllgrad abhängig vom Prozentwert des Status.
        int filledWidth = (int) (status.getPercentage() * barWidth);
        batch.setColor(Color.CYAN);
        batch.draw(WHITE_PIXEL, getX(), getY(), filledWidth, barHeight);

        batch.setColor(Color.WHITE);
    }
}
