package at.peckventure.menu;

import at.peckventure.Const;
import at.peckventure.FontManager;
import at.peckventure.LanguageManager;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;

public class AddServerScreen implements Screen {
    private final Game game;
    private Stage stage;
    private Texture backgroundTexture;
    private Image backgroundImage;
    private Skin skin;
    private FileHandle serverDataFile;

    public AddServerScreen(Game game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage();
        Gdx.input.setInputProcessor(stage);

        // FontManager und Skin verwenden
        FontManager fontManager = FontManager.getInstance();
        skin = fontManager.getSkin();

        backgroundTexture = new Texture("textures/background/forest.png");
        backgroundImage = new Image(backgroundTexture);
        backgroundImage.setSize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        stage.addActor(backgroundImage);

        // Titel mit Übersetzung
        Label titleLabel = new Label(LanguageManager.INSTANCE.getText("menu.title"), skin);
        titleLabel.setFontScale(3f);
        titleLabel.setAlignment(Align.center);

        // Übersetzte Buttons
        String addButtonText = LanguageManager.INSTANCE.getText("menu.add_server");
        String backButtonText = LanguageManager.INSTANCE.getText("menu.back");
        TextButton addButton = new TextButton(addButtonText, skin);
        TextButton backButton = new TextButton(backButtonText, skin);

        // Textfelder mit Platzhaltern
        String serverNamePlaceholder = LanguageManager.INSTANCE.getText("menu.server_name");
        String addressPlaceholder = LanguageManager.INSTANCE.getText("menu.address");
        final TextField nameInput = new TextField(serverNamePlaceholder, skin);
        final TextField addressInput = new TextField(addressPlaceholder, skin);

        // Labels mit Übersetzungen
        String serverNameLabelText = LanguageManager.INSTANCE.getText("menu.server_name_label");
        String addressLabelText = LanguageManager.INSTANCE.getText("menu.address");
        Label nameLabel = new Label(serverNameLabelText, skin);
        Label addressLabel = new Label(addressLabelText, skin);

        // Layout
        Table inputTable = new Table();
        inputTable.center();
        inputTable.add(nameLabel).pad(10).center();
        inputTable.add(nameInput).width(600).height(80).pad(10).center();
        inputTable.row();
        inputTable.add(addressLabel).pad(10).center();
        inputTable.add(addressInput).width(600).height(80).pad(10).center();

        Table buttonTable = new Table();
        buttonTable.add(addButton).size(400, 120).pad(10);
        buttonTable.add(backButton).size(400, 120).pad(10);

        Table rootTable = new Table();
        rootTable.setFillParent(true);
        rootTable.center();
        rootTable.add(titleLabel).expandX().center().padTop(50);
        rootTable.row();
        rootTable.add(inputTable).expand().center().pad(20);
        rootTable.row();
        rootTable.add(buttonTable).expandX().center().padBottom(10);

        stage.addActor(rootTable);

        serverDataFile = Const.gameDir.child("serverdata.txt");
        if (!serverDataFile.exists()) {
            serverDataFile.writeString("", false);
        }

        // Button Listener
        addButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                String serverName = nameInput.getText();
                if (serverName == null || serverName.trim().isEmpty()) {
                    serverName = "Server";
                }
                String address = addressInput.getText();
                if (address == null || address.trim().isEmpty()) {
                    address = "127.0.0.1:25565";
                }
                String entry = serverName + ";" + address + "\n";
                serverDataFile.writeString(entry, true);
                game.setScreen(new MultiPlayer(game));
            }
        });

        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new MultiPlayer(game));
            }
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
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}

    @Override
    public void dispose() {
        backgroundTexture.dispose();
        stage.dispose();
        if (skin != null) skin.dispose();
    }
}
