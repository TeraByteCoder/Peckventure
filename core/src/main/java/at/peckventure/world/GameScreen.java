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
    public void show()
    {
        batch = new SpriteBatch();
        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth() / 2f, Gdx.graphics.getHeight() / 2f);
        stage = new Stage(new StretchViewport(Gdx.graphics.getWidth() / 2f, Gdx.graphics.getHeight() / 2f, camera));
        uiStage = new Stage(new ScreenViewport());

        // Chat zur UI-Stage hinzufügen (statt zur game stage)
        chatUI = new ChatUI(uiStage);

        // InputMultiplexer für beide Stages
        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(uiStage);
        multiplexer.addProcessor(stage);
        multiplexer.addProcessor(new InputAdapter()
        {
            @Override
            public boolean keyDown(int keycode)
            {
                // Wenn T gedrückt wird, Chat ein/aus schalten
                if (keycode == Input.Keys.T)
                {
                    chatUI.toggleChat();
                    // return true => Event wird "verbraucht" und NICHT weitergeleitet
                    return true;
                }
                return false;
            }
        });
        Gdx.input.setInputProcessor(multiplexer);


        // Welt laden
        WorldIO.LoadedWorld loaded = WorldIO.loadWorld(worldName, physicsWorld);
        worldConfig = loaded.getConfig();
        WorldGenerator generator = new WorldGenerator(worldConfig.getSeed(), physicsWorld);
        FileHandle worldDir = Gdx.files.absolute(at.peckventure.Const.savesDir + "/" + worldName);
        RegionManager regionManager = new RegionManager(worldDir);
        tilemap = new InfiniteTilemap(physicsWorld, generator, loaded.getLoadedChunks(), regionManager);

        float spawnX = worldConfig.getPlayerX();
        float spawnY = worldConfig.getPlayerY();
        if (spawnX == 0 && spawnY == 0)
        {
            spawnX = 0;
            int terrainHeight = generator.getHeight((int) spawnX);
            spawnY = terrainHeight * Block.BLOCK_SIZE + 400;
        }
        player = new Player(physicsWorld, spawnX, spawnY);
        stage.addActor(player);

        // Inventar-UI erstellen
        inventoryUI = new InventoryUI(uiStage);
        Globals.inventoryUI = inventoryUI;
        // Falls Inventardaten gespeichert sind, diese laden; ansonsten als Test ein Item hinzufügen
        if (!worldConfig.getInventoryHotbar().isEmpty() && !worldConfig.getInventoryMain().isEmpty())
        {
            inventoryUI.getInventory().deserialize(worldConfig.getInventoryHotbar(), worldConfig.getInventoryMain());
        }


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
