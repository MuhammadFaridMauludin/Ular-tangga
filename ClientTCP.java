package ulartangga;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import java.util.Random;

public class ClientTCP extends JFrame {
    private static final String SERVER_IP = "localhost"; // Ganti dengan IP server
    private static final int SERVER_PORT = 12345;
    
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    
    private int playerId = -1;
    private int[] playerPositions = {0, 0, 0};
    private boolean isMyTurn = false;
    private boolean gameEnded = false;
    private boolean rollAgain = false;
    private java.util.List<String> gameLog = new java.util.ArrayList<>();
    
    // Leaderboard
    private java.util.Map<Integer, Integer> leaderboard = new java.util.HashMap<>();
    
    // UI Components
    private JPanel boardPanel;
    private JButton rollButton;
    private JLabel statusLabel;
    private JLabel diceLabel;
    private JLabel[] playerLabels = new JLabel[3];
    
    // Colors untuk pemain
    private Color[] playerColors = {Color.RED, Color.BLUE, Color.GREEN};
    private boolean gameStarted;
    
    public ClientTCP() {
        initializeUI();
        connectToServer();
    }
    
    private void initializeUI() {
        setTitle("Ular Tangga - Pemain ?");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        
        // Panel atas untuk status
        JPanel topPanel = new JPanel(new FlowLayout());
        statusLabel = new JLabel("Menunggu koneksi...");
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        diceLabel = new JLabel("Dadu: -");
        diceLabel.setFont(new Font("Arial", Font.BOLD, 14));
        
        topPanel.add(statusLabel);
        topPanel.add(Box.createHorizontalStrut(20));
        topPanel.add(diceLabel);
        
        add(topPanel, BorderLayout.NORTH);
        
        // Panel tengah untuk papan
        boardPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawBoard(g);
            }
        };
        boardPanel.setPreferredSize(new Dimension(600, 600));
        boardPanel.setBackground(Color.WHITE);
        add(boardPanel, BorderLayout.CENTER);
        
        // Panel bawah untuk kontrol
        JPanel bottomPanel = new JPanel(new FlowLayout());
        rollButton = new JButton("Lempar Dadu");
        rollButton.setEnabled(false);
        rollButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                rollDice();
            }
        });
        
        bottomPanel.add(rollButton);
        
        // Label untuk posisi pemain
        for (int i = 0; i < 3; i++) {
            playerLabels[i] = new JLabel("Pemain " + (i + 1) + ": 0");
            playerLabels[i].setForeground(playerColors[i]);
            playerLabels[i].setFont(new Font("Arial", Font.BOLD, 12));
            bottomPanel.add(Box.createHorizontalStrut(10));
            bottomPanel.add(playerLabels[i]);
        }
        
        add(bottomPanel, BorderLayout.SOUTH);
        
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }
    
    private void drawBoard(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int cellSize = 50;
        int boardSize = 10;
        int offsetX = 50;
        int offsetY = 50;
        
        // Gambar kotak-kotak papan
        for (int row = 0; row < boardSize; row++) {
            for (int col = 0; col < boardSize; col++) {
                int x = offsetX + col * cellSize;
                int y = offsetY + (9 - row) * cellSize; // Baris terbalik
                
                // Hitung nomor kotak
                int cellNumber;
                if (row % 2 == 0) {
                    cellNumber = row * 10 + col + 1;
                } else {
                    cellNumber = row * 10 + (9 - col) + 1;
                }
                
                // Warna kotak
                if ((row + col) % 2 == 0) {
                    g2d.setColor(new Color(240, 240, 240));
                } else {
                    g2d.setColor(Color.WHITE);
                }
                g2d.fillRect(x, y, cellSize, cellSize);
                
                // Border kotak
                g2d.setColor(Color.BLACK);
                g2d.drawRect(x, y, cellSize, cellSize);
                
                // Nomor kotak
                g2d.setFont(new Font("Arial", Font.BOLD, 10));
                FontMetrics fm = g2d.getFontMetrics();
                String numberStr = String.valueOf(cellNumber);
                int textX = x + (cellSize - fm.stringWidth(numberStr)) / 2;
                int textY = y + fm.getAscent() + 2;
                g2d.drawString(numberStr, textX, textY);
            }
        }
        
        // Gambar garis ular (merah)
        g2d.setStroke(new BasicStroke(3));
        g2d.setColor(Color.RED);
        drawSnakeLines(g2d, cellSize, offsetX, offsetY);
        
        // Gambar garis tangga (hijau)
        g2d.setColor(Color.GREEN);
        drawLadderLines(g2d, cellSize, offsetX, offsetY);
        
        // Gambar kepala ular dan kaki tangga
        drawSnakesAndLadders(g2d, cellSize, offsetX, offsetY);
        
        // Gambar posisi pemain
        for (int i = 0; i < 3; i++) {
            if (playerPositions[i] > 0) {
                Point pos = getCellPosition(playerPositions[i], cellSize, offsetX, offsetY);
                g2d.setColor(playerColors[i]);
                
                // Offset untuk pemain yang berbeda di kotak yang sama
                int playerOffset = i * 12;
                g2d.fillOval(pos.x + 5 + playerOffset, pos.y + 30, 15, 15);
                
                // Nomor pemain
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.BOLD, 10));
                g2d.drawString(String.valueOf(i + 1), pos.x + 10 + playerOffset, pos.y + 41);
            }
        }
    }
    
    private void drawSnakeLines(Graphics2D g2d, int cellSize, int offsetX, int offsetY) {
        // Gambar garis ular dari kepala ke ekor
        int[][] snakes = {{16, 6}, {47, 26}, {49, 11}, {56, 53}, {62, 19}, 
                         {64, 60}, {87, 24}, {93, 73}, {95, 75}, {98, 78}};
        
        for (int[] snake : snakes) {
            Point head = getCellCenter(snake[0], cellSize, offsetX, offsetY);
            Point tail = getCellCenter(snake[1], cellSize, offsetX, offsetY);
            
            // Gambar garis melengkung
            g2d.drawLine(head.x, head.y, tail.x, tail.y);
            
            // Gambar panah di ekor
            drawArrow(g2d, head, tail, Color.RED);
        }
    }
    
    private void drawLadderLines(Graphics2D g2d, int cellSize, int offsetX, int offsetY) {
        // Gambar garis tangga dari bawah ke atas
        int[][] ladders = {{1, 38}, {4, 14}, {9, 21}, {21, 42}, {28, 84}, 
                          {36, 44}, {51, 67}, {71, 91}, {80, 100}};
        
        for (int[] ladder : ladders) {
            Point bottom = getCellCenter(ladder[0], cellSize, offsetX, offsetY);
            Point top = getCellCenter(ladder[1], cellSize, offsetX, offsetY);
            
            // Gambar tangga (garis ganda)
            g2d.drawLine(bottom.x - 5, bottom.y, top.x - 5, top.y);
            g2d.drawLine(bottom.x + 5, bottom.y, top.x + 5, top.y);
            
            // Gambar anak tangga
            int steps = Math.abs(ladder[1] - ladder[0]) / 10;
            for (int i = 0; i <= steps; i++) {
                int stepX1 = bottom.x - 5 + (top.x - bottom.x) * i / (steps + 1);
                int stepY = bottom.y + (top.y - bottom.y) * i / (steps + 1);
                int stepX2 = bottom.x + 5 + (top.x - bottom.x) * i / (steps + 1);
                g2d.drawLine(stepX1, stepY, stepX2, stepY);
            }
            
            // Gambar panah di atas
            drawArrow(g2d, bottom, top, Color.GREEN);
        }
    }
    
    private void drawSnakesAndLadders(Graphics2D g2d, int cellSize, int offsetX, int offsetY) {
        // Gambar kepala ular
        int[] snakeHeads = {16, 47, 49, 56, 62, 64, 87, 93, 95, 98};
        for (int head : snakeHeads) {
            Point pos = getCellPosition(head, cellSize, offsetX, offsetY);
            g2d.setColor(Color.RED);
            g2d.fillOval(pos.x + 35, pos.y + 5, 12, 12);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 8));
            g2d.drawString("S", pos.x + 39, pos.y + 13);
        }
        
        // Gambar kaki tangga
        int[] ladderBottoms = {1, 4, 9, 21, 28, 36, 51, 71, 80};
        for (int bottom : ladderBottoms) {
            Point pos = getCellPosition(bottom, cellSize, offsetX, offsetY);
            g2d.setColor(Color.GREEN);
            g2d.fillOval(pos.x + 35, pos.y + 5, 12, 12);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 8));
            g2d.drawString("L", pos.x + 39, pos.y + 13);
        }
    }
    
    private void drawArrow(Graphics2D g2d, Point from, Point to, Color color) {
        g2d.setColor(color);
        
        // Hitung sudut panah
        double angle = Math.atan2(to.y - from.y, to.x - from.x);
        int arrowLength = 8;
        double arrowAngle = Math.PI / 6;
        
        // Titik ujung panah
        int x1 = (int) (to.x - arrowLength * Math.cos(angle - arrowAngle));
        int y1 = (int) (to.y - arrowLength * Math.sin(angle - arrowAngle));
        int x2 = (int) (to.x - arrowLength * Math.cos(angle + arrowAngle));
        int y2 = (int) (to.y - arrowLength * Math.sin(angle + arrowAngle));
        
        // Gambar panah
        g2d.drawLine(to.x, to.y, x1, y1);
        g2d.drawLine(to.x, to.y, x2, y2);
    }
    
    private Point getCellCenter(int cellNumber, int cellSize, int offsetX, int offsetY) {
        Point pos = getCellPosition(cellNumber, cellSize, offsetX, offsetY);
        return new Point(pos.x + cellSize/2, pos.y + cellSize/2);
    }
    
    private Point getCellPosition(int cellNumber, int cellSize, int offsetX, int offsetY) {
        if (cellNumber < 1 || cellNumber > 100) return new Point(0, 0);
        
        int row = (cellNumber - 1) / 10;
        int col = (cellNumber - 1) % 10;
        
        if (row % 2 == 1) {
            col = 9 - col; // Baris ganjil terbalik
        }
        
        int x = offsetX + col * cellSize;
        int y = offsetY + (9 - row) * cellSize;
        
        return new Point(x, y);
    }
    
    private boolean isSnake(int position) {
        return position == 16 || position == 47 || position == 49 || 
               position == 56 || position == 62 || position == 64 || 
               position == 87 || position == 93 || position == 95 || position == 98;
    }
    
    private boolean isLadder(int position) {
        return position == 1 || position == 4 || position == 9 || 
               position == 21 || position == 28 || position == 36 || 
               position == 51 || position == 71 || position == 80;
    }
    
    private void connectToServer() {
        try {
            socket = new Socket(SERVER_IP, SERVER_PORT);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
            
            // Thread untuk menerima pesan dari server
            new Thread(() -> {
                try {
                    String message;
                    while ((message = reader.readLine()) != null) {
                        handleServerMessage(message);
                    }
                } catch (IOException e) {
                    statusLabel.setText("Koneksi terputus!");
                }
            }).start();
            
        } catch (IOException e) {
            statusLabel.setText("Gagal terhubung ke server!");
        }
    }
    
    private void handleServerMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            if (message.startsWith("PLAYER_ID:")) {
                playerId = Integer.parseInt(message.substring(10));
                setTitle("Ular Tangga - Pemain " + (playerId + 1));
                statusLabel.setText("Terhubung sebagai Pemain " + (playerId + 1));
                
            } else if (message.equals("GAME_START")) {
                gameStarted = true;
                statusLabel.setText("Game dimulai!");
                
            } else if (message.startsWith("STATE:")) {
                String[] parts = message.substring(6).split(":");
                for (int i = 0; i < 3; i++) {
                    playerPositions[i] = Integer.parseInt(parts[i]);
                    playerLabels[i].setText("Pemain " + (i + 1) + ": " + playerPositions[i]);
                }
                boardPanel.repaint();
                
            } else if (message.startsWith("TURN:")) {
                int currentPlayer = Integer.parseInt(message.substring(5));
                isMyTurn = (currentPlayer == playerId);
                rollButton.setEnabled(isMyTurn && gameStarted && !gameEnded);
                
                if (isMyTurn) {
                    if (rollAgain) {
                        statusLabel.setText("Giliran Anda! (Bonus dadu 6)");
                    } else {
                        statusLabel.setText("Giliran Anda!");
                    }
                } else {
                    statusLabel.setText("Giliran Pemain " + (currentPlayer + 1));
                }
                
            } else if (message.startsWith("ROLL_AGAIN:")) {
                int playerIdRollAgain = Integer.parseInt(message.substring(12));
                if (playerIdRollAgain == playerId) {
                    rollAgain = true;
                    statusLabel.setText("Dadu 6! Lempar lagi!");
                    rollButton.setEnabled(true);
                } else {
                    statusLabel.setText("Pemain " + (playerIdRollAgain + 1) + " dapat dadu 6! Lempar lagi!");
                }
                
            } else if (message.startsWith("BOUNCE:")) {
                String[] parts = message.substring(7).split(":");
                int playerIdBounce = Integer.parseInt(parts[0]);
                int position = Integer.parseInt(parts[1]);
                int dice = Integer.parseInt(parts[2]);
                statusLabel.setText("Pemain " + (playerIdBounce + 1) + " tidak bisa maju (dadu " + dice + " dari kotak " + position + ")");
                
            } else if (message.startsWith("PLAYER_FINISHED:")) {
                String[] parts = message.substring(17).split(":");
                int finishedPlayer = Integer.parseInt(parts[0]);
                int rank = Integer.parseInt(parts[1]);
                
                String rankText = "";
                switch(rank) {
                    case 1: rankText = "Juara 1"; break;
                    case 2: rankText = "Juara 2"; break;
                    case 3: rankText = "Juara 3"; break;
                }
                
                if (finishedPlayer == playerId) {
                    statusLabel.setText("Selamat! Anda " + rankText + "!");
                } else {
                    statusLabel.setText("Pemain " + (finishedPlayer + 1) + " finish sebagai " + rankText + "!");
                }
                
            } else if (message.startsWith("LEADERBOARD:")) {
                gameEnded = true;
                rollButton.setEnabled(false);
                showLeaderboard(message.substring(12));
                
            } else if (message.startsWith("SNAKE:")) {
                String[] parts = message.substring(6).split(":");
                int playerIdSnake = Integer.parseInt(parts[0]);
                int from = Integer.parseInt(parts[1]);
                int to = Integer.parseInt(parts[2]);
                statusLabel.setText("Pemain " + (playerIdSnake + 1) + " terkena ular! (" + from + "→" + to + ")");
                
            } else if (message.startsWith("LADDER:")) {
                String[] parts = message.substring(7).split(":");
                int playerIdLadder = Integer.parseInt(parts[0]);
                int from = Integer.parseInt(parts[1]);
                int to = Integer.parseInt(parts[2]);
                statusLabel.setText("Pemain " + (playerIdLadder + 1) + " naik tangga! (" + from + "→" + to + ")");
            }
            
            rollAgain = false; // Reset flag setelah processing
        });
    }
    
    private void showLeaderboard(String leaderboardData) {
        StringBuilder leaderboardText = new StringBuilder("🏆 LEADERBOARD 🏆\n\n");
        
        String[] entries = leaderboardData.split(";");
        for (String entry : entries) {
            String[] parts = entry.split(",");
            int playerIdLB = Integer.parseInt(parts[0]);
            int rank = Integer.parseInt(parts[1]);
            
            String medal = "";
            switch(rank) {
                case 1: medal = "🥇 "; break;
                case 2: medal = "🥈 "; break;
                case 3: medal = "🥉 "; break;
            }
            
            leaderboardText.append(medal)
                          .append("Posisi ").append(rank).append(": ")
                          .append("Pemain ").append(playerIdLB + 1)
                          .append(" (Posisi akhir: ").append(playerPositions[playerIdLB]).append(")")
                          .append("\n");
        }
        
        leaderboardText.append("\nTerima kasih telah bermain!");
        
        JOptionPane.showMessageDialog(this, leaderboardText.toString(), 
                                    "Game Selesai - Leaderboard", 
                                    JOptionPane.INFORMATION_MESSAGE);
        
        statusLabel.setText("Game selesai! Lihat leaderboard di popup.");
    }
    
    private void rollDice() {
        if (!isMyTurn) return;
        
        Random random = new Random();
        int diceRoll = random.nextInt(6) + 1;
        
        diceLabel.setText("Dadu: " + diceRoll);
        rollButton.setEnabled(false);
        
        // Kirim hasil dadu ke server
        writer.println("ROLL:" + diceRoll);
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ClientTCP();
        });
    }
}