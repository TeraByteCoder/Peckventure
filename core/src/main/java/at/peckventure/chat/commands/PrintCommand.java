package at.peckventure.chat.commands;

import at.peckventure.chat.ChatUI;

public class PrintCommand extends Command {
    public PrintCommand() {
        super("print");
    }
    @Override
    public void execute(String[] args, ChatUI chatUI) {
        String output = String.join(" ", args);
        System.out.println(output);
        chatUI.addMessage("Console: " + output);
    }
}
