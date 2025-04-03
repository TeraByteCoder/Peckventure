package at.peckventure.ui;

import at.peckventure.status.Status;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;

public class HealthUI extends Actor {
    private final Status status;
    private final int heartSize = 16;
    private final int spacing = 2;
    // Anzahl der Herzen berechnet aus max. Leben, wobei jedes Herz 10 Leben repräsentiert
    private final int numHearts;
    private final int uiWidth;

    private static final TextureRegion WHITE_PIXEL;
    static {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        WHITE_PIXEL = new TextureRegion(new Texture(pixmap));
        pixmap.dispose();
    }

    // Konstruktor erhält nur das Status-Objekt; Position und Größe werden intern festgelegt.
    public HealthUI(Stage stage, Status status) {
        this.status = status;
        stage.addActor(this);
        // Bei jedem Herz stehen 10 Leben. Verwende Math.ceil, falls max nicht exakt durch 10 teilbar ist.
        numHearts = (int) Math.ceil(status.getMax() / 10.0);
        uiWidth = (heartSize + spacing) * numHearts;
        setSize(uiWidth, heartSize);
    }

    // Positioniert sich automatisch oben rechts (mit 20px Rand)
    @Override
    public void act(float delta) {
        super.act(delta);
        if(getStage() != null) {
            float stageWidth = getStage().getWidth();
            float stageHeight = getStage().getHeight();
            setPosition(stageWidth - getWidth() - 20, stageHeight - getHeight() - 20);
        }
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        // Berechnung: jedes volles Herz entspricht 10 Leben.
        int fullHearts = (int) (status.getCurrent() / 10);
        int remainder = (int) (status.getCurrent() % 10);
        boolean partialHeart = remainder > 0;

        for (int i = 0; i < numHearts; i++) {
            float drawX = getX() + i * (heartSize + spacing);
            float drawY = getY();

            // Hintergrund: leeres Herz
            batch.setColor(Color.DARK_GRAY);
            batch.draw(WHITE_PIXEL, drawX, drawY, heartSize, heartSize);

            // Volles Herz
            if (i < fullHearts) {
                batch.setColor(Color.RED);
                batch.draw(WHITE_PIXEL, drawX, drawY, heartSize, heartSize);
            }
            // Partielles Herz (proportionale Füllung)
            else if (i == fullHearts && partialHeart) {
                float fillWidth = heartSize * (remainder / 10f);
                batch.setColor(Color.RED);
                batch.draw(WHITE_PIXEL, drawX, drawY, fillWidth, heartSize);
            }
        }
        batch.setColor(Color.WHITE);
    }
}
