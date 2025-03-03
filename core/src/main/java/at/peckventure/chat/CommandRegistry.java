package at.peckventure.chat;

import at.peckventure.chat.commands.Command;
import at.peckventure.chat.commands.GiveCommand;
import at.peckventure.chat.commands.PrintCommand;

import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;

public class CommandRegistry
{
    private static final Map<String, Command> commands = new HashMap<>();

    public static void registerCommand(Command command)
    {
        commands.put(command.getName().toLowerCase(), command);
    }

    public void executeCommand(String input, ChatUI chatUI)
    {
        String[] parts = input.split(" ");
        if (parts.length == 0) return;
        Command cmd = commands.get(parts[0].toLowerCase());
        if (cmd != null)
        {
            String[] args = Arrays.copyOfRange(parts, 1, parts.length);
            cmd.execute(args, chatUI);
        } else
        {
            chatUI.addMessage("Unknown command: " + parts[0]);
        }
    }

    static
    {
        registerCommand(new PrintCommand());
        registerCommand(new GiveCommand());
    }

    public static void init()
    {

    }
}
