package at.peckventure.menu;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics.DisplayMode;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

public class Settings implements com.badlogic.gdx.Screen {
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

        // Hintergrund laden
        backgroundTexture = new Texture("textures/background/forest.png");
        backgroundImage = new Image(backgroundTexture);
        backgroundImage.setSize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        stage.addActor(backgroundImage);

        // Skin laden (sollte alle nötigen Drawables enthalten)
        Skin skin = new Skin(Gdx.files.internal("ui/uiskin.json"));

        // Sprachdatei laden: Es wird der in den Preferences gesetzte Sprachcode verwendet.
        String langCode = GameSettings.getLanguage(); // z. B. "en_us", "de_de", "de_at", "de_ch"
        JsonReader reader = new JsonReader();
        JsonValue texts = reader.parse(Gdx.files.internal("lang/" + langCode + ".json"));

        // Erstelle Slider für Musik- und Soundlautstärke sowie Checkbox für VSync.
        final Slider musicSlider = new Slider(0f, 1f, 0.1f, false, skin);
        final Slider soundSlider = new Slider(0f, 1f, 0.1f, false, skin);
        final CheckBox vsyncCheckbox = new CheckBox(texts.getString("menu.vsync"), skin);

        // Setze die initialen Werte aus den Preferences
        musicSlider.setValue(GameSettings.getMusicVolume());
        soundSlider.setValue(GameSettings.getSoundVolume());
        vsyncCheckbox.setChecked(GameSettings.isVSync());

        // Tabellenlayout erstellen
        Table table = new Table();
        table.setFillParent(true);
        table.align(Align.center);
        stage.addActor(table);

        // Texte aus der Sprachdatei (Fälle, in denen ein Schlüssel fehlt, werden mit Fallback-Texten abgedeckt)
        String musicLabelText = texts.has("menu.music_volume") ? texts.getString("menu.music_volume") : "Music Volume";
        String soundLabelText = texts.has("menu.sound_volume") ? texts.getString("menu.sound_volume") : "Sound Volume";
        String vsyncLabelText = texts.has("menu.vsync") ? texts.getString("menu.vsync") : "VSync";
        String languageLabelText = texts.has("menu.language") ? texts.getString("menu.language") : "Language";
        String backButtonText = texts.has("menu.back") ? texts.getString("menu.back") : "Back";

        // Musiklautstärke hinzufügen
        table.add(new Label(musicLabelText, skin)).pad(10);
        table.add(musicSlider).width(300).pad(10);
        table.row();

        // Soundlautstärke hinzufügen
        table.add(new Label(soundLabelText, skin)).pad(10);
        table.add(soundSlider).width(300).pad(10);
        table.row();

        // VSync hinzufügen
        table.add(new Label(vsyncLabelText, skin)).pad(10);
        table.add(vsyncCheckbox).pad(10);
        table.row();

        // Sprache auswählen (SelectBox)
        final SelectBox<String> languageSelect = new SelectBox<>(skin);
        languageSelect.setItems("en_us", "de_de", "de_at", "de_ch");
        languageSelect.setSelected(GameSettings.getLanguage());
        table.add(new Label(languageLabelText, skin)).pad(10);
        table.add(languageSelect).width(200).pad(10);
        table.row();

        // Back-Button hinzufügen
        TextButton backButton = new TextButton(backButtonText, skin);
        table.add(backButton).colspan(2).padTop(20);

        // Listener für Musiklautstärke
        musicSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                float value = musicSlider.getValue();
                GameSettings.setMusicVolume(value);
                System.out.println("Musiklautstärke aktualisiert auf: " + value);
            }
        });

        // Listener für Soundlautstärke
        soundSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                float value = soundSlider.getValue();
                GameSettings.setSoundVolume(value);
                System.out.println("Soundlautstärke aktualisiert auf: " + value);
            }
        });

        // Listener für VSync
        vsyncCheckbox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                boolean vsync = vsyncCheckbox.isChecked();
                GameSettings.setVSync(vsync);
                Gdx.graphics.setVSync(vsync);
                System.out.println("VSync aktualisiert auf: " + vsync);
            }
        });

        // Listener für Sprache: Nach Änderung wird die Settings-Seite neu geladen,
        // sodass die neuen Texte sofort sichtbar sind.
        languageSelect.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                String selectedLang = languageSelect.getSelected();
                GameSettings.setLanguage(selectedLang);
                System.out.println("Sprache aktualisiert auf: " + selectedLang);
                game.setScreen(new Settings(game));
            }
        });

        // Listener für den Back-Button
        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
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
