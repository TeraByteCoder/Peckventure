package at.peckventure.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RegionManager {
    private FileHandle regionsDir;
    private Map<String, RegionFile> regionCache = new HashMap<>();
    public static final int REGION_SIZE = RegionFile.CHUNKS_PER_REGION; // Anzahl Chunks pro Region in einer Richtung

    public RegionManager(FileHandle worldDir) {
        regionsDir = worldDir.child("regions");
        if (!regionsDir.exists()) {
            regionsDir.mkdirs();
        }
    }

    // Ermittelt (und cached) die Region-Datei für gegebene Region-Koordinaten
    public RegionFile getRegionFile(int regionX, int regionY) {
        String key = regionX + "_" + regionY;
        if (regionCache.containsKey(key)) {
            return regionCache.get(key);
        }
        FileHandle regionFileHandle = regionsDir.child("r." + regionX + "." + regionY + ".pvr");
        try {
            RegionFile regionFile = new RegionFile(regionFileHandle.file());
            regionCache.put(key, regionFile);
            return regionFile;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void closeAll() {
        for (RegionFile rf : regionCache.values()) {
            try {
                rf.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        regionCache.clear();
    }
}
