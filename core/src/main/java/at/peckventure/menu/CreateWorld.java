package at.peckventure.menu;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;

public class CreateWorld implements Screen
{
    private Game game;

    public CreateWorld(Game game)
    {
        this.game = game;
    }

    @Override
    public void show()
    {

    }

    @Override
    public void render(float v)
    {

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

    }

    // 📌 Erstellt eine neue Welt (Ordner)
    private void createNewWorld() {
        String worldName = "World_" + System.currentTimeMillis();
        FileHandle newWorldDir = Gdx.files.local("saves/" + worldName);
        newWorldDir.mkdirs(); // Erstellt den neuen Ordner für die Welt
    }
}
