package at.peckventure.world;

import com.badlogic.gdx.files.FileHandle;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MobRegionManager {
    private final FileHandle mobRegionsDir;
    private final Map<String, MobRegionFile> mobRegionCache = new HashMap<>();
    public static final int REGION_SIZE = RegionFile.CHUNKS_PER_REGION;

    public MobRegionManager(FileHandle worldDir) {
        mobRegionsDir = worldDir.child("mob_regions");
        if (!mobRegionsDir.exists()) {
            mobRegionsDir.mkdirs();
        }
    }

    public MobRegionFile getMobRegionFile(int regionX, int regionY) {
        String key = regionX + "_" + regionY;
        if (mobRegionCache.containsKey(key)) {
            return mobRegionCache.get(key);
        }
        FileHandle regionFileHandle = mobRegionsDir.child("m." + regionX + "." + regionY + ".pvr");
        try {
            MobRegionFile mobRegionFile = new MobRegionFile(regionFileHandle.file());
            mobRegionCache.put(key, mobRegionFile);
            return mobRegionFile;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void closeAll() {
        for (MobRegionFile mrf : mobRegionCache.values()) {
            try {
                mrf.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        mobRegionCache.clear();
    }
}
