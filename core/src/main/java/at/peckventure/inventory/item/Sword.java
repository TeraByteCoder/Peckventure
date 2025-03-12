package at.peckventure.inventory.item;

import com.badlogic.gdx.graphics.Texture;

/**
 * Ein Item mit ID, Name, Textur und Stack-Logik (max. 32).
 */
public class Sword
{
    private final String id;
    private final String name;
    private final Texture texture;

    private int stackSize = 1;           // aktuelle Anzahl in diesem Stack
    public static final int MAX_STACK_SIZE = 32; // globales Stacklimit

    public Sword(String id, String name, Texture texture)
    {
        this.id = id;
        this.name = name;
        this.texture = texture;
    }

    public String getId()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public Texture getTexture()
    {
        return texture;
    }

    public int getStackSize()
    {
        return stackSize;
    }

    public void setStackSize(int size)
    {
        // Stackgröße darf das Limit nicht überschreiten
        this.stackSize = Math.min(size, MAX_STACK_SIZE);
    }

    /**
     * Erhöht die Stackgröße um amount, begrenzt durch MAX_STACK_SIZE.
     *
     * @return Wie viele tatsächlich hinzugefügt wurden.
     */
    public int incrementStack(int amount)
    {
        int oldSize = stackSize;
        stackSize = Math.min(stackSize + amount, MAX_STACK_SIZE);
        return stackSize - oldSize; // tatsächlich hinzugefügt
    }
}
