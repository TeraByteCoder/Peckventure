package at.peckventure.menu;

import at.peckventure.Const;
import at.peckventure.Globals;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

public class FirstStartScreen implements Screen {
    private final Game game;
    private Stage stage;
    private Texture backgroundTexture;
    private Image backgroundImage;

    public FirstStartScreen(Game game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage();
        Gdx.input.setInputProcessor(stage);

        // Hintergrund wie im MainMenu
        backgroundTexture = new Texture("textures/background/forest.png");
        backgroundImage = new Image(backgroundTexture);
        backgroundImage.setSize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        stage.addActor(backgroundImage);

        // Erstelle einen Skin (oder verwende den existierenden, falls du schon einen hast)
        Skin skin = new Skin();
        BitmapFont font = new BitmapFont();
        skin.add("default", font);

        // TextField-Stil ähnlich wie im MainMenu (hier können weitere Anpassungen erfolgen)
        TextField.TextFieldStyle textFieldStyle = new TextField.TextFieldStyle();
        textFieldStyle.font = font;
        textFieldStyle.fontColor = Color.WHITE;
        // Optional: Setze hier einen Hintergrund für das Textfeld, falls vorhanden
        // textFieldStyle.background = new TextureRegionDrawable(new Texture("textures/gui/textfield.png"));

        final TextField usernameField = new TextField("", textFieldStyle);
        usernameField.setMessageText("Username");

        // Button-Stil analog zum MainMenu
        Texture buttonTexture = new Texture("textures/gui/button1.png");
        TextureRegionDrawable buttonDrawable = new TextureRegionDrawable(buttonTexture);
        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.up = buttonDrawable;
        buttonStyle.down = buttonDrawable;
        buttonStyle.font = font;

        TextButton submitButton = new TextButton("Submit", buttonStyle);

        // Table-Layout: Zentriere das Textfeld und den Button
        Table table = new Table();
        table.setFillParent(true);
        table.center();
        table.add(usernameField).width(300).height(50).pad(10);
        table.row();
        table.add(submitButton).width(300).height(80).pad(10);
        stage.addActor(table);

        // Beim Klick auf "Submit" wird überprüft, ob ein gültiger Username eingegeben wurde.
        submitButton.addListener(new ClickListener(){
            @Override
            public void clicked(InputEvent event, float x, float y) {
                String inputUsername = usernameField.getText().trim();
                if(!inputUsername.isEmpty()){
                    // Wenn keine UUID vorhanden ist, generiere eine neue
                    String uuid = Globals.uuid;
                    if(uuid == null || uuid.isEmpty()){
                        uuid = java.util.UUID.randomUUID().toString();
                        Globals.uuid = uuid;
                    }
                    Globals.username = inputUsername;

                    // Schreibe die Einstellungen in die Datei
                    FileHandle settingsFile = Const.gameDir.child("settings.txt");
                    settingsFile.writeString("uuid=" + uuid + "\nusername=" + inputUsername, false);

                    // Nach erfolgreicher Eingabe zum MainMenu wechseln
                    game.setScreen(new MainMenu(game));
                }
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
        // Optional: Bei Bedarf das Table-Layout oder die Stage anpassen
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
