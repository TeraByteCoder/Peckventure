package at.peckventure;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public enum Textures {
    // Static textures
    DIRT("textures/blocks/dirt.png", false),
    GRASS_BLOCK("textures/blocks/grass_block.png", false),
    GRASSRAMPLEFT("textures/blocks/grass_ramp_left.png", false),
    GRASSRAMPRIGHT("textures/blocks/grass_ramp_right.png", false),
    BEETLE("textures/criters/garden_foliage_beetle_idle.png", false),
    INVENTORY_SLOT("textures/inventory_slot.png", false),
    TEST_ITEM("textures/items/test_item.png", false),
    SPEED_POTION("textures/items/speed_potion.png", false),
    WOOD("textures/items/wood.png", false),
    SPRUCE_LOG("textures/blocks/log_spruce.png", false),
    SPRUCE_LEAVES("textures/blocks/leaves_spruce.png", false),
    PHYTON("textures/mobs/cobra.png", false),
    FOX("textures/mobs/cobra.png", false),

    // Animated textures
    PHYTON_IDLE("textures/mobs/cobra.png", true, 8, 5, 0, 0.2f),
    PHYTON_MOVING("textures/mobs/cobra.png", true, 8, 5, 1, 0.2f),
    PHYTON_ATTACKING("textures/mobs/cobra.png", true, 11, 5, 2, 0.2f),
    PHYTON_DAMAGE("textures/mobs/cobra.png", true, 4, 5, 3, 0.2f),

    PHYTON_DYING("textures/mobs/cobra.png", true, 6, 5, 4, 0.2f),


    FOX_IDLE("textures/mobs/fox.png", true, 8, 5, 0, 0.2f),
    FOX_MOVING("textures/mobs/fox.png", true, 14, 7, 2, 0.3f),
    FOX_ATTACKING("textures/mobs/fox.png", true, 6, 5, 2, 0.2f),
    FOX_DAMAGE("textures/mobs/fox.png", true, 4, 5, 3, 0.2f),

    FOX_DYING("textures/mobs/cobra.png", true, 6, 5, 4, 0.2f),

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

    // Constructor for static textures
    Textures(String texturePath, boolean isAnimated) {
        this.texturePath = texturePath;
        this.isAnimated = isAnimated;
        this.totalCols = 1;
        this.totalRows = 1;
        this.rowIndex = 0;
        this.frameDuration = 0;
    }

    // Constructor for animated textures
    Textures(String texturePath, boolean isAnimated, int totalCols, int totalRows,
             int rowIndex, float frameDuration) {
        this.texturePath = texturePath;
        this.isAnimated = isAnimated;
        this.totalCols = totalCols;
        this.totalRows = totalRows;
        this.rowIndex = rowIndex;
        this.frameDuration = frameDuration;
    }

    public void loadTexture() {
        // Only load if graphics environment exists
        if (Gdx.graphics == null) return;

        if (isAnimated) {
            // Load animation
            animation = SpriteSheetLoader.loadRow(
                texturePath, totalCols, totalRows, rowIndex, frameDuration);
        } else {
            // Load static texture
            texture = new Texture(Gdx.files.internal(texturePath));
        }
    }

    public Texture getTexture() {
        if (Gdx.graphics == null || isAnimated) {
            return null;
        }
        return texture;
    }

    public Animation<TextureRegion> getAnimation() {
        if (Gdx.graphics == null || !isAnimated) {
            return null;
        }
        return animation;
    }

    public boolean isAnimated() {
        return isAnimated;
    }

    public void dispose() {
        if (texture != null) {
            texture.dispose();
        }
        // Note: We don't need to explicitly dispose Animation objects
        // as they just reference TextureRegions from a Texture
    }

    static {
        // Only load if graphics are available
        if (Gdx.gl != null) {
            for (Textures t : values()) {
                t.loadTexture();
            }
        }
    }

    public static void init() {
        // Can remain empty - static block handles loading when possible
    }
}
