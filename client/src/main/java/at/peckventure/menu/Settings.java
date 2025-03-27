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
                if (Gdx.graphics instanceof Lwjgl3Graphics) {
                    Lwjgl3Graphics graphics = (Lwjgl3Graphics) Gdx.graphics;
                    window = graphics.getWindow();
                    initialWindowX = window.getPositionX();
                    initialWindowY = window.getPositionY();
                }
            }

            @Override
            public void drag(InputEvent event, float x, float y, int pointer) {
                super.drag(event, x, y, pointer);
                if (window != null && !Gdx.graphics.isFullscreen()) {
                    float deltaX = x - startX;
                    float deltaY = y - startY;
                    int newWindowX = initialWindowX + (int) deltaX;
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

        Skin skin = new Skin(Gdx.files.internal("ui/uiskin.json"));

        // ===============================
        // Inhalt der einzelnen Tabs
        // ===============================
        // Audio-Einstellungen
        final Slider musicSlider = new Slider(0, 1, 0.1f, false, skin);
        musicSlider.setValue(GameSettings.getMusicVolume());
        final Slider soundSlider = new Slider(0, 1, 0.1f, false, skin);
        soundSlider.setValue(GameSettings.getSoundVolume());
        final CheckBox fullscreenCheckbox = new CheckBox(" Vollbild", skin);
        fullscreenCheckbox.setChecked(GameSettings.isFullscreen());

        final Table audioTable = new Table();
        audioTable.add(new Label("Musiklautstärke", new Label.LabelStyle(new BitmapFont(), null))).pad(10);
        audioTable.add(musicSlider).width(200).pad(10).row();
        audioTable.add(new Label("Soundlautstärke", new Label.LabelStyle(new BitmapFont(), null))).pad(10);
        audioTable.add(soundSlider).width(200).pad(10).row();
        audioTable.add(fullscreenCheckbox).colspan(2).pad(10).row();

        // Video-Einstellungen
        final TextButton resolutionButton = new TextButton("640x480", buttonStyle);
        final CheckBox vsyncCheckbox = new CheckBox(" VSync", skin);

        final Table videoTable = new Table();
        videoTable.add(new Label("Video-Einstellungen", new Label.LabelStyle(new BitmapFont(), null))).row();
        videoTable.add(new Label("Auflösung:", new Label.LabelStyle(new BitmapFont(), null))).pad(10);
        videoTable.add(resolutionButton).pad(10).row();
        videoTable.add(new Label("Grafikeinstellungen:", new Label.LabelStyle(new BitmapFont(), null))).pad(10);
        videoTable.add(vsyncCheckbox).pad(10).row();

        // Tastenbelegung
        final Label keyBindingsLabel = new Label("Tastenbelegung", new Label.LabelStyle(new BitmapFont(), null));
        final Label upLabel = new Label("Bewege nach oben: W", new Label.LabelStyle(new BitmapFont(), null));
        final Label downLabel = new Label("Bewege nach unten: S", new Label.LabelStyle(new BitmapFont(), null));
        final Label leftLabel = new Label("Bewege nach links: A", new Label.LabelStyle(new BitmapFont(), null));
        final Label rightLabel = new Label("Bewege nach rechts: D", new Label.LabelStyle(new BitmapFont(), null));

        final Table keyBindingsTable = new Table();
        keyBindingsTable.add(keyBindingsLabel).row();
        keyBindingsTable.add(upLabel).pad(10).row();
        keyBindingsTable.add(downLabel).pad(10).row();
        keyBindingsTable.add(leftLabel).pad(10).row();
        keyBindingsTable.add(rightLabel).pad(10).row();

        // ===============================
        // Layout: Tab-Buttons und Content-Bereich
        // ===============================
        Table rootTable = new Table();
        rootTable.setFillParent(true);
        stage.addActor(rootTable);

        // Tab-Buttons
        Table tabButtons = new Table();
        final TextButton btnAudio = new TextButton("Audio", buttonStyle);
        final TextButton btnVideo = new TextButton("Video", buttonStyle);
        final TextButton btnKeyBindings = new TextButton("Tastenbelegung", buttonStyle);
        tabButtons.add(btnAudio).pad(5);
        tabButtons.add(btnVideo).pad(5);
        tabButtons.add(btnKeyBindings).pad(5);
        rootTable.add(tabButtons).colspan(2).padBottom(20).row();

        // Content-Table für den aktuellen Tab
        final Table contentTable = new Table();
        rootTable.add(contentTable).colspan(2).expand().fill().row();

        // Standardmäßig wird der Audio-Tab angezeigt
        contentTable.add(audioTable).expand().fill();

        // Zurück-Button, der immer sichtbar ist
        TextButton backButton = new TextButton("Zurück", buttonStyle);
        rootTable.add(backButton).colspan(2).padTop(20).size(200, 60);

        // ===============================
        // Listener für die Tab-Buttons
        // ===============================
        btnAudio.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                contentTable.clearChildren();
                contentTable.add(audioTable).expand().fill();
            }
        });
        btnVideo.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                contentTable.clearChildren();
                contentTable.add(videoTable).expand().fill();
            }
        });
        btnKeyBindings.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                contentTable.clearChildren();
                contentTable.add(keyBindingsTable).expand().fill();
            }
        });

        // ===============================
        // Event-Handler
        // ===============================
        // Audio-Einstellungen
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
                boolean isFullscreen = fullscreenCheckbox.isChecked();
                GameSettings.setFullscreen(isFullscreen);
                if (isFullscreen) {
                    Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
                } else {
                    Gdx.graphics.setWindowedMode(640, 480);
                }
            }
        });
        // Zurück-Button
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
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
