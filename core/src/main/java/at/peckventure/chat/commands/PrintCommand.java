package at.peckventure.chat.commands;

import at.peckventure.chat.ChatUI;
import at.peckventure.entities.Player;

public class PrintCommand extends Command
{
    public PrintCommand()
    {
        super("print");
    }

    @Override
    public void execute(String[] args, ChatUI chatUI,  Player executor)
    {
        String output = String.join(" ", args);
        System.out.println(output);
        chatUI.addMessage("Console: " + output);
    }
}
