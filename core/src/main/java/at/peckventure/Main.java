package at.peckventure;

import at.peckventure.menu.MainMenu;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends Game {
    @Override
    public void create()
    {
        // Hole die Monitorgröße
        Graphics.DisplayMode displayMode = Gdx.graphics.getDisplayMode();

        // Setze randlosen Fullscreen
        Gdx.graphics.setUndecorated(true);
        Gdx.graphics.setWindowedMode(displayMode.width, displayMode.height);

        // Checke, ob es alles vorm start gibt
        OnStartCheck.checkOnStart();

        // Starte das Hauptmenü
        this.setScreen(new MainMenu(this));
    }
}
