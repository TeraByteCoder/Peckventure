package at.peckventure.menu;

import at.peckventure.multiplayer.Network;
import at.peckventure.multiplayer.NetworkPackets;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class LoadingScreen implements Screen {
    private final Game game;
    private final String serverAddress;
    private String serverHost;
    private int serverPort;

    private Stage stage;
    private BitmapFont font;
    private Texture backgroundTexture;
    private Image backgroundImage;

    private Label titleLabel;
    private Label statusLabel;
    private Label errorLabel;
    private TextButton retryButton;
    private TextButton backButton;
    private boolean connected = false;

    // Eigene Client-Instanz statt des zentralen NetworkClient
    private Client client;

    public LoadingScreen(Game game, String serverAddress) {
        this.game = game;
        this.serverAddress = serverAddress;
        // Parse der Serveradresse: host:port
        if (serverAddress.contains(":")) {
            int colonIndex = serverAddress.indexOf(":");
            serverHost = serverAddress.substring(0, colonIndex);
            try {
                serverPort = Integer.parseInt(serverAddress.substring(colonIndex + 1));
            } catch (NumberFormatException e) {
                serverPort = 4242;
            }
        } else {
            serverHost = serverAddress;
            serverPort = 4242;
        }
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        // Schriftart initialisieren
        font = new BitmapFont();
        font.getData().setScale(2f);

        // Hintergrund laden
        backgroundTexture = new Texture("textures/background/forest.png");
        backgroundImage = new Image(backgroundTexture);
        backgroundImage.setSize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        stage.addActor(backgroundImage);

        // UI-Elemente erstellen
        titleLabel = new Label("Connecting to Server", new Label.LabelStyle(font, Color.WHITE));
        titleLabel.setFontScale(3f);
        statusLabel = new Label("Connecting...", new Label.LabelStyle(font, Color.WHITE));
        statusLabel.setFontScale(2f);
        errorLabel = new Label("", new Label.LabelStyle(font, Color.RED));
        errorLabel.setFontScale(2f);
        errorLabel.setVisible(false);

        // Button-Styling
        Texture buttonTexture = new Texture("textures/gui/button1.png");
        TextureRegionDrawable buttonDrawable = new TextureRegionDrawable(new TextureRegion(buttonTexture));
        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.up = buttonDrawable;
        buttonStyle.down = buttonDrawable;
        buttonStyle.font = font;

        retryButton = new TextButton("Retry", buttonStyle);
        retryButton.setVisible(false);
        backButton = new TextButton("Back", buttonStyle);
        backButton.setVisible(false);

        // Listener für Retry
        retryButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                errorLabel.setVisible(false);
                retryButton.setVisible(false);
                backButton.setVisible(false);
                statusLabel.setText("Connecting...");
                startConnection();
            }
        });
        // Listener für Back: Zurück ins Hauptmenü
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new MainMenu(game));
            }
        });

        // Layout mit Table
        Table rootTable = new Table();
        rootTable.setFillParent(true);
        rootTable.center();
        rootTable.add(titleLabel).pad(20);
        rootTable.row();
        rootTable.add(statusLabel).pad(20);
        rootTable.row();
        rootTable.add(errorLabel).pad(20);
        rootTable.row();
        Table buttonTable = new Table();
        buttonTable.add(retryButton).pad(10);
        buttonTable.add(backButton).pad(10);
        rootTable.row();
        rootTable.add(buttonTable);
        stage.addActor(rootTable);

        // Asynchron den Verbindungsaufbau starten
        Gdx.app.postRunnable(() -> startConnection());
    }

    private void startConnection() {
        // Prüfen, ob der Server per Socket erreichbar ist
        if (!isServerOnline(serverHost, serverPort, 2000)) {
            showError("Server is offline!");
            return;
        }

        // Erstelle eine neue Client-Instanz
        client = new Client();
        // Klassen registrieren (z.B. NetworkPackets)
        Network.register(client.getKryo());

        // Füge einen Listener hinzu
        client.addListener(new Listener() {
            @Override
            public void connected(Connection connection) {
                if (!connected) {
                    connection.sendTCP(new NetworkPackets.PingRequestPacket());
                }
            }

            @Override
            public void received(Connection connection, Object object) {
                if (object instanceof NetworkPackets.PingPacket) {
                    if (!connected) {
                        Gdx.app.postRunnable(() -> {
                            connected = true;
                            // Wechsel zum MultiPlayerGameScreen nach erfolgreichem Ping
                            game.setScreen(new MultiPlayerGameScreen(game, serverAddress));
                        });
                    }
                }
            }

            @Override
            public void idle(Connection connection)
            {

            }

            @Override
            public void disconnected(Connection connection) {
                Gdx.app.postRunnable(() -> showError("Disconnected from server!"));
            }
        });

        // Starte den Client in einem separaten Thread
        new Thread(() -> {
            try {
                client.start();
                client.connect(5000, serverHost, serverPort, serverPort + 222);
            } catch (Exception e) {
                Gdx.app.postRunnable(() -> showError("Connection failed!"));
            }
        }).start();

        // Timer, um zu prüfen, ob die Verbindung innerhalb von 10 Sekunden hergestellt wird
        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                if (!isConnected()) {
                    showError("Connection timed out!");
                }
            }
        }, 10f);
    }

    private boolean isConnected() {
        return client != null && client.isConnected();
    }

    // Prüft per Socket, ob der Server erreichbar ist
    private boolean isServerOnline(String host, int port, int timeoutMillis) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMillis);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private void showError(String message) {
        Gdx.app.postRunnable(() -> {
            statusLabel.setText("Error");
            errorLabel.setText(message);
            errorLabel.setVisible(true);
            retryButton.setVisible(true);
            backButton.setVisible(true);
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
    public void pause() { }

    @Override
    public void resume() { }

    @Override
    public void hide() { }

    @Override
    public void dispose() {
        stage.dispose();
        backgroundTexture.dispose();
        font.dispose();
        if (client != null) {
            client.stop();
        }
    }
}
