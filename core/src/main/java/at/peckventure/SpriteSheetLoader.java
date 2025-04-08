package at.peckventure;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

public class SpriteSheetLoader {

    public static Animation<TextureRegion> loadRow(String path, int totalCols, int totalRows, int rowIndex, float frameDuration) {
        Texture sheet = new Texture(path);
        TextureRegion[][] tmp = TextureRegion.split(sheet, sheet.getWidth() / totalCols, sheet.getHeight() / totalRows);

        Array<TextureRegion> frames = new Array<>();
        for (int col = 0; col < totalCols; col++) {
            frames.add(tmp[rowIndex][col]);
        }

        return new Animation<>(frameDuration, frames, Animation.PlayMode.LOOP);
    }
}
