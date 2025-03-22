package at.peckventure.inventory.item;

import com.badlogic.gdx.graphics.Texture;

/**
 * Ein Item mit ID, Name, Textur und Stack-Logik (max. 32).
 */
public class Sword extends Item
{
    public Sword(String id, String name, Texture texture)
    {
        super(id, name, texture);
    }

}
