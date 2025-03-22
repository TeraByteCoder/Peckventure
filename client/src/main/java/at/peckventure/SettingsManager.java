package at.peckventure;

import com.badlogic.gdx.files.FileHandle;

import java.util.UUID;

public class SettingsManager
{
    public static void init()
    {
        FileHandle settingsFile = Const.gameDir.child("settings.txt");

        // Falls die Datei nicht existiert oder leer ist, schreibe beide Einstellungen mit Standardwerten
        if (!settingsFile.exists() || settingsFile.readString().trim().isEmpty())
        {
            String uuid = UUID.randomUUID().toString();
            String username = "Herbert";
            settingsFile.writeString("uuid=" + uuid + "\nusername=" + username, false);
            Globals.uuid = uuid;
            Globals.username = username;
        } else
        {
            String content = settingsFile.readString();
            String uuid = null;
            String username = null;

            // Zerlege den Inhalt zeilenweise, um die einzelnen Einstellungen auszulesen
            String[] lines = content.split("\\r?\\n");
            for (String line : lines)
            {
                if (line.startsWith("uuid="))
                {
                    uuid = line.substring("uuid=".length()).trim();
                } else if (line.startsWith("username="))
                {
                    username = line.substring("username=".length()).trim();
                }
            }

            // Falls die UUID fehlt, generiere eine neue
            if (uuid == null || uuid.isEmpty())
            {
                uuid = UUID.randomUUID().toString();
            }
            // Falls der Username fehlt, setze den Standardwert "Herbert"
            if (username == null || username.isEmpty())
            {
                username = "Herbert";
            }

            // Schreibe die Einstellungen zurück in die Datei, um sie zu vereinheitlichen
            settingsFile.writeString("uuid=" + uuid + "\nusername=" + username, false);
            Globals.uuid = uuid;
            Globals.username = username;
        }
    }
}
