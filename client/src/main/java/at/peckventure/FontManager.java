package at.peckventure;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;

public class FontManager {
    private static FontManager instance;
    private Skin skin;

    private FontManager() {
        initializeFonts();
    }

    public static FontManager getInstance() {
        if (instance == null) {
            instance = new FontManager();
        }
        return instance;
    }

    private void initializeFonts() {
        // Lade die Basis-Skin
        skin = new Skin(Gdx.files.internal("ui/uiskin.json"));

        // Font mit FreeTypeFontGenerator erzeugen
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("ui/unifont-16.0.02.otf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();

        // Erzeuge verschiedene Fontgrößen für verschiedene Zwecke
        parameter.size = 16;
        parameter.characters = FreeTypeFontGenerator.DEFAULT_CHARS + "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯабвгдеёжзийклмнопрстуфхцчшщъыьэюя";
        BitmapFont regularFont = generator.generateFont(parameter);

        parameter.size = 24;
        BitmapFont titleFont = generator.generateFont(parameter);

        // Setze die Fonts in der Skin
        skin.add("default", regularFont, BitmapFont.class);
        skin.add("font", regularFont, BitmapFont.class);
        skin.add("subtitle", titleFont, BitmapFont.class);
        skin.add("window", titleFont, BitmapFont.class);
        skin.add("list", regularFont, BitmapFont.class);

        // Aktualisiere die wichtigsten UI-Stile
        updateStyles();

        generator.dispose();
    }

    private void updateStyles() {
        // Aktualisiere alle wichtigen Stile in der Skin
        BitmapFont defaultFont = skin.get("default", BitmapFont.class);
        BitmapFont titleFont = skin.get("subtitle", BitmapFont.class);

        // Button-Stil aktualisieren
        TextButton.TextButtonStyle textButtonStyle = skin.get("default", TextButton.TextButtonStyle.class);
        textButtonStyle.font = defaultFont;

        // Label-Stil aktualisieren
        Label.LabelStyle labelStyle = skin.get("default", Label.LabelStyle.class);
        labelStyle.font = defaultFont;

        // Weitere Stile hier aktualisieren...
    }

    public Skin getSkin() {
        return skin;
    }

    public void dispose() {
        if (skin != null) {
            skin.dispose();
        }
    }
}
