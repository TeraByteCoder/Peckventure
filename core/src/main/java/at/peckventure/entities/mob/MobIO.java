package at.peckventure.entities.mob;

import com.badlogic.gdx.physics.box2d.World;
import java.io.*;

public class MobIO {
    public static byte[] serialize(Mob mob) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DataOutputStream dos = new DataOutputStream(baos)) {
            int id = MobRegistry.getMobId(mob);
            dos.writeInt(id);
            dos.writeFloat(mob.getX());
            dos.writeFloat(mob.getY());
            // Hier können weitere Parameter ergänzt werden
        } catch (IOException e) {
            e.printStackTrace();
        }
        return baos.toByteArray();
    }

    public static Mob deserialize(byte[] data, World world) {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        try (DataInputStream dis = new DataInputStream(bais)) {
            int id = dis.readInt();
            float x = dis.readFloat();
            float y = dis.readFloat();
            return MobRegistry.createMob(id, world, x, y);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
