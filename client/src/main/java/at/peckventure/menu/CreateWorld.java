package at.peckventure.menu;

import at.peckventure.world.WorldConfig;
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
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.util.Random;

import static at.peckventure.Const.savesDir;

public class CreateWorld implements Screen {
    private final Game game;
    private Stage stage;
    private Texture backgroundTexture;
    private Image backgroundImage;
    private BitmapFont font;
    private Texture textFieldTexture;
    private JsonValue texts;
    private Skin skin;
    public CreateWorld(Game game) {
        this.game = game;
    }

    @Override
    public void show() {
        // Load the skin like in the Settings class
        skin = new Skin(Gdx.files.internal("ui/uiskin.json"));
        // Sprachdatei laden
        String langCode = GameSettings.getLanguage(); // z. B. "en_us", "de_de", "de_at", "de_ch"
        JsonReader reader = new JsonReader();
        texts = reader.parse(Gdx.files.internal("lang/" + langCode + ".json"));

        stage = new Stage();
        Gdx.input.setInputProcessor(stage);
        font = new BitmapFont();
        font.getData().setScale(2f);

        backgroundTexture = new Texture("textures/background/forest.png");
        backgroundImage = new Image(backgroundTexture);
        backgroundImage.setSize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        stage.addActor(backgroundImage);

        // Label Style
        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = font;
        labelStyle.fontColor = Color.WHITE;

        // Titel mit Übersetzung
        String titleText = texts.has("menu.create_world") ? texts.getString("menu.create_world") : "Create World";
        Label titleLabel = new Label(titleText, labelStyle);
        titleLabel.setFontScale(3f);
        titleLabel.setAlignment(Align.center);

        Texture buttonTexture = new Texture("textures/gui/button1.png");
        TextureRegionDrawable buttonDrawable = new TextureRegionDrawable(buttonTexture);
        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.up = buttonDrawable;
        buttonStyle.down = buttonDrawable;
        buttonStyle.font = font;

        // Übersetzte Buttons
        String createWorldText = texts.has("menu.create_world_button") ? texts.getString("menu.create_world_button") : "Create World";
        String backButtonText = texts.has("menu.back") ? texts.getString("menu.back") : "Back";
        TextButton createWorldButton = new TextButton(createWorldText, buttonStyle);
        TextButton backButton = new TextButton(backButtonText, buttonStyle);

        TextField.TextFieldStyle textFieldStyle = new TextField.TextFieldStyle();
        textFieldStyle.font = font;
        textFieldStyle.fontColor = Color.WHITE;

// Create "Allow Cheats" checkbox with translation
        String allowCheatsText = texts.has("menu.allow_cheats") ? texts.getString("menu.allow_cheats") : "Allow Cheats";
        final CheckBox allowCheatsCheckBox = new CheckBox(" " + allowCheatsText, skin);
// Make the checkbox bigger
        allowCheatsCheckBox.getLabel().setFontScale(1.5f);  // Increase font size

        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.DARK_GRAY);
        pixmap.fill();
        textFieldTexture = new Texture(pixmap);
        pixmap.dispose();
        textFieldStyle.background = new TextureRegionDrawable(new TextureRegion(textFieldTexture));

        // Übersetzte Labels und Platzhalter
        String worldNamePlaceholder = texts.has("menu.world_name_placeholder") ? texts.getString("menu.world_name_placeholder") : "New World";
        String seedPlaceholder = texts.has("menu.world_seed_placeholder") ? texts.getString("menu.world_seed_placeholder") : "Seed";
        final TextField worldNameInput = new TextField(worldNamePlaceholder, textFieldStyle);
        final TextField seedInput = new TextField(seedPlaceholder, textFieldStyle);

        String worldNameLabelText = texts.has("menu.world_name") ? texts.getString("menu.world_name") : "World Name:";
        String seedLabelText = texts.has("menu.world_seed") ? texts.getString("menu.world_seed") : "World Seed:";
        Label worldNameLabel = new Label(worldNameLabelText, labelStyle);
        Label seedLabel = new Label(seedLabelText, labelStyle);

        Table inputTable = new Table();
        inputTable.center();
        inputTable.add(worldNameLabel).pad(10).center();
        inputTable.add(worldNameInput).width(600).height(80).pad(10).center();
        // Add after the inputTable definition
        inputTable.row();
        inputTable.row();
        inputTable.add(allowCheatsCheckBox).colspan(2).pad(20).left();  // Increased padding for more space
        inputTable.row();
        inputTable.add(seedLabel).pad(10).center();
        inputTable.add(seedInput).width(600).height(80).pad(10).center();

        Table buttonTable = new Table();
        buttonTable.add(createWorldButton).size(400, 120).pad(10);
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

        createWorldButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                String worldName = worldNameInput.getText();
                if (worldName == null || worldName.trim().isEmpty()) {
                    worldName = "New World";
                }

                FileHandle newWorldDir = Gdx.files.absolute(savesDir + "/" + worldName);
                while (newWorldDir.exists()) {
                    worldName += "_";
                    newWorldDir = Gdx.files.absolute(savesDir + "/" + worldName);
                }

                String seedText = seedInput.getText();
                long seed;
                if (seedText.isEmpty()) seed = new Random().nextInt();
                else {
                    try {
                        seed = Long.parseLong(seedText);
                    } catch (NumberFormatException e) {
                        seed = seedText.isEmpty() ? System.currentTimeMillis() : seedText.hashCode();
                    }
                }

                newWorldDir.mkdirs();
                WorldConfig config = new WorldConfig(seed);
                FileHandle configFile = newWorldDir.child("worldconfig.txt");
                config.save(configFile);

                game.setScreen(new SinglePlayerGameScreen(game, worldName, allowCheatsCheckBox.isChecked()));
            }
        });

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
        if (textFieldTexture != null) {
            textFieldTexture.dispose();
        }
        if (skin != null) {
            skin.dispose();
        }
    }
}
