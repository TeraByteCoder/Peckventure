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
        // Setze die Kamera so, dass sie etwa die Hälfte der Bildschirmgröße abbildet
        camera.setToOrtho(false, Gdx.graphics.getWidth() / 2f, Gdx.graphics.getHeight() / 2f);

        // Laden der Weltkonfiguration und der bereits gespeicherten Chunks
        WorldIO.LoadedWorld loaded = WorldIO.loadWorld(worldName, physicsWorld);
        worldConfig = loaded.getConfig();

        // Erzeuge den WorldGenerator mit dem geladenen Seed
        WorldGenerator generator = new WorldGenerator(worldConfig.getSeed(), physicsWorld);

        // Erstelle den RegionManager aus dem Weltordner
        FileHandle worldDir = Gdx.files.absolute(at.peckventure.Const.savesDir + "/" + worldName);
        RegionManager regionManager = new RegionManager(worldDir);

        // Erzeuge die InfiniteTilemap und übergebe die vorab geladenen Chunks und den RegionManager
        tilemap = new InfiniteTilemap(physicsWorld, generator, loaded.getLoadedChunks(), regionManager);

        // Bestimme einen Spawnpunkt (hier beispielhaft anhand des Terrain-Generators)
        float spawnX = 500;
        int terrainHeight = generator.getHeight((int) spawnX);
        float spawnY = terrainHeight * Block.BLOCK_SIZE + 400;
        player = new Player(physicsWorld, spawnX, spawnY);

        // Erstelle eine Stage, die den Spieler (und evtl. weitere Actors) verwaltet
        stage = new Stage(new StretchViewport(Gdx.graphics.getWidth() / 2f, Gdx.graphics.getHeight() / 2f, camera));
        stage.addActor(player);
    }

    @Override
    public void render(float delta) {
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
        // Die InfiniteTilemap führt intern den asynchronen Ladevorgang durch
        tilemap.render(batch, player);
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
        // Beim Pausieren wird der aktuelle Zustand (alle geladenen Chunks) gespeichert.
        WorldIO.saveWorld(worldName, worldConfig, tilemap.getLoadedChunks());
    }

    @Override
    public void resume() {
        // Hier können zusätzliche Resumes-Aktionen erfolgen, falls nötig.
    }

    @Override
    public void hide() {
        // Falls zusätzliche Ressourcen freigegeben werden sollen, hier erledigen.
    }

    @Override
    public void dispose() {
        // Zuerst alle grafischen und physikalischen Ressourcen freigeben
        batch.dispose();
        stage.dispose();
        physicsWorld.dispose();
        // Speichere zuletzt alle noch im Speicher befindlichen Chunks
        WorldIO.saveWorld(worldName, worldConfig, tilemap.getLoadedChunks());
        // Wichtiger Hinweis: Schalte den Executor des asynchronen Chunk-Lade-Systems ab,
        // sodass keine Hintergrundthreads mehr laufen.
        tilemap.dispose();
    }
}
