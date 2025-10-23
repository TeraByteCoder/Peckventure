package at.peckventure;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

public class SpriteSheetLoader {

    /**
     * Lädt eine Zeile (Animation) aus dem Sprite-Sheet.
     *
     * @param path         Pfad zur Bilddatei
     * @param totalCols    Maximale Anzahl der Spalten im gesamten Sheet
     * @param totalRows    Maximale Anzahl der Zeilen im gesamten Sheet
     * @param rowIndex     Index der Zeile, die geladen werden soll (0-basiert)
     * @param frameDuration Dauer eines Frames in Sekunden
     * @param frameCountOpt Optional: tatsächliche Anzahl Frames in dieser Zeile
     *                      (wenn du weniger als totalCols hast)
     * @return Loopende Animation mit den gewählten Frames
     */
    public static Animation<TextureRegion> loadRow(
        String path,
        int totalCols,
        int totalRows,
        int rowIndex,
        float frameDuration,
        int... frameCountOpt) {

        if (Gdx.gl == null) {
            return null;
        }

        // Textur laden und in Regionen splitten
        Texture sheet = new Texture(path);
        int cellW = sheet.getWidth()  / totalCols;
        int cellH = sheet.getHeight() / totalRows;
        TextureRegion[][] tmp = TextureRegion.split(sheet, cellW, cellH);

        // Bestimme, wie viele Frames wir wirklich aus dieser Zeile nehmen
        int frameCount = (frameCountOpt.length > 0)
            ? frameCountOpt[0]
            : totalCols;

        // Frames sammeln
        Array<TextureRegion> frames = new Array<>(frameCount);
        for (int col = 0; col < frameCount; col++) {
            frames.add(tmp[rowIndex][col]);
        }

        // Animation erstellen und zurückgeben
        return new Animation<>(frameDuration, frames, Animation.PlayMode.LOOP);
    }
}
