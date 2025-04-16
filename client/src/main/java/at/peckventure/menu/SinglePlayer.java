package at.peckventure.menu;

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
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;

import static at.peckventure.Const.savesDir;

public class SinglePlayer implements Screen {
    private final Game game;
    private Stage stage;
    private Texture backgroundTexture;
    private Image backgroundImage;
    private Label titleLabel;
    private Table worldTable;
    private Skin skin;

    public SinglePlayer(Game game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage();
        Gdx.input.setInputProcessor(stage);

        // FontManager und Skin verwenden
        FontManager fontManager = FontManager.getInstance();
        skin = fontManager.getSkin();

        // Hintergrund
        backgroundTexture = new Texture("textures/background/forest.png");
        backgroundImage = new Image(backgroundTexture);
        backgroundImage.setSize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        stage.addActor(backgroundImage);

        // Titel
        titleLabel = new Label(LanguageManager.INSTANCE.getText("menu.singleplayer"), skin);
        titleLabel.setFontScale(2f);
        titleLabel.setAlignment(Align.center);

        // Buttons mit Skin erstellen
        TextButton createWorldButton = new TextButton(LanguageManager.INSTANCE.getText("menu.create_world"), skin);
        TextButton backButton = new TextButton(LanguageManager.INSTANCE.getText("menu.back"), skin);

        // Welten-Liste
        worldTable = new Table();
        worldTable.top().pad(20);

        ScrollPane scrollPane = new ScrollPane(worldTable, skin);
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

    private void loadWorlds(FileHandle savesDir) {
        FileHandle[] worldDirs = savesDir.list();
        for (FileHandle world : worldDirs) {
            if (world.isDirectory()) {
                addWorldButton(world.name());
            }
        }
    }

    private void addWorldButton(final String worldName) {
        TextButton worldButton = new TextButton(worldName, skin);
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
        backgroundTexture.dispose();
        stage.dispose();
    }
}
