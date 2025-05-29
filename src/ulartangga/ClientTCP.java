package ulartangga;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import java.util.Random;

public class ClientTCP extends JFrame {
    private JPanel boardPanel;
    private JButton rollButton;
    private JLabel statusLabel;
    private JLabel diceLabel;
    private JLabel[] playerLabels = new JLabel[3];
    private Color[] playerColors = {Color.RED, Color.BLUE, Color.GREEN};
    
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
        rollButton.addActionListener(e -> rollDice());
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
                int y = offsetY + (9 - row) * cellSize;
                
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
        
        // Gambar ular dan tangga
        g2d.setStroke(new BasicStroke(3));
        drawSnakeLines(g2d, cellSize, offsetX, offsetY);
        drawLadderLines(g2d, cellSize, offsetX, offsetY);
        drawSnakesAndLadders(g2d, cellSize, offsetX, offsetY);
        
        // Gambar posisi pemain
        for (int i = 0; i < 3; i++) {
            if (playerPositions[i] > 0) {
                Point pos = getCellPosition(playerPositions[i], cellSize, offsetX, offsetY);
                g2d.setColor(playerColors[i]);
                int playerOffset = i * 12;
                g2d.fillOval(pos.x + 5 + playerOffset, pos.y + 30, 15, 15);
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.BOLD, 10));
                g2d.drawString(String.valueOf(i + 1), pos.x + 10 + playerOffset, pos.y + 41);
            }
        }
    }
    
    // Helper methods untuk drawing
    private Point getCellPosition(int cellNumber, int cellSize, int offsetX, int offsetY) {
        // Implementation for cell position calculation
    }
    
    private void drawSnakeLines(Graphics2D g2d, int cellSize, int offsetX, int offsetY) {
        // Implementation for drawing snake lines
    }
    
    private void drawLadderLines(Graphics2D g2d, int cellSize, int offsetX, int offsetY) {
        // Implementation for drawing ladder lines
    }
}