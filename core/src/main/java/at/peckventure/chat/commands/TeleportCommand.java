package at.peckventure.chat.commands;

import at.peckventure.entities.Player;
import at.peckventure.multiplayer.NetworkManager;
import at.peckventure.multiplayer.NetworkPackets;
import at.peckventure.world.Box2DOperationManager;
import at.peckventure.world.block.Block;

public class TeleportCommand extends Command {

    public TeleportCommand() {
        super("teleport");
    }

    public String execute(String[] args, Player executor) {
        if (args.length < 2) {
            return "Usage: /teleport <x> <y>";
        }

        // Definiere den Umrechnungsfaktor: z.B. 32 Pixel = 1 Meter
        final double PIXELS_PER_METER = 32.0;

        // Aktuelle Position in Pixeln abrufen und in Meter umrechnen
        double currentXInMeters = executor.getX() / PIXELS_PER_METER;
        double currentYInMeters = executor.getY() / PIXELS_PER_METER;
        double newX, newY;

        try {
            // Verarbeite X-Koordinate (absolute oder relativ, Eingabe in Metern)
            if (args[0].startsWith("~")) {
                String offsetStr = args[0].substring(1);
                double offset = offsetStr.isEmpty() ? 0 : Double.parseDouble(offsetStr);
                newX = currentXInMeters + offset;
            } else {
                newX = Double.parseDouble(args[0]);
            }

            // Verarbeite Y-Koordinate (absolute oder relativ, Eingabe in Metern)
            if (args[1].startsWith("~")) {
                String offsetStr = args[1].substring(1);
                double offset = offsetStr.isEmpty() ? 0 : Double.parseDouble(offsetStr);
                newY = currentYInMeters + offset;
            } else {
                newY = Double.parseDouble(args[1]);
            }
        } catch (NumberFormatException e) {
            return "Ungültiges Zahlenformat bei den Koordinaten!";
        }

        // Aktualisiere die Position des Spielers:
        // Box2D rechnet mit Metern, daher direkt newX und newY verwenden.
        Box2DOperationManager.queueOperation(() -> {
            float angle = executor.getBody().getAngle();
            executor.getBody().setTransform((float)newX, (float)newY, angle);
            // Aktualisiere die Actor-Position in Pixeln, damit der Effekt sofort sichtbar wird.
            executor.setPosition((float)(newX * PIXELS_PER_METER), (float)(newY * PIXELS_PER_METER));
        });

        try {
            NetworkPackets.ServerPositionChangePacket packet = new NetworkPackets.ServerPositionChangePacket();
            packet.x = (float)newX;
            packet.y = (float)newY;
            NetworkManager.getInstance().sendToPlayerTCP(packet, executor);
        } catch (IllegalStateException e) {
            // Exception ggf. loggen
        }
        return "Teleportiert zu (" + newX + ", " + newY + ")";
    }

}
