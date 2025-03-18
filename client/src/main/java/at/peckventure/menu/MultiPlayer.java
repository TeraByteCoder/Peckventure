package at.peckventure.menu;

import at.peckventure.Const;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.utils.Align;

public class MultiPlayer implements Screen {
    private final Game game;
    private Stage stage;
    private Texture backgroundTexture;
    private Image backgroundImage;
    private Label titleLabel;
    private Table serverTable;
    private BitmapFont font;
    private FileHandle serverDataFile;

    public MultiPlayer(Game game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage();
        Gdx.input.setInputProcessor(stage);
        font = new BitmapFont();
        backgroundTexture = new Texture("textures/background/forest.png");
        backgroundImage = new Image(backgroundTexture);
        backgroundImage.setSize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        stage.addActor(backgroundImage);
        Label.LabelStyle titleStyle = new Label.LabelStyle();
        titleStyle.font = font;
        titleLabel = new Label("Multiplayer", titleStyle);
        titleLabel.setFontScale(2f);
        titleLabel.setAlignment(Align.center);
        Texture buttonTexture = new Texture("textures/gui/button1.png");
        TextureRegionDrawable buttonDrawable = new TextureRegionDrawable(buttonTexture);
        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.up = buttonDrawable;
        buttonStyle.down = buttonDrawable;
        buttonStyle.font = font;
        TextButton addServerButton = new TextButton("Add Server", buttonStyle);
        TextButton backButton = new TextButton("Back", buttonStyle);
        serverTable = new Table();
        serverTable.top().pad(20);
        ScrollPane scrollPane = new ScrollPane(serverTable);
        scrollPane.setScrollingDisabled(true, false);
        serverDataFile = Const.gameDir.child("serverdata.txt");
        if (!serverDataFile.exists()) {
            serverDataFile.writeString("", false);
        }
        loadServers();
        Table rootTable = new Table();
        rootTable.setFillParent(true);
        rootTable.top();
        rootTable.add(titleLabel).padTop(50).expandX().center();
        rootTable.row();
        rootTable.add(scrollPane).expand().fill().pad(20);
        rootTable.row();
        Table buttonTable = new Table();
        buttonTable.add(addServerButton).size(300, 80).pad(10);
        buttonTable.add(backButton).size(300, 80).pad(10);
        rootTable.add(buttonTable).padBottom(10).expandX().bottom();
        stage.addActor(rootTable);
        addServerButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new AddServerScreen(game));
            }
        });
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new MainMenu(game));
            }
        });
    }

    private void loadServers() {
        String data = serverDataFile.readString();
        String[] lines = data.split("\n");
        for (String line : lines) {
            if (line.trim().isEmpty()) continue;
            String[] parts = line.split(";");
            if (parts.length >= 2) {
                addServerButton(parts[0], parts[1]);
            }
        }
    }

    private void addServerButton(final String serverName, final String serverAddress) {
        Texture buttonTexture = new Texture("textures/gui/button1.png");
        TextureRegionDrawable buttonDrawable = new TextureRegionDrawable(buttonTexture);
        TextButton.TextButtonStyle serverButtonStyle = new TextButton.TextButtonStyle();
        serverButtonStyle.up = buttonDrawable;
        serverButtonStyle.down = buttonDrawable;
        serverButtonStyle.font = font;
        TextButton serverButton = new TextButton(serverName + " (" + serverAddress + ")", serverButtonStyle);
        serverButton.getLabel().setFontScale(1.5f);
        serverButton.pad(10);
        serverButton.setSize(300, 60);
        serverButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new MultiPlayerGameScreen(game, serverName, serverAddress));
            }
        });
        serverTable.add(serverButton).size(400, 80).padBottom(10).row();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {}

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
        font.dispose();
    }
}
