package at.peckventure.menu;

import at.peckventure.ClientGlobal;
import at.peckventure.GameContactListener;
import at.peckventure.Globals;
import at.peckventure.chat.SinglePlayerChatExecutor;
import at.peckventure.entities.ControlledPlayer;
import at.peckventure.entities.Player;
import at.peckventure.entities.mob.MobMap;
import at.peckventure.inventory.InventoryUI;
import at.peckventure.inventory.SinglePlayerInventoryManager;
import at.peckventure.ui.DebugOverlay;
import at.peckventure.ui.EnergyUI;
import at.peckventure.ui.HealthUI;
import at.peckventure.world.Box2DOperationManager;
import at.peckventure.world.MobRegionManager;
import at.peckventure.world.PlayerData;
import at.peckventure.world.RegionManager;
import at.peckventure.world.SinglePlayerMap;
import at.peckventure.world.WorldConfig;
import at.peckventure.world.WorldIO;
import at.peckventure.world.block.Block;
import at.peckventure.world.generator.WorldGenerator;
import at.peckventure.chat.ChatUI;
import at.peckventure.InputManager;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.StretchViewport;

public class SinglePlayerGameScreen implements Screen {

    private final Game game;
    private final String worldName;
    private OrthographicCamera camera;
    private SpriteBatch batch;

    // Stages
    private Stage stage;      // Spielwelt
    private Stage uiStage;    // HUD, Chat, Inventar etc.
    private Stage pauseStage; // Pause-Overlay

    // Welt und Map
    private final World physicsWorld;
    private SinglePlayerMap tilemap;
    private WorldConfig worldConfig;
    private PlayerData playerData;
    private Player player;

    // UI-Komponenten
    private ChatUI chatUI;
    private HealthUI healthUI;
    private EnergyUI energyUI;
    private InventoryUI inventoryUI;
    private DebugOverlay debugOverlay;

    // Pause-Status
    private boolean paused = false;

    // Texture für den Button-Style (wird in dispose() freigegeben)
    private Texture pauseButtonTexture;
    // White-Textur für den halbtransparenten Overlay-Hintergrund
    private Texture whiteTexture;

    public SinglePlayerGameScreen(Game game, String worldName) {
        this.game = game;
        this.worldName = worldName;
        this.physicsWorld = new World(new Vector2(0, -19.81f), true);
    }

    @Override
    public void show() {
        batch = new SpriteBatch();

        // Erzeuge eine 1x1 weiße Textur für den Overlay-Hintergrund
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        whiteTexture = new Texture(pixmap);
        pixmap.dispose();

        // Kamera und Stage für die Spielwelt
        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth() / 2f, Gdx.graphics.getHeight() / 2f);
        stage = new Stage(new StretchViewport(Gdx.graphics.getWidth() / 2f, Gdx.graphics.getHeight() / 2f, camera));

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
        InputManager.getInstance().setChatToggle(new InputManager.ChatToggle() {
            @Override
            public void toggleChat() {
                chatUI.toggleChat();
            }
            @Override
            public void cancelChat() {
                chatUI.cancelChat();
            }
            @Override
            public boolean isChatActive() {
                return chatUI.isChatActive();
            }
        });

        // InputMultiplexer: pauseStage hat höchste Priorität
        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(pauseStage);
        multiplexer.addProcessor(InputManager.getInstance());
        multiplexer.addProcessor(uiStage);
        multiplexer.addProcessor(stage);
        Gdx.input.setInputProcessor(multiplexer);

        // Welt laden
        FileHandle worldDir = Gdx.files.absolute(at.peckventure.Const.savesDir + "/" + worldName);
        WorldIO.LoadedWorld loadedWorld = WorldIO.loadWorld(worldDir, physicsWorld);
        worldConfig = loadedWorld.getConfig();
        playerData = PlayerData.load(worldDir, Globals.uuid);

        // Weltgenerator und Map initialisieren
        WorldGenerator generator = new WorldGenerator(worldConfig.getSeed(), physicsWorld);
        RegionManager regionManager = new RegionManager(worldDir);
        MobRegionManager mobRegionManager = new MobRegionManager(worldDir);
        tilemap = new SinglePlayerMap(physicsWorld, generator, loadedWorld.getLoadedChunks(), regionManager, mobRegionManager);

        // Spielerposition ermitteln (ggf. Standard-Spawn)
        float spawnX = playerData.getPlayerX();
        float spawnY = playerData.getPlayerY();
        if (spawnX == 0 && spawnY == 0) {
            spawnX = 0;
            int terrainHeight = generator.getHeight((int) spawnX);
            spawnY = terrainHeight * Block.BLOCK_SIZE + 400;
        }

        // Spieler erstellen und zur Stage hinzufügen
        player = ControlledPlayer.getInstance(physicsWorld, spawnX, spawnY);
        stage.addActor(player);

        // HUD und UI initialisieren
        inventoryUI = new InventoryUI(uiStage, new SinglePlayerInventoryManager());
        healthUI = new HealthUI(uiStage, ControlledPlayer.getInstance().getHealthStatus());
        energyUI = new EnergyUI(uiStage, ControlledPlayer.getInstance().getEnergyStatus());
        debugOverlay = new DebugOverlay(uiStage);

        ControlledPlayer.getInstance().getHealthStatus().setCurrent(playerData.getHealth());
        ControlledPlayer.getInstance().getEnergyStatus().setCurrent(playerData.getEnergy());
        if (!playerData.getInventoryHotbar().isEmpty() && !playerData.getInventoryMain().isEmpty()) {
            ControlledPlayer.getInstance().getInventory().deserialize(
                playerData.getInventoryHotbar(),
                playerData.getInventoryMain()
            );
        }

        Globals.physicsWorld = physicsWorld;
        tilemap.startChunkUpdateThread(player);
        Box2DOperationManager.processOperations();

        // Erzeuge das Pause-Overlay
        createPauseOverlay();
    }

    private void createPauseOverlay() {
        // Label "Paused" erstellen
        Label.LabelStyle labelStyle = new Label.LabelStyle(new BitmapFont(), Color.WHITE);
        Label pausedLabel = new Label("Paused", labelStyle);
        pausedLabel.setFontScale(2f);
        pausedLabel.setPosition(
            (pauseStage.getViewport().getWorldWidth() - pausedLabel.getPrefWidth()) / 2f,
            (pauseStage.getViewport().getWorldHeight() - pausedLabel.getPrefHeight()) / 2f + 60
        );

        // TextButtonStyle erstellen
        TextButtonStyle buttonStyle = new TextButtonStyle();
        // Button-Textur laden – hier über Gdx.files.internal
        pauseButtonTexture = new Texture(Gdx.files.internal("textures/gui/button1.png"));
        TextureRegion buttonRegion = new TextureRegion(pauseButtonTexture);
        // Erstelle zunächst ein leeres TextureRegionDrawable und setze anschließend die Region
        TextureRegionDrawable buttonDrawable = new TextureRegionDrawable();
        buttonDrawable.setRegion(buttonRegion);
        buttonStyle.up = buttonDrawable;
        buttonStyle.down = buttonDrawable;
        buttonStyle.font = new BitmapFont();

        // "Back to Game"-Button erstellen
        TextButton resumeButton = new TextButton("Back to Game", buttonStyle);
        resumeButton.setSize(300, 80);
        resumeButton.setPosition(
            (pauseStage.getViewport().getWorldWidth() - resumeButton.getWidth()) / 2f,
            (pauseStage.getViewport().getWorldHeight() - resumeButton.getHeight()) / 2f - 20
        );
        resumeButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                paused = false;
            }
        });

        pauseStage.addActor(pausedLabel);
        pauseStage.addActor(resumeButton);
    }

    @Override
    public void render(float delta) {
        // Umschalten zwischen Pause und Spiel via ESC
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            paused = !paused;
        }

        // Render immer zuerst die Spielszene
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Box2DOperationManager.processOperations();
        physicsWorld.step(delta, 6, 2);

        camera.position.set(
            player.getX() + player.getWidth() / 2,
            player.getY() + player.getHeight() / 2,
            0
        );
        camera.zoom = 2.0f;
        camera.update();

        stage.act(delta);
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        tilemap.render(batch);
        player.draw(batch);
        batch.end();
        stage.draw();

        uiStage.act(delta);
        uiStage.draw();

        // Falls pausiert, wird ein halbtransparentes Overlay gezeichnet und anschließend das Pause-Menü
        if (paused) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            batch.begin();
            batch.setColor(0, 0, 0, 0.7f);
            batch.draw(whiteTexture, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            batch.setColor(1, 1, 1, 1);
            batch.end();
            pauseStage.act(delta);
            pauseStage.draw();
        }
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        uiStage.getViewport().update(width, height, true);
        pauseStage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {
        WorldIO.saveWorld(worldName, worldConfig, tilemap.getLoadedChunks(), ControlledPlayer.getInstance());
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        batch.dispose();
        stage.dispose();
        uiStage.dispose();
        pauseStage.dispose();
        physicsWorld.dispose();
        tilemap.dispose();
        WorldIO.saveWorld(worldName, worldConfig, tilemap.getLoadedChunks(), ControlledPlayer.getInstance());
        if (pauseButtonTexture != null) {
            pauseButtonTexture.dispose();
        }
        if (whiteTexture != null) {
            whiteTexture.dispose();
        }
    }
}
