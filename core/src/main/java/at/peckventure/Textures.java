package at.peckventure;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

public enum Textures {
    // Static textures
    DIRT("textures/blocks/dirt.png", false),
    GRASS_BLOCK("textures/blocks/grass_block.png", false),
    GRASSRAMPLEFT("textures/blocks/grass_ramp_left.png", false),
    GRASSRAMPRIGHT("textures/blocks/grass_ramp_right.png", false),
    GRASSPATCHLEFT("textures/blocks/grass_patch_left.png", false),
    GRASSPATCHRIGHT("textures/blocks/grass_patch_right.png", false),
    BEETLE("textures/criters/garden_foliage_beetle_idle.png", false),
    INVENTORY_SLOT("textures/inventory_slot.png", false),
    TEST_ITEM("textures/items/test_item.png", false),
    SPEED_POTION("textures/items/speed_potion.png", false),
    WOOD("textures/items/wood.png", false),
    SPRUCE_LOG("textures/blocks/log_spruce.png", false),
    SPRUCE_LEAVES("textures/blocks/leaves_spruce.png", false),
    PHYTON("textures/mobs/cobra.png", false),
    FOX("textures/mobs/fox.png", false),

    // Animated textures
    PHYTON_IDLE("textures/mobs/cobra.png", true, 8, 5, 0, 0.2f),
    PHYTON_MOVING("textures/mobs/cobra.png", true, 8, 5, 1, 0.2f),
    PHYTON_ATTACKING("textures/mobs/cobra.png", true, 8, 5, 2, 0.2f, 6),
    PHYTON_DAMAGE("textures/mobs/cobra.png", true, 8, 5, 3, 0.2f, 4),
    PHYTON_DYING("textures/mobs/cobra.png", true, 8, 5, 4, 0.2f, 6),

    FOX_IDLE("textures/mobs/fox.png", true, 8, 5, 0, 0.2f),
    FOX_MOVING("textures/mobs/fox.png", true, 14, 7, 2, 0.3f),
    FOX_ATTACKING("textures/mobs/fox.png", true, 6, 5, 2, 0.2f),
    FOX_DAMAGE("textures/mobs/fox.png", true, 4, 5, 3, 0.2f),
    FOX_DYING("textures/mobs/fox.png", true, 6, 5, 4, 0.2f),

    WOODPECKER_FLYING("textures/woodpecker/woodpecker.png", true, 4, 4, 1, 0.1f),
    WOODPECKER_IDLE("textures/woodpecker/woodpecker_idle.png", false),


    ;
    private Texture texture;
    private Animation<TextureRegion> animation;
    private final String texturePath;
    private final boolean isAnimated;

    // Animation properties
    private final int totalCols;
    private final int totalRows;
    private final int rowIndex;
    private final float frameDuration;
    private final int numberOfFrames;

    // Constructor for static textures
    Textures(String texturePath, boolean isAnimated) {
        this.texturePath = texturePath;
        this.isAnimated = isAnimated;
        this.totalCols = 1;
        this.totalRows = 1;
        this.rowIndex = 0;
        this.frameDuration = 0;
        this.numberOfFrames = 1;
    }

    // Constructor for animated textures
    Textures(
        String texturePath,
        boolean isAnimated,
        int totalCols,
        int totalRows,
        int rowIndex,
        float frameDuration,
        int... numberOfFramesOpt
    ) {
        this.texturePath = texturePath;
        this.isAnimated = isAnimated;
        this.totalCols = totalCols;
        this.totalRows = totalRows;
        this.rowIndex = rowIndex;
        this.frameDuration = frameDuration;
        // If an explicit frame count is given, use it; otherwise use totalCols
        this.numberOfFrames = (numberOfFramesOpt != null && numberOfFramesOpt.length > 0)
            ? numberOfFramesOpt[0]
            : totalCols;
    }

    public void loadTexture() {
        if (Gdx.graphics == null) return;

        if (isAnimated) {
            // Load animation with optional frame count
            animation = SpriteSheetLoader.loadRow(
                texturePath,
                totalCols,
                totalRows,
                rowIndex,
                frameDuration,
                numberOfFrames
            );
        } else {
            texture = new Texture(Gdx.files.internal(texturePath));
        }
    }

    public Texture getTexture() {
        if (Gdx.graphics == null || isAnimated) return null;
        return texture;
    }

    public Animation<TextureRegion> getAnimation() {
        if (Gdx.graphics == null || !isAnimated) return null;
        return animation;
    }

    public boolean isAnimated() {
        return isAnimated;
    }

    public void dispose() {
        if (texture != null) texture.dispose();
    }

    static {
        if (Gdx.gl != null) {
            for (Textures t : values()) {
                t.loadTexture();
            }
        }
    }

    public static void init() {
        // Static block handles loading
    }
}
