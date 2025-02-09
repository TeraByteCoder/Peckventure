package at.peckventure.world;

import at.peckventure.entities.Player;
import at.peckventure.world.block.Block;
import at.peckventure.world.generator.WorldGenerator;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
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
    private World world;


    public GameScreen(Game game, String worldName) {
        this.game = game;
        this.worldName = worldName;
        this.world = new World(new Vector2(0, -9.81f), true);
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth() / 2f, Gdx.graphics.getHeight() / 2f);

        // Erzeuge die Tilemap (und somit den WorldGenerator)
        tilemap = new InfiniteTilemap(world, System.currentTimeMillis());
        WorldGenerator generator = tilemap.getWorldGenerator();

        // Definiere einen Spawn-Punkt (z.B. x = 500) und bestimme die Terrain-Höhe
        float spawnX = 500;
        int terrainHeight = generator.getHeight((int) spawnX); // Höhe in Blockeinheiten
        float spawnY = terrainHeight * Block.BLOCK_SIZE + 400; // 100 Pixel oberhalb des Terrains

        player = new Player(world,spawnX, spawnY);

        stage = new Stage(new StretchViewport(Gdx.graphics.getWidth() / 2f, Gdx.graphics.getHeight() / 2f, camera));
        stage.addActor(player);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(delta);
        world.step(delta, 6, 2);

        // Kamera folgt dem Spieler (immer zentriert)
        camera.position.set(player.getX() + player.getWidth() / 2, player.getY() + player.getHeight() / 2, 0);
        camera.zoom = 2.0f;
        camera.update();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
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
    public void pause() { }

    @Override
    public void resume() { }

    @Override
    public void hide() { }

    @Override
    public void dispose() {
        batch.dispose();
        stage.dispose();
    }
}
