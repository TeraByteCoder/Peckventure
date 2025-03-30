package at.peckventure.chat.commands;

import at.peckventure.entities.Player;
import at.peckventure.world.block.Block;

public class TeleportCommand extends Command {

    public TeleportCommand() {
        super("teleport");
    }

    @Override
    public String execute(String[] args, Player executor) {
        // Erwartet zwei Parameter: x und y
        if (args.length < 2) {
            return "Usage: /teleport <x> <y>";
        }

        // Hole aktuelle Position (in Pixeln)
        double currentX = executor.getX();
        double currentY = executor.getY();
        double newX, newY;

        try {
            // Verarbeite X-Koordinate (absolute oder relativ)
            if (args[0].startsWith("~")) {
                String offsetStr = args[0].substring(1);
                double offset = offsetStr.isEmpty() ? 0 : Double.parseDouble(offsetStr);
                newX = currentX + offset;
            } else {
                newX = Double.parseDouble(args[0]);
            }

            // Verarbeite Y-Koordinate (absolute oder relativ)
            if (args[1].startsWith("~")) {
                String offsetStr = args[1].substring(1);
                double offset = offsetStr.isEmpty() ? 0 : Double.parseDouble(offsetStr);
                newY = currentY + offset;
            } else {
                newY = Double.parseDouble(args[1]);
            }
        } catch (NumberFormatException e) {
            return "Ungültiges Zahlenformat bei den Koordinaten!";
        }

        // Aktualisiere die Position des Spielers:
        // Da Box2D mit Metern rechnet, wird die neue Position in Meter umgerechnet.
        float angle = executor.getBody().getAngle();
        executor.getBody().setTransform((float)(newX / Block.BLOCK_SIZE), (float)(newY / Block.BLOCK_SIZE), angle);
        // Setze auch direkt die Actor-Position (in Pixeln), damit der Effekt sofort sichtbar wird.
        executor.setPosition((float)newX, (float)newY);

        // Hier könnte optional ein Netzwerk-Paket versendet werden, um den Zustand mit anderen Clients zu synchronisieren.
        return "Teleportiert zu (" + newX + ", " + newY + ")";
    }
}
