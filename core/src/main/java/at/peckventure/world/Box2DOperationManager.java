package at.peckventure.world;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Box2DOperationManager {
    private static final Queue<Runnable> operationQueue = new ConcurrentLinkedQueue<>();

    // Diese Methode wird von anderen Threads aufgerufen, um eine Änderung anzufordern
    public static void queueOperation(Runnable operation) {
        operationQueue.add(operation);
    }

    // Diese Methode wird im Hauptthread (z. B. im Render-Loop) aufgerufen
    public static void processOperations() {
        while (!operationQueue.isEmpty()) {
            Runnable operation = operationQueue.poll();
            if (operation != null) {
                operation.run();
            }
        }
    }
}

