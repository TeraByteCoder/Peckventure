package at.peckventure.inventory;


import java.util.HashMap;
import java.util.Map;

import at.peckventure.Textures;
import at.peckventure.inventory.item.Sword;

public class ItemRegistry
{

    public interface ItemFactory
    {
        Sword create();
    }

    private static final Map<String, ItemFactory> registry = new HashMap<>();

    public static void register(String id, ItemFactory factory)
    {
        registry.put(id, factory);
    }

    public static Sword createItem(String id)
    {
        ItemFactory factory = registry.get(id);
        return (factory != null) ? factory.create() : null;
    }

    public static boolean contains(String id)
    {
        return registry.containsKey(id);
    }

    static
    {
        ItemRegistry.register("sword", () ->
            new Sword("sword", "Schwert", Textures.TEST_ITEM.getTexture())
        );
    }

    public static void init()
    {

    }
}
