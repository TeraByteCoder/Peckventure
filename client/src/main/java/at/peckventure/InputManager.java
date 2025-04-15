package at.peckventure;

import at.peckventure.chat.ChatUI;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;

public class InputManager extends InputAdapter {
    private static InputManager instance;
    private boolean leftPressed, rightPressed, jumpPressed, wPressed, sPressed, peckPressed;
    private ChatToggle chatToggle;
    private EscapeHandler escapeHandler;


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
        if (keycode == Input.Keys.T) {
            // Nur aktivieren, wenn der Chat noch nicht offen ist
            if (chatToggle != null && !chatToggle.isChatActive()) {
                chatToggle.toggleChat();
            }
            return true;
        }

        if (keycode == Input.Keys.UP) {
            ChatUI.getInstance().loadLastMessage(true);
            return true;
        }
        if (keycode == Input.Keys.DOWN) {
            ChatUI.getInstance().loadLastMessage(false);
        }

        if (keycode == Input.Keys.ESCAPE) {
            if (escapeHandler != null) {
                escapeHandler.handleEscape();
                return true;
            } else if (chatToggle != null) {
                chatToggle.cancelChat();
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
        return false;
    }

    public boolean isLeftPressed() {
        return leftPressed && !inputsPaused && (escapeHandler == null || !escapeHandler.isMenuActive());
    }

    public boolean isRightPressed() {
        return rightPressed && !inputsPaused && (escapeHandler == null || !escapeHandler.isMenuActive());
    }

    public boolean isJumpPressed() {
        return jumpPressed && !inputsPaused && (escapeHandler == null || !escapeHandler.isMenuActive());
    }

    public boolean isWPressed() {
        return wPressed && !inputsPaused && (escapeHandler == null || !escapeHandler.isMenuActive());
    }

    public boolean isSPressed() {
        return sPressed && !inputsPaused && (escapeHandler == null || !escapeHandler.isMenuActive());
    }
    public boolean isPeckPressed() {
        return peckPressed && !inputsPaused && (escapeHandler == null || !escapeHandler.isMenuActive());
    }
}
