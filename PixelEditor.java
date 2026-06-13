import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public class PixelEditor extends JFrame {
    private int[] pixels;
    private int currentColor = 0xFF8B5E3C;
    private JPanel gridPanel;
    private final Runnable onSave;
    private String blockName;
    private int dragStartX = -1, dragStartY = -1;
    private int dragEndX = -1, dragEndY = -1;
    private boolean dragging;

    private static final int CELL = 20;
    private static final int[] PALETTE = {
        0xFF8B5E3C, 0xFF5A9E4A, 0xFF8A8A8A, 0xFF6B4423,
        0xFF2D5A1E, 0xFF3366AA, 0xFFE8D5A0, 0xFFFFFFFF,
        0xFF000000, 0xFFFF0000, 0xFF00FF00, 0xFF0000FF,
        0xFFFFFF00, 0xFFFF00FF, 0xFF00FFFF, 0xFFCC8833
    };

    public PixelEditor(String name, int[] existingPixels, Runnable onSaveCallback) {
        this.blockName = name;
        this.pixels = existingPixels != null ? existingPixels : new int[BlockTexture.SIZE * BlockTexture.SIZE];
        this.onSave = onSaveCallback;
        setTitle("Pixel Editor - " + name);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        setResizable(false);

        gridPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                int sz = BlockTexture.SIZE;
                for (int y = 0; y < sz; y++) {
                    for (int x = 0; x < sz; x++) {
                        g.setColor(new Color(pixels[y * sz + x], true));
                        g.fillRect(x * CELL, y * CELL, CELL, CELL);
                        g.setColor(new Color(80, 80, 80, 160));
                        g.drawRect(x * CELL, y * CELL, CELL, CELL);
                    }
                }
                if (dragging && dragStartX >= 0) {
                    int x1 = Math.min(dragStartX, dragEndX);
                    int y1 = Math.min(dragStartY, dragEndY);
                    int x2 = Math.max(dragStartX, dragEndX);
                    int y2 = Math.max(dragStartY, dragEndY);
                    g.setColor(new Color(255, 255, 255, 100));
                    g.fillRect(x1 * CELL, y1 * CELL, (x2 - x1 + 1) * CELL, (y2 - y1 + 1) * CELL);
                    g.setColor(new Color(255, 255, 255, 220));
                    g.drawRect(x1 * CELL, y1 * CELL, (x2 - x1 + 1) * CELL - 1, (y2 - y1 + 1) * CELL - 1);
                    g.setColor(new Color(0, 0, 0, 180));
                    g.drawRect(x1 * CELL - 1, y1 * CELL - 1, (x2 - x1 + 1) * CELL + 1, (y2 - y1 + 1) * CELL + 1);
                }
            }
        };
        gridPanel.setPreferredSize(new Dimension(BlockTexture.SIZE * CELL, BlockTexture.SIZE * CELL));
        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int x = e.getX() / CELL;
                int y = e.getY() / CELL;
                if (x < 0 || x >= BlockTexture.SIZE || y < 0 || y >= BlockTexture.SIZE) return;
                if (SwingUtilities.isRightMouseButton(e)) {
                    currentColor = pixels[y * BlockTexture.SIZE + x];
                } else {
                    dragStartX = x; dragStartY = y;
                    dragEndX = x; dragEndY = y;
                    dragging = true;
                }
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (dragging && !SwingUtilities.isRightMouseButton(e)) {
                    int x = e.getX() / CELL;
                    int y = e.getY() / CELL;
                    x = Math.max(0, Math.min(BlockTexture.SIZE - 1, x));
                    y = Math.max(0, Math.min(BlockTexture.SIZE - 1, y));
                    int x1 = Math.min(dragStartX, x), x2 = Math.max(dragStartX, x);
                    int y1 = Math.min(dragStartY, y), y2 = Math.max(dragStartY, y);
                    for (int py = y1; py <= y2; py++)
                        for (int px = x1; px <= x2; px++)
                            pixels[py * BlockTexture.SIZE + px] = currentColor;
                    dragging = false;
                    dragStartX = dragStartY = -1;
                    dragEndX = dragEndY = -1;
                    gridPanel.repaint();
                    previewLabel.repaint();
                }
            }
        };
        gridPanel.addMouseListener(ma);
        gridPanel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragging) {
                    int x = e.getX() / CELL;
                    int y = e.getY() / CELL;
                    dragEndX = Math.max(0, Math.min(BlockTexture.SIZE - 1, x));
                    dragEndY = Math.max(0, Math.min(BlockTexture.SIZE - 1, y));
                    gridPanel.repaint();
                }
            }
        });

        JPanel palettePanel = new JPanel(new GridLayout(2, 8, 2, 2));
        for (int c : PALETTE) {
            JButton btn = new JButton();
            btn.setBackground(new Color(c));
            btn.setPreferredSize(new Dimension(24, 24));
            btn.addActionListener(e -> { currentColor = c; updateCurrentDisplay(); });
            palettePanel.add(btn);
        }

        JPanel bottomPanel = new JPanel(new FlowLayout());
        JButton saveBtn = new JButton("Save & Close");
        saveBtn.addActionListener(e -> {
            dispose();
            if (onSave != null) onSave.run();
        });
        JButton clearBtn = new JButton("Clear All");
        clearBtn.addActionListener(e -> {
            for (int i = 0; i < pixels.length; i++) pixels[i] = 0;
            gridPanel.repaint();
            previewLabel.repaint();
        });
        JButton fillBtn = new JButton("Fill");
        fillBtn.addActionListener(e -> {
            for (int i = 0; i < pixels.length; i++) pixels[i] = currentColor;
            gridPanel.repaint();
            previewLabel.repaint();
        });
        JButton saveTxtBtn = new JButton("Save TXT");
        saveTxtBtn.addActionListener(e -> saveAsTxt());
        JButton loadTxtBtn = new JButton("Load TXT");
        loadTxtBtn.addActionListener(e -> loadFromTxt());

        currentDisplay = new JLabel();
        currentDisplay.setPreferredSize(new Dimension(24, 24));
        currentDisplay.setOpaque(true);
        updateCurrentDisplay();

        previewLabel = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                int sz = BlockTexture.SIZE;
                for (int y = 0; y < sz; y++) {
                    for (int x = 0; x < sz; x++) {
                        g.setColor(new Color(pixels[y * sz + x], true));
                        g.fillRect(x * 2, y * 2, 2, 2);
                    }
                }
            }
        };
        previewLabel.setPreferredSize(new Dimension(32, 32));
        previewLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        bottomPanel.add(new JLabel("Current:"));
        bottomPanel.add(currentDisplay);
        bottomPanel.add(previewLabel);
        bottomPanel.add(clearBtn);
        bottomPanel.add(fillBtn);
        bottomPanel.add(saveTxtBtn);
        bottomPanel.add(loadTxtBtn);
        bottomPanel.add(saveBtn);

        JPanel topPanel = new JPanel(new FlowLayout());
        topPanel.add(new JLabel("Left-click: paint  |  Right-click: pick color  |  Close to cancel"));

        add(topPanel, BorderLayout.NORTH);
        add(gridPanel, BorderLayout.CENTER);
        add(palettePanel, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JLabel currentDisplay;
    private JLabel previewLabel;

    private void updateCurrentDisplay() {
        currentDisplay.setBackground(new Color(currentColor));
        currentDisplay.repaint();
    }

    public int[] getPixels() { return pixels; }
    public String getBlockName() { return blockName; }

    private void loadFromTxt() {
        JFileChooser fc = new JFileChooser(".");
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            java.util.List<String> lines = java.nio.file.Files.readAllLines(fc.getSelectedFile().toPath());
            int sz = BlockTexture.SIZE;
            int pi = 0;
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split("\\s+");
                for (String p : parts) {
                    if (pi >= pixels.length) break;
                    pixels[pi++] = (int) Long.parseLong(p.replace("0x", "").replace("0X", ""), 16);
                }
            }
            gridPanel.repaint();
            previewLabel.repaint();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Load failed: " + ex.getMessage());
        }
    }

    private void saveAsTxt() {
        JFileChooser fc = new JFileChooser(".");
        fc.setSelectedFile(new java.io.File(blockName + ".txt"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try (java.io.PrintWriter pw = new java.io.PrintWriter(fc.getSelectedFile())) {
            pw.println("# Block texture: " + blockName);
            pw.println("# 16x16 hex ARGB values");
            int sz = BlockTexture.SIZE;
            for (int y = 0; y < sz; y++) {
                StringBuilder sb = new StringBuilder();
                for (int x = 0; x < sz; x++) {
                    if (x > 0) sb.append(' ');
                    sb.append(String.format("0x%08X", pixels[y * sz + x]));
                }
                pw.println(sb);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Save failed: " + ex.getMessage());
        }
    }
}
