package at.peckventure.menu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

public class GameSettings {
    private static final String PREFS_NAME = "PeckventureSettings";
    private static final String MUSIC_VOLUME_KEY = "musicVolume";
    private static final String SOUND_VOLUME_KEY = "soundVolume";
    private static final String FULLSCREEN_KEY = "fullscreen";

    private static Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);

    public static float getMusicVolume() {
        return prefs.getFloat(MUSIC_VOLUME_KEY, 0.5f); // Standardwert: 0.5
    }

    public static void setMusicVolume(float volume) {
        prefs.putFloat(MUSIC_VOLUME_KEY, volume);
        prefs.flush();
    }

    public static float getSoundVolume() {
        return prefs.getFloat(SOUND_VOLUME_KEY, 0.5f); // Standardwert: 0.5
    }

    public static void setSoundVolume(float volume) {
        prefs.putFloat(SOUND_VOLUME_KEY, volume);
        prefs.flush();
    }

    public static boolean isFullscreen() {
        return prefs.getBoolean(FULLSCREEN_KEY, false); // Standardwert: false
    }

    public static void setFullscreen(boolean fullscreen) {
        prefs.putBoolean(FULLSCREEN_KEY, fullscreen);
        prefs.flush();
    }
}
