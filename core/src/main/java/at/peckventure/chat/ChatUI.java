package at.peckventure.chat;


import at.peckventure.InputManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.utils.Align;

public class ChatUI
{
    private final Stage stage;
    private final BitmapFont font;
    private final TextField chatInput;
    private final Table messageTable;
    private final ScrollPane scrollPane;
    private final Table containerTable;

    private static final int MAX_MESSAGES = 1000;
    private final float visibleLinesHeight;
    private final CommandRegistry commandRegistry;

    public ChatUI(Stage stage)
    {
        this.stage = stage;
        font = new BitmapFont();
        font.getData().setScale(1.5f);

        // Wir gehen von 20 Zeilen aus
        visibleLinesHeight = 20 * font.getLineHeight();

        // Nachrichten-Tabelle
        messageTable = new Table();
        messageTable.left().bottom();

        // ScrollPane
        scrollPane = new ScrollPane(messageTable);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(false, false);

        // TextField
        TextField.TextFieldStyle tfs = new TextField.TextFieldStyle();
        tfs.font = font;
        tfs.fontColor = Color.WHITE;
        commandRegistry = new CommandRegistry();
        chatInput = new TextField("", tfs);
        chatInput.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
            @Override
            public boolean keyDown(com.badlogic.gdx.scenes.scene2d.InputEvent event, int keycode) {
                if(keycode == com.badlogic.gdx.Input.Keys.T) {
                    toggleChat(); // Schließt den Chat, wenn er offen ist
                    return true;  // Event verbrauchen, damit T nicht ins Textfeld gelangt
                }
                if(keycode == com.badlogic.gdx.Input.Keys.ESCAPE) {
                    cancelChat();
                    return true;
                }
                return false;
            }
        });

        chatInput.setMessageText("Enter command or chat...");
        chatInput.setVisible(false);

        // Container unten links
        containerTable = new Table();
        containerTable.setFillParent(true);
        containerTable.bottom().left().padLeft(20).padBottom(20);

        containerTable.add(scrollPane).width(400).height(visibleLinesHeight).row();
        containerTable.add(chatInput).width(400);

        stage.addActor(containerTable);

        // Enter-Listener
        chatInput.setTextFieldListener((textField, key) ->
        {
            if (key == '\r' || key == '\n')
            {
                String text = chatInput.getText().trim();
                if (text.isEmpty())
                {
                    closeChat();
                } else
                {
                    processChatInput(text);
                    closeChat();
                }
            }
        });
    }

    private void processChatInput(String text)
    {
        if (text.startsWith("/"))
        {
            commandRegistry.executeCommand(text.substring(1), this);
        } else
        {
            addMessage("Player: " + text);
        }
    }

    private void executeCommand(String command)
    {
        String[] args = command.split(" ");
        if (args.length == 0) return;
        if (args[0].equalsIgnoreCase("text"))
        {
            addMessage("Console: " + command.substring(5));
        } else
        {
            addMessage("Unknown command: " + args[0]);
        }
    }

    public void addMessage(String message)
    {
        Label.LabelStyle ls = new Label.LabelStyle(font, Color.WHITE);
        Label label = new Label(message, ls);
        label.setWrap(true);
        label.setAlignment(Align.left);
        messageTable.add(label).width(380).left().padBottom(5).row();

        // Maximale Anzahl von Nachrichten
        if (messageTable.getChildren().size > MAX_MESSAGES)
        {
            Actor oldest = messageTable.getChildren().first();
            oldest.remove();
        }

        // Automatisch ganz nach unten scrollen
        scrollPane.layout();
        scrollPane.setScrollPercentY(1f);

        // Fade-Out, wenn Chat zu ist
        if (!chatInput.isVisible())
        {
            label.addAction(Actions.sequence(
                Actions.delay(5f),
                Actions.fadeOut(1f),
                Actions.run(label::remove)
            ));
        }
    }

    private void openChat()
    {
        chatInput.setVisible(true);
        stage.setKeyboardFocus(chatInput);
        InputManager.getInstance().pauseInputs();


        // Alle Nachrichten sofort voll sichtbar machen
        for (Actor actor : messageTable.getChildren())
        {
            actor.clearActions();
            actor.getColor().a = 1f;
        }
    }

    private void closeChat() {
        chatInput.setText("");
        chatInput.setVisible(false);
        stage.setKeyboardFocus(null);

        // Fade-Out für alle Nachrichten neu starten
        for (Actor actor : messageTable.getChildren()) {
            actor.clearActions();
            actor.addAction(Actions.sequence(
                Actions.delay(5f),
                Actions.fadeOut(1f),
                Actions.run(actor::remove)
            ));
        }

        // Wichtig: Inputs wieder freigeben
        at.peckventure.InputManager.getInstance().resumeInputs();
    }



    public void toggleChat()
    {
        if (chatInput.isVisible())
        {
            closeChat();
        } else
        {
            openChat();
        }
    }

    public void cancelChat() {
        closeChat();
    }

}
