package at.peckventure.world;

import at.peckventure.entities.Player;
import at.peckventure.world.block.Block;
import at.peckventure.world.generator.WorldGenerator;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.StretchViewport;

public class GameScreen implements Screen {
    private Game game;
    private String worldName;
    private OrthographicCamera camera;
    private SpriteBatch batch;
    private Player player;
    private Stage stage;
    private InfiniteTilemap tilemap;
    private World physicsWorld;
    private WorldConfig worldConfig;

    public GameScreen(Game game, String worldName) {
        this.game = game;
        this.worldName = worldName;
        // Erstelle die physikalische Welt (Box2D)
        this.physicsWorld = new World(new Vector2(0, -19.81f), true);
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth() / 2f, Gdx.graphics.getHeight() / 2f);

        // Laden der Weltkonfiguration und der bereits gespeicherten Chunks
        WorldIO.LoadedWorld loaded = WorldIO.loadWorld(worldName, physicsWorld);
        worldConfig = loaded.getConfig();

        // Erzeuge den WorldGenerator mit dem geladenen Seed
        WorldGenerator generator = new WorldGenerator(worldConfig.getSeed(), physicsWorld);

        // Erstelle den RegionManager aus dem Weltordner
        FileHandle worldDir = Gdx.files.absolute(at.peckventure.Const.savesDir + "/" + worldName);
        RegionManager regionManager = new RegionManager(worldDir);

        // Erzeuge die InfiniteTilemap – übergebe die vorab geladenen Chunks und den RegionManager
        tilemap = new InfiniteTilemap(physicsWorld, generator, loaded.getLoadedChunks(), regionManager);

        // Bestimme den Spawnpunkt anhand der gespeicherten Spielerposition, falls vorhanden
        float spawnX = worldConfig.getPlayerX();
        float spawnY = worldConfig.getPlayerY();
        if (spawnX == 0 && spawnY == 0) {
            // Falls keine Spielerposition gespeichert wurde, generiere einen Standard-Spawnpunkt.
            spawnX = 0;
            int terrainHeight = generator.getHeight((int) spawnX);
            spawnY = terrainHeight * Block.BLOCK_SIZE + 400;
        }
        player = new Player(physicsWorld, spawnX, spawnY);

        // Erstelle eine Stage, die den Spieler (und evtl. weitere Actors) verwaltet
        stage = new Stage(new StretchViewport(Gdx.graphics.getWidth() / 2f, Gdx.graphics.getHeight() / 2f, camera));
        stage.addActor(player);

        // Starte den Hintergrund-Thread, der die Chunk-Liste aktualisiert
        tilemap.startChunkUpdateThread(player);
    }

    @Override
    public void render(float delta) {
        Box2DOperationManager.processOperations();
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);

        // Box2D Schritt – hier wird die Physik simuliert
        physicsWorld.step(delta, 6, 2);

        // Aktualisiere die Kamera so, dass sie dem Spieler folgt
        camera.position.set(player.getX() + player.getWidth() / 2, player.getY() + player.getHeight() / 2, 0);
        camera.zoom = 2.0f;
        camera.update();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        // Der Render-Thread zeichnet einfach alle aktuell geladenen Chunks,
        // Änderungen in der Liste werden automatisch beim nächsten Frame sichtbar.
        tilemap.render(batch);
        player.draw(batch);
        batch.end();
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        camera.setToOrtho(false, width / 2f, height / 2f);
    }

    @Override
    public void pause() {
        // Speichere beim Pausieren den aktuellen Zustand (alle geladenen Chunks)
        WorldIO.saveWorld(worldName, worldConfig, tilemap.getLoadedChunks(), player);
    }

    @Override
    public void resume() {
        // Zusätzliche Resumes-Aktionen falls nötig.
    }

    @Override
    public void hide() {
        // Freigeben zusätzlicher Ressourcen, falls nötig.
    }

    @Override
    public void dispose() {
        // Zuerst alle grafischen und physikalischen Ressourcen freigeben
        batch.dispose();
        stage.dispose();
        physicsWorld.dispose();
        // Speichere zuletzt alle noch im Speicher befindlichen Chunks
        WorldIO.saveWorld(worldName, worldConfig, tilemap.getLoadedChunks(), player);
        // Wichtiger Hinweis: Schalte den Chunk-Update-Thread ab, sodass keine Hintergrundthreads mehr laufen.
        tilemap.dispose();
    }
}
