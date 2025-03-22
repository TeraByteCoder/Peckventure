package at.peckventure.chat;

import at.peckventure.Globals;
import at.peckventure.entities.Player;
import at.peckventure.menu.SinglePlayer;

public class SinglePlayerChatExecutor implements ChatExecutor
{
    CommandRegistry commandRegistry;

    public SinglePlayerChatExecutor()
    {
        this.commandRegistry = new CommandRegistry();
    }
    @Override
    public void processChatInput(String text, Player sender)
    {
        if (text.startsWith("/"))
        {
            commandRegistry.executeCommand(text.substring(1), sender);
        } else
        {
            ChatUI.getInstance().addMessage(Globals.username +": " + text);
        }
    }
}
