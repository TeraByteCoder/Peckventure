package at.peckventure.menu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

public class GameSettings {
    private static final String PREFS_NAME = "PeckventureSettings.txt";
    private static Preferences prefs = null;

    private static Preferences getPrefs() {
        if (prefs == null && Gdx.app != null) {
            prefs = Gdx.app.getPreferences(PREFS_NAME);
            // Standardwerte setzen, falls sie noch nicht existieren:
            if (!prefs.contains("musicVolume")) {
                prefs.putFloat("musicVolume", 0.5f);
            }
            if (!prefs.contains("soundVolume")) {
                prefs.putFloat("soundVolume", 0.5f);
            }
            if (!prefs.contains("fullscreen")) {
                prefs.putBoolean("fullscreen", false);
            }
            if (!prefs.contains("vsync")) {
                prefs.putBoolean("vsync", true);
            }
            if (!prefs.contains("resolution")) {
                prefs.putString("resolution", "640x480");
            }
            // Standard-Sprache (z.B. en_us als Default, kann auch de_de o.ä. sein)
            if (!prefs.contains("language")) {
                prefs.putString("language", "en_us");
            }
            prefs.flush();
        }
        return prefs;
    }

    public static float getMusicVolume() {
        Preferences p = getPrefs();
        return (p != null) ? p.getFloat("musicVolume", 0.5f) : 0.5f;
    }

    public static void setMusicVolume(float volume) {
        Preferences p = getPrefs();
        if (p != null) {
            p.putFloat("musicVolume", volume);
            p.flush();
        }
    }

    public static float getSoundVolume() {
        Preferences p = getPrefs();
        return (p != null) ? p.getFloat("soundVolume", 0.5f) : 0.5f;
    }

    public static void setSoundVolume(float volume) {
        Preferences p = getPrefs();
        if (p != null) {
            p.putFloat("soundVolume", volume);
            p.flush();
        }
    }

    public static boolean isFullscreen() {
        Preferences p = getPrefs();
        return (p != null) ? p.getBoolean("fullscreen", false) : false;
    }

    public static void setFullscreen(boolean fullscreen) {
        Preferences p = getPrefs();
        if (p != null) {
            p.putBoolean("fullscreen", fullscreen);
            p.flush();
        }
    }

    public static boolean isVSync() {
        Preferences p = getPrefs();
        return (p != null) ? p.getBoolean("vsync", true) : true;
    }

    public static void setVSync(boolean vsync) {
        Preferences p = getPrefs();
        if (p != null) {
            p.putBoolean("vsync", vsync);
            p.flush();
        }
    }

    public static String getResolution() {
        Preferences p = getPrefs();
        return (p != null) ? p.getString("resolution", "640x480") : "640x480";
    }

    public static void setResolution(String resolution) {
        Preferences p = getPrefs();
        if (p != null) {
            p.putString("resolution", resolution);
            p.flush();
        }
    }

    // Spracheinstellungen
    public static String getLanguage() {
        Preferences p = getPrefs();
        return (p != null) ? p.getString("language", "en_us") : "en_us";
    }

    public static void setLanguage(String language) {
        Preferences p = getPrefs();
        if (p != null) {
            p.putString("language", language);
            p.flush();
        }
    }
}
