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
                    broadcastMessage("TURN:0");
                }
            }
        } catch (IOException e) {
            System.err.println("Error pada server: " + e.getMessage());
        }
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
            }
        }
        
        public void sendMessage(String message) {
            if (writer != null) {
                writer.println(message);
            }
 }
}
}
