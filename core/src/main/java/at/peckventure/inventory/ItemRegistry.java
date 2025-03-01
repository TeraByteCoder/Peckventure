package at.peckventure.inventory;


import java.util.HashMap;
import java.util.Map;
import at.peckventure.inventory.item.Item;
public class ItemRegistry {

    public interface ItemFactory {
        Item create();
    }

    private static final Map<String, ItemFactory> registry = new HashMap<>();

    public static void register(String id, ItemFactory factory) {
        registry.put(id, factory);
    }

    public static Item createItem(String id) {
        ItemFactory factory = registry.get(id);
        return (factory != null) ? factory.create() : null;
    }

    public static boolean contains(String id) {
        return registry.containsKey(id);
    }
}
