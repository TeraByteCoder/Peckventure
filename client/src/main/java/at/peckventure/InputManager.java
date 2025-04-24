package at.peckventure;

import at.peckventure.chat.ChatUI;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;

public class InputManager extends InputAdapter {
    private static InputManager instance;
    private boolean leftPressed, rightPressed, jumpPressed, wPressed, sPressed, peckPressed;
    private ChatToggle chatToggle;
    private EscapeHandler escapeHandler;

    private boolean landPressed = false;


    public interface ChatToggle {
        void toggleChat();
        void cancelChat();
        boolean isChatActive();
    }

    public interface EscapeHandler {
        void handleEscape();
        boolean isMenuActive();
    }

    private InputManager() {
    }

    public static InputManager getInstance() {
        if (instance == null) {
            instance = new InputManager();
        }
        return instance;
    }

    public void setChatToggle(ChatToggle chatToggle) {
        this.chatToggle = chatToggle;
    }

    public void setEscapeHandler(EscapeHandler escapeHandler) {
        this.escapeHandler = escapeHandler;
    }

    @Override
    public boolean keyDown(int keycode) {
        // WICHTIG: Wenn Chat aktiv ist, ALLE Tasten blockieren außer ESC
        // Dies verhindert, dass E das Inventar öffnet, während der Chat aktiv ist
        if (chatToggle != null && chatToggle.isChatActive()) {
            if (keycode == Input.Keys.ESCAPE) {
                chatToggle.cancelChat();
                return true;
            }
            // Wichtig: Alle anderen Eingaben blockieren und an das Textfeld weiterleiten
            return true; // Hier ist die Änderung: true statt false
        }

        // Chat ist nicht aktiv, normales Verhalten
        if (keycode == Input.Keys.T) {
            if (chatToggle != null) {
                // Chat öffnen
                chatToggle.toggleChat();
                return true;
            }
        }

        if (keycode == Input.Keys.ESCAPE) {
            if (escapeHandler != null) {
                escapeHandler.handleEscape();
                return true;
            }
        }

        if (inputsPaused) {
            return true;
        }

        if (keycode == Input.Keys.A) {
            leftPressed = true;
        }
        if (keycode == Input.Keys.D) {
            rightPressed = true;
        }
        if (keycode == Input.Keys.SPACE) {
            jumpPressed = true;
        }
        if (keycode == Input.Keys.W) {
            wPressed = true;
        }
        if (keycode == Input.Keys.S) {
            sPressed = true;
        }
        if (keycode == Input.Keys.F) {
            peckPressed = true;
        }
        return false;
    }

    private boolean inputsPaused = false;

    public void pauseInputs() {
        inputsPaused = true;
    }

    public void resumeInputs() {
        inputsPaused = false;
    }

    @Override
    public boolean keyUp(int keycode) {
        // Auch hier: Wenn Chat aktiv ist, alle keyUp-Events blockieren
        if (chatToggle != null && chatToggle.isChatActive()) {
            return true; // Änderung hier: true statt false
        }

        if (keycode == Input.Keys.A) {
            leftPressed = false;
        }
        if (keycode == Input.Keys.D) {
            rightPressed = false;
        }
        if (keycode == Input.Keys.SPACE) {
            jumpPressed = false;
        }
        if (keycode == Input.Keys.W) {
            wPressed = false;
        }
        if (keycode == Input.Keys.S) {
            sPressed = false;
        }
        if (keycode == Input.Keys.F) {
            peckPressed = false;
        }
        if (keycode == Input.Keys.C) {
            landPressed = true;
        }
        return false;
    }

    // Diese Methoden werden unverändert gelassen, da sie bereits
    // den Chat-Status berücksichtigen

    public boolean isLeftPressed() {
        return leftPressed && !inputsPaused &&
            (chatToggle == null || !chatToggle.isChatActive()) &&
            (escapeHandler == null || !escapeHandler.isMenuActive());
    }

    public boolean isRightPressed() {
        return rightPressed && !inputsPaused &&
            (chatToggle == null || !chatToggle.isChatActive()) &&
            (escapeHandler == null || !escapeHandler.isMenuActive());
    }

    public boolean isJumpPressed() {
        return jumpPressed && !inputsPaused &&
            (chatToggle == null || !chatToggle.isChatActive()) &&
            (escapeHandler == null || !escapeHandler.isMenuActive());
    }

    public boolean isWPressed() {
        return wPressed && !inputsPaused &&
            (chatToggle == null || !chatToggle.isChatActive()) &&
            (escapeHandler == null || !escapeHandler.isMenuActive());
    }

    public boolean isSPressed() {
        return sPressed && !inputsPaused &&
            (chatToggle == null || !chatToggle.isChatActive()) &&
            (escapeHandler == null || !escapeHandler.isMenuActive());
    }

    public boolean isPeckPressed() {
        return peckPressed && !inputsPaused &&
            (chatToggle == null || !chatToggle.isChatActive()) &&
            (escapeHandler == null || !escapeHandler.isMenuActive());
    }
    public boolean isLandPressed() {
        return landPressed && !inputsPaused &&
            (chatToggle == null || !chatToggle.isChatActive()) &&
            (escapeHandler == null || !escapeHandler.isMenuActive());
    }

    public void resetLandPressed()
    {
        landPressed = false;
    }

}
