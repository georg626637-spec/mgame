import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;

public class BlockEditor extends JFrame {
    private static final int TEX_CELL = 20;
    private static final int SHAPE_CELL = 18;
    private static final int[] PALETTE = {
        0xFF8B5E3C, 0xFF5A9E4A, 0xFF8A8A8A, 0xFF6B4423,
        0xFF2D5A1E, 0xFF3366AA, 0xFFE8D5A0, 0xFFFFFFFF,
        0xFF000000, 0xFFFF0000, 0xFF00FF00, 0xFF0000FF,
        0xFFFFFF00, 0xFFFF00FF, 0xFF00FFFF, 0xFFCC8833
    };

    private String blockName;
    private int[] texPixels;
    private int[][][] shapeColors = new int[16][16][16];
    private int currentColor = 0xFF8B5E3C;
    private int slice = 7, axis = 1;

    private JPanel texGrid, shapeGrid, previewPanel;
    private JLabel sliceLabel, infoLabel, previewLabel;
    private JLabel currentDisplay;
    private Runnable onSave;

    private int texSX = -1, texSY = -1, texEX = -1, texEY = -1;
    private boolean texDragging;

    private int shSX = -1, shSY = -1;
    private boolean shDragging;
    private enum ShapeTool { PENCIL, LINE, RECT, PICK }
    private ShapeTool shapeTool = ShapeTool.PENCIL;

    public BlockEditor(String name, int[] existingPixels, Runnable onSaveCallback) {
        this.blockName = name;
        this.texPixels = existingPixels != null ? existingPixels : new int[256];
        this.onSave = onSaveCallback;

        setTitle("Block Editor - " + name);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        setResizable(false);

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topBar.add(new JLabel("Left-click drag: paint  |  Right-click: pick color  |  Close to cancel"));
        add(topBar, BorderLayout.NORTH);

        JPanel main = new JPanel(new GridLayout(1, 3, 6, 0));
        main.add(buildTexturePanel());
        main.add(buildShapePanel());
        main.add(buildPreviewPanel());
        add(main, BorderLayout.CENTER);

        add(buildBottomBar(), BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JPanel buildTexturePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(80, 80, 140), 1),
            "Texture", javax.swing.border.TitledBorder.LEFT,
            javax.swing.border.TitledBorder.TOP,
            new Font("Monospaced", Font.BOLD, 11), new Color(180, 180, 255)));

        texGrid = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                int sz = 16;
                for (int y = 0; y < sz; y++) {
                    for (int x = 0; x < sz; x++) {
                        g.setColor(new Color(texPixels[y * sz + x], true));
                        g.fillRect(x * TEX_CELL, y * TEX_CELL, TEX_CELL, TEX_CELL);
                        g.setColor(new Color(80, 80, 80, 160));
                        g.drawRect(x * TEX_CELL, y * TEX_CELL, TEX_CELL, TEX_CELL);
                    }
                }
                if (texDragging && texSX >= 0) {
                    int x1 = Math.min(texSX, texEX), y1 = Math.min(texSY, texEY);
                    int x2 = Math.max(texSX, texEX), y2 = Math.max(texSY, texEY);
                    g.setColor(new Color(255, 255, 255, 100));
                    g.fillRect(x1 * TEX_CELL, y1 * TEX_CELL, (x2 - x1 + 1) * TEX_CELL, (y2 - y1 + 1) * TEX_CELL);
                    g.setColor(new Color(255, 255, 255, 220));
                    g.drawRect(x1 * TEX_CELL, y1 * TEX_CELL, (x2 - x1 + 1) * TEX_CELL - 1, (y2 - y1 + 1) * TEX_CELL - 1);
                    g.setColor(new Color(0, 0, 0, 180));
                    g.drawRect(x1 * TEX_CELL - 1, y1 * TEX_CELL - 1, (x2 - x1 + 1) * TEX_CELL + 1, (y2 - y1 + 1) * TEX_CELL + 1);
                }
            }
        };
        texGrid.setPreferredSize(new Dimension(320, 320));
        texGrid.setBackground(new Color(15, 15, 30));
        MouseAdapter tma = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int x = e.getX() / TEX_CELL, y = e.getY() / TEX_CELL;
                if (x < 0 || x >= 16 || y < 0 || y >= 16) return;
                if (SwingUtilities.isRightMouseButton(e)) {
                    currentColor = texPixels[y * 16 + x];
                    updateCurrentDisplay();
                } else {
                    texSX = x; texSY = y; texEX = x; texEY = y;
                    texDragging = true;
                }
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (texDragging && !SwingUtilities.isRightMouseButton(e)) {
                    int x = e.getX() / TEX_CELL, y = e.getY() / TEX_CELL;
                    x = Math.max(0, Math.min(15, x));
                    y = Math.max(0, Math.min(15, y));
                    int x1 = Math.min(texSX, x), x2 = Math.max(texSX, x);
                    int y1 = Math.min(texSY, y), y2 = Math.max(texSY, y);
                    for (int py = y1; py <= y2; py++)
                        for (int px = x1; px <= x2; px++)
                            texPixels[py * 16 + px] = currentColor;
                    texDragging = false;
                    texSX = texSY = -1;
                    texEX = texEY = -1;
                    texGrid.repaint();
                    previewLabel.repaint();
                }
            }
        };
        texGrid.addMouseListener(tma);
        texGrid.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (texDragging) {
                    int x = e.getX() / TEX_CELL, y = e.getY() / TEX_CELL;
                    texEX = Math.max(0, Math.min(15, x));
                    texEY = Math.max(0, Math.min(15, y));
                    texGrid.repaint();
                }
            }
        });
        panel.add(texGrid, BorderLayout.WEST);

        JPanel side = new JPanel(new BorderLayout());
        JPanel palettePanel = new JPanel(new GridLayout(8, 2, 2, 2));
        for (int c : PALETTE) {
            JButton btn = new JButton();
            btn.setBackground(new Color(c));
            btn.setPreferredSize(new Dimension(22, 22));
            btn.addActionListener(e -> { currentColor = c; updateCurrentDisplay(); });
            palettePanel.add(btn);
        }
        JButton moreColor = new JButton("\u00B7\u00B7\u00B7");
        moreColor.setFont(new Font("Monospaced", Font.BOLD, 10));
        moreColor.addActionListener(e -> {
            Color cl = JColorChooser.showDialog(this, "Color", new Color(currentColor));
            if (cl != null) { currentColor = cl.getRGB() | 0xFF000000; updateCurrentDisplay(); }
        });
        palettePanel.add(moreColor);

        currentDisplay = new JLabel();
        currentDisplay.setPreferredSize(new Dimension(22, 22));
        currentDisplay.setOpaque(true);
        updateCurrentDisplay();

        JPanel currentRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
        currentRow.setBackground(new Color(25, 25, 50));
        currentRow.add(new JLabel("C:"));
        currentRow.add(currentDisplay);

        side.add(palettePanel, BorderLayout.NORTH);
        side.add(currentRow, BorderLayout.SOUTH);
        panel.add(side, BorderLayout.EAST);

        previewLabel = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                int sz = 16;
                for (int y = 0; y < sz; y++)
                    for (int x = 0; x < sz; x++) {
                        g.setColor(new Color(texPixels[y * sz + x], true));
                        g.fillRect(x * 2, y * 2, 2, 2);
                    }
            }
        };
        previewLabel.setPreferredSize(new Dimension(32, 32));
        previewLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        JPanel previewHolder = new JPanel(new FlowLayout());
        previewHolder.setBackground(new Color(25, 25, 50));
        previewHolder.add(new JLabel("Prev:"));
        previewHolder.add(previewLabel);
        panel.add(previewHolder, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel buildShapePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(80, 80, 140), 1),
            "Shape", javax.swing.border.TitledBorder.LEFT,
            javax.swing.border.TitledBorder.TOP,
            new Font("Monospaced", Font.BOLD, 11), new Color(180, 180, 255)));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 2));
        JRadioButton rx = new JRadioButton("X");
        JRadioButton ry = new JRadioButton("Y", true);
        JRadioButton rz = new JRadioButton("Z");
        ButtonGroup bg = new ButtonGroup();
        bg.add(rx); bg.add(ry); bg.add(rz);
        rx.addActionListener(e -> { axis = 0; slice = Math.min(slice, 15); updateShapeInfo(); shapeGrid.repaint(); });
        ry.addActionListener(e -> { axis = 1; slice = Math.min(slice, 15); updateShapeInfo(); shapeGrid.repaint(); });
        rz.addActionListener(e -> { axis = 2; slice = Math.min(slice, 15); updateShapeInfo(); shapeGrid.repaint(); });
        top.add(new JLabel("Axis:")); top.add(rx); top.add(ry); top.add(rz);

        JButton pu = new JButton("\u25B2");
        pu.addActionListener(e -> { slice = Math.max(0, slice - 1); updateShapeInfo(); shapeGrid.repaint(); previewPanel.repaint(); });
        JButton pd = new JButton("\u25BC");
        pd.addActionListener(e -> { slice = Math.min(15, slice + 1); updateShapeInfo(); shapeGrid.repaint(); previewPanel.repaint(); });
        sliceLabel = new JLabel("Slice: 8/16");
        top.add(pu); top.add(pd); top.add(sliceLabel);
        panel.add(top, BorderLayout.NORTH);

        shapeGrid = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                for (int i = 0; i < 16; i++) for (int j = 0; j < 16; j++) {
                    int v = shapeColors[vx(i,j)][vy(i,j)][vz(i,j)];
                    g.setColor(v != 0 ? new Color(v, true) : new Color(30, 30, 50));
                    g.fillRect(i * SHAPE_CELL, j * SHAPE_CELL, SHAPE_CELL, SHAPE_CELL);
                    g.setColor(new Color(80, 80, 80, v != 0 ? 80 : 160));
                    g.drawRect(i * SHAPE_CELL, j * SHAPE_CELL, SHAPE_CELL, SHAPE_CELL);
                }
            }
        };
        shapeGrid.setPreferredSize(new Dimension(288, 288));
        shapeGrid.setBackground(new Color(15, 15, 30));
        MouseAdapter sma = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int gi = e.getX() / SHAPE_CELL, gj = e.getY() / SHAPE_CELL;
                if (gi < 0 || gi >= 16 || gj < 0 || gj >= 16) return;
                if (shapeTool == ShapeTool.PICK || SwingUtilities.isRightMouseButton(e)) {
                    int v = shapeColors[vx(gi,gj)][vy(gi,gj)][vz(gi,gj)];
                    if (v != 0) currentColor = v;
                    updateCurrentDisplay();
                    return;
                }
                shSX = gi; shSY = gj; shDragging = true;
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (!shDragging) return;
                shDragging = false;
                if (shapeTool == ShapeTool.LINE || shapeTool == ShapeTool.RECT) {
                    int gi = Math.max(0, Math.min(15, e.getX() / SHAPE_CELL));
                    int gj = Math.max(0, Math.min(15, e.getY() / SHAPE_CELL));
                    if (shapeTool == ShapeTool.LINE) shapeLine(shSX, shSY, gi, gj);
                    else shapeRect(shSX, shSY, gi, gj);
                    shapeGrid.repaint();
                    previewPanel.repaint();
                    updateShapeInfo();
                }
            }
        };
        shapeGrid.addMouseListener(sma);
        shapeGrid.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (shapeTool == ShapeTool.PENCIL && shDragging) {
                    int gi = e.getX() / SHAPE_CELL, gj = e.getY() / SHAPE_CELL;
                    if (gi >= 0 && gi < 16 && gj >= 0 && gj < 16)
                        shapeColors[vx(gi,gj)][vy(gi,gj)][vz(gi,gj)] = currentColor;
                    shapeGrid.repaint();
                    previewPanel.repaint();
                }
            }
        });
        panel.add(shapeGrid, BorderLayout.CENTER);

        JPanel bot = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
        JToggleButton pt = new JToggleButton("Pen", true);
        JToggleButton lt = new JToggleButton("Line");
        JToggleButton rt = new JToggleButton("Rect");
        JToggleButton pk = new JToggleButton("Pick");
        ButtonGroup tg = new ButtonGroup();
        tg.add(pt); tg.add(lt); tg.add(rt); tg.add(pk);
        pt.addActionListener(e -> shapeTool = ShapeTool.PENCIL);
        lt.addActionListener(e -> shapeTool = ShapeTool.LINE);
        rt.addActionListener(e -> shapeTool = ShapeTool.RECT);
        pk.addActionListener(e -> shapeTool = ShapeTool.PICK);
        bot.add(new JLabel("Tool:")); bot.add(pt); bot.add(lt); bot.add(rt); bot.add(pk);

        infoLabel = new JLabel("Filled: 0/4096");
        bot.add(infoLabel);
        panel.add(bot, BorderLayout.SOUTH);

        updateShapeInfo();
        return panel;
    }

    private JPanel buildPreviewPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(80, 80, 140), 1),
            "Preview", javax.swing.border.TitledBorder.LEFT,
            javax.swing.border.TitledBorder.TOP,
            new Font("Monospaced", Font.BOLD, 11), new Color(180, 180, 255)));

        previewPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                renderPreview((Graphics2D) g, getWidth(), getHeight());
            }
        };
        previewPanel.setPreferredSize(new Dimension(300, 300));
        previewPanel.setBackground(new Color(15, 15, 30));
        panel.add(previewPanel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildBottomBar() {
        JPanel bot = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 4));

        JButton texClear = new JButton("Tex Clear");
        texClear.addActionListener(e -> {
            Arrays.fill(texPixels, 0);
            texGrid.repaint();
            previewLabel.repaint();
        });
        JButton texFill = new JButton("Tex Fill");
        texFill.addActionListener(e -> {
            Arrays.fill(texPixels, currentColor);
            texGrid.repaint();
            previewLabel.repaint();
        });

        JButton shClear = new JButton("Shape Clear");
        shClear.addActionListener(e -> { shapeClear(); shapeGrid.repaint(); previewPanel.repaint(); updateShapeInfo(); });
        JButton shFill = new JButton("Shape Fill");
        shFill.addActionListener(e -> { shapeFill(); shapeGrid.repaint(); previewPanel.repaint(); updateShapeInfo(); });
        JButton shInv = new JButton("Shape Inv");
        shInv.addActionListener(e -> { shapeInv(); shapeGrid.repaint(); previewPanel.repaint(); updateShapeInfo(); });

        JButton saveTxt = new JButton("Save TXT");
        saveTxt.addActionListener(e -> saveAsTxt());

        JButton loadTxt = new JButton("Load TXT");
        loadTxt.addActionListener(e -> loadFromTxt());

        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(e -> dispose());

        JButton save = new JButton("Save & Close");
        save.addActionListener(e -> {
            save.setEnabled(false);
            cancel.setEnabled(false);
            try {
                if (onSave != null) onSave.run();
                dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
                save.setEnabled(true);
                cancel.setEnabled(true);
            }
        });

        bot.add(texClear); bot.add(texFill);
        bot.add(shClear); bot.add(shFill); bot.add(shInv);
        bot.add(saveTxt); bot.add(loadTxt);
        bot.add(cancel); bot.add(save);
        bot.setBackground(new Color(25, 25, 50));
        return bot;
    }

    // --- Shape coordinate helpers ---
    private int vx(int i, int j) { return axis == 0 ? slice : i; }
    private int vy(int i, int j) { return axis == 1 ? slice : (axis == 0 ? j : j); }
    private int vz(int i, int j) { return axis == 2 ? slice : (axis == 1 ? j : i); }

    // --- Shape tools ---
    private void shapeLine(int x0, int y0, int x1, int y1) {
        int dx = Math.abs(x1 - x0), dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1, err = dx - dy;
        while (true) {
            shapeColors[vx(x0,y0)][vy(x0,y0)][vz(x0,y0)] = currentColor;
            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x0 += sx; }
            if (e2 < dx) { err += dx; y0 += sy; }
        }
    }

    private void shapeRect(int x0, int y0, int x1, int y1) {
        int a = Math.min(x0, x1), b = Math.max(x0, x1), c = Math.min(y0, y1), d = Math.max(y0, y1);
        for (int i = a; i <= b; i++) for (int j = c; j <= d; j++)
            shapeColors[vx(i,j)][vy(i,j)][vz(i,j)] = currentColor;
    }

    private void shapeClear() { for (int x = 0; x < 16; x++) for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) shapeColors[x][y][z] = 0; }
    private void shapeFill() { for (int x = 0; x < 16; x++) for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) shapeColors[x][y][z] = currentColor; }
    private void shapeInv() { for (int x = 0; x < 16; x++) for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) shapeColors[x][y][z] = shapeColors[x][y][z] != 0 ? 0 : currentColor; }

    // --- 3D Preview render ---
    private void renderPreview(Graphics2D g, int w, int h) {
        int S = 10, cx = w / 2, cy = h / 2;
        int[][] fs = {{3,2,6,7},{1,5,6,2},{4,5,6,7}};

        ArrayList<Integer> xs = new ArrayList<>(), ys = new ArrayList<>(), zs = new ArrayList<>();
        double[] dp = new double[4096];
        int n = 0;
        for (int x = 0; x < 16; x++) for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) {
            if (shapeColors[x][y][z] == 0) continue;
            xs.add(x); ys.add(y); zs.add(z);
            dp[n++] = x + y + z;
        }

        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) order[i] = i;
        Arrays.sort(order, (a, b) -> Double.compare(dp[a], dp[b]));

        for (int si = 0; si < n; si++) {
            int i = order[si], x = xs.get(i), y = ys.get(i), z = zs.get(i);
            int vc = shapeColors[x][y][z];
            int r = (vc >> 16) & 0xFF, gr = (vc >> 8) & 0xFF, bl = vc & 0xFF;
            int[][] cr = new int[8][2];
            for (int ci = 0; ci < 8; ci++) {
                int cx2 = ci == 0 || ci == 3 || ci == 4 || ci == 7 ? x : x + 1;
                int cy2 = ci == 0 || ci == 1 || ci == 4 || ci == 5 ? y : y + 1;
                int cz2 = ci == 0 || ci == 1 || ci == 2 || ci == 3 ? z : z + 1;
                cr[ci][0] = (cx2 - cz2) * S + cx;
                cr[ci][1] = (int) ((cx2 + cz2) * S / 2.0 - cy2 * S + cy);
            }
            for (int f = 0; f < 3; f++) {
                int[] fv = fs[f];
                int[] px = {cr[fv[0]][0], cr[fv[1]][0], cr[fv[2]][0], cr[fv[3]][0]};
                int[] py = {cr[fv[0]][1], cr[fv[1]][1], cr[fv[2]][1], cr[fv[3]][1]};
                double sh = 0.6 + 0.4 * (f == 0 ? 0.7 : f == 1 ? 0.5 : 0.3);
                g.setColor(new Color(Math.min(255, (int)(r*sh)), Math.min(255, (int)(gr*sh)), Math.min(255, (int)(bl*sh))));
                g.fillPolygon(px, py, 4);
                g.setColor(new Color(0, 0, 0, 40));
                g.drawPolygon(px, py, 4);
            }
        }
    }

    private void updateCurrentDisplay() {
        if (currentDisplay != null) {
            currentDisplay.setBackground(new Color(currentColor));
            currentDisplay.repaint();
        }
    }

    private void updateShapeInfo() {
        char ac = "XYZ".charAt(axis);
        sliceLabel.setText("Slice " + ac + "=" + slice + "/15");
        int n = 0;
        for (int x = 0; x < 16; x++) for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++)
            if (shapeColors[x][y][z] != 0) n++;
        infoLabel.setText("Filled: " + n + "/4096");
    }

    public int[] getTexPixels() { return texPixels; }
    public int[][][] getShapeColors() { return shapeColors; }

    private void loadFromTxt() {
        JFileChooser fc = new JFileChooser(".");
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            java.util.List<String> lines = java.nio.file.Files.readAllLines(fc.getSelectedFile().toPath());
            int pi = 0;
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split("\\s+");
                for (String p : parts) {
                    if (pi >= texPixels.length) break;
                    texPixels[pi++] = (int) Long.parseLong(p.replace("0x", "").replace("0X", ""), 16);
                }
            }
            texGrid.repaint();
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
            pw.println("# Block: " + blockName);
            pw.println("# 16x16 hex ARGB texture + 16x16x16 hex ARGB shape");
            pw.println("# --- TEXTURE ---");
            int sz = 16;
            for (int y = 0; y < sz; y++) {
                StringBuilder sb = new StringBuilder();
                for (int x = 0; x < sz; x++) {
                    if (x > 0) sb.append(' ');
                    sb.append(String.format("0x%08X", texPixels[y * sz + x]));
                }
                pw.println(sb);
            }
            pw.println("# --- SHAPE ---");
            for (int y = 0; y < 16; y++) {
                pw.println("# Y=" + y);
                for (int z = 0; z < 16; z++) {
                    StringBuilder sb = new StringBuilder();
                    for (int x = 0; x < 16; x++) {
                        if (x > 0) sb.append(' ');
                        sb.append(String.format("0x%08X", shapeColors[x][y][z]));
                    }
                    pw.println(sb);
                }
                pw.println();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Save failed: " + ex.getMessage());
        }
    }
}
