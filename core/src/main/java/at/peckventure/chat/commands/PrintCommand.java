package at.peckventure.chat.commands;

import at.peckventure.entities.Player;

public class PrintCommand extends Command
{
    public PrintCommand()
    {
        super("print");
    }

    @Override
    public String execute(String[] args,  Player executor)
    {
        String output = String.join(" ", args);
        System.out.println(output);
        return "Console: " + output;
    }
}
