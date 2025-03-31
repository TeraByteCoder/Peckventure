package at.peckventure.lwjgl3;

import at.peckventure.Main;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.Graphics.DisplayMode;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import java.io.File;

public class Lwjgl3Launcher {
    public static void main(String[] args) {
        if (StartupHelper.startNewJvmIfRequired()) return; // Unterstützt macOS und hilft auf Windows.
        createApplication();
    }

    private static Lwjgl3Application createApplication() {
        return new Lwjgl3Application(new Main(), getDefaultConfiguration());
    }

    private static Lwjgl3ApplicationConfiguration getDefaultConfiguration() {
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("Peckventure");
        configuration.useVsync(true);
        configuration.setForegroundFPS(Lwjgl3ApplicationConfiguration.getDisplayMode().refreshRate + 1);

        // Preferences-Verzeichnis einrichten (wird später von LibGDX genutzt)
        String appData = System.getenv("APPDATA");
        if (appData == null || appData.isEmpty()) {
            appData = System.getProperty("user.home");
        }
        File peckventureDir = new File(appData, "peckventure");
        if (!peckventureDir.exists()) {
            peckventureDir.mkdirs();
        }
        configuration.setPreferencesConfig(peckventureDir.getAbsolutePath(), Files.FileType.Absolute);

        // Auflösung festlegen: Verwende als Standard die aktuelle Monitorauflösung
        DisplayMode currentMode = Lwjgl3ApplicationConfiguration.getDisplayMode();
        int width = currentMode.width;
        int height = currentMode.height;
        configuration.setWindowedMode(width, height);

        // Fenster-Icons bleiben unverändert
        configuration.setWindowIcon("libgdx128.png", "libgdx64.png", "libgdx32.png", "libgdx16.png");
        return configuration;
    }
}
