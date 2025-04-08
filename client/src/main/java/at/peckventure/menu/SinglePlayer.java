package at.peckventure.menu;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import static at.peckventure.Const.savesDir;

public class SinglePlayer implements Screen {
    private final Game game;
    private Stage stage;
    private Texture backgroundTexture;
    private Image backgroundImage;
    private Label titleLabel;
    private Table worldTable;
    private BitmapFont font;
    private JsonValue texts;

    public SinglePlayer(Game game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage();
        Gdx.input.setInputProcessor(stage);

        // Schriftart
        font = new BitmapFont();

        // Hintergrund
        backgroundTexture = new Texture("textures/background/forest.png");
        backgroundImage = new Image(backgroundTexture);
        backgroundImage.setSize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        stage.addActor(backgroundImage);

        // Sprachdatei laden
        String langCode = GameSettings.getLanguage();
        texts = new JsonReader().parse(Gdx.files.internal("lang/" + langCode + ".json"));

        // Titel
        Label.LabelStyle titleStyle = new Label.LabelStyle();
        titleStyle.font = font;
        titleLabel = new Label(getText("menu.singleplayer", "Singleplayer"), titleStyle);
        titleLabel.setFontScale(2f);
        titleLabel.setAlignment(Align.center);

        // Button-Stil
        Texture buttonTexture = new Texture("textures/gui/button1.png");
        TextureRegionDrawable buttonDrawable = new TextureRegionDrawable(buttonTexture);
        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.up = buttonDrawable;
        buttonStyle.down = buttonDrawable;
        buttonStyle.font = font;

        // Buttons
        TextButton createWorldButton = new TextButton(getText("menu.create_world", "Create World"), buttonStyle);
        TextButton backButton = new TextButton(getText("menu.back", "Back"), buttonStyle);

        // Welten-Liste
        worldTable = new Table();
        worldTable.top().pad(20);

        ScrollPane scrollPane = new ScrollPane(worldTable);
        scrollPane.setScrollingDisabled(true, false);

        loadWorlds(savesDir);

        // Layout
        Table rootTable = new Table();
        rootTable.setFillParent(true);

        rootTable.top();
        rootTable.add(titleLabel).padTop(50).expandX().center();
        rootTable.row();
        rootTable.add(scrollPane).expand().fill().pad(20);
        rootTable.row();

        Table buttonTable = new Table();
        buttonTable.add(createWorldButton).size(300, 80).pad(10);
        buttonTable.add(backButton).size(300, 80).pad(10);

        rootTable.add(buttonTable).padBottom(10).expandX().bottom();

        stage.addActor(rootTable);

        // Button Events
        createWorldButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new CreateWorld(game));
            }
        });

        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new MainMenu(game));
            }
        });
    }

    private String getText(String key, String fallback) {
        return texts.has(key) ? texts.getString(key) : fallback;
    }

    private void loadWorlds(FileHandle savesDir) {
        FileHandle[] worldDirs = savesDir.list();
        for (FileHandle world : worldDirs) {
            if (world.isDirectory()) {
                addWorldButton(world.name());
            }
        }
    }

    private void addWorldButton(final String worldName) {
        Texture buttonTexture = new Texture("textures/gui/button1.png");
        TextureRegionDrawable buttonDrawable = new TextureRegionDrawable(buttonTexture);

        TextButton.TextButtonStyle worldButtonStyle = new TextButton.TextButtonStyle();
        worldButtonStyle.up = buttonDrawable;
        worldButtonStyle.down = buttonDrawable;
        worldButtonStyle.font = font;

        TextButton worldButton = new TextButton(worldName, worldButtonStyle);
        worldButton.getLabel().setFontScale(1.5f);
        worldButton.pad(10);
        worldButton.setSize(300, 60);

        worldButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new SinglePlayerGameScreen(game, worldName));
            }
        });

        worldTable.add(worldButton).size(400, 80).padBottom(10).row();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int i, int i1) { }

    @Override
    public void pause() { }

    @Override
    public void resume() { }

    @Override
    public void hide() { }

    @Override
    public void dispose() {
        backgroundTexture.dispose();
        stage.dispose();
        font.dispose();
    }
}
