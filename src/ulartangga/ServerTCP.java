package ulartangga;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ServerTCP {
    private static final int PORT = 12345;
    private static final int MAX_PLAYERS = 3;
    private static List<ClientHandler> players = new ArrayList<>();
    private static int[] playerPositions = {0, 0, 0};
    private static int currentPlayer = 0;
    private static boolean gameStarted = false;
    private static boolean gameEnded = false;
    
    //
    private static Map<Integer, Integer> snakes = new HashMap<>();
    private static Map<Integer, Integer> ladders = new HashMap<>();
    
    static {
        // Ular (dari kepala ke ekor)
        snakes.put(16, 6);
        snakes.put(47, 26);
        snakes.put(49, 11);
        snakes.put(56, 53);
        snakes.put(62, 19);
        snakes.put(64, 60);
        snakes.put(87, 24);
        snakes.put(93, 73);
        snakes.put(95, 75);
        snakes.put(98, 78);
        
        // Tangga (dari bawah ke atas)
        ladders.put(1, 38);
        ladders.put(4, 14);
        ladders.put(9, 21);
        ladders.put(21, 42);
        ladders.put(28, 84);
        ladders.put(36, 44);
        ladders.put(51, 67);
        ladders.put(71, 91);
        ladders.put(80, 100);
    }
    
    public static void main(String[] args) {
        System.out.println("Server Ular Tangga dimulai di port " + PORT);
        
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (players.size() < MAX_PLAYERS) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket, players.size());
                players.add(clientHandler);
                
                System.out.println("Pemain " + (players.size()) + " terhubung");
                
                new Thread(clientHandler).start();
                
                if (players.size() == MAX_PLAYERS) {
                    gameStarted = true;
                    broadcastMessage("GAME_START");
                    broadcastGameState();
                    broadcastMessage("TURN:0"); // Pemain 1 mulai
                }
            }
        } catch (IOException e) {
            System.err.println("Error pada server: " + e.getMessage());
        }
    }
    
    private static List<Integer> winnerOrder = new ArrayList<>();
    private static boolean[] hasFinished = {false, false, false};
    
    public static synchronized void processMove(int playerId, int diceRoll) {
        if (gameEnded || currentPlayer != playerId || hasFinished[playerId]) {
            return;
        }
        
        int oldPosition = playerPositions[playerId];
        int newPosition = oldPosition + diceRoll;
        boolean rollAgain = false;
        
        // Aturan khusus untuk mendekati 100
        if (oldPosition >= 97) {
            int stepsToWin = 100 - oldPosition;
            if (diceRoll > stepsToWin) {
                newPosition = oldPosition; // Tetap di posisi jika melebihi
                broadcastMessage("BOUNCE:" + playerId + ":" + oldPosition + ":" + diceRoll);
            } else if (diceRoll == stepsToWin) {
                newPosition = 100; // Menang tepat
            } else {
                newPosition = oldPosition + diceRoll;
            }
        } else {
            if (newPosition > 100) {
                newPosition = oldPosition;
                broadcastMessage("BOUNCE:" + playerId + ":" + oldPosition + ":" + diceRoll);
            } else {
                if (snakes.containsKey(newPosition)) {
                    int snakeEnd = snakes.get(newPosition);
                    broadcastMessage("SNAKE:" + playerId + ":" + newPosition + ":" + snakeEnd);
                    newPosition = snakeEnd;
                }
                else if (ladders.containsKey(newPosition)) {
                    int ladderEnd = ladders.get(newPosition);
                    broadcastMessage("LADDER:" + playerId + ":" + newPosition + ":" + ladderEnd);
                    newPosition = ladderEnd;
                }
            }
        }
        
        playerPositions[playerId] = newPosition;
        
        // Cek kemenangan
        if (newPosition == 100) {
            hasFinished[playerId] = true;
            winnerOrder.add(playerId);
            broadcastMessage("PLAYER_FINISHED:" + playerId + ":" + winnerOrder.size());
            
            // Cek apakah semua pemain sudah selesai atau hanya 1 yang tersisa
            int remainingPlayers = 0;
            for (boolean finished : hasFinished) {
                if (!finished) remainingPlayers++;
            }
            
            if (remainingPlayers <= 1 || winnerOrder.size() == MAX_PLAYERS) {
                // Tambahkan pemain yang belum selesai ke urutan terakhir
                for (int i = 0; i < MAX_PLAYERS; i++) {
                    if (!hasFinished[i]) {
                        winnerOrder.add(i);
                        hasFinished[i] = true;
                    }
                }
                gameEnded = true;
                broadcastLeaderboard();
                return;
            }
        }
        
        if (diceRoll == 6 && !hasFinished[playerId]) {
            rollAgain = true;
            broadcastMessage("ROLL_AGAIN:" + playerId);
        }
        
        broadcastGameState();
        if (!rollAgain) {
            do {
                currentPlayer = (currentPlayer + 1) % MAX_PLAYERS;
            } while (hasFinished[currentPlayer] && !gameEnded);
        }
        
        if (!gameEnded) {
            broadcastMessage("TURN:" + currentPlayer);
        }
    }
    
    private static void broadcastLeaderboard() {
        StringBuilder leaderboard = new StringBuilder("LEADERBOARD:");
        for (int i = 0; i < winnerOrder.size(); i++) {
            int playerId = winnerOrder.get(i);
            leaderboard.append(playerId).append(",").append(i + 1);
            if (i < winnerOrder.size() - 1) {
                leaderboard.append(";");
            }
        }
        broadcastMessage(leaderboard.toString());
    }
    
    public static void broadcastMessage(String message) {
        for (ClientHandler player : players) {
            player.sendMessage(message);
        }
    }
    
    public static void broadcastGameState() {
        String state = "STATE:" + playerPositions[0] + ":" + playerPositions[1] + ":" + playerPositions[2];
        broadcastMessage(state);
    }
    
    static class ClientHandler implements Runnable {
        private Socket socket;
        private BufferedReader reader;
        private PrintWriter writer;
        private int playerId;
        
        public ClientHandler(Socket socket, int playerId) {
            this.socket = socket;
            this.playerId = playerId;
            
            try {
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                writer = new PrintWriter(socket.getOutputStream(), true);
                
                // Kirim ID pemain ke client
                writer.println("PLAYER_ID:" + playerId);
                
            } catch (IOException e) {
                System.err.println("Error membuat ClientHandler: " + e.getMessage());
            }
        }
        
        @Override
        public void run() {
            try {
                String message;
                while ((message = reader.readLine()) != null) {
                    if (message.startsWith("ROLL:")) {
                        int diceRoll = Integer.parseInt(message.substring(5));
                        System.out.println("Pemain " + (playerId + 1) + " melempar dadu: " + diceRoll);
                        processMove(playerId, diceRoll);
                    }
                }
            } catch (IOException e) {
                System.err.println("Error pada ClientHandler: " + e.getMessage());
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    System.err.println("Error menutup socket: " + e.getMessage());
                }
            }
        }
        
        public void sendMessage(String message) {
            if (writer != null) {
                writer.println(message);
            }
  } 
}
}