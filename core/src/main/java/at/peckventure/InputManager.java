package at.peckventure;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;

public class InputManager extends InputAdapter {
    private static InputManager instance;
    private boolean leftPressed, rightPressed, jumpPressed;
    // Referenz auf den Chat, die du beim Setup setzen kannst
    private ChatToggle chatToggle;

    public interface ChatToggle {
        void toggleChat();

        void cancelChat();

        boolean isChatActive();
    }

    private InputManager() { }

    public static InputManager getInstance() {
        if (instance == null) {
            instance = new InputManager();
        }
        return instance;
    }

    public void setChatToggle(ChatToggle chatToggle) {
        this.chatToggle = chatToggle;
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

        if (keycode == Input.Keys.ESCAPE) {
            if (chatToggle != null) {
                chatToggle.cancelChat();
            }
            return true;
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
        return false;
    }

    public boolean isLeftPressed() {
        return leftPressed;
    }

    public boolean isRightPressed() {
        return rightPressed;
    }

    public boolean isJumpPressed() {
        return jumpPressed;
    }


}
