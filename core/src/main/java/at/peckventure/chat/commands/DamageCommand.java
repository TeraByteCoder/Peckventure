package at.peckventure.chat.commands;

import at.peckventure.LanguageManager;
import at.peckventure.entities.Player;
import at.peckventure.multiplayer.NetworkManager;
import at.peckventure.multiplayer.NetworkPackets;

public class DamageCommand extends Command {

    public DamageCommand() {
        super("damage");
    }

    @Override
    public String execute(String[] args, Player executor) {
        if(!executor.isOperator()) return LanguageManager.INSTANCE.getText("command.permission.denied");
        int amount;
        if (args.length < 1) {
            // Wenn kein Parameter angegeben ist, setze den Schaden auf Const.MaxHealth.
            amount = executor.getHealthStatus().getMax();
        } else {
            try {
                amount = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                return LanguageManager.INSTANCE.getText("command.invalid.amount") + args[0];
            }
        }

        if (amount < 0) {
            return LanguageManager.INSTANCE.getText("command.negative.damage.is.not.allowed");
        }

        executor.getHealthStatus().damage(amount);

        try {
            NetworkPackets.PlayerStatusUpdatePacket packet = new NetworkPackets.PlayerStatusUpdatePacket();
            packet.energy = executor.getEnergyStatus().getCurrent();
            packet.health = executor.getHealthStatus().getCurrent();
            NetworkManager.getInstance().sendToPlayerTCP(packet, executor);
        } catch (IllegalStateException e) {
        }

        return LanguageManager.INSTANCE.getText("command.inflicted") + amount + LanguageManager.INSTANCE.getText("command.damage.points");
    }
}
