// File: com/demo/game/network/GameClient.java
package com.demo.game.network;

import com.demo.game.models.User;
import com.demo.game.network.messages.*;
import com.demo.game.ui.SceneManager; // To get the client's username

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class GameClient implements Runnable {

    private final String serverAddress;
    private final int serverPort;
    private Socket socket;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;
    private volatile boolean running = false;
    private final BlockingQueue<NetworkMessage> outgoingMessages = new LinkedBlockingQueue<>();
    private final ExecutorService networkExecutor = Executors.newFixedThreadPool(2); // One for sending, one for receiving

    // Callbacks to notify the UI/Game Layer
    private Consumer<NetworkMessage> onMessageReceived;
    private Consumer<Boolean> onConnectionStatusChanged; // True for connected, false for disconnected

    public GameClient(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }

    // Setters for callbacks (must be set before starting the client thread)
    public void setOnMessageReceived(Consumer<NetworkMessage> onMessageReceived) {
        this.onMessageReceived = onMessageReceived;
    }

    public void setOnConnectionStatusChanged(Consumer<Boolean> onConnectionStatusChanged) {
        this.onConnectionStatusChanged = onConnectionStatusChanged;
    }

    @Override
    public void run() {
        running = true;
        try {
            socket = new Socket(serverAddress, serverPort);
            outputStream = new ObjectOutputStream(socket.getOutputStream());
            inputStream = new ObjectInputStream(socket.getInputStream());
            System.out.println("Connected to server: " + serverAddress + ":" + serverPort);

            // Notify UI/Game of successful connection
            if (onConnectionStatusChanged != null) {
                onConnectionStatusChanged.accept(true);
            }

            // Start sender and receiver threads
            networkExecutor.submit(this::sendMessages);
            networkExecutor.submit(this::receiveMessages);

            // Send initial info
            User localUser = SceneManager.getInstance().getCurrentUser();
            String username = (localUser != null) ? localUser.getUsername() : "Player";
            sendMessage(new ClientInfoMessage(username));


        } catch (IOException e) {
            System.err.println("Failed to connect to server: " + e.getMessage());
            running = false; // Ensure loops stop
            if (onConnectionStatusChanged != null) {
                onConnectionStatusChanged.accept(false); // Notify failure
            }
        }
        // Keep the main client thread alive while sender/receiver run (or until disconnect)
        while (running && !networkExecutor.isShutdown()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                stopClient();
            }
        }
        System.out.println("GameClient main loop finished.");
    }

    // --- Sending Logic (Runs in its own thread) ---
    private void sendMessages() {
        while (running) {
            try {
                NetworkMessage message = outgoingMessages.take(); // Blocks until message is available
                if (outputStream != null) {
                    try {
                        outputStream.writeObject(message);
                        outputStream.reset(); // Clear object stream cache
                        outputStream.flush();
                    } catch (SocketException e) {
                        System.out.println("Server connection lost while sending.");
                        stopClient(); // Trigger shutdown
                    } catch (IOException e) {
                        if (running) {
                            System.err.println("IO Error sending message: " + e.getMessage());
                            stopClient();
                        }
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                running = false;
            }
        }
        System.out.println("GameClient sender thread finished.");
    }

    // --- Receiving Logic (Runs in its own thread) ---
    private void receiveMessages() {
        while (running) {
            try {
                if (inputStream != null) {
                    NetworkMessage message = (NetworkMessage) inputStream.readObject();
                    // Pass the received message to the UI/Game layer via callback
                    if (onMessageReceived != null) {
                        // Crucially, ensure the callback runs on the JavaFX thread if it updates UI/FXGL
                        javafx.application.Platform.runLater(() -> onMessageReceived.accept(message));
                    }
                } else {
                    // InputStream closed, likely disconnected
                    if (running) stopClient();
                }
            } catch (ClassNotFoundException e) {
                System.err.println("Received unknown message type from server: " + e.getMessage());
            } catch (EOFException | SocketException e) {
                System.out.println("Server connection closed.");
                stopClient(); // Trigger shutdown
            } catch (IOException e) {
                if (running) {
                    System.err.println("IO Error receiving message: " + e.getMessage());
                    stopClient();
                }
            }
        }
        System.out.println("GameClient receiver thread finished.");
    }

    // --- Public Methods ---

    // Method for the game/UI to send a message
    public void sendMessage(NetworkMessage message) {
        if (running) {
            outgoingMessages.offer(message); // Non-blocking add to queue
        }
    }

    public void stopClient() {
        if (!running) return; // Prevent multiple shutdowns
        System.out.println("Stopping client connection...");
        running = false;

        // Signal disconnection
        if (onConnectionStatusChanged != null) {
            javafx.application.Platform.runLater(() -> onConnectionStatusChanged.accept(false));
        }

        // Interrupt threads and close resources
        networkExecutor.shutdownNow(); // Interrupt sender/receiver threads
        outgoingMessages.clear(); // Clear any pending messages

        try {
            if (outputStream != null) outputStream.close();
            if (inputStream != null) inputStream.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("Error closing client socket/streams: " + e.getMessage());
        }
        System.out.println("Client connection stopped.");
    }

    public boolean isRunning() {
        return running;
    }
}