package at.peckventure.server.network;

import at.peckventure.entities.Player;
import at.peckventure.multiplayer.NetworkManager;
import at.peckventure.server.entities.ServerPlayer;
import com.esotericsoftware.kryonet.Server;
import com.esotericsoftware.kryonet.Listener;
import at.peckventure.multiplayer.Network;

public class NetworkServer extends NetworkManager {
    private final Server server;
    private final int tcpPort;
    private final int udpPort;

    // Privater Konstruktor
    private NetworkServer(int tcpPort, int udpPort) {
        this.tcpPort = tcpPort;
        this.udpPort = udpPort;
        server = new Server(65536, 65536);
        // Registrierung der Klassen, die über Kryo serialisiert werden
        Network.register(server.getKryo());
    }

    // Öffentliche statische Init-Methode
    public static synchronized Server init(int tcpPort, int udpPort) {
        if (instance != null) {
            throw new IllegalStateException("NetworkManager wurde bereits initialisiert!");
        }
        NetworkServer NetworkServer = new NetworkServer(tcpPort, udpPort);
        instance = NetworkServer;

        return NetworkServer.server;
    }

    @Override
    public void connect(int timeout) {
        server.start();
        try {
            server.bind(tcpPort, udpPort);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void sendTCP(Object object) {
        server.sendToAllTCP(object);
    }

    @Override
    public void sendUDP(Object object) {
        server.sendToAllUDP(object);
    }

    @Override
    public void addListener(Listener listener) {
        server.addListener(listener);
    }

    @Override
    public boolean isConnected() {
        // Hier könnte man weitere Statusprüfungen ergänzen
        return true;
    }

    // Sende eine Nachricht gezielt an einen Spieler über dessen Connection
    @Override
    public void sendToPlayerTCP(Object object, Player player) {
        if (player instanceof ServerPlayer) {
            ((ServerPlayer) player).getConnection().sendTCP(object);
        }
    }

    @Override
    public void sendToPlayerUDP(Object object, Player player) {
        if (player instanceof ServerPlayer) {
            ((ServerPlayer) player).getConnection().sendUDP(object);
        }
    }

    // Sende an alle außer einem bestimmten Spieler, ermittele dazu die Connection-ID
    @Override
    public void sendToAllExceptTCP(Object object, Player player) {
        if (player instanceof ServerPlayer) {
            int connId = ((ServerPlayer) player).getConnection().getID();
            server.sendToAllExceptTCP(connId, object);
        } else {
            server.sendToAllTCP(object);
        }
    }

    @Override
    public void sendToAllExceptUDP(Object object, Player player) {
        if (player instanceof ServerPlayer) {
            int connId = ((ServerPlayer) player).getConnection().getID();
            server.sendToAllExceptUDP(connId, object);
        } else {
            server.sendToAllUDP(object);
        }
    }

    public Server getServer()
    {
        return server;
    }


}
