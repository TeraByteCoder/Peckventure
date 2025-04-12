package at.peckventure.menu;

import at.peckventure.NetworkClient;
import at.peckventure.Globals;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public class MultiplayerEscapeMenu {
    private boolean visible = false;
    private Window menuWindow;
    private final Game game;
    private final MultiPlayerGameScreen gameScreen;
    private Skin skin;

    public MultiplayerEscapeMenu(Game game, MultiPlayerGameScreen gameScreen, Stage uiStage) {
        this.game = game;
        this.gameScreen = gameScreen;

        // Skin für UI-Elemente laden
        skin = new Skin();
        skin.addRegions(new TextureAtlas(Gdx.files.internal("ui/uiskin.atlas")));
        //skin.add("default-font", new BitmapFont(Gdx.files.internal("ui/font.fnt")));
        skin.load(Gdx.files.internal("ui/uiskin.json"));

        // Menüfenster erstellen und konfigurieren
        createMenuWindow(uiStage);
    }

    private void createMenuWindow(Stage uiStage) {
        menuWindow = new Window("Menu", skin);
        menuWindow.setMovable(false);

        // Fenstergröße festlegen
        float menuWidth = 200;
        float menuHeight = 250;
        menuWindow.setSize(menuWidth, menuHeight);
        // Das Menü mittig positionieren
        menuWindow.setPosition(
            (Gdx.graphics.getWidth() - menuWidth) / 2,
            (Gdx.graphics.getHeight() - menuHeight) / 2
        );

        // Table für das Layout der Elemente erstellen
        Table contentTable = new Table();
        contentTable.pad(10);
        contentTable.defaults().fillX().space(8).center();

        // Server-Info hinzufügen
        Label serverInfoLabel = new Label("Server: " + gameScreen.getServerHost(), skin);
        contentTable.add(serverInfoLabel);
        contentTable.row();

        // Spieler-Info hinzufügen
        Label playerInfoLabel = new Label("Player: " + Globals.username, skin);
        contentTable.add(playerInfoLabel).padBottom(15);
        contentTable.row();

        // Buttons erstellen
        TextButton resumeButton = new TextButton("Continue", skin);
        TextButton disconnectButton = new TextButton("Disconnect", skin);
        TextButton quitButton = new TextButton("Quit Game", skin);

        contentTable.add(resumeButton).height(40);
        contentTable.row();
        contentTable.add(disconnectButton).height(40);
        contentTable.row();
        contentTable.add(quitButton).height(40);

        // Button-Listener einrichten
        resumeButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                toggleMenu();
            }
        });

        disconnectButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // Bestätigungsdialog anzeigen
                Dialog dialog = new Dialog("Confirm", skin) {
                    @Override
                    protected void result(Object object) {
                        if ((Boolean) object) {
                            // Bei Bestätigung trennen und zurück zum Multiplayer-Menü wechseln
                            NetworkClient.getInstance().close();
                            game.setScreen(new MultiPlayer(game));
                        }
                    }
                };
                dialog.text("Disconnect from server?");
                dialog.button("Yes", true);
                dialog.button("No", false);
                dialog.show(uiStage);
            }
        });

        quitButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // Bestätigungsdialog anzeigen
                Dialog dialog = new Dialog("Confirm", skin) {
                    @Override
                    protected void result(Object object) {
                        if ((Boolean) object) {
                            // Bei Bestätigung trennen und das Spiel beenden
                            NetworkClient.getInstance().close();
                            Gdx.app.exit();
                        }
                    }
                };
                dialog.text("Quit game?");
                dialog.button("Yes", true);
                dialog.button("No", false);
                dialog.show(uiStage);
            }
        });

        menuWindow.add(contentTable).expand().fill();
        menuWindow.setVisible(false);
        uiStage.addActor(menuWindow);
    }

    public void toggleMenu() {
        visible = !visible;
        menuWindow.setVisible(visible);
    }

    public boolean isVisible() {
        return visible;
    }

    public void resize(int width, int height) {
        float menuWidth = 200;
        float menuHeight = 250;
        // Beim Resizen ebenfalls zentriert positionieren
        menuWindow.setPosition(
            (width - menuWidth) / 2,
            (height - menuHeight) / 2
        );
    }
}
