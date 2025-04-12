package at.peckventure.chat.commands;

import at.peckventure.entities.Player;
import at.peckventure.status.EffectRegistry;
import at.peckventure.status.StatusEffect;
import at.peckventure.multiplayer.NetworkManager;
import at.peckventure.multiplayer.NetworkPackets;

public class EffectCommand extends Command {

    public EffectCommand() {
        super("effect");
    }

    @Override
    public String execute(String[] args, Player executor) {
        if(!executor.isOperator()) return "You do not have permission to perform this command!";
        // Prüfe, ob mindestens Name und Level angegeben wurden.
        if (args.length < 2) {
            return "Usage: effect <name> <level> [duration]";
        }

        // Erster Parameter: Name des Effekts
        String effectName = args[0];

        // Zweiter Parameter: Level (muss eine gültige Zahl sein)
        int level;
        try {
            level = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            return "Invalid level: " + args[1];
        }

        // Dritter Parameter: Dauer (optional, Default z. B. 5 Sekunden)
        float duration = 5f;
        if (args.length >= 3) {
            try {
                duration = Float.parseFloat(args[2]);
            } catch (NumberFormatException e) {
                return "Invalid duration: " + args[2];
            }
        }

        // Überprüfe, ob der Effekt in der Registry registriert ist
        if (!EffectRegistry.contains(effectName)) {
            return "Effect '" + effectName + "' does not exist.";
        }

        // Erzeuge den Effekt über die Registry
        StatusEffect effect = EffectRegistry.createEffect(effectName, level, duration);
        if (effect == null) {
            return "Could not create effect '" + effectName + "'.";
        }

        // Füge den Effekt zum Spieler hinzu
        executor.addEffect(effect);

        // Optional: Sende ein Update-Paket
        try {
            NetworkPackets.EffectUpdatePacket packet = new NetworkPackets.EffectUpdatePacket();
            packet.effects = executor.serializeEffects();
            NetworkManager.getInstance().sendToPlayerTCP(packet, executor);
        } catch (IllegalStateException e) {
            // Fehlerhafte Paketübertragung ignorieren
        }

        return "Applied effect '" + effectName + "' with level " + level + " and duration " + duration + " seconds.";
    }
}
