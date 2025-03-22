package at.peckventure.chat;

import at.peckventure.entities.Player;

public abstract class ChatExecutor
{
    public abstract void processChatInput(String text, Player sender);
}
