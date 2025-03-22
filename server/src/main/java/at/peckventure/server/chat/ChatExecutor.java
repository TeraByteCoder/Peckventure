package at.peckventure.server.chat;

import at.peckventure.chat.CommandRegistry;
import at.peckventure.multiplayer.NetworkPackets;
import at.peckventure.server.GameServer;
import at.peckventure.server.entities.ServerPlayer;

public class ChatExecutor
{
    private final CommandRegistry commandRegistry;

    // Mit dem Flag wird bestimmt, ob wir im Multiplayer-Modus arbeiten.
    public ChatExecutor()
    {
        commandRegistry = new CommandRegistry();
    }

    /**
     * Verarbeitet die eingegebene Chat-Nachricht.
     * Falls die Eingabe mit "/" beginnt, wird sie als Command behandelt.
     * Andernfalls als normaler Chattext.
     */
    public void processChatInput(String input, ServerPlayer sender)
    {
        if (input.startsWith("/"))
        {
            executeCommand(input.substring(1), sender);
        } else
        {
            sendChatMessage(input, sender);
        }
    }

    /**
     * Sendet einen normalen Chattext.
     * Im Multiplayer-Fall delegiert diese Methode an einen externen Sender,
     * ansonsten wird die Nachricht direkt lokal (z. B. in der ChatUI) angezeigt.
     */
    private void sendChatMessage(String message, ServerPlayer sender)
    {
        System.out.println("[" + sender.getUsername() + "] " + message);
        NetworkPackets.ChatMessagePacket packet = new NetworkPackets.ChatMessagePacket();
        packet.message = sender.getUsername() + " : " + message;
        GameServer.instance.getServer().sendToAllTCP(packet);
    }

    /**
     * Führt einen Command aus.
     * Im Multiplayer-Fall wird der Command an einen externen Sender übertragen,
     * ansonsten wird er direkt lokal verarbeitet.
     */
    private void executeCommand(String command, ServerPlayer sender)
    {
        NetworkPackets.ChatMessagePacket packet = new NetworkPackets.ChatMessagePacket();
        packet.message = commandRegistry.executeCommand(command, sender);
        sender.getConnection().sendTCP(packet);
    }
}
