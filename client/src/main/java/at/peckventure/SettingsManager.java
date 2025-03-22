package at.peckventure;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.files.FileHandle;

import java.util.UUID;

public class SettingsManager {
    /**
     * Prüft, ob die Einstellungen (insbesondere ein gesetzter Username) vorhanden sind.
     * Wenn ja, werden die Globals gesetzt und true zurückgegeben.
     * Fehlen die Daten, wird false zurückgegeben.
     */
    public static boolean init() {
        FileHandle settingsFile = Const.gameDir.child("settings.txt");

        if (!settingsFile.exists() || settingsFile.readString().trim().isEmpty()) {
            return false;
        } else {
            String content = settingsFile.readString();
            String uuid = null;
            String username = null;
            String[] lines = content.split("\\r?\\n");
            for (String line : lines) {
                if (line.startsWith("uuid=")) {
                    uuid = line.substring("uuid=".length()).trim();
                } else if (line.startsWith("username=")) {
                    username = line.substring("username=".length()).trim();
                }
            }
            // Wenn einer der Werte fehlt, wird der FirstStartScreen benötigt
            if (uuid == null || uuid.isEmpty() || username == null || username.isEmpty()) {
                return false;
            }
            Globals.uuid = uuid;
            Globals.username = username;
            return true;
        }
    }
}
