package at.peckventure;
import at.peckventure.multiplayer.Network;
import at.peckventure.multiplayer.NetworkPackets;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Listener;

public class NetworkClient {
    private static NetworkClient instance;
    private final Client client;
    private final String host;
    private final int tcpPort;
    private final int udpPort;

    // Privater Konstruktor, damit kein weiterer Instanzen erstellt werden können.
    private NetworkClient(String host, int tcpPort, int udpPort) {
        this.host = host;
        this.tcpPort = tcpPort;
        this.udpPort = udpPort;
        client = new Client(65536, 65536);

        Network.register(client.getKryo());
    }

    // Initialisierungsmethode; muss vor dem ersten Aufruf von getInstance() aufgerufen werden.
    public static synchronized void init(String host, int tcpPort, int udpPort) {
        if (instance == null) {
            instance = new NetworkClient(host, tcpPort, udpPort);
        }
    }

    // Liefert die einzige Instanz; falls noch nicht initialisiert, wird eine Exception geworfen.
    public static NetworkClient getInstance() {
        if (instance == null) {
            throw new IllegalStateException("NetworkClient wurde noch nicht initialisiert! Rufe init() zuerst auf.");
        }
        return instance;
    }

    public void connect(int timeout) {
        client.start();
        try {
            client.connect(timeout, host, tcpPort, udpPort);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendUDP(Object object) {
        client.sendUDP(object);
    }

    public void sendTCP(Object object) {
        client.sendTCP(object);
    }

    public void addListener(Listener listener) {
        client.addListener(listener);
    }

    public boolean isConnected() {
        return client.isConnected();
    }

}
