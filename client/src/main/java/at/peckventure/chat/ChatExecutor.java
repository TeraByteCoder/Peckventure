package at.peckventure.chat;

import at.peckventure.entities.Player;

public interface ChatExecutor
{
    public void processChatInput(String text, Player sender);
}
