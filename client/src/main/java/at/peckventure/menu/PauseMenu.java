package at.peckventure.menu;

import at.peckventure.FontManager;
import at.peckventure.LanguageManager;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.scenes.scene2d.InputEvent;

public class PauseMenu implements Screen {
    private final Game game;
    private final Screen previousScreen;
    private Stage stage;
    private Texture buttonTexture;
    private Skin skin;

    public PauseMenu(Game game, Screen previousScreen) {
        this.game = game;
        this.previousScreen = previousScreen;
    }

    @Override
    public void show() {
        stage = new Stage();
        Gdx.input.setInputProcessor(stage);

        // FontManager und Skin verwenden
        FontManager fontManager = FontManager.getInstance();
        skin = fontManager.getSkin();

        // Buttons erstellen
        TextButton settingsButton = new TextButton(LanguageManager.INSTANCE.getText("menu.settings.button"), skin);
        TextButton mainMenuButton = new TextButton(LanguageManager.INSTANCE.getText("menu.main.menu"), skin);
        TextButton resumeButton = new TextButton(LanguageManager.INSTANCE.getText("menu.back.to.game"), skin);

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
        Label pausedLabel = new Label(LanguageManager.INSTANCE.getText("menu.paused"), skin);
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
        if (buttonTexture != null) {
            buttonTexture.dispose();
        }
        // Skin wird vom FontManager verwaltet und sollte nicht hier disposed werden
    }
}
