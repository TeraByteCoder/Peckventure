package at.peckventure.menu;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.graphics.g2d.BitmapFont;

public class MainMenu implements Screen {
    private Game game;
    private Stage stage;
    private Texture backgroundTexture;
    private Image backgroundImage;
    private Label titleLabel;

    public MainMenu(Game game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage();
        Gdx.input.setInputProcessor(stage);

        // Hintergrund
        backgroundTexture = new Texture("textures/background/forest.png");
        backgroundImage = new Image(backgroundTexture);
        backgroundImage.setSize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        stage.addActor(backgroundImage);

        // Titel
        Label.LabelStyle titleStyle = new Label.LabelStyle();
        titleStyle.font = new BitmapFont();
        titleLabel = new Label("Peckventure", titleStyle);
        titleLabel.setFontScale(2f);
        titleLabel.setAlignment(Align.center);

        // Button-Stil
        Texture buttonTexture = new Texture("textures/gui/button1.png");
        TextureRegionDrawable buttonDrawable = new TextureRegionDrawable(buttonTexture);
        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.up = buttonDrawable;
        buttonStyle.down = buttonDrawable;
        buttonStyle.font = new BitmapFont();

        // Buttons
        TextButton singlePlayerButton = new TextButton("SinglePlayer", buttonStyle);
        TextButton multiPlayerButton = new TextButton("MultiPlayer", buttonStyle);
        TextButton creditsButton = new TextButton("Credits", buttonStyle);
        TextButton settingsButton = new TextButton("Einstellungen", buttonStyle);
        TextButton exitButton = new TextButton("Beenden", buttonStyle);

        Table rootTable = new Table();
        rootTable.setFillParent(true);
        Table buttonTable = new Table();

        // Titel
        rootTable.top();
        rootTable.add(titleLabel).padTop(50).expandX().center();
        rootTable.row();

        // Buttons platzieren
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
        stage.addActor(rootTable);

        // Button Events
        singlePlayerButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new SinglePlayer(game));
            }
        });

        settingsButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new Settings(game)); // 🎯 Geht zu Settings
            }
        });

        exitButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit();
            }
        });

        multiPlayerButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new MultiPlayer(game));
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
    public void resize(int i, int i1) {}

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}

    @Override
    public void dispose() {
        backgroundTexture.dispose();
        stage.dispose();
    }
}
