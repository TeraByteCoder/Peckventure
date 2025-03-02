package at.peckventure.chat.commands;

import at.peckventure.chat.ChatUI;
import at.peckventure.inventory.ItemRegistry;
import at.peckventure.inventory.item.Item;

import static at.peckventure.Globals.inventoryUI;

public class GiveCommand extends Command
{
    public GiveCommand()
    {
        super("give");
    }

    @Override
    public void execute(String[] args, ChatUI chatUI)
    {
        if (args.length < 2) {
            chatUI.addMessage("Usage: /give <item> <amount>");
            return;
        }

        // Erstes Argument als String
        String itemName = args[0];

        // Zweites Argument in einen int umwandeln
        int amount;
        try {
            amount = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            chatUI.addMessage("Invalid amount: " + args[1]);
            return;
        }

        // Beispielausgabe
        chatUI.addMessage("Giving " + amount + " of item: " + itemName);

        // Hier kannst du dann deine Logik zum Hinzufügen des Items implementieren
        // Nutze das 'sword'-Item aus der Registry
        if (ItemRegistry.contains(itemName))
        {
            Item item = ItemRegistry.createItem(itemName);
            inventoryUI.addItem(item, amount);
        }
    }

}
