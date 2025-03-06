package at.peckventure.chat.commands;

import at.peckventure.Globals;
import at.peckventure.chat.ChatUI;
import at.peckventure.entities.Player;
import at.peckventure.entities.mob.Mob;
import at.peckventure.entities.mob.MobRegistration;
import at.peckventure.entities.mob.MobRegistry;
import at.peckventure.inventory.ItemRegistry;
import at.peckventure.inventory.item.Item;

import static at.peckventure.Globals.inventoryUI;
import static at.peckventure.Globals.player;

public class SummonCommand extends Command
{
    public SummonCommand()
    {
        super("summon");
    }

    @Override
    public void execute(String[] args, ChatUI chatUI, Player executor)
    {
        if (args.length < 1 || args.length == 3 || args.length > 4)
        {
            chatUI.addMessage("Usage: /summon <entity> <location>");
            return;
        }

        // Erstes Argument als String
        String entityName = args[0];

        float position_x;
        float position_y;
        // Zweites Argument in einen int umwandeln
        if (args.length == 3)
        {
            try
            {
                position_x = Float.parseFloat(args[1]);
                position_y = Float.parseFloat(args[2]);
            } catch (NumberFormatException e)
            {
                chatUI.addMessage("Invalid position: " + args[1] +  " " + args[2]);
                return;
            }
        }

        else
        {
            position_x = executor.getX();
            position_y = executor.getY();
        }

        // Beispielausgabe
        chatUI.addMessage("Summoning " + entityName + " at " + position_x + " " + position_y);

        // Hier kannst du dann deine Logik zum Hinzufügen des Items implementieren
        // Nutze das 'sword'-Item aus der Registry
        if (MobRegistry.isRegistered(entityName))
        {
            Mob mob = MobRegistry.createMob(entityName, Globals.physicsWorld,  position_x, position_y);
        }
        else {
            chatUI.addMessage("Mob " + entityName + " not found");
        }
    }

}
