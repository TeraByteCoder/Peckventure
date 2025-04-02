package at.peckventure.menu;

import at.peckventure.*;
import at.peckventure.chat.ChatUI;
import at.peckventure.chat.SinglePlayerChatExecutor;
import at.peckventure.entities.ControlledPlayer;
import at.peckventure.entities.mob.MobMap;
import at.peckventure.inventory.InventoryUI;
import at.peckventure.ui.DebugOverlay;
import at.peckventure.ui.EnergyUI;
import at.peckventure.ui.HealthUI;
import at.peckventure.world.Box2DOperationManager;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.StretchViewport;

public abstract class GameScreen implements Screen
{
    protected final Game game;
    protected ParallaxBackgroundActor background;
    protected OrthographicCamera camera;
    protected SpriteBatch batch;

    // Gemeinsame Stages
    protected Stage stage;       // Spielwelt
    protected Stage uiStage;     // HUD, Chat, Inventar etc.
    protected Stage pauseStage;  // Pause-Overlay
    protected Stage backgroundStage; // Hintergrund-Stufe für Parallax

    // Physikwelt und weitere gemeinsame Felder
    protected final World physicsWorld;
    // Weitere gemeinsame Felder wie Player, Map, etc. können hier ergänzt werden.

    // Texturen für Buttons/Overlays
    protected Texture pauseButtonTexture;
    protected Texture whiteTexture;

    protected ChatUI chatUI;

    protected ControlledPlayer player;

    protected InventoryUI inventoryUI;

    protected HealthUI healthUI;
    protected EnergyUI energyUI;
    protected DebugOverlay debugOverlay;


    public GameScreen(Game game)
    {
        this.game = game;
        this.physicsWorld = new World(new Vector2(0, -19.81f), true);
    }

    /**
     * Initialisiert alle gemeinsamen Komponenten, inklusive Kamera, Stages
     * und den Parallax-Hintergrund, der für beide Modi genutzt wird.
     */
    @Override
    public void show()
    {
        batch = new SpriteBatch();

        // Erzeuge eine 1x1 weiße Textur für den Overlay-Hintergrund
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        whiteTexture = new Texture(pixmap);
        pixmap.dispose();

        // Kamera und Stage für die Spielwelt
        float worldWidth = Gdx.graphics.getWidth() / 2f;
        float worldHeight = Gdx.graphics.getHeight() / 2f;

        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth() / 2f, Gdx.graphics.getHeight() / 2f);
        stage = new Stage(new StretchViewport(worldWidth, worldHeight, camera));

        backgroundStage = new Stage(new ScreenViewport());
        background = new ParallaxBackgroundActor(camera);
        backgroundStage.addActor(background);


        background.addLayer("textures/background/paralax/sky.png", 0.0f);
        background.addLayer("textures/background/paralax/cloud.png", 0.1f);
        background.addLayer("textures/background/paralax/mountain2.png", 0.3f);
        background.addLayer("textures/background/paralax/pine2.png", 0.8f);          // Bereits sehr nahe


// Den Vordergrund (sollte am meisten mit der Kamera mitgehen) fügst du zuletzt hinzu:
        background.addLayer("textures/background/paralax/pine1.png", 1.0f);


        // Stage für HUD/Chat/Inventar
        uiStage = new Stage(new ScreenViewport());
        // Stage für das Pause-Menü
        pauseStage = new Stage(new ScreenViewport());

        Globals.mobs = new MobMap(stage);
        ClientGlobal.stage = stage;


        // Physik-Listener
        physicsWorld.setContactListener(new GameContactListener());

        // Chat initialisieren
        chatUI = new ChatUI(uiStage, new SinglePlayerChatExecutor());
        InputManager.getInstance().setChatToggle(new InputManager.ChatToggle()
        {
            @Override
            public void toggleChat()
            {
                chatUI.toggleChat();
            }

            @Override
            public void cancelChat()
            {
                chatUI.cancelChat();
            }

            @Override
            public boolean isChatActive()
            {
                return chatUI.isChatActive();
            }
        });

        player = ControlledPlayer.getInstance(physicsWorld, 0, 0);

        // InputMultiplexer: pauseStage hat höchste Priorität
        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(pauseStage);
        multiplexer.addProcessor(InputManager.getInstance());
        multiplexer.addProcessor(uiStage);
        multiplexer.addProcessor(stage);
        Gdx.input.setInputProcessor(multiplexer);

        healthUI = new HealthUI(uiStage, ControlledPlayer.getInstance().getHealthStatus());
        energyUI = new EnergyUI(uiStage, ControlledPlayer.getInstance().getEnergyStatus());
        debugOverlay = new DebugOverlay(uiStage);
        Globals.physicsWorld = physicsWorld;
        Box2DOperationManager.processOperations();
    }

    @Override
    public void resize(int width, int height)
    {
        stage.getViewport().update(width, height, true);
        uiStage.getViewport().update(width, height, true);
        pauseStage.getViewport().update(width, height, true);
        backgroundStage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose()
    {
        batch.dispose();
        stage.dispose();
        uiStage.dispose();
        pauseStage.dispose();
        physicsWorld.dispose();
        if (pauseButtonTexture != null)
        {
            pauseButtonTexture.dispose();
        }
        if (whiteTexture != null)
        {
            whiteTexture.dispose();
        }
    }

}
