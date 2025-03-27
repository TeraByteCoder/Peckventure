package at.peckventure.menu;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.scenes.scene2d.InputEvent;

public class PauseMenu implements Screen {
    private final Game game;
    private final Screen previousScreen;
    private Stage stage;
    private Texture buttonTexture;

    public PauseMenu(Game game, Screen previousScreen) {
        this.game = game;
        this.previousScreen = previousScreen;
    }

    @Override
    public void show() {
        stage = new Stage();
        Gdx.input.setInputProcessor(stage);

        // Button-Textur laden
        buttonTexture = new Texture("textures/gui/button1.png");
        TextureRegionDrawable buttonDrawable = new TextureRegionDrawable(buttonTexture);

        // TextButtonStyle mit der geladenen Textur und einem Standard-Font
        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.up = buttonDrawable;
        buttonStyle.down = buttonDrawable;
        buttonStyle.font = new BitmapFont();

        // Buttons erstellen
        TextButton settingsButton = new TextButton("Settings", buttonStyle);
        TextButton mainMenuButton = new TextButton("Main Menu", buttonStyle);
        TextButton resumeButton = new TextButton("Back to Game", buttonStyle);

        // ClickListener für Settings: Nur Log-Ausgabe
        settingsButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.log("PauseMenu", "Settings button clicked (no action assigned)");
            }
        });

        // ClickListener für Main Menu: Wechsel zum MainMenu
        mainMenuButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new MainMenu(game));
            }
        });

        // ClickListener für Back to Game: Zurück zum vorherigen Screen
        resumeButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(previousScreen);
            }
        });

        // Table-Layout zur Zentrierung der Elemente
        Table table = new Table();
        table.setFillParent(true);
        table.center();

        // Überschrift "Paused" hinzufügen
        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = new BitmapFont();
        Label pausedLabel = new Label("Paused", labelStyle);
        pausedLabel.setFontScale(2f);
        pausedLabel.setAlignment(Align.center);

        // Elemente zum Table hinzufügen
        table.add(pausedLabel).padBottom(50).center();
        table.row();
        table.add(settingsButton).size(300, 80).pad(20);
        table.row();
        table.add(mainMenuButton).size(300, 80).pad(20);
        table.row();
        table.add(resumeButton).size(300, 80).pad(20);

        stage.addActor(table);
    }

    @Override
    public void render(float delta) {
        // Optional: Halbdurchsichtiger Hintergrund, damit man das Spiel dahinter sieht
        Gdx.gl.glClearColor(0, 0, 0, 0.7f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // ESC-Taste: Setzt den vorherigen Screen wieder
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(previousScreen);
        }

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {
        // Hier kannst du bei Bedarf den Pause-Zustand behandeln
    }

    @Override
    public void resume() {
        // Hier kannst du bei Bedarf den Resume-Zustand behandeln
    }

    @Override
    public void hide() {
        dispose();
    }

    @Override
    public void dispose() {
        stage.dispose();
        buttonTexture.dispose();
    }
}
