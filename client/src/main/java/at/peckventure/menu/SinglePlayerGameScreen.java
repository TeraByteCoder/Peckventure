package at.peckventure.menu;

import at.peckventure.Globals;
import at.peckventure.InputManager;
import at.peckventure.chat.ChatUI;
import at.peckventure.chat.SinglePlayerChatExecutor;
import at.peckventure.entities.ControlledPlayer;
import at.peckventure.entities.Player;
import at.peckventure.inventory.InventoryUI;
import at.peckventure.inventory.SinglePlayerInventoryManager;
import at.peckventure.rpc.DiscordPresence;
import at.peckventure.ui.DebugOverlay;
import at.peckventure.ui.EnergyUI;
import at.peckventure.ui.HealthUI;
import at.peckventure.world.*;
import at.peckventure.world.block.Block;
import at.peckventure.world.generator.WorldGenerator;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;


public class SinglePlayerGameScreen extends GameScreen {

    private final String worldName;
    private SinglePlayerMap tilemap;
    private WorldConfig worldConfig;
    private PlayerData playerData;
    private Player player;

    private boolean paused = false;

    private Texture pauseButtonTexture;
    private Texture whiteTexture;

    public SinglePlayerGameScreen(Game game, String worldName) {
        super(game);
        this.worldName = worldName;
    }



    @Override
    public void show() {
        DiscordPresence.updateToIngameSP(worldName);

        super.show();

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

        FileHandle worldDir = Gdx.files.absolute(at.peckventure.Const.savesDir + "/" + worldName);
        WorldIO.LoadedWorld loadedWorld = WorldIO.loadWorld(worldDir, physicsWorld);
        worldConfig = loadedWorld.getConfig();
        playerData = PlayerData.load(worldDir, Globals.uuid);

        WorldGenerator generator = new WorldGenerator(worldConfig.getSeed(), physicsWorld);
        RegionManager regionManager = new RegionManager(worldDir);
        MobRegionManager mobRegionManager = new MobRegionManager(worldDir);
        tilemap = new SinglePlayerMap(physicsWorld, generator, loadedWorld.getLoadedChunks(), regionManager, mobRegionManager);

        float spawnX = playerData.getPlayerX();
        float spawnY = playerData.getPlayerY();
        if (spawnX == 0 && spawnY == 0) {
            spawnX = 0;
            int terrainHeight = generator.getHeight((int) spawnX);
            spawnY = terrainHeight * Block.BLOCK_SIZE + 400;
        }

        ControlledPlayer.getInstance().getBody().setTransform(spawnX / Block.BLOCK_SIZE, spawnY / Block.BLOCK_SIZE, ControlledPlayer.getInstance().getBody().getAngle());
        player = ControlledPlayer.getInstance(physicsWorld, spawnX, spawnY);
        player.getEnergyStatus().setMax(playerData.getMaxEnergy());
        player.getHealthStatus().setMax(playerData.getMaxHealth());
        player.deserializeEffects(playerData.getEffects());
        stage.addActor(player);

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

        tilemap.startChunkUpdateThread(player);
        Box2DOperationManager.processOperations();
        createPauseOverlay();
    }

    private void createPauseOverlay() {
        Label.LabelStyle labelStyle = new Label.LabelStyle(new BitmapFont(), Color.WHITE);
        Label pausedLabel = new Label("Paused", labelStyle);
        pausedLabel.setFontScale(2f);
        pausedLabel.setPosition(
            (pauseStage.getViewport().getWorldWidth() - pausedLabel.getPrefWidth()) / 2f,
            (pauseStage.getViewport().getWorldHeight() - pausedLabel.getPrefHeight()) / 2f + 60
        );

        TextButtonStyle buttonStyle = new TextButtonStyle();
        pauseButtonTexture = new Texture(Gdx.files.internal("textures/gui/button1.png"));
        TextureRegion buttonRegion = new TextureRegion(pauseButtonTexture);
        TextureRegionDrawable buttonDrawable = new TextureRegionDrawable();
        buttonDrawable.setRegion(buttonRegion);
        buttonStyle.up = buttonDrawable;
        buttonStyle.down = buttonDrawable;
        buttonStyle.font = new BitmapFont();

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

        TextButton returnToMenuButton = new TextButton("Return to Main Menu", buttonStyle);
        returnToMenuButton.setSize(300, 80);
        returnToMenuButton.setPosition(
            (pauseStage.getViewport().getWorldWidth() - returnToMenuButton.getWidth()) / 2f,
            (pauseStage.getViewport().getWorldHeight() - returnToMenuButton.getHeight()) / 2f - 120
        );
        returnToMenuButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // Welt speichern
                WorldIO.saveWorld(worldName, worldConfig, tilemap.getLoadedChunks(), ControlledPlayer.getInstance());

                tilemap.stopChunkUpdateThread();

                ControlledPlayer.getInstance().remove();

                dispose();


                // Discord Presence zurücksetzen
                DiscordPresence.start();



                game.setScreen(new MainMenu(game));
            }
        });
        pauseStage.addActor(returnToMenuButton);



        // Dynamisch weiße Textur erzeugen
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        whiteTexture = new Texture(pixmap);
        pixmap.dispose();

        pauseStage.addActor(pausedLabel);
        pauseStage.addActor(resumeButton);
    }

    @Override
    public void render(float delta) {
        // ESC zum Pausieren/Fortsetzen
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            paused = !paused;
        }

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (!paused) {
            Box2DOperationManager.processOperations();
            physicsWorld.step(delta, 6, 2);

            player.act(delta);
            stage.act(delta);
            uiStage.act(delta);
            DiscordPresence.updateToIngameSP(worldName);


        }

        camera.position.set(
            player.getX() + player.getWidth() / 2,
            player.getY() + player.getHeight() / 2,
            0
        );
        camera.zoom = 2.0f;
        camera.update();
        backgroundStage.draw();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        // Falls tilemap.render() Fehler wirft, einfach auskommentieren
        tilemap.render(batch);

        player.draw(batch);
        batch.end();

        stage.draw();
        uiStage.draw();

        if (paused) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            batch.begin();
            batch.setColor(0, 0, 0, 0.7f);
            batch.draw(whiteTexture, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            batch.setColor(1, 1, 1, 1);
            batch.end();
            pauseStage.act(delta);
            pauseStage.draw();
            DiscordPresence.updateToPaused();
        }
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
        super.dispose();
        tilemap.dispose();
        whiteTexture.dispose();
        pauseButtonTexture.dispose();
        WorldIO.saveWorld(worldName, worldConfig, tilemap.getLoadedChunks(), ControlledPlayer.getInstance());
    }
}
