package at.peckventure.menu;

import at.peckventure.FontManager;
import at.peckventure.LanguageManager;
import at.peckventure.world.WorldConfig;
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

import java.util.Random;

import static at.peckventure.Const.savesDir;

public class CreateWorld implements Screen {
    private final Game game;
    private Stage stage;
    private Texture backgroundTexture;
    private Image backgroundImage;
    private Skin skin;

    public CreateWorld(Game game) {
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
        String titleText = LanguageManager.INSTANCE.getText("menu.create_world");
        Label titleLabel = new Label(titleText, skin);
        titleLabel.setFontScale(3f);
        titleLabel.setAlignment(Align.center);

        // Übersetzte Buttons
        String createWorldText = LanguageManager.INSTANCE.getText("menu.create_world_button");
        String backButtonText = LanguageManager.INSTANCE.getText("menu.back");
        TextButton createWorldButton = new TextButton(createWorldText, skin);
        TextButton backButton = new TextButton(backButtonText, skin);

        // Create "Allow Cheats" checkbox with translation
        String allowCheatsText = LanguageManager.INSTANCE.getText("menu.allow_cheats");
        final CheckBox allowCheatsCheckBox = new CheckBox(" " + allowCheatsText, skin);
        // Make the checkbox bigger
        allowCheatsCheckBox.getLabel().setFontScale(1.5f);  // Increase font size

        // Übersetzte Labels und Platzhalter
        String worldNamePlaceholder = LanguageManager.INSTANCE.getText("menu.world_name_placeholder");
        String seedPlaceholder = LanguageManager.INSTANCE.getText("menu.world_seed_placeholder");
        final TextField worldNameInput = new TextField(worldNamePlaceholder, skin);
        final TextField seedInput = new TextField(seedPlaceholder, skin);

        String worldNameLabelText = LanguageManager.INSTANCE.getText("menu.world_name");
        String seedLabelText = LanguageManager.INSTANCE.getText("menu.world_seed");
        Label worldNameLabel = new Label(worldNameLabelText, skin);
        Label seedLabel = new Label(seedLabelText, skin);

        Table inputTable = new Table();
        inputTable.center();
        inputTable.add(worldNameLabel).pad(10).center();
        inputTable.add(worldNameInput).width(600).height(80).pad(10).center();
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
        if (skin != null) {
            skin.dispose();
        }
    }
}
