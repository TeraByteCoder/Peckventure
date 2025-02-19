package at.peckventure.world.block;

import com.badlogic.gdx.physics.box2d.World;
import java.util.HashMap;
import java.util.Map;

public class BlockRegistry {
    private static final Map<Integer, BlockCreator> registry = new HashMap<>();
    // Neue Map, die Blockklassen ihren IDs zuordnet
    private static final Map<Class<? extends Block>, Integer> classToId = new HashMap<>();

    /**
     * Registriert einen neuen Blocktyp unter der gegebenen ID und speichert zusätzlich die Blockklasse.
     *
     * @param id      Die eindeutige Block-ID.
     * @param clazz   Die Klasse des Blocks (z. B. DirtBlock.class).
     * @param creator Die Factory, die den Block erzeugt.
     */
    public static void registerBlock(int id, Class<? extends Block> clazz, BlockCreator creator) {
        registry.put(id, creator);
        classToId.put(clazz, id);
    }

    /**
     * Erzeugt einen Block anhand der registrierten Factory.
     *
     * @param id     Die Block-ID.
     * @param world  Die Box2D-World.
     * @param worldX Die X-Position in Weltkoordinaten.
     * @param worldY Die Y-Position in Weltkoordinaten.
     * @param args   Optionale zusätzliche Parameter.
     * @return Den erzeugten Block oder null, wenn kein Block unter dieser ID registriert ist.
     */
    public static Block createBlock(int id, World world, int worldX, int worldY, Object... args) {
        BlockCreator creator = registry.get(id);
        return (creator != null) ? creator.create(world, worldX, worldY, args) : null;
    }

    /**
     * Ermittelt die ID eines Blocks anhand seiner Klasse.
     * Falls der Blocktyp nicht registriert ist, wird 0 zurückgegeben.
     *
     * @param block Der Block.
     * @return Die zugewiesene ID oder 0, falls nicht gefunden.
     */
    public static int getBlockId(Block block) {

        // Spezielle Unterscheidung für GrassRamp:
        if (block instanceof GrassRamp) {
            GrassRamp ramp = (GrassRamp) block;
            if (ramp.isLeftRamp()) {
                return BlockRegistration.GRASSRAMPLEFT_ID;
            } else {
                return BlockRegistration.GRASSRAMPRIGHT_ID;
            }
        }

        // Standardfall für alle anderen Blocktypen
        for (Map.Entry<Class<? extends Block>, Integer> entry : classToId.entrySet()) {
            if (entry.getKey().isAssignableFrom(block.getClass())) {
                return entry.getValue();
            }
        }
        return 0;
    }



}
