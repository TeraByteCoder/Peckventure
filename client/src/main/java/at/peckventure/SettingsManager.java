package at.peckventure;

import com.badlogic.gdx.files.FileHandle;
import java.util.UUID;

public class SettingsManager {
    public static void init() {
        FileHandle settingsFile = Const.gameDir.child("settings.txt");
        if (!settingsFile.exists() || settingsFile.readString().trim().isEmpty()) {
            String uuid = UUID.randomUUID().toString();
            settingsFile.writeString("uuid=" + uuid, false);
            Globals.uuid = uuid;
        } else {
            String content = settingsFile.readString();
            String[] parts = content.split("=");
            if (parts.length >= 2) {
                Globals.uuid = parts[1].trim();
            } else {
                String uuid = UUID.randomUUID().toString();
                settingsFile.writeString("uuid=" + uuid, false);
                Globals.uuid = uuid;
            }
        }
    }
}
