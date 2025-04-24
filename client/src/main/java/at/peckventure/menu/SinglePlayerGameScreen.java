package at.peckventure.menu;

import at.peckventure.Globals;
import at.peckventure.InputManager;
import at.peckventure.LanguageManager;
import at.peckventure.chat.ChatUI;
import at.peckventure.chat.SinglePlayerChatExecutor;
import at.peckventure.entities.ControlledPlayer;
import at.peckventure.entities.MobSpawner;
import at.peckventure.entities.Player;
import at.peckventure.inventory.InventoryUI;
import at.peckventure.inventory.SinglePlayerInventoryManager;
import at.peckventure.light.LightSystem;
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
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

public class SinglePlayerGameScreen extends GameScreen {

    private MobSpawner mobSpawner;

    private final String worldName;
    private SinglePlayerMap tilemap;
    private WorldConfig worldConfig;
    private PlayerData playerData;
    private Player player;
    private Box2DDebugRenderer debugRenderer;

    private boolean paused = false;
    private boolean debugOverlayVisible = false;

    private Texture pauseButtonTexture;
    private Texture whiteTexture;
    private Boolean operator;
    private LightSystem lightSystem;

    private boolean isDead = false;
    private Stage deathStage;
    private Texture blackOverlayTexture;

    // InputProcessor für Chat-Steuerung
    private final InputProcessor chatInputProcessor = new InputProcessor() {
        @Override
        public boolean keyDown(int keycode) {
            // ESC schließt den Chat
            if (keycode == Input.Keys.ESCAPE) {
                chatUI.cancelChat();
                updateInputProcessors();
                return true;
            }

            // Alle anderen Tasten werden weitergeleitet
            return false;
        }

        @Override
        public boolean keyUp(int keycode) { return false; }

        @Override
        public boolean keyTyped(char character) { return false; }

        @Override
        public boolean touchDown(int screenX, int screenY, int pointer, int button) { return false; }

        @Override
        public boolean touchUp(int screenX, int screenY, int pointer, int button) { return false; }

        @Override
        public boolean touchCancelled(int i, int i1, int i2, int i3) {
            return false;
        }

        @Override
        public boolean touchDragged(int screenX, int screenY, int pointer) { return false; }

        @Override
        public boolean mouseMoved(int screenX, int screenY) { return false; }

        @Override
        public boolean scrolled(float amountX, float amountY) { return false; }
    };

    // InputProcessor für Spiel-Steuerung
    private final InputProcessor gameInputProcessor = new InputProcessor() {
        @Override
        public boolean keyDown(int keycode) {
            // T öffnet den Chat
            if (keycode == Input.Keys.T) {
                chatUI.toggleChat();
                updateInputProcessors();
                return true;
            }
            // ESC pausiert das Spiel
            else if (keycode == Input.Keys.ESCAPE) {
                paused = !paused;
                return true;
            }
            // F3 für Debug-Overlay
            else if (keycode == Input.Keys.F3) {
                debugOverlayVisible = !debugOverlayVisible;
                if (debugOverlayVisible) {
                    debugOverlay.show();
                } else {
                    debugOverlay.hide();
                }
                return true;
            }
            // L für Licht-Modus
            else if (keycode == Input.Keys.L) {
                lightSystem.toggleDarkMode();
                return true;
            }

            return false; // Andere Tasten werden weitergeleitet
        }

        @Override
        public boolean keyUp(int keycode) { return false; }

        @Override
        public boolean keyTyped(char character) { return false; }

        @Override
        public boolean touchDown(int screenX, int screenY, int pointer, int button) { return false; }

        @Override
        public boolean touchUp(int screenX, int screenY, int pointer, int button) { return false; }

        @Override
        public boolean touchCancelled(int i, int i1, int i2, int i3) {
            return false;
        }

        @Override
        public boolean touchDragged(int screenX, int screenY, int pointer) { return false; }

        @Override
        public boolean mouseMoved(int screenX, int screenY) { return false; }

        @Override
        public boolean scrolled(float amountX, float amountY) { return false; }
    };

    // InputMultiplexer für dynamisches Umschalten
    private InputMultiplexer inputMultiplexer;

    public SinglePlayerGameScreen(Game game, String worldName) {
        super(game);
        this.worldName = worldName;
    }

    public SinglePlayerGameScreen(Game game, String worldName, boolean operator) {
        super(game);
        this.worldName = worldName;
        this.operator = operator;
    }

    @Override
    public void show() {
        // Initialisiere den debugRenderer
        debugRenderer = new Box2DDebugRenderer();

        DiscordPresence.updateToIngameSP(worldName);

        super.show();

        // Chat initialisieren
        chatUI = new ChatUI(uiStage, new SinglePlayerChatExecutor());

        // InputManager-Integration für Rückwärtskompatibilität
        InputManager.getInstance().setChatToggle(new InputManager.ChatToggle() {
            @Override
            public void toggleChat() {
                chatUI.toggleChat();
                updateInputProcessors();
            }

            @Override
            public void cancelChat() {
                chatUI.cancelChat();
                updateInputProcessors();
            }

            @Override
            public boolean isChatActive() {
                return chatUI.isChatActive();
            }
        });

        // EscapeHandler für das Pausensystem
        InputManager.getInstance().setEscapeHandler(new InputManager.EscapeHandler() {
            @Override
            public void handleEscape() {
                // Wenn der Chat aktiv ist, wird dies in chatInputProcessor behandelt
                if (!chatUI.isChatActive()) {
                    paused = !paused;
                }
            }

            @Override
            public boolean isMenuActive() {
                return paused;
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

        // Stelle sicher, dass der Player komplett neu initialisiert wird
        // Zuerst prüfen, ob bereits eine Instanz existiert und diese zurücksetzen
        if (ControlledPlayer.hasInstance()) {
            ControlledPlayer.getInstance().remove();
            ControlledPlayer.reset(); // Diese Methode wird in ControlledPlayer implementiert
        }

        // Jetzt den Player neu initialisieren
        player = ControlledPlayer.getInstance(physicsWorld, spawnX, spawnY);
        player.getEnergyStatus().setMax(playerData.getMaxEnergy());
        player.getHealthStatus().setMax(playerData.getMaxHealth());
        player.deserializeEffects(playerData.getEffects());
        if (operator!= null)
            player.setOperator(operator);
        else
            player.setOperator(playerData.isOperator());
        stage.addActor(player);

        // Lichtsystem initialisieren
        lightSystem = new LightSystem(physicsWorld, player);

        // Nach dem Erstellen des Players:
        inventoryUI = new InventoryUI(uiStage, new SinglePlayerInventoryManager());
        healthUI = new HealthUI(uiStage, ControlledPlayer.getInstance().getHealthStatus());
        energyUI = new EnergyUI(uiStage, ControlledPlayer.getInstance().getEnergyStatus());
        debugOverlay = new DebugOverlay(uiStage);
        // Debug Overlay wird erst bei F3-Druck angezeigt

        ControlledPlayer.getInstance().getHealthStatus().setCurrent(playerData.getHealth());
        ControlledPlayer.getInstance().getEnergyStatus().setCurrent(playerData.getEnergy());
        if (!playerData.getInventoryHotbar().isEmpty() && !playerData.getInventoryMain().isEmpty()) {
            ControlledPlayer.getInstance().getInventory().deserialize(
                playerData.getInventoryHotbar(),
                playerData.getInventoryMain()
            );
        }
        mobSpawner = new MobSpawner(physicsWorld, player);
        tilemap.startChunkUpdateThread(player);
        Box2DOperationManager.processOperations();
        createPauseOverlay();
        createDeathOverlay();

        // InputMultiplexer initialisieren
        inputMultiplexer = new InputMultiplexer();
        updateInputProcessors();
        Gdx.input.setInputProcessor(inputMultiplexer);

        float playerX = player.getX();
        float playerY = player.getY();
    }

    // Diese Methode aktualisiert die Input-Prozessoren basierend auf dem Spielzustand
    private void updateInputProcessors() {
        inputMultiplexer.clear();

        // Death und Pause-Stages haben immer höchste Priorität
        inputMultiplexer.addProcessor(deathStage);
        inputMultiplexer.addProcessor(pauseStage);

        if (chatUI.isChatActive()) {
            // Chat ist aktiv: UI-Stages + Chat-Input haben Priorität
            inputMultiplexer.addProcessor(uiStage);
            inputMultiplexer.addProcessor(chatInputProcessor);
        } else {
            // Chat ist nicht aktiv: Game-Input + normales UI
            inputMultiplexer.addProcessor(gameInputProcessor);
            inputMultiplexer.addProcessor(uiStage);
            inputMultiplexer.addProcessor(stage);
            inputMultiplexer.addProcessor(InputManager.getInstance());
        }
    }

    private void createPauseOverlay() {
        Label.LabelStyle labelStyle = new Label.LabelStyle(new BitmapFont(), Color.WHITE);
        Label pausedLabel = new Label(LanguageManager.INSTANCE.getText("menu.game.paused"), labelStyle);
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

        TextButton resumeButton = new TextButton(LanguageManager.INSTANCE.getText("menu.back.to.game"), buttonStyle);
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

        TextButton returnToMenuButton = new TextButton(LanguageManager.INSTANCE.getText("menu.return.to.main.menu"), buttonStyle);
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

                // WICHTIG: Erst Thread stoppen, dann Ressourcen freigeben
                tilemap.stopChunkUpdateThread();

                // Player zurücksetzen und entfernen
                ControlledPlayer.getInstance().remove();

                // WICHTIG: Die Singleton-Instanz komplett zurücksetzen
                ControlledPlayer.reset();

                // Weitere Manager zurücksetzen
                Box2DOperationManager.clear();

                // Alle Ressourcen freigeben
                dispose();

                // Discord Presence zurücksetzen
                DiscordPresence.start();

                // Zum Hauptmenü wechseln
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

    private void createDeathOverlay() {
        // halbtransparente schwarze Textur
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(0, 0, 0, 0.7f);
        pixmap.fill();
        blackOverlayTexture = new Texture(pixmap);
        pixmap.dispose();

        deathStage = new Stage(uiStage.getViewport(), batch);
        Label.LabelStyle style = new Label.LabelStyle(new BitmapFont(), Color.RED);
        Label diedLabel = new Label(LanguageManager.INSTANCE.getText("menu.you.died"), style);
        diedLabel.setFontScale(3f);
        diedLabel.setPosition(
            (deathStage.getViewport().getWorldWidth() - diedLabel.getPrefWidth()) / 2f,
            (deathStage.getViewport().getWorldHeight() - diedLabel.getPrefHeight()) / 2f + 50
        );
        deathStage.addActor(diedLabel);

        // Button-Stil wie im Pause-Overlay
        TextButton.TextButtonStyle btnStyle = new TextButton.TextButtonStyle();
        btnStyle.up = new TextureRegionDrawable(new TextureRegion(new Texture(Gdx.files.internal("textures/gui/button1.png"))));
        btnStyle.down = btnStyle.up;
        btnStyle.font = new BitmapFont();

        TextButton respawn = new TextButton(LanguageManager.INSTANCE.getText("menu.respawn"), btnStyle);
        respawn.setSize(300, 80);
        respawn.setPosition(
            (deathStage.getViewport().getWorldWidth() - respawn.getWidth()) / 2f,
            diedLabel.getY() - 100
        );
        respawn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // Welt neu starten: dieselbe Screen-Instanz neu setzen
                game.setScreen(new SinglePlayerGameScreen(game, worldName, operator));
            }
        });
        deathStage.addActor(respawn);

        TextButton toMenu = new TextButton(LanguageManager.INSTANCE.getText("menu.return.to.main.menu"), btnStyle);
        toMenu.setSize(300, 80);
        toMenu.setPosition(
            (deathStage.getViewport().getWorldWidth() - toMenu.getWidth()) / 2f,
            respawn.getY() - 100
        );
        toMenu.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // Welt speichern und zurück zum Menü
                WorldIO.saveWorld(worldName, worldConfig, tilemap.getLoadedChunks(), ControlledPlayer.getInstance());
                tilemap.stopChunkUpdateThread();
                ControlledPlayer.getInstance().remove();
                ControlledPlayer.reset();
                Box2DOperationManager.clear();
                dispose();
                DiscordPresence.start();
                game.setScreen(new MainMenu(game));
            }
        });
        deathStage.addActor(toMenu);
    }

    @Override
    public void render(float delta) {
        // Überprüfen, ob sich der Chat-Status geändert hat
        if (chatUI.isChatActive() != inputMultiplexer.getProcessors().contains(chatInputProcessor, true)) {
            updateInputProcessors();
        }

        // Clear the screen
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.3f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Update game state if not paused
        if (!paused) {
            mobSpawner.update(delta);
            Box2DOperationManager.processOperations();
            physicsWorld.step(delta, 6, 2);

            player.act(delta);
            stage.act(delta);
            uiStage.act(delta);
            DiscordPresence.updateToIngameSP(worldName);
        }

        // Update the camera position to follow the player
        camera.position.set(
            player.getX() + player.getWidth() / 2,
            player.getY() + player.getHeight() / 2,
            0
        );
        camera.zoom = 1.0f;
        camera.update();

        // Draw background
        backgroundStage.draw();

        // Draw the world and player
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        tilemap.render(batch);
        player.draw(batch);
        batch.end();

        // Debug-Renderer optional anzeigen, um zu sehen, ob Box2D-Bodies vorhanden sind
        if (debugOverlayVisible) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            debugRenderer.render(physicsWorld, camera.combined);
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }

        // Andere Spielelemente zeichnen
        stage.draw();

        // WICHTIG: Lichtsystem NACH allem anderen rendern
        // damit es wie in Terraria über allem liegt
        if (!paused) {
            //lightSystem.updateAndRender(camera);
        }
        // UI-Elemente zeichnen
        uiStage.draw();

        if (!isDead) {
            int currentHealth = (int) ControlledPlayer.getInstance().getHealthStatus().getCurrent();
            if (currentHealth <= 0) {
                isDead = true;
                updateInputProcessors(); // Input-Prozessoren für Todes-Status aktualisieren
            }
        }

        if (isDead) {
            // Schwarze halbtransparente Fläche
            Gdx.gl.glEnable(GL20.GL_BLEND);
            batch.begin();
            batch.draw(blackOverlayTexture, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            batch.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);

            deathStage.act(delta);
            deathStage.draw();
            return; // nichts weiter zeichnen
        }

        // Wenn pausiert, Pause-Overlay zeichnen
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
        // Überschreiben, um möglicherweise nötige Aufräumarbeiten durchzuführen
        // wenn der Screen versteckt wird (aber nicht disposed)
    }

    @Override
    public void dispose() {
        try {
            // Speicher den Spielstand beim Beenden
            if (worldName != null && worldConfig != null && tilemap != null && tilemap.getLoadedChunks() != null) {
                WorldIO.saveWorld(worldName, worldConfig, tilemap.getLoadedChunks(), ControlledPlayer.getInstance());
            }

            // Beende Thread sicher
            if (tilemap != null) {
                tilemap.stopChunkUpdateThread();
            }

            // Rufe die übergeordnete dispose-Methode auf
            super.dispose();

            // Überprüfe und räume jede Ressource separat auf
            if (tilemap != null) {
                tilemap.dispose();
                tilemap = null;
            }

            if (whiteTexture != null) {
                whiteTexture.dispose();
                whiteTexture = null;
            }

            if (pauseButtonTexture != null) {
                pauseButtonTexture.dispose();
                pauseButtonTexture = null;
            }

            if (debugRenderer != null) {
                debugRenderer.dispose();
                debugRenderer = null;
            }
            if (lightSystem != null) {
                lightSystem.dispose();
                lightSystem = null;
            }

            // UI-Komponenten nicht versuchen zu dispose(), da die Methode nicht existiert
            // Stattdessen nur die Referenzen auf null setzen
            chatUI = null;
            inventoryUI = null;
            healthUI = null;
            energyUI = null;
            debugOverlay = null;

            // Weitere Referenzen auf null setzen
            player = null;
            worldConfig = null;
            playerData = null;

            // System.gc() aufrufen, um Garbage Collection zu ermutigen
            System.gc();

        } catch (Exception e) {
            // Fehler beim Aufräumen protokollieren
            System.err.println("Error during dispose: " + e.getMessage());
            if (Gdx.app != null) {
                Gdx.app.error("SinglePlayerGameScreen", "Error during dispose", e);
            }
        }
    }
}
