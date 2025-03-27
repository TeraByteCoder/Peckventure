package at.peckventure.menu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class PauseMenu extends ScreenAdapter {

    private final SpriteBatch batch;
    private final OrthographicCamera camera;
    private final BitmapFont font;
    private final Game game;
    private final Screen previousScreen;

    // Neuer Konstruktor: Übergabe von Game und dem vorherigen Screen
    public PauseMenu(Game game, Screen previousScreen) {
        this.game = game;
        this.previousScreen = previousScreen;
        batch = new SpriteBatch();
        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        font = new BitmapFont();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 0.7f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        batch.setProjectionMatrix(camera.combined);

        batch.begin();
        font.getData().setScale(2);
        font.draw(batch, "Paused", Gdx.graphics.getWidth() / 2f - 50, Gdx.graphics.getHeight() / 2f + 20);
        font.getData().setScale(1);
        font.draw(batch, "Press ESC to Resume", Gdx.graphics.getWidth() / 2f - 80, Gdx.graphics.getHeight() / 2f - 20);
        batch.end();

        // Bei ESC zurück zum Spiel
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(previousScreen);
        }
    }

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
    }
}
