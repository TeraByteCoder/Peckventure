package at.peckventure.chat.commands;

import at.peckventure.entities.Player;
import at.peckventure.multiplayer.NetworkManager;
import at.peckventure.multiplayer.NetworkPackets;

public class HealCommand extends Command {

    public HealCommand() {
        super("heal");
    }

    @Override
    public String execute(String[] args, Player executor) {
        if(!executor.isOperator()) return "You do not have permission to perform this command!";
        int amount;
        if (args.length < 1) {
            amount = executor.getHealthStatus().getMax();
        } else {
            try {
                amount = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                return "Invalid amount: " + args[0];
            }
        }

        if (amount < 0) {
            return "Negative healing is not allowed.";
        }

        executor.getHealthStatus().heal(amount);

        try {
            NetworkPackets.PlayerStatusUpdatePacket packet = new NetworkPackets.PlayerStatusUpdatePacket();
            packet.energy = executor.getEnergyStatus().getCurrent();
            packet.health = executor.getHealthStatus().getCurrent();
            NetworkManager.getInstance().sendToPlayerTCP(packet, executor);
        } catch (IllegalStateException e) {
        }

        return "Healed " + amount + " health points.";
    }
}
