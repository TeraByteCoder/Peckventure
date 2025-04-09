package at.peckventure.world;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import com.badlogic.gdx.Gdx;

public class Box2DOperationManager {
    private static final Queue<Runnable> operationQueue = new ConcurrentLinkedQueue<>();
    private static boolean isProcessing = false;

    // Diese Methode wird von anderen Threads aufgerufen, um eine Änderung anzufordern
    public static void queueOperation(Runnable operation) {
        operationQueue.add(operation);
    }

    // Für Kompatibilität mit dem alten Code (falls noch irgendwo verwendet)
    public static void addOperation(Runnable operation) {
        queueOperation(operation);
    }

    // Diese Methode wird im Hauptthread (z. B. im Render-Loop) aufgerufen
    public static void processOperations() {
        isProcessing = true;
        while (!operationQueue.isEmpty()) {
            Runnable operation = operationQueue.poll();
            if (operation != null) {
                try {
                    operation.run();
                } catch (Exception e) {
                    Gdx.app.error("Box2DOperationManager", "Error while processing operation", e);
                }
            }
        }
        isProcessing = false;
    }

    // Neue Methode zum Zurücksetzen des Managers
    public static void clear() {
        synchronized (operationQueue) {
            operationQueue.clear();
        }
        isProcessing = false;
        Gdx.app.debug("Box2DOperationManager", "All pending operations cleared");
    }
}
