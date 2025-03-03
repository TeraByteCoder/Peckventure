package at.peckventure.menu;

import at.peckventure.world.GameScreen;
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

import static at.peckventure.Const.savesDir;

public class SinglePlayer implements Screen
{
    private final Game game;
    private Stage stage;
    private Texture backgroundTexture;
    private Image backgroundImage;
    private Label titleLabel;
    private Table worldTable; // Enthält die Welt-Buttons
    private BitmapFont font; // Schriftart für Buttons

    public SinglePlayer(Game game)
    {
        this.game = game;
    }

    @Override
    public void show()
    {
        stage = new Stage();
        Gdx.input.setInputProcessor(stage);

        // 📌 Standard-Schriftart erstellen
        font = new BitmapFont();

        // 🎨 Hintergrundbild
        backgroundTexture = new Texture("textures/background/forest.png");
        backgroundImage = new Image(backgroundTexture);
        backgroundImage.setSize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        stage.addActor(backgroundImage);

        // 📌 Titel oben
        Label.LabelStyle titleStyle = new Label.LabelStyle();
        titleStyle.font = font;
        titleLabel = new Label("Singleplayer", titleStyle);
        titleLabel.setFontScale(2f);
        titleLabel.setAlignment(Align.center);

        // 🔘 Button-Stil mit eigener Textur
        Texture buttonTexture = new Texture("textures/gui/button1.png");
        TextureRegionDrawable buttonDrawable = new TextureRegionDrawable(buttonTexture);
        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.up = buttonDrawable;
        buttonStyle.down = buttonDrawable;
        buttonStyle.font = font; // ✅ Jetzt wird die Schriftart gesetzt

        TextButton createWorldButton = new TextButton("Create World", buttonStyle);
        TextButton backButton = new TextButton("Back", buttonStyle);

        // 📌 Scrollbare Liste für Welten
        worldTable = new Table();
        worldTable.top().pad(20); // 20 Pixel Padding oben & unten

        ScrollPane scrollPane = new ScrollPane(worldTable);
        scrollPane.setScrollingDisabled(true, false); // Horizontal deaktiviert, vertikal aktiv

        loadWorlds(savesDir); // Lade alle vorhandenen Welten
        // 📌 Speicherpfad ausgeben
        System.out.println("Welten werden gespeichert unter: " + savesDir.file().getAbsolutePath());

        // 📌 Hauptlayout
        Table rootTable = new Table();
        rootTable.setFillParent(true);

        // Titel hinzufügen
        rootTable.top();
        rootTable.add(titleLabel).padTop(50).expandX().center();
        rootTable.row();

        // ScrollPane mit Welten
        rootTable.add(scrollPane).expand().fill().pad(20);
        rootTable.row();

        // 📌 Buttons unten nebeneinander setzen
        Table buttonTable = new Table();
        buttonTable.add(createWorldButton).size(300, 80).pad(10);
        buttonTable.add(backButton).size(300, 80).pad(10);

        rootTable.add(buttonTable).padBottom(10).expandX().bottom();

        stage.addActor(rootTable);

        // Button-Events
        createWorldButton.addListener(new ClickListener()
        {
            @Override
            public void clicked(InputEvent event, float x, float y)
            {
                game.setScreen(new CreateWorld(game));
            }
        });

        backButton.addListener(new ClickListener()
        {
            @Override
            public void clicked(InputEvent event, float x, float y)
            {
                game.setScreen(new MainMenu(game));
            }
        });
    }

    // 📌 Methode zum Laden der Welten
    private void loadWorlds(FileHandle savesDir)
    {
        FileHandle[] worldDirs = savesDir.list();
        for (FileHandle world : worldDirs)
        {
            if (world.isDirectory())
            { // Nur Verzeichnisse als Welten anzeigen
                addWorldButton(world.name());
            }
        }
    }

    // 📌 Fügt einen Button für eine gespeicherte Welt hinzu
    private void addWorldButton(final String worldName)
    {
        // Verwende denselben Button-Stil wie "Create World" & "Back"
        Texture buttonTexture = new Texture("textures/gui/button1.png");
        TextureRegionDrawable buttonDrawable = new TextureRegionDrawable(buttonTexture);

        TextButton.TextButtonStyle worldButtonStyle = new TextButton.TextButtonStyle();
        worldButtonStyle.up = buttonDrawable;
        worldButtonStyle.down = buttonDrawable;
        worldButtonStyle.font = font; // ✅ Jetzt hat er dieselbe Schriftart

        // Welt-Button erstellen
        TextButton worldButton = new TextButton(worldName, worldButtonStyle);
        worldButton.getLabel().setFontScale(1.5f);
        worldButton.pad(10);
        worldButton.setSize(300, 60);

        // Klick-Event für Welt-Button
        worldButton.addListener(new ClickListener()
        {
            @Override
            public void clicked(InputEvent event, float x, float y)
            {
                game.setScreen(new GameScreen(game, worldName)); // Wechselt zur Spielwelt
            }
        });


        // Button zur Scroll-Tabelle hinzufügen
        worldTable.add(worldButton).size(400, 80).padBottom(10).row();
    }


    @Override
    public void render(float delta)
    {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int i, int i1)
    {

    }

    @Override
    public void pause()
    {

    }

    @Override
    public void resume()
    {

    }

    @Override
    public void hide()
    {

    }

    @Override
    public void dispose()
    {
        backgroundTexture.dispose();
        stage.dispose();
        font.dispose(); // ✅ Speicher aufräumen
    }
}
