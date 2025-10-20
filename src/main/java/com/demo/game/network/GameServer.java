// File: com/demo/game/network/GameServer.java
package com.demo.game.network;

import com.almasb.fxgl.core.math.Vec2;
import com.almasb.fxgl.dsl.FXGL;
import com.almasb.fxgl.time.LocalTimer;
import com.demo.game.Config;
import com.demo.game.models.User;
import com.demo.game.network.messages.*;
import com.demo.game.ui.SceneManager;
import javafx.geometry.Point2D;
import javafx.util.Duration;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class GameServer implements Runnable {
    // ... (constants remain the same) ...
    public static final int PORT = 12345;
    private static final double GAME_UPDATE_RATE_HZ = 60.0;
    private static final double GAME_UPDATE_INTERVAL_MS = 1000.0 / GAME_UPDATE_RATE_HZ;

    private ServerSocket serverSocket;
    private final ConcurrentHashMap<Integer, ClientHandler> clients = new ConcurrentHashMap<>();
    private final BlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<>();
    private final ExecutorService taskExecutor = Executors.newSingleThreadExecutor();
    private int nextClientId = 1; // **FIX**: Start non-host IDs from 1
    private volatile boolean running = false;
    private ScheduledExecutorService gameLoopExecutor;

    // --- Lobby State ---
    private final ConcurrentHashMap<Integer, String> playerUsernames = new ConcurrentHashMap<>();

    // --- Authoritative Game State ---
    // ... (playerStates, bombHolderId, etc. remain the same) ...
    private volatile boolean gameStarted = false;
    private volatile boolean gameOver = false;
    private final ConcurrentHashMap<Integer, PlayerServerState> playerStates = new ConcurrentHashMap<>();
    private volatile int bombHolderId = -1;
    private volatile Point2D bombPosition = new Point2D(-100, -100);
    private LocalTimer bombTimer;
    private volatile double bombStartTime = -1;
    private LocalTimer passCooldownTimer;
    private volatile double lastTickTime = System.nanoTime() / 1_000_000_000.0;

    // ... (PlayerServerState inner class remains the same) ...
    private static class PlayerServerState {
        int id;
        String username;
        Point2D position;
        Vec2 velocity = new Vec2();
        boolean eliminated = false;
        Point2D inputDirection = Point2D.ZERO;

        PlayerServerState(int id, String username, Point2D position) {
            this.id = id;
            this.username = username;
            this.position = position;
        }
    }


    public GameServer() {
        // **FIX**: No longer pre-populate the host.
        // The host will connect as a client and be assigned ID 0.
        bombTimer = FXGL.newLocalTimer();
        passCooldownTimer = FXGL.newLocalTimer();
    }

    @Override
    public void run() {
        running = true;
        taskExecutor.submit(this::processTasks);

        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Server started on port " + PORT);

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Client connected: " + clientSocket.getInetAddress());

                    // **FIX**: Assign ID 0 to the first client (the host),
                    // and incrementing IDs to all others.
                    int clientId = (clients.isEmpty()) ? 0 : nextClientId++;

                    ClientHandler handler = new ClientHandler(clientSocket, clientId, this);
                    clients.put(clientId, handler);
                    new Thread(handler).start();

                } catch (SocketTimeoutException e) {
                    // Ignore
                } catch (IOException e) {
                    if (running) {
                        System.err.println("Error accepting client connection: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Could not start server: " + e.getMessage());
        } finally {
            stopServer();
        }
    }

    // ... (submitTask and processTasks remain the same) ...
    public void submitTask(Runnable task) {
        try { taskQueue.put(task); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
    private void processTasks() {
        while (running) {
            try { Runnable task = taskQueue.take(); task.run(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); running = false; } catch (Exception e) { System.err.println("Error processing game task: " + e.getMessage()); e.printStackTrace(); }
        }
    }

    // --- Server Actions (Called via submitTask) ---

    public void handleClientInfo(int clientId, ClientInfoMessage msg) {
        if (gameStarted) return;

        // **FIX**: Add (Host) tag to client 0
        String username = (clientId == 0) ? msg.username + " (Host)" : msg.username;
        playerUsernames.put(clientId, username);
        System.out.println("Client " + clientId + " registered as: " + username);

        broadcastLobbyUpdate();
    }

    // ... (handlePlayerInput and handlePassBombAttempt remain the same) ...
    public void handlePlayerInput(int clientId, PlayerInputMessage msg) {
        if (!gameStarted || gameOver) return;
        PlayerServerState state = playerStates.get(clientId);
        if (state == null || state.eliminated) return;

        switch (msg.inputType) {
            case MOVE_LEFT:  state.inputDirection = new Point2D(-1, state.inputDirection.getY()); break;
            case MOVE_RIGHT: state.inputDirection = new Point2D( 1, state.inputDirection.getY()); break;
            case MOVE_UP:    state.inputDirection = new Point2D(state.inputDirection.getX(), -1); break;
            case MOVE_DOWN:  state.inputDirection = new Point2D(state.inputDirection.getX(),  1); break;
            case STOP_X:     state.inputDirection = new Point2D(0, state.inputDirection.getY()); break;
            case STOP_Y:     state.inputDirection = new Point2D(state.inputDirection.getX(),  0); break;
            case PASS_BOMB:  handlePassBombAttempt(clientId); break;
        }
        if (state.inputDirection.magnitude() > 0) {
            state.inputDirection = state.inputDirection.normalize();
        }
        state.velocity = new Vec2(state.inputDirection.multiply(Config.PLAYER_SPEED));
    }
    private void handlePassBombAttempt(int passerId) {
        if (bombHolderId != passerId || gameOver) return;
        if (!passCooldownTimer.elapsed(Config.PASS_COOLDOWN)) return;
        PlayerServerState passerState = playerStates.get(passerId);
        if (passerState == null || passerState.eliminated) return;
        PlayerServerState target = null;
        double closestDistSq = Config.PASS_RANGE * Config.PASS_RANGE;
        for(PlayerServerState potentialTarget : playerStates.values()) {
            if (potentialTarget.id != passerId && !potentialTarget.eliminated) {
                double dx = passerState.position.getX() - potentialTarget.position.getX();
                double dy = passerState.position.getY() - potentialTarget.position.getY();
                double distSq = dx * dx + dy * dy;
                if (distSq <= closestDistSq) {
                    closestDistSq = distSq;
                    target = potentialTarget;
                }
            }
        }
        if (target != null) {
            System.out.println("Server: Player " + passerId + " passing bomb to " + target.id);
            bombHolderId = target.id;
            bombTimer.capture();
            bombStartTime = FXGL.getGameTimer().getNow();
            passCooldownTimer.capture();
            broadcast(new BombPassMessage(target.id));
        }
    }
    // ... (removeClient remains the same) ...
    public void removeClient(int clientId) {
        clients.remove(clientId);
        playerUsernames.remove(clientId);
        PlayerServerState removedPlayer = playerStates.remove(clientId);
        System.out.println("Client " + clientId + " disconnected.");
        if (!gameStarted) {
            broadcastLobbyUpdate();
        } else if (removedPlayer != null && !removedPlayer.eliminated) {
            removedPlayer.eliminated = true;
            if (bombHolderId == clientId) {
                resetBomb();
            }
            checkWinCondition();
        }
    }


    public void startGame() {
        if (gameStarted) return;
        System.out.println("Server starting game...");

        List<Point2D> spawnPoints = List.of(
                new Point2D(100, 100), new Point2D(Config.SCREEN_WIDTH - 100, 100),
                new Point2D(100, Config.SCREEN_HEIGHT - 100), new Point2D(Config.SCREEN_WIDTH - 100, Config.SCREEN_HEIGHT - 100),
                new Point2D(Config.SCREEN_WIDTH / 2.0, 100), new Point2D(Config.SCREEN_WIDTH / 2.0, Config.SCREEN_HEIGHT - 100)
        );

        // **FIX**: Use SPoint2D for the message
        Map<Integer, SPoint2D> initialPositions = new HashMap<>();
        List<Integer> playerIds = new ArrayList<>(playerUsernames.keySet());
        Collections.shuffle(playerIds);

        int spawnIndex = 0;
        playerStates.clear();

        for (int id : playerIds) {
            Point2D pos = spawnPoints.get(spawnIndex % spawnPoints.size());
            // **FIX**: Convert to SPoint2D for the map
            initialPositions.put(id, new SPoint2D(pos));
            playerStates.put(id, new PlayerServerState(id, playerUsernames.get(id), pos));
            spawnIndex++;
        }

        if (!playerIds.isEmpty()) {
            bombHolderId = playerIds.get(new Random().nextInt(playerIds.size()));
            bombTimer.capture();
            bombStartTime = FXGL.getGameTimer().getNow();
            passCooldownTimer.capture();
            System.out.println("Initial bomb holder: " + bombHolderId);
        } else {
            bombHolderId = -1;
            bombStartTime = -1;
        }

        PlayerServerState initialHolderState = playerStates.get(bombHolderId);
        bombPosition = (initialHolderState != null) ? initialHolderState.position : new Point2D(-100,-100);

        gameStarted = true;
        gameOver = false;

        // **FIX**: Get usernames *in order of their IDs*
        List<String> orderedUsernames = playerIds.stream()
                .sorted() // Sort by ID (0, 1, 2...)
                .map(playerUsernames::get)
                .collect(Collectors.toList());

        // **FIX**: Send the message with SPoint2D map
        broadcast(new GameStartMessage(initialPositions, orderedUsernames));

        lastTickTime = System.nanoTime() / 1_000_000_000.0;
        if (gameLoopExecutor != null) gameLoopExecutor.shutdownNow();
        gameLoopExecutor = Executors.newSingleThreadScheduledExecutor();
        gameLoopExecutor.scheduleAtFixedRate(this::gameTick, 0, (long)GAME_UPDATE_INTERVAL_MS, TimeUnit.MILLISECONDS);
        System.out.println("Game loop started.");
    }

    // --- Game Loop (Runs on gameLoopExecutor) ---
    private void gameTick() {
        if (!running || !gameStarted || gameOver) {
            return;
        }

        try {
            double now = System.nanoTime() / 1_000_000_000.0;
            double tpf = now - lastTickTime;
            lastTickTime = now;
            if (tpf > 0.1) tpf = 0.1;

            // 1. Update Player Positions
            // **FIX**: Use SPoint2D for the message map
            Map<Integer, SPoint2D> currentPositions = new HashMap<>();
            for (PlayerServerState state : playerStates.values()) {
                if (!state.eliminated) {
                    Vec2 currentVelocity = state.velocity;
                    state.position = state.position.add(currentVelocity.mul(tpf).toPoint2D());

                    // ... (clamping logic remains the same) ...
                    state.position = new Point2D(
                            Math.max(Config.WALL_SIZE, Math.min(state.position.getX(), Config.SCREEN_WIDTH - Config.WALL_SIZE - Config.PLAYER_SIZE)),
                            Math.max(Config.WALL_SIZE, Math.min(state.position.getY(), Config.SCREEN_HEIGHT - Config.WALL_SIZE - Config.PLAYER_SIZE))
                    );

                    // **FIX**: Convert to SPoint2D for the message
                    currentPositions.put(state.id, new SPoint2D(state.position));
                }
            }

            // 2. Update Bomb Position & Check Timer
            // ... (bomb logic remains the same) ...
            double bombTimeRemaining = -1.0;
            if (bombHolderId != -1) {
                PlayerServerState holder = playerStates.get(bombHolderId);
                if (holder != null && !holder.eliminated) {
                    bombPosition = holder.position.add(Config.PLAYER_SIZE / 2.0 - Config.BOMB_SIZE / 2.0, Config.PLAYER_SIZE / 2.0 - Config.BOMB_SIZE / 2.0);
                    if (bombStartTime > 0) {
                        double elapsedSeconds = FXGL.getGameTimer().getNow() - bombStartTime;
                        bombTimeRemaining = Math.max(0.0, Config.BOMB_TIMER_DURATION.toSeconds() - elapsedSeconds);
                    } else {
                        bombTimeRemaining = Config.BOMB_TIMER_DURATION.toSeconds();
                    }
                    if (bombTimer.elapsed(Config.BOMB_TIMER_DURATION)) {
                        System.out.println("Server: Bomb exploded on player " + bombHolderId);
                        holder.eliminated = true;
                        broadcast(new PlayerEliminatedMessage(bombHolderId));
                        resetBomb();
                        checkWinCondition();
                        bombTimeRemaining = -1.0;
                    }
                } else {
                    resetBomb();
                    bombTimeRemaining = -1.0;
                    bombStartTime = -1.0;
                }
            } else {
                bombPosition = new Point2D(-100, -100);
                bombTimeRemaining = -1.0;
                bombStartTime = -1.0;
            }

            // 3. Broadcast Game State Update
            if (!gameOver) {
                // **FIX**: Convert bombPosition to SPoint2D for the message
                broadcast(new GameStateUpdateMessage(currentPositions, bombHolderId, new SPoint2D(bombPosition), bombTimeRemaining));
            }

        } catch (Exception e) {
            System.err.println("Error during game tick: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ... (resetBomb and checkWinCondition remain the same) ...
    private void resetBomb() {
        List<Integer> activePlayerIds = playerStates.values().stream().filter(p -> !p.eliminated).map(p -> p.id).collect(Collectors.toList());
        if (!activePlayerIds.isEmpty()) {
            bombHolderId = activePlayerIds.get(new Random().nextInt(activePlayerIds.size()));
            bombTimer.capture();
            bombStartTime = FXGL.getGameTimer().getNow();
            passCooldownTimer.capture();
            System.out.println("Server: Bomb reset and given to " + bombHolderId);
            broadcast(new BombPassMessage(bombHolderId));
        } else {
            bombHolderId = -1;
            bombStartTime = -1;
            bombPosition = new Point2D(-100,-100);
            System.out.println("Server: Bomb reset, no active players left.");
        }
    }
    private void checkWinCondition() {
        if (gameOver) return;
        List<PlayerServerState> activePlayers = playerStates.values().stream().filter(p -> !p.eliminated).collect(Collectors.toList());
        if (activePlayers.size() <= 1) {
            gameOver = true;
            String winnerUsername = activePlayers.isEmpty() ? "No one" : activePlayers.get(0).username;
            System.out.println("Server: Game Over! Winner: " + winnerUsername);
            broadcast(new GameOverMessage(winnerUsername));
            if (gameLoopExecutor != null) {
                gameLoopExecutor.shutdown();
                try {
                    if (!gameLoopExecutor.awaitTermination(1, TimeUnit.SECONDS)) { gameLoopExecutor.shutdownNow(); }
                } catch (InterruptedException e) { gameLoopExecutor.shutdownNow(); Thread.currentThread().interrupt(); }
                System.out.println("Game loop stopped.");
            }
        }
    }

    // ... (Broadcasting and Shutdown methods remain the same) ...
    public void broadcast(NetworkMessage message) {
        for (ClientHandler handler : clients.values()) {
            handler.sendMessage(message);
        }
    }
    public void broadcastLobbyUpdate() {
        List<String> currentPlayers = new ArrayList<>(playerUsernames.values());
        broadcast(new LobbyUpdateMessage(currentPlayers));
    }
    public List<String> getCurrentPlayerUsernames() {
        return new ArrayList<>(playerUsernames.values());
    }
    public void stopServer() {
        if (!running) return;
        System.out.println("Stopping server...");
        running = false;
        gameOver = true;
        if (gameLoopExecutor != null) { gameLoopExecutor.shutdownNow(); }
        taskExecutor.shutdownNow();
        clients.values().forEach(ClientHandler::closeConnection);
        clients.clear();
        playerStates.clear();
        playerUsernames.clear();
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing server socket: " + e.getMessage());
        }
        System.out.println("Server stopped.");
    }
}