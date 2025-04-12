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
        if(!executor.isOperator()) return "You do not have permission to perform this command!";
        String output = String.join(" ", args);
        System.out.println(output);
        return "Console: " + output;
    }
}
