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
    public String execute(String[] args,  Player executor)
    {
        if (args.length < 1 || args.length > 4)
        {
           return "Usage: /summon <entity> <x> <y> <amount> ";
        }

        // Erstes Argument als String
        String entityName = args[0];

        float position_x;
        float position_y;
        // Zweites Argument in einen int umwandeln
        if (args.length >= 3)
        {
            try
            {
                position_x = Float.parseFloat(args[1]);
                position_y = Float.parseFloat(args[2]);
            } catch (NumberFormatException e)
            {
                return "Invalid position: " + args[1] + " " + args[2];
            }
        } else
        {
            position_x = executor.getX();
            position_y = executor.getY();
        }


        if (MobRegistry.isRegistered(entityName))
        {
            if (args.length == 2 || args.length == 4)
            {
                int amount;
                if (args.length == 4)
                {
                    amount = Integer.parseInt(args[1]);
                }

                else {
                    amount = Integer.parseInt(args[1]);
                }
                for (int i = 0; i < amount; i++)
                {
                    MobRegistry.createMob(entityName, Globals.physicsWorld, position_x, position_y+i*10);
                }
            } else
            {
                MobRegistry.createMob(entityName, Globals.physicsWorld, position_x, position_y);
            }
            return "Summoning " + entityName + " at " + position_x + " " + position_y;

        } else
        {
            return "Mob " + entityName + " not found";
        }
    }

}
