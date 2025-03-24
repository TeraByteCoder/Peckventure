package at.peckventure.menu;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.DragListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window;

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

        // -----------------------------------------
        // DRAG-BEREICH, UM DAS FENSTER ZU VERSCHIEBEN
        // -----------------------------------------
        final Label dragBar = new Label("Fenster ziehen", new Label.LabelStyle(new BitmapFont(), null));
        dragBar.setSize(Gdx.graphics.getWidth(), 30);
        dragBar.setPosition(0, Gdx.graphics.getHeight() - 30);
        stage.addActor(dragBar);

        dragBar.addListener(new DragListener() {
            float startX, startY;
            int initialWindowX, initialWindowY;
            Lwjgl3Window window = null;

            @Override
            public void dragStart(InputEvent event, float x, float y, int pointer) {
                super.dragStart(event, x, y, pointer);
                startX = x;
                startY = y;

                // Prüfen, ob wir überhaupt im Lwjgl3-Modus laufen
                if (Gdx.graphics instanceof Lwjgl3Graphics) {
                    Lwjgl3Graphics graphics = (Lwjgl3Graphics) Gdx.graphics;
                    window = graphics.getWindow();
                    // Aktuelle Fensterposition abfragen
                    initialWindowX = window.getPositionX();
                    initialWindowY = window.getPositionY();
                }
            }

            @Override
            public void drag(InputEvent event, float x, float y, int pointer) {
                super.drag(event, x, y, pointer);
                // Nur verschieben, wenn wir ein Lwjgl3Window haben (Fenstermodus)
                if (window != null && !Gdx.graphics.isFullscreen()) {
                    float deltaX = x - startX;
                    float deltaY = y - startY;
                    int newWindowX = initialWindowX + (int) deltaX;
                    // Beachte: Die Y-Achse in der Stage wächst nach oben,
                    // während die Fensterposition "von oben" gemessen wird.
                    int newWindowY = initialWindowY - (int) deltaY;
                    window.setPosition(newWindowX, newWindowY);
                }
            }
        });
        // -----------------------------------------

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

        // Event-Handler für Musiklautstärke
        musicSlider.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                GameSettings.setMusicVolume(musicSlider.getValue());
            }
        });

        // Event-Handler für Soundlautstärke
        soundSlider.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                GameSettings.setSoundVolume(soundSlider.getValue());
            }
        });

        // Event-Handler für Vollbild-/Fenstermoduswechsel
        fullscreenCheckbox.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                boolean isFullscreen = fullscreenCheckbox.isChecked();
                GameSettings.setFullscreen(isFullscreen);
                if (isFullscreen) {
                    // Vollbildmodus aktivieren
                    Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
                } else {
                    // Fenstermodus aktivieren (z.B. 640x480)
                    Gdx.graphics.setWindowedMode(640, 480);
                }
            }
        });

        // Event-Handler für den Zurück-Button
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // Zurück zum Hauptmenü (oder zu einem anderen Screen)
                game.setScreen(new MainMenu(game));
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
