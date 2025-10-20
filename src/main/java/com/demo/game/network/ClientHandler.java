// File: com/demo/game/network/ClientHandler.java
package com.demo.game.network;

import com.demo.game.network.messages.*;

import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final int clientId;
    private final GameServer server;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;
    private volatile boolean running = false;

    public ClientHandler(Socket socket, int clientId, GameServer server) {
        this.socket = socket;
        this.clientId = clientId;
        this.server = server;
    }

    @Override
    public void run() {
        running = true;
        try {
            outputStream = new ObjectOutputStream(socket.getOutputStream());
            inputStream = new ObjectInputStream(socket.getInputStream());

            // Loop to read messages from the client
            while (running) {
                try {
                    NetworkMessage message = (NetworkMessage) inputStream.readObject();
                    handleMessage(message);
                } catch (ClassNotFoundException e) {
                    System.err.println("Client " + clientId + " sent unknown message type: " + e.getMessage());
                } catch (EOFException | SocketException e) {
                    // Client disconnected abruptly
                    System.out.println("Client " + clientId + " connection lost.");
                    running = false; // Exit loop
                } catch (IOException e) {
                    if (running) {
                        System.err.println("IO Error reading from client " + clientId + ": " + e.getMessage());
                        running = false; // Assume disconnection on other IO errors
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error setting up streams for client " + clientId + ": " + e.getMessage());
        } finally {
            server.submitTask(() -> server.removeClient(clientId)); // Ensure removal happens on the game thread
            closeConnection();
        }
    }

    private void handleMessage(NetworkMessage message) {
        // Enqueue the handling logic to run on the server's main game thread
        if (message instanceof ClientInfoMessage) {
            server.submitTask(() -> server.handleClientInfo(clientId, (ClientInfoMessage) message));
        } else if (message instanceof PlayerInputMessage) {
            server.submitTask(() -> server.handlePlayerInput(clientId, (PlayerInputMessage) message));
        }
        // Add more message types here if needed (e.g., chat messages)
    }

    // Sends a message to this specific client
    public synchronized void sendMessage(NetworkMessage message) {
        if (!running || outputStream == null) return;
        try {
            outputStream.writeObject(message);
            outputStream.reset(); // Important to clear object stream cache
            outputStream.flush();
        } catch (SocketException e) {
            System.out.println("Failed to send message to client " + clientId + " (disconnected?).");
            running = false; // Stop trying to send/receive if socket is broken
        } catch (IOException e) {
            System.err.println("IO Error sending message to client " + clientId + ": " + e.getMessage());
            running = false;
        }
    }

    public void closeConnection() {
        running = false;
        try {
            if (outputStream != null) outputStream.close();
            if (inputStream != null) inputStream.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("Error closing connection for client " + clientId + ": " + e.getMessage());
        }
    }

    public int getClientId() {
        return clientId;
    }
}