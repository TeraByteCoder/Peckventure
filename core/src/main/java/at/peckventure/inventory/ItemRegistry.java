package at.peckventure.inventory;


import java.util.HashMap;
import java.util.Map;

import at.peckventure.Textures;
import at.peckventure.inventory.item.Item;
import at.peckventure.inventory.item.SpeedPotion;
import at.peckventure.inventory.item.Wood;

public class ItemRegistry
{

    public interface ItemFactory
    {
        Item create();
    }

    private static final Map<String, ItemFactory> registry = new HashMap<>();

    public static void register(String id, ItemFactory factory)
    {
        registry.put(id, factory);
    }

    public static Item createItem(String id)
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
        ItemRegistry.register("wood", () ->
            new Wood("wood", "Wood", Textures.WOOD.getTexture())
        );
        ItemRegistry.register("speed_potion", () ->
            new SpeedPotion("speed_potion", "Trank der Geschwindigkeit", Textures.SPEED_POTION.getTexture()));
    }

    public static void init()
    {

    }
}
