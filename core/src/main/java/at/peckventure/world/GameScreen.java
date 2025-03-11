package at.peckventure.world;

import at.peckventure.Globals;
import at.peckventure.Textures;
import at.peckventure.entities.Player;
import at.peckventure.inventory.InventoryUI;
import at.peckventure.inventory.ItemRegistry;
import at.peckventure.inventory.item.Item;
import at.peckventure.world.block.Block;
import at.peckventure.world.generator.WorldGenerator;
import com.badlogic.gdx.*;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import at.peckventure.chat.ChatUI;
import at.peckventure.InputManager;

import java.io.File;

public class GameScreen implements Screen
{

    private ChatUI chatUI;
    private final Game game;
    private final String worldName;
    private OrthographicCamera camera;
    private SpriteBatch batch;
    private Player player;
    private Stage stage;
    private Stage uiStage;
    private InfiniteTilemap tilemap;
    private final World physicsWorld;
    private WorldConfig worldConfig;

    // Inventar-UI (arbeitet auf der separaten UI-Stage)
    private InventoryUI inventoryUI;

    public GameScreen(Game game, String worldName)
    {
        this.game = game;
        this.worldName = worldName;
        this.physicsWorld = new World(new Vector2(0, -19.81f), true);
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth() / 2f, Gdx.graphics.getHeight() / 2f);
        stage = new Stage(new StretchViewport(Gdx.graphics.getWidth() / 2f, Gdx.graphics.getHeight() / 2f, camera));
        Globals.gamestage = stage;
        uiStage = new Stage(new ScreenViewport());
        FileHandle worldDir = Gdx.files.absolute(at.peckventure.Const.savesDir + "/" + worldName);

        chatUI = new ChatUI(uiStage);
        InputManager.getInstance().setChatToggle(new InputManager.ChatToggle() {
            public void toggleChat() {
                chatUI.toggleChat();
            }
            public void cancelChat() {
                chatUI.cancelChat();
            }
            public boolean isChatActive() {
                return chatUI.isChatActive();
            }
        });
        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(InputManager.getInstance());
        multiplexer.addProcessor(uiStage);
        multiplexer.addProcessor(stage);
        Gdx.input.setInputProcessor(multiplexer);

        WorldIO.LoadedWorld loaded = WorldIO.loadWorld(worldName, physicsWorld);
        worldConfig = loaded.getConfig();
        WorldGenerator generator = new WorldGenerator(worldConfig.getSeed(), physicsWorld);
        RegionManager regionManager = new RegionManager(worldDir);
        MobRegionManager mobRegionManager = new MobRegionManager(worldDir);
        tilemap = new InfiniteTilemap(physicsWorld, generator, loaded.getLoadedChunks(), regionManager, mobRegionManager);

        float spawnX = worldConfig.getPlayerX();
        float spawnY = worldConfig.getPlayerY();
        if (spawnX == 0 && spawnY == 0) {
            spawnX = 0;
            int terrainHeight = generator.getHeight((int) spawnX);
            spawnY = terrainHeight * Block.BLOCK_SIZE + 400;
        }
        player = new Player(physicsWorld, spawnX, spawnY);
        stage.addActor(player);

        inventoryUI = new InventoryUI(uiStage);
        if (!worldConfig.getInventoryHotbar().isEmpty() && !worldConfig.getInventoryMain().isEmpty()) {
            inventoryUI.getInventory().deserialize(worldConfig.getInventoryHotbar(), worldConfig.getInventoryMain());
        }
        Globals.inventoryUI = inventoryUI;
        Globals.player = player;
        Globals.physicsWorld = physicsWorld;
        tilemap.startChunkUpdateThread(player);
    }


    @Override
    public void render(float delta)
    {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Box2DOperationManager.processOperations();
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        physicsWorld.step(delta, 6, 2);
        camera.position.set(player.getX() + player.getWidth() / 2, player.getY() + player.getHeight() / 2, 0);
        camera.zoom = 6.0f;
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
    }

    @Override
    public void resize(int width, int height)
    {
        stage.getViewport().update(width, height, true);
        uiStage.getViewport().update(width, height, true);
    }

    @Override
    public void pause()
    {
        WorldIO.saveWorld(worldName, worldConfig, tilemap.getLoadedChunks(), player, inventoryUI.getInventory());
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
        batch.dispose();
        stage.dispose();
        uiStage.dispose();
        physicsWorld.dispose();
        WorldIO.saveWorld(worldName, worldConfig, tilemap.getLoadedChunks(), player, inventoryUI.getInventory());
        tilemap.dispose();
    }
}
