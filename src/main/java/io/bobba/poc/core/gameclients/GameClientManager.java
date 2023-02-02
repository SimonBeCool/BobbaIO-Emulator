package io.bobba.poc.core.gameclients;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.bobba.poc.misc.logging.LogLevel;
import io.bobba.poc.misc.logging.Logging;
import io.bobba.poc.net.ClientConnection;
import io.bobba.poc.net.IConnectionHandler;

public class GameClientManager implements IConnectionHandler {
    private final Map<Integer, GameClient> clients = new ConcurrentHashMap<>();
    private final GameClientMessageHandler messageHandler = new GameClientMessageHandler();

    @Override
    public void handleNewConnection(ClientConnection newConnection) {
        int connectionId = newConnection.getId();
        if (!clients.containsKey(connectionId)) {
            clients.put(connectionId, new GameClient(connectionId, newConnection));
            Logging.getInstance().writeLine("New gameclient created (" + connectionId + ")", LogLevel.Debug, getClass());
        }
    }

    @Override
    public void handleDisconnect(ClientConnection connection) {
        int connectionId = connection.getId();
        GameClient disconnected = clients.remove(connectionId);
        if (disconnected != null) {
            disconnected.stop();
            Logging.getInstance().writeLine("Gameclient dropped (" + connectionId + ")", LogLevel.Debug, getClass());
        }
    }

    @Override
    public void handleMessage(ClientConnection connection, String message) {
        int connectionId = connection.getId();
        GameClient client = clients.get(connectionId);
        if (client != null) {
            client.handleMessage(message);
        }
    }

    public GameClientMessageHandler getSharedMessageHandler() {
        return messageHandler;
    }
}
