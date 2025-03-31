package at.peckventure.multiplayer;

import at.peckventure.entities.Player;
import com.esotericsoftware.kryonet.Listener;

public abstract class NetworkManager {
    protected static NetworkManager instance;

    public static NetworkManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("NetworkManager wurde noch nicht initialisiert!");
        }
        return instance;
    }

    public abstract void connect(int timeout);
    public abstract void sendTCP(Object object);
    public abstract void sendUDP(Object object);
    public abstract void addListener(Listener listener);
    public abstract boolean isConnected();

    // Varianten, die statt einer Connection einen Player erwarten
    public abstract void sendToPlayerTCP(Object object, Player player);
    public abstract void sendToPlayerUDP(Object object, Player player);
    public abstract void sendToAllExceptTCP(Object object, Player player);
    public abstract void sendToAllExceptUDP(Object object, Player player);
}
