package at.peckventure.chat.commands;

import at.peckventure.Globals;
import at.peckventure.entities.Player;
import at.peckventure.entities.mob.MobRegistry;

public class SummonCommand extends Command
{
    public SummonCommand()
    {
        super("summon");
    }

    @Override
    public String execute(String[] args, Player executor)
    {
        if(!executor.isOperator()) return "You do not have permission to perform this command!";
        // Command usage explanation
        if (args.length < 1 || args.length > 4)
        {
            return "Usage: /summon <entity> [x] [y] [amount]";
        }

        // The first argument is always the mob/entity name
        String entityName = args[0];

        // Default position to the player's position
        float positionX = executor.getX();
        float positionY = executor.getY();

        // Default amount is 1
        int amount = 1;

        // If there are more arguments, parse accordingly:
        switch (args.length)
        {
            case 2:
                // /summon <entity> <amount>
                try
                {
                    amount = Integer.parseInt(args[1]);
                }
                catch (NumberFormatException e)
                {
                    return "Invalid amount: " + args[1];
                }
                break;

            case 3:
                // /summon <entity> <x> <y>
                try
                {
                    positionX = Float.parseFloat(args[1]);
                    positionY = Float.parseFloat(args[2]);
                }
                catch (NumberFormatException e)
                {
                    return "Invalid position: " + args[1] + " " + args[2];
                }
                break;

            case 4:
                // /summon <entity> <x> <y> <amount>
                try
                {
                    positionX = Float.parseFloat(args[1]);
                    positionY = Float.parseFloat(args[2]);
                }
                catch (NumberFormatException e)
                {
                    return "Invalid position: " + args[1] + " " + args[2];
                }

                try
                {
                    amount = Integer.parseInt(args[3]);
                }
                catch (NumberFormatException e)
                {
                    return "Invalid amount: " + args[3];
                }
                break;

            // case 1 means no extra parsing is required
        }

        // Check if the entity is valid
        if (MobRegistry.isRegistered(entityName))
        {
            // Create the specified mob(s)
            for (int i = 0; i < amount; i++)
            {
                MobRegistry.createMob(entityName, Globals.physicsWorld, positionX, positionY + i * 10);
            }
            return "Summoning " + amount + " " + entityName + "(s) at " + positionX + " " + positionY;
        }
        else
        {
            return "Mob " + entityName + " not found";
        }
    }
}
