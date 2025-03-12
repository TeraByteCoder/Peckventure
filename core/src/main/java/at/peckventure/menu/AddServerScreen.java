package at.peckventure.menu;

import at.peckventure.Const;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.utils.Align;

public class AddServerScreen implements Screen {
    private final Game game;
    private Stage stage;
    private Texture backgroundTexture;
    private Image backgroundImage;
    private BitmapFont font;
    private Texture textFieldTexture;
    private FileHandle serverDataFile;

    public AddServerScreen(Game game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage();
        Gdx.input.setInputProcessor(stage);
        font = new BitmapFont();
        font.getData().setScale(2f);
        backgroundTexture = new Texture("textures/background/forest.png");
        backgroundImage = new Image(backgroundTexture);
        backgroundImage.setSize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        stage.addActor(backgroundImage);
        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = font;
        labelStyle.fontColor = Color.WHITE;
        Label titleLabel = new Label("Add Server", labelStyle);
        titleLabel.setFontScale(3f);
        titleLabel.setAlignment(Align.center);
        Texture buttonTexture = new Texture("textures/gui/button1.png");
        TextureRegionDrawable buttonDrawable = new TextureRegionDrawable(buttonTexture);
        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.up = buttonDrawable;
        buttonStyle.down = buttonDrawable;
        buttonStyle.font = font;
        TextButton addButton = new TextButton("Add Server", buttonStyle);
        TextButton backButton = new TextButton("Back", buttonStyle);
        TextField.TextFieldStyle textFieldStyle = new TextField.TextFieldStyle();
        textFieldStyle.font = font;
        textFieldStyle.fontColor = Color.WHITE;
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.DARK_GRAY);
        pixmap.fill();
        textFieldTexture = new Texture(pixmap);
        pixmap.dispose();
        textFieldStyle.background = new TextureRegionDrawable(new TextureRegion(textFieldTexture));
        final TextField nameInput = new TextField("Server Name", textFieldStyle);
        final TextField addressInput = new TextField("ip:port", textFieldStyle);
        Label nameLabel = new Label("Server Name:", labelStyle);
        Label addressLabel = new Label("Address:", labelStyle);
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
        if (textFieldTexture != null)
            textFieldTexture.dispose();
    }
}
