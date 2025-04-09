package at.peckventure;

import at.peckventure.server.GameServer;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ServerLauncher {
    private static final String DEFAULT_SERVER_FOLDER_NAME = "peckventure_server";

    public static void main(String[] args) {
        // Determine server folder
        String serverFolder = determineServerFolder(args);

        System.out.println("Server will use folder: " + serverFolder);
        System.out.println("Working Dir: " + System.getProperty("user.dir"));

        // Set the server folder as a system property so GameServer can access it
        System.setProperty("peckventure.server.folder", serverFolder);

        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    GameServer server = new GameServer();
                    OnStartCheck.checkOnStart();
                    server.start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, config);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            GameServer.instance.stopServer();
        }));
    }

    /**
     * Determines the server folder path
     * @param args Command-line arguments
     * @return Path to the server folder
     */
    private static String determineServerFolder(String[] args) {
        // Check if a server folder is provided as a command-line argument
        if (args.length > 0 && !args[0].trim().isEmpty()) {
            File providedFolder = new File(args[0]);
            if (providedFolder.exists() || providedFolder.mkdirs()) {
                return providedFolder.getAbsolutePath();
            }
        }

        // Default to roaming folder if no valid path provided
        return getDefaultServerFolder();
    }

    /**
     * Gets the default server folder in the user's roaming directory
     * @return Path to the default server folder
     */
    private static String getDefaultServerFolder() {
        String osName = System.getProperty("os.name").toLowerCase();
        Path roamingFolder;

        if (osName.contains("win")) {
            // Use %APPDATA% environment variable for Windows
            String appData = System.getenv("APPDATA");
            if (appData != null) {
                roamingFolder = Paths.get(appData, DEFAULT_SERVER_FOLDER_NAME);
            } else {
                // Fallback if %APPDATA% is not set
                roamingFolder = Paths.get(System.getProperty("user.home"), "AppData", "Roaming", DEFAULT_SERVER_FOLDER_NAME);
            }
        } else if (osName.contains("mac")) {
            // macOS: Library/Application Support
            roamingFolder = Paths.get(System.getProperty("user.home"), "Library", "Application Support", DEFAULT_SERVER_FOLDER_NAME);
        } else {
            // Linux/Unix: ~/.config
            roamingFolder = Paths.get(System.getProperty("user.home"), ".config", DEFAULT_SERVER_FOLDER_NAME);
        }

        // Create the folder if it doesn't exist
        File roamingFolderFile = roamingFolder.toFile();
        if (!roamingFolderFile.exists()) {
            roamingFolderFile.mkdirs();
        }

        return roamingFolderFile.getAbsolutePath();
    }
}
