package at.peckventure.chat.commands;

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

    public abstract String execute(String[] args, Player executor);
}
