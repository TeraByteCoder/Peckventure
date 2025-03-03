package at.peckventure.menu;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;

public class Settings implements Screen
{
    private final Game game;
    private Stage stage;
    private Texture backgroundTexture;
    private Image backgroundImage;
    private Label titleLabel;

    public Settings(Game game)
    {
        this.game = game;
    }

    @Override
    public void show()
    {
        stage = new Stage();
        Gdx.input.setInputProcessor(stage);

        // 🎨 Hintergrundbild
        backgroundTexture = new Texture("textures/background/forest.png");
        backgroundImage = new Image(backgroundTexture);
        backgroundImage.setSize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        stage.addActor(backgroundImage);

        // 📌 Titel
        Label.LabelStyle titleStyle = new Label.LabelStyle();
        titleStyle.font = new BitmapFont();
        titleLabel = new Label("Einstellungen", titleStyle);
        titleLabel.setFontScale(2f);
        titleLabel.setAlignment(Align.center);

        // 📌 Infotext
        Label.LabelStyle textStyle = new Label.LabelStyle();
        textStyle.font = new BitmapFont();
        Label infoLabel = new Label("Hier kannst du deine Spieleinstellungen anpassen.", textStyle);
        infoLabel.setFontScale(1.2f);
        infoLabel.setAlignment(Align.center);

        // 🔘 Button-Stil
        Texture buttonTexture = new Texture("textures/gui/button1.png");
        TextureRegionDrawable buttonDrawable = new TextureRegionDrawable(buttonTexture);
        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.up = buttonDrawable;
        buttonStyle.down = buttonDrawable;
        buttonStyle.font = new BitmapFont();

        // 📌 Zurück-Button
        TextButton backButton = new TextButton("Zurück", buttonStyle);

        // 📌 Layout-Tabelle
        Table rootTable = new Table();
        rootTable.setFillParent(true);
        rootTable.top();
        rootTable.add(titleLabel).padTop(50).expandX().center();
        rootTable.row();
        rootTable.add(infoLabel).padTop(20).expandX().center();
        rootTable.row();
        rootTable.add(backButton).size(300, 80).padTop(50);

        stage.addActor(rootTable);

        // 📌 Button Event
        backButton.addListener(new ClickListener()
        {
            @Override
            public void clicked(InputEvent event, float x, float y)
            {
                game.setScreen(new MainMenu(game)); // 🎯 Geht zurück ins Hauptmenü
            }
        });
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
    }
}
