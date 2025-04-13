package at.peckventure.menu;

import at.peckventure.Globals;
import at.peckventure.LanguageManager;
import at.peckventure.SettingsManager;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.scenes.scene2d.Actor;

public class MainMenu implements Screen {
    private final Game game;
    private Stage stage;
    private Texture backgroundTexture;
    private Image backgroundImage;
    private Skin skin;

    public MainMenu(Game game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage();
        Gdx.input.setInputProcessor(stage);

        // Hintergrundbild laden
        backgroundTexture = new Texture("textures/background/forest.png");
        backgroundImage = new Image(backgroundTexture);
        backgroundImage.setSize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        stage.addActor(backgroundImage);

        // Skin laden
        skin = new Skin(Gdx.files.internal("ui/uiskin.json"));


        // Tabelle für Layout
        Table rootTable = new Table();
        rootTable.setFillParent(true);
        rootTable.align(Align.top);
        stage.addActor(rootTable);

        // Titel
        Label titleLabel = new Label("Peckventure", skin); // Spielname bleibt konstant
        titleLabel.setFontScale(2f);
        titleLabel.setAlignment(Align.center);
        rootTable.add(titleLabel).padTop(50).expandX().center();
        rootTable.row();

        LanguageManager.INSTANCE.setLangcode(GameSettings.getLanguage());

        // Buttons
        final TextButton singlePlayerButton = new TextButton(LanguageManager.INSTANCE.getText("menu.singleplayer"), skin);
        final TextButton multiPlayerButton = new TextButton(LanguageManager.INSTANCE.getText("menu.multiplayer"), skin);
        final TextButton creditsButton = new TextButton(LanguageManager.INSTANCE.getText("menu.credits"), skin);
        final TextButton settingsButton = new TextButton(LanguageManager.INSTANCE.getText("menu.settings"), skin);
        final TextButton exitButton = new TextButton(LanguageManager.INSTANCE.getText("menu.quit"), skin);

        // Button-Tabelle
        Table buttonTable = new Table();
        buttonTable.center();
        buttonTable.add(singlePlayerButton).size(300, 80).pad(20);
        buttonTable.row();
        buttonTable.add(multiPlayerButton).size(300, 80).pad(20);
        buttonTable.row();
        buttonTable.add(creditsButton).size(300, 80).pad(20);
        buttonTable.row();
        buttonTable.add(settingsButton).size(300, 80).pad(20);
        buttonTable.row();
        buttonTable.add(exitButton).size(300, 80).pad(20);

        rootTable.add(buttonTable).expandY().center();

        // Button-Aktionen
        singlePlayerButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new SinglePlayer(game));
            }
        });

        multiPlayerButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new MultiPlayer(game));
            }
        });

        settingsButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new Settings(game));
            }
        });

        exitButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Gdx.app.exit();
            }
        });
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() { }

    @Override
    public void resume() { }

    @Override
    public void hide() { }

    @Override
    public void dispose() {
        backgroundTexture.dispose();
        stage.dispose();
    }
}
