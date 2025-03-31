package at.peckventure.menu;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

public class Settings implements Screen {
    private final Game game;
    private Stage stage;
    private Texture backgroundTexture;
    private Image backgroundImage;
    private Skin skin;
    private Table contentTable; // Bereich, in dem die Tab-Inhalte angezeigt werden.
    private JsonValue texts;    // Übersetzungen

    // Hilfsklasse, die Sprachcode und Anzeigetext kapselt
    public static class LanguageOption {
        public final String code;
        public final String displayName;

        public LanguageOption(String code, String displayName) {
            this.code = code;
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

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

        // Skin laden
        skin = new Skin(Gdx.files.internal("ui/uiskin.json"));

        // Sprachdatei laden anhand der in den Preferences gesetzten Sprache
        String langCode = GameSettings.getLanguage(); // z. B. "en_us", "de_de", "de_at", "de_ch"
        JsonReader reader = new JsonReader();
        texts = reader.parse(Gdx.files.internal("lang/" + langCode + ".json"));

        // Hauptcontainer
        Table mainTable = new Table();
        mainTable.setFillParent(true);
        mainTable.align(Align.top);
        stage.addActor(mainTable);

        // Tab-Leiste: Vier Tabs als TextButtons
        final TextButton generalTab = new TextButton(texts.has("menu.tab_allgemein") ? texts.getString("menu.tab_allgemein") : "Allgemein", skin);
        final TextButton audioTab = new TextButton(texts.has("menu.tab_audio") ? texts.getString("menu.tab_audio") : "Audio", skin);
        final TextButton keyBindingsTab = new TextButton(texts.has("menu.tab_tastenbelegung") ? texts.getString("menu.tab_tastenbelegung") : "Tastenbelegung", skin);
        final TextButton videoTab = new TextButton(texts.has("menu.tab_video") ? texts.getString("menu.tab_video") : "Video", skin);

        Table tabBar = new Table();
        tabBar.add(generalTab).pad(5);
        tabBar.add(audioTab).pad(5);
        tabBar.add(keyBindingsTab).pad(5);
        tabBar.add(videoTab).pad(5);

        // Inhaltsbereich, der je nach Tab gefüllt wird
        contentTable = new Table();
        contentTable.padTop(20);

        // Back-Button am unteren Rand
        TextButton backButton = new TextButton(texts.has("menu.back") ? texts.getString("menu.back") : "Back", skin);

        // Hauptlayout zusammenbauen: Tab-Leiste oben, Content in der Mitte, Back-Button unten
        mainTable.add(tabBar).expandX().fillX().padTop(10);
        mainTable.row();
        mainTable.add(contentTable).expand().fill().pad(10);
        mainTable.row();
        mainTable.add(backButton).padBottom(10);

        // Standardmäßig den Allgemein-Tab laden
        loadGeneralTab();

        // Listener für die Tabs
        generalTab.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                loadGeneralTab();
            }
        });

        audioTab.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                loadAudioTab();
            }
        });

        keyBindingsTab.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                loadKeyBindingsTab();
            }
        });

        videoTab.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                loadVideoTab();
            }
        });

        // Listener für Back-Button: Wechselt zurück ins Hauptmenü
        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new MainMenu(game));
            }
        });
    }

    // Laden des "Allgemein"-Tabs, hier wird u. a. auch die Sprachauswahl angeboten.
    private void loadGeneralTab() {
        contentTable.clear();
        Label languageLabel = new Label(texts.has("menu.language") ? texts.getString("menu.language") : "Language", skin);
        final SelectBox<LanguageOption> languageSelect = new SelectBox<>(skin);

        // Sprachoptionen: Code und Anzeige-Bezeichnung
        LanguageOption[] options = new LanguageOption[] {
            new LanguageOption("en_us", texts.has("menu.language_en") ? texts.getString("menu.language_en") : "English"),
            new LanguageOption("de_de", texts.has("menu.language_de") ? texts.getString("menu.language_de") : "Deutsch"),
            new LanguageOption("de_at", texts.has("menu.language_de_at") ? texts.getString("menu.language_de_at") : "Österreichisch"),
            new LanguageOption("de_ch", texts.has("menu.language_de_ch") ? texts.getString("menu.language_de_ch") : "Schweizerisch")
        };
        languageSelect.setItems(options);
        // Vorbelegung anhand des gespeicherten Sprachcodes
        for (LanguageOption option : options) {
            if (option.code.equals(GameSettings.getLanguage())) {
                languageSelect.setSelected(option);
                break;
            }
        }

        contentTable.add(languageLabel).pad(10);
        contentTable.add(languageSelect).width(200).pad(10);
        contentTable.row();

        // Listener für Sprachwechsel: direkt das Menü neu laden
        languageSelect.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                LanguageOption selectedOption = languageSelect.getSelected();
                GameSettings.setLanguage(selectedOption.code);
                System.out.println("Sprache aktualisiert auf: " + selectedOption.code);
                game.setScreen(new Settings(game));
            }
        });
    }

    // Laden des "Audio"-Tabs: Musik- und Soundlautstärke
    private void loadAudioTab() {
        contentTable.clear();
        Label musicLabel = new Label(texts.has("menu.music_volume") ? texts.getString("menu.music_volume") : "Music Volume", skin);
        final Slider musicSlider = new Slider(0f, 1f, 0.1f, false, skin);
        musicSlider.setValue(GameSettings.getMusicVolume());

        Label soundLabel = new Label(texts.has("menu.sound_volume") ? texts.getString("menu.sound_volume") : "Sound Volume", skin);
        final Slider soundSlider = new Slider(0f, 1f, 0.1f, false, skin);
        soundSlider.setValue(GameSettings.getSoundVolume());

        contentTable.add(musicLabel).pad(10);
        contentTable.add(musicSlider).width(300).pad(10);
        contentTable.row();
        contentTable.add(soundLabel).pad(10);
        contentTable.add(soundSlider).width(300).pad(10);
        contentTable.row();

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
    }

    // Laden des "Tastenbelegung"-Tabs (Beispielhaft; hier kannst du später deine Konfiguration integrieren)
    private void loadKeyBindingsTab() {
        contentTable.clear();
        Label keyBindingsLabel = new Label(texts.has("menu.key_bindings") ? texts.getString("menu.key_bindings") : "Key Bindings", skin);
        // Hier könnte man später die Tastenbelegung zur Anpassung anbieten
        contentTable.add(keyBindingsLabel).pad(10);
        contentTable.row();
        Label info = new Label(texts.has("menu.key_bindings_info") ? texts.getString("menu.key_bindings_info") : "Tastenbelegung bearbeiten...", skin);
        contentTable.add(info).pad(10);
    }

    // Laden des "Video"-Tabs: VSync und evtl. weitere Videoeinstellungen
    private void loadVideoTab() {
        contentTable.clear();
        Label vsyncLabel = new Label(texts.has("menu.vsync") ? texts.getString("menu.vsync") : "VSync", skin);
        final CheckBox vsyncCheckbox = new CheckBox("", skin);
        vsyncCheckbox.setChecked(GameSettings.isVSync());

        contentTable.add(vsyncLabel).pad(10);
        contentTable.add(vsyncCheckbox).pad(10);
        contentTable.row();

        // Weitere Videoeinstellungen (z. B. Auflösung) könnten hier ergänzt werden

        vsyncCheckbox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                boolean vsync = vsyncCheckbox.isChecked();
                GameSettings.setVSync(vsync);
                Gdx.graphics.setVSync(vsync);
                System.out.println("VSync aktualisiert auf: " + vsync);
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
    public void pause() {  }

    @Override
    public void resume() {  }

    @Override
    public void hide() {  }

    @Override
    public void dispose() {
        backgroundTexture.dispose();
        stage.dispose();
    }
}
