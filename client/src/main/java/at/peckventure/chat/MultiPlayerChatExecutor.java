package at.peckventure.chat;

import at.peckventure.NetworkClient;
import at.peckventure.entities.Player;
import at.peckventure.menu.MultiPlayerGameScreen;
import at.peckventure.multiplayer.NetworkPackets;

public class MultiPlayerChatExecutor implements ChatExecutor
{
    @Override
    public void processChatInput(String text, Player sender)
    {
        NetworkPackets.ChatMessagePacket packet = new NetworkPackets.ChatMessagePacket();
        packet.message = text;
        NetworkClient.getInstance().sendTCP(packet);
    }
}
