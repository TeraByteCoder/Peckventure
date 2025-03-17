package at.peckventure;

import at.peckventure.server.GameServer;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;

public class ServerLauncher {
    public static void main(String[] args) {        System.out.println("Working Dir: " + System.getProperty("user.dir"));


        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        // Hier kannst du z. B. die Log-Level oder andere Optionen konfigurieren
        new HeadlessApplication(new ApplicationAdapter() {
            @Override
            public void create() {
                try {
                    GameServer server = new GameServer();
                    server.start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, config);
    }
}
