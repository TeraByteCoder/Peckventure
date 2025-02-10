package at.peckventure.menu;

import at.peckventure.world.GameScreen;
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
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;

import static at.peckventure.Const.savesDir;

public class CreateWorld implements Screen {
    private Game game;
    private Stage stage;
    private Texture backgroundTexture;
    private Image backgroundImage;
    private Label titleLabel;
    private Table worldTable; // Enthält die Welt-Buttons (z. B. für eine Liste bereits vorhandener Welten)
    private BitmapFont font;  // Schriftart für Buttons und Labels
    // Texture für den Hintergrund der Textfelder (wird im dispose() aufgeräumt)
    private Texture textFieldTexture;

    public CreateWorld(Game game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage();
        Gdx.input.setInputProcessor(stage);

        // Standard-Schriftart
        font = new BitmapFont();

        // Hintergrundbild
        backgroundTexture = new Texture("textures/background/forest.png");
        backgroundImage = new Image(backgroundTexture);
        backgroundImage.setSize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        stage.addActor(backgroundImage);

        // Titel oben
        Label.LabelStyle titleStyle = new Label.LabelStyle();
        titleStyle.font = font;
        titleLabel = new Label("Create World", titleStyle);
        titleLabel.setFontScale(2f);
        titleLabel.setAlignment(Align.center);

        // Button-Stil mit eigener Textur
        Texture buttonTexture = new Texture("textures/gui/button1.png");
        TextureRegionDrawable buttonDrawable = new TextureRegionDrawable(buttonTexture);
        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.up = buttonDrawable;
        buttonStyle.down = buttonDrawable;
        buttonStyle.font = font; // Schriftart setzen

        // Buttons
        TextButton createWorldButton = new TextButton("Create World", buttonStyle);
        TextButton backButton = new TextButton("Back", buttonStyle);

        // Scrollbare Liste für Welten (optional; kann z. B. vorhandene Welten anzeigen)
        worldTable = new Table();
        worldTable.top().pad(20); // 20 Pixel Padding oben & unten

        ScrollPane scrollPane = new ScrollPane(worldTable);
        scrollPane.setScrollingDisabled(true, false); // Horizontal deaktiviert, vertikal aktiv

        // ───────────────────────────────────────────────
        // Textfelder für Eingaben: Weltname und Seed
        // Erstelle zunächst einen einfachen TextField-Stil
        TextField.TextFieldStyle textFieldStyle = new TextField.TextFieldStyle();
        textFieldStyle.font = font;
        textFieldStyle.fontColor = Color.WHITE;
        // Erzeuge einen einfachen Hintergrund (einfarbig)
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.DARK_GRAY);
        pixmap.fill();
        textFieldTexture = new Texture(pixmap);
        pixmap.dispose();
        textFieldStyle.background = new TextureRegionDrawable(new TextureRegion(textFieldTexture));

        // Erstelle die Eingabefelder
        final TextField worldNameInput = new TextField("", textFieldStyle);
        final TextField seedInput = new TextField("", textFieldStyle);

        // Labels für die Eingabefelder
        Label worldNameLabel = new Label("World Name:", titleStyle);
        Label seedLabel = new Label("Seed:", titleStyle);

        // Tabelle für die Eingabefelder
        Table inputTable = new Table();
        inputTable.add(worldNameLabel).pad(5);
        inputTable.add(worldNameInput).width(300).pad(5);
        inputTable.row();
        inputTable.add(seedLabel).pad(5);
        inputTable.add(seedInput).width(300).pad(5);
        // ───────────────────────────────────────────────

        // Hauptlayout (Root Table)
        Table rootTable = new Table();
        rootTable.setFillParent(true);
        rootTable.top();

        // Aufbau der Tabelle:
        // 1. Titel oben
        rootTable.add(titleLabel).padTop(50).expandX().center();
        rootTable.row();
        // 2. ScrollPane (z. B. Liste vorhandener Welten)
        rootTable.add(scrollPane).expand().fill().pad(20);
        rootTable.row();
        // 3. Eingabefelder für Weltname und Seed
        rootTable.add(inputTable).pad(20);
        rootTable.row();
        // 4. Buttons unten
        Table buttonTable = new Table();
        buttonTable.add(createWorldButton).size(300, 80).pad(10);
        buttonTable.add(backButton).size(300, 80).pad(10);
        rootTable.add(buttonTable).padBottom(10).expandX().bottom();

        stage.addActor(rootTable);

        // ───────────────────────────────────────────────
        // Button-Events

        // "Create World"-Button: Liest die Eingaben aus und erstellt den Welt-Ordner samt Konfigurationsdatei
        createWorldButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // Lese Eingaben
                String worldName = worldNameInput.getText();
                String seedText = seedInput.getText();
                // Falls kein Weltname eingegeben wurde, verwende einen Standardnamen
                if (worldName == null || worldName.trim().isEmpty()) {
                    worldName = "World_" + System.currentTimeMillis();
                }
                long seed;
                try {
                    seed = Long.parseLong(seedText);
                } catch (NumberFormatException e) {
                    // Falls der Seed nicht korrekt eingegeben wurde, verwende z. B. die aktuelle Zeit als Seed
                    seed = System.currentTimeMillis();
                }

                // Erstelle einen neuen Ordner im Verzeichnis saves (verwende dabei den in Const.savesDir definierten Pfad)
                FileHandle newWorldDir = Gdx.files.absolute(savesDir + "/" + worldName);

                if (!newWorldDir.exists()) {
                    newWorldDir.mkdirs();
                }
                // Erstelle eine Weltkonfigurationsdatei, in der der Seed gespeichert wird
                FileHandle worldConfigFile = newWorldDir.child("worldconfig.txt");
                worldConfigFile.writeString("seed="+String.valueOf(seed), false);

                // Starte den GameScreen mit der erstellten Welt (hier wird der Weltname übergeben)
                game.setScreen(new GameScreen(game, worldName));
            }
        });

        // "Back"-Button: Gehe zurück zum vorherigen Screen
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new SinglePlayer(game));
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
        // Optional: Hier kannst du z. B. stage.getViewport().update(width, height, true) aufrufen
    }

    @Override
    public void pause() {
        // Nicht benötigt
    }

    @Override
    public void resume() {
        // Nicht benötigt
    }

    @Override
    public void hide() {
        // Nicht benötigt
    }

    @Override
    public void dispose() {
        backgroundTexture.dispose();
        stage.dispose();
        font.dispose();
        if (textFieldTexture != null) {
            textFieldTexture.dispose();
        }
    }
}
