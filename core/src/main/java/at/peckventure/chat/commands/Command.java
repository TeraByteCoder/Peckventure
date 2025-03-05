package at.peckventure.chat.commands;

import at.peckventure.chat.ChatUI;
import at.peckventure.entities.Player;

public abstract class Command
{
    private final String name;

    public Command(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public abstract void execute(String[] args, ChatUI chatUI, Player executor);
}
