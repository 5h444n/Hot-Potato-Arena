// File: com/demo/game/network/GameServer.java
package com.demo.game.network;

import com.almasb.fxgl.core.math.Vec2;
import com.almasb.fxgl.dsl.FXGL; // Need FXGL to access GameTimer
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

    public static final int PORT = 12345;
    private static final double GAME_UPDATE_RATE_HZ = 60.0;
    private static final double GAME_UPDATE_INTERVAL_MS = 1000.0 / GAME_UPDATE_RATE_HZ;

    private ServerSocket serverSocket;
    private final ConcurrentHashMap<Integer, ClientHandler> clients = new ConcurrentHashMap<>();
    private final BlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<>();
    private final ExecutorService taskExecutor = Executors.newSingleThreadExecutor();
    private int nextClientId = 1;
    private volatile boolean running = false;
    private ScheduledExecutorService gameLoopExecutor;

    // --- Lobby State ---
    private final ConcurrentHashMap<Integer, String> playerUsernames = new ConcurrentHashMap<>();

    // --- Authoritative Game State ---
    private volatile boolean gameStarted = false;
    private volatile boolean gameOver = false;
    private final ConcurrentHashMap<Integer, PlayerServerState> playerStates = new ConcurrentHashMap<>();
    private volatile int bombHolderId = -1;
    private volatile Point2D bombPosition = new Point2D(-100, -100);
    private LocalTimer bombTimer; // Used for checking *if* duration elapsed
    private volatile double bombStartTime = -1; // Time when current bomb timer started (using GameTimer)
    private LocalTimer passCooldownTimer;
    private volatile double lastTickTime = System.nanoTime() / 1_000_000_000.0;


    // Simple class to hold server-side player state
    private static class PlayerServerState {
        int id;
        String username;
        Point2D position;
        Vec2 velocity = new Vec2(); // Defaults to 0,0
        boolean eliminated = false;
        Point2D inputDirection = Point2D.ZERO;

        PlayerServerState(int id, String username, Point2D position) {
            this.id = id;
            this.username = username;
            this.position = position;
        }
    }


    public GameServer() {
        User hostUser = SceneManager.getInstance().getCurrentUser();
        String hostUsername = (hostUser != null) ? hostUser.getUsername() : "Host";
        playerUsernames.put(0, hostUsername + " (Host)");
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

                    int clientId = nextClientId++;
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

    public void submitTask(Runnable task) {
        try {
            taskQueue.put(task);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void processTasks() {
        while (running) {
            try {
                Runnable task = taskQueue.take();
                task.run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                running = false;
            } catch (Exception e) {
                System.err.println("Error processing game task: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }


    // --- Server Actions (Called via submitTask) ---

    public void handleClientInfo(int clientId, ClientInfoMessage msg) {
        if (gameStarted) return;
        playerUsernames.put(clientId, msg.username);
        System.out.println("Client " + clientId + " registered as: " + msg.username);
        broadcastLobbyUpdate();
    }

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
            bombTimer.capture(); // Reset elapsed check timer
            bombStartTime = FXGL.getGameTimer().getNow(); // RECORD START TIME for remaining time calc
            passCooldownTimer.capture();

            broadcast(new BombPassMessage(target.id));
        }
    }


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

        Map<Integer, Point2D> initialPositions = new HashMap<>();
        List<Integer> playerIds = new ArrayList<>(playerUsernames.keySet());
        Collections.shuffle(playerIds);

        int spawnIndex = 0;
        playerStates.clear();

        for (int id : playerIds) {
            Point2D pos = spawnPoints.get(spawnIndex % spawnPoints.size());
            initialPositions.put(id, pos);
            playerStates.put(id, new PlayerServerState(id, playerUsernames.get(id), pos));
            spawnIndex++;
        }

        if (!playerIds.isEmpty()) {
            bombHolderId = playerIds.get(new Random().nextInt(playerIds.size()));
            bombTimer.capture(); // Start check timer
            bombStartTime = FXGL.getGameTimer().getNow(); // RECORD START TIME
            passCooldownTimer.capture();
            System.out.println("Initial bomb holder: " + bombHolderId);
        } else {
            bombHolderId = -1;
            bombStartTime = -1; // Ensure start time is reset
        }
        // Safely get bomb position
        PlayerServerState initialHolderState = playerStates.get(bombHolderId);
        bombPosition = (initialHolderState != null) ? initialHolderState.position : new Point2D(-100,-100);

        gameStarted = true;
        gameOver = false;

        List<String> orderedUsernames = playerIds.stream()
                .map(playerUsernames::get)
                .collect(Collectors.toList());
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
            Map<Integer, Point2D> currentPositions = new HashMap<>();
            for (PlayerServerState state : playerStates.values()) {
                if (!state.eliminated) {
                    Vec2 currentVelocity = state.velocity;
                    state.position = state.position.add(currentVelocity.mul(tpf).toPoint2D());

                    double halfSize = Config.PLAYER_SIZE / 2.0;
                    state.position = new Point2D(
                            Math.max(Config.WALL_SIZE, Math.min(state.position.getX(), Config.SCREEN_WIDTH - Config.WALL_SIZE - Config.PLAYER_SIZE)),
                            Math.max(Config.WALL_SIZE, Math.min(state.position.getY(), Config.SCREEN_HEIGHT - Config.WALL_SIZE - Config.PLAYER_SIZE))
                    );
                    currentPositions.put(state.id, state.position);
                }
            }

            // 2. Update Bomb Position & Check Timer
            double bombTimeRemaining = -1.0;
            if (bombHolderId != -1) {
                PlayerServerState holder = playerStates.get(bombHolderId);
                if (holder != null && !holder.eliminated) {
                    bombPosition = holder.position.add(Config.PLAYER_SIZE / 2.0 - Config.BOMB_SIZE / 2.0, Config.PLAYER_SIZE / 2.0 - Config.BOMB_SIZE / 2.0);

                    // --- CALCULATE REMAINING TIME using bombStartTime ---
                    if (bombStartTime > 0) {
                        // Use FXGL's GameTimer to get current time reliably
                        double elapsedSeconds = FXGL.getGameTimer().getNow() - bombStartTime;
                        bombTimeRemaining = Math.max(0.0, Config.BOMB_TIMER_DURATION.toSeconds() - elapsedSeconds);
                    } else {
                        bombTimeRemaining = Config.BOMB_TIMER_DURATION.toSeconds(); // Should not happen
                    }
                    // ---

                    // Check for explosion using elapsed(Duration)
                    if (bombTimer.elapsed(Config.BOMB_TIMER_DURATION)) {
                        System.out.println("Server: Bomb exploded on player " + bombHolderId);
                        holder.eliminated = true;
                        broadcast(new PlayerEliminatedMessage(bombHolderId));
                        resetBomb(); // This resets bombStartTime
                        checkWinCondition();
                        bombTimeRemaining = -1.0;
                    }
                } else {
                    resetBomb();
                    bombTimeRemaining = -1.0;
                    bombStartTime = -1.0; // Ensure reset
                }
            } else {
                bombPosition = new Point2D(-100, -100);
                bombTimeRemaining = -1.0;
                bombStartTime = -1.0; // Ensure reset
            }


            // 3. Broadcast Game State Update
            if (!gameOver) {
                broadcast(new GameStateUpdateMessage(currentPositions, bombHolderId, bombPosition, bombTimeRemaining));
            }

        } catch (Exception e) {
            System.err.println("Error during game tick: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void resetBomb() {
        List<Integer> activePlayerIds = playerStates.values().stream()
                .filter(p -> !p.eliminated)
                .map(p -> p.id)
                .collect(Collectors.toList());

        if (!activePlayerIds.isEmpty()) {
            bombHolderId = activePlayerIds.get(new Random().nextInt(activePlayerIds.size()));
            bombTimer.capture(); // Reset check timer
            bombStartTime = FXGL.getGameTimer().getNow(); // RECORD START TIME
            passCooldownTimer.capture();
            System.out.println("Server: Bomb reset and given to " + bombHolderId);
            broadcast(new BombPassMessage(bombHolderId));
        } else {
            bombHolderId = -1;
            bombStartTime = -1; // RESET START TIME
            bombPosition = new Point2D(-100,-100);
            System.out.println("Server: Bomb reset, no active players left.");
        }
    }

    private void checkWinCondition() {
        if (gameOver) return;

        List<PlayerServerState> activePlayers = playerStates.values().stream()
                .filter(p -> !p.eliminated)
                .collect(Collectors.toList());

        if (activePlayers.size() <= 1) {
            gameOver = true;
            String winnerUsername = activePlayers.isEmpty() ? "No one" : activePlayers.get(0).username;
            System.out.println("Server: Game Over! Winner: " + winnerUsername);
            broadcast(new GameOverMessage(winnerUsername));

            if (gameLoopExecutor != null) {
                gameLoopExecutor.shutdown();
                try {
                    if (!gameLoopExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                        gameLoopExecutor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    gameLoopExecutor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
                System.out.println("Game loop stopped.");
            }
        }
    }


    // --- Broadcasting ---

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

    // --- Shutdown ---

    public void stopServer() {
        if (!running) return;
        System.out.println("Stopping server...");
        running = false;
        gameOver = true;

        if (gameLoopExecutor != null) {
            gameLoopExecutor.shutdownNow();
        }
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