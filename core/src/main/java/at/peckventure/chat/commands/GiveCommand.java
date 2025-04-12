package at.peckventure.chat.commands;

import at.peckventure.entities.Player;
import at.peckventure.inventory.ItemRegistry;
import at.peckventure.inventory.item.Item;
import at.peckventure.multiplayer.NetworkManager;
import at.peckventure.multiplayer.NetworkPackets;

public class GiveCommand extends Command
{
    public GiveCommand()
    {
        super("give");
    }

    @Override
    public String execute(String[] args,  Player executor)
    {
        if(!executor.isOperator()) return "You do not have permission to perform this command!";
        if (args.length < 2)
        {
            return"Usage: /give <item> <amount>";

        }

        // Erstes Argument als String
        String itemName = args[0];

        // Zweites Argument in einen int umwandeln
        int amount;
        try
        {
            amount = Integer.parseInt(args[1]);
        } catch (NumberFormatException e)
        {
            return"Invalid amount: " + args[1];
        }


        if (ItemRegistry.contains(itemName))
        {
            Item item = ItemRegistry.createItem(itemName);
            executor.getInventory().addItem(item, amount);
        }

        try{
            NetworkPackets.InventoryUpdatePacket updatePacket = new NetworkPackets.InventoryUpdatePacket();
            updatePacket.hotbarData = executor.getInventory().serializeHotbar();
            updatePacket.mainInventoryData = executor.getInventory().serializeMain();
            NetworkManager.getInstance().sendToPlayerTCP(updatePacket, executor);
        }
        catch (IllegalStateException e)
        {

        }

        return "Giving " + amount + " of item: " + itemName;
    }

}
