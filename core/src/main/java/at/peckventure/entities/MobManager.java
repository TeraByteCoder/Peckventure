package at.peckventure.entities;

import at.peckventure.entities.mob.Mob;
import at.peckventure.entities.MobFile;
import at.peckventure.entities.mob.MobIO;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.physics.box2d.World;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class MobManager {
    private final MobFile mobFile;
    private final List<Mob> activeMobs = new CopyOnWriteArrayList<>();
    private final List<Mob> inactiveMobs = new CopyOnWriteArrayList<>();

    public MobManager(FileHandle worldDir, World world) {
        MobFile temp = null;
        FileHandle entityFile = worldDir.child("entitydata.psv");
        try {
            temp = new MobFile(entityFile.file());
            for (int i = 0; i < MobFile.MAX_MOBS; i++) {
                byte[] data = temp.readMob(i);
                if (data != null) {
                    Mob mob = MobIO.deserialize(data, world);
                    if (mob != null) inactiveMobs.add(mob);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        mobFile = temp;
    }


    public List<Mob> getActiveMobs() {
        return activeMobs;
    }

    public List<Mob> getInactiveMobs() {
        return inactiveMobs;
    }

    public synchronized void addMob(Mob mob) {
        activeMobs.add(mob);
        saveMobsToFile();
    }

    public synchronized void unloadMob(Mob mob) {
        if (activeMobs.remove(mob)) {
            inactiveMobs.add(mob);
            saveMobsToFile();
        }
    }

    public synchronized void loadMob(Mob mob) {
        if (inactiveMobs.remove(mob)) {
            activeMobs.add(mob);
            saveMobsToFile();
        }
    }

    public synchronized void saveMobsToFile() {
        List<byte[]> dataList = new ArrayList<>();
        for (Mob mob : activeMobs) {
            dataList.add(MobIO.serialize(mob));
        }
        for (Mob mob : inactiveMobs) {
            dataList.add(MobIO.serialize(mob));
        }
        try {
            mobFile.saveAllMobs(dataList);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void close() {
        try {
            mobFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
