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
import com.badlogic.gdx.graphics.g2d.BitmapFont;

public class Settings implements Screen {
    private final Game game;
    private Stage stage;
    private Texture backgroundTexture;
    private Image backgroundImage;

    public Settings(Game game) {
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

        // Button-Stil
        Texture buttonTexture = new Texture("textures/gui/button1.png");
        TextureRegionDrawable buttonDrawable = new TextureRegionDrawable(buttonTexture);
        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.up = buttonDrawable;
        buttonStyle.down = buttonDrawable;
        buttonStyle.font = new BitmapFont();

        // Schieberegler für Musiklautstärke
        final Slider musicSlider = new Slider(0, 1, 0.1f, false, new Skin(Gdx.files.internal("ui/uiskin.json")));
        musicSlider.setValue(GameSettings.getMusicVolume());

        // Schieberegler für Soundlautstärke
        final Slider soundSlider = new Slider(0, 1, 0.1f, false, new Skin(Gdx.files.internal("ui/uiskin.json")));
        soundSlider.setValue(GameSettings.getSoundVolume());

        // Checkbox für Vollbildmodus
        final CheckBox fullscreenCheckbox = new CheckBox(" Vollbild", new Skin(Gdx.files.internal("ui/uiskin.json")));
        fullscreenCheckbox.setChecked(GameSettings.isFullscreen());

        // Zurück-Button
        TextButton backButton = new TextButton("Zurück", buttonStyle);

        // Layout
        Table rootTable = new Table();
        rootTable.setFillParent(true);
        rootTable.center();

        rootTable.add(new Label("Musiklautstärke", new Label.LabelStyle(new BitmapFont(), null))).pad(10);
        rootTable.add(musicSlider).width(200).pad(10).row();
        rootTable.add(new Label("Soundlautstärke", new Label.LabelStyle(new BitmapFont(), null))).pad(10);
        rootTable.add(soundSlider).width(200).pad(10).row();
        rootTable.add(fullscreenCheckbox).colspan(2).pad(10).row();
        rootTable.add(backButton).colspan(2).pad(20).size(200, 60);

        stage.addActor(rootTable);

        // Event-Handler
        musicSlider.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                GameSettings.setMusicVolume(musicSlider.getValue());
            }
        });

        soundSlider.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                GameSettings.setSoundVolume(soundSlider.getValue());
            }
        });

        fullscreenCheckbox.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                GameSettings.setFullscreen(fullscreenCheckbox.isChecked());
                Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
            }
        });

        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new MainMenu(game)); // Zurück zum Hauptmenü
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
