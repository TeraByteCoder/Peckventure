package at.peckventure.chat;

import at.peckventure.chat.commands.*;
import at.peckventure.entities.Player;

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

    public String executeCommand(String input, Player executor)
    {
        String[] parts = input.split(" ");
        if (parts.length == 0) return "No command entered";
        Command cmd = commands.get(parts[0].toLowerCase());
        if (cmd != null)
        {
            String[] args = Arrays.copyOfRange(parts, 1, parts.length);
             return cmd.execute(args, executor);
        } else
        {
            return "Unknown command: " + parts[0];
        }
    }

    static
    {
        registerCommand(new PrintCommand());
        registerCommand(new GiveCommand());
        registerCommand(new SummonCommand());
        registerCommand(new TeleportCommand());
        registerCommand(new HealCommand());
        registerCommand(new DamageCommand());
        registerCommand(new EffectCommand());
    }

    public static void init()
    {

    }
}
