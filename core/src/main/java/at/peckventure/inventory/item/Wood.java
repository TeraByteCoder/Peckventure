package at.peckventure.inventory.item;

import com.badlogic.gdx.graphics.Texture;

/**
 * Ein Item mit ID, Name, Textur und Stack-Logik (max. 32).
 */
public class Wood extends Item
{
    public Wood(String id, String name, Texture texture)
    {
        super(id, name, texture);
    }

}
