import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.function.Consumer;

public class ShapeEditor extends JFrame {
    private final int[][][] colors = new int[16][16][16];
    private int slice = 7, axis = 1;
    private final String name;
    private final Consumer<BlockShape> onSave;
    private JPanel grid, preview;
    private JLabel slLabel, info;
    private static final int CS = 20;
    private static final int[] PAL = {
        0xFF8B5E3C, 0xFF5A9E4A, 0xFF8A8A8A, 0xFF6B4423,
        0xFF2D5A1E, 0xFF3366AA, 0xFFE8D5A0, 0xFFCC8888,
        0xFFB44A4A, 0xFF44AA88, 0xFFAA66CC, 0xFFFFAA33
    };
    private int curCol = 0xFF8B5E3C;
    private enum Tool { PENCIL, LINE, RECT, PICK }
    private Tool tool = Tool.PENCIL;
    private int sx = -1, sy = -1;
    private boolean drag;

    public ShapeEditor(String name, Consumer<BlockShape> onSave) {
        this.name = name;
        this.onSave = onSave;
        setTitle("Shape Editor - " + name);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        setResizable(false);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JRadioButton rx = new JRadioButton("X");
        JRadioButton ry = new JRadioButton("Y", true);
        JRadioButton rz = new JRadioButton("Z");
        ButtonGroup bg = new ButtonGroup();
        bg.add(rx); bg.add(ry); bg.add(rz);
        rx.addActionListener(e -> { axis = 0; slice = Math.min(slice, 15); upd(); rep(); });
        ry.addActionListener(e -> { axis = 1; slice = Math.min(slice, 15); upd(); rep(); });
        rz.addActionListener(e -> { axis = 2; slice = Math.min(slice, 15); upd(); rep(); });
        top.add(new JLabel("Axis:")); top.add(rx); top.add(ry); top.add(rz);

        JButton pu = new JButton("\u25B2");
        pu.addActionListener(e -> { slice = Math.max(0, slice - 1); upd(); rep(); });
        JButton pd = new JButton("\u25BC");
        pd.addActionListener(e -> { slice = Math.min(15, slice + 1); upd(); rep(); });
        slLabel = new JLabel("Slice: 8/16");
        top.add(pu); top.add(pd); top.add(slLabel);

        JToggleButton pt = new JToggleButton("Pencil", true);
        JToggleButton lt = new JToggleButton("Line");
        JToggleButton rt = new JToggleButton("Rect");
        JToggleButton pk = new JToggleButton("Pick");
        ButtonGroup tg = new ButtonGroup();
        tg.add(pt); tg.add(lt); tg.add(rt); tg.add(pk);
        pt.addActionListener(e -> tool = Tool.PENCIL);
        lt.addActionListener(e -> tool = Tool.LINE);
        rt.addActionListener(e -> tool = Tool.RECT);
        pk.addActionListener(e -> tool = Tool.PICK);
        top.add(new JLabel("Tool:")); top.add(pt); top.add(lt); top.add(rt); top.add(pk);
        add(top, BorderLayout.NORTH);

        grid = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                for (int i = 0; i < 16; i++) for (int j = 0; j < 16; j++) {
                    int v = colors[vx(i,j)][vy(i,j)][vz(i,j)];
                    g.setColor(v != 0 ? new Color(v, true) : new Color(30, 30, 50));
                    g.fillRect(i * CS, j * CS, CS, CS);
                    g.setColor(new Color(80, 80, 80, v != 0 ? 80 : 160));
                    g.drawRect(i * CS, j * CS, CS, CS);
                }
            }
        };
        grid.setPreferredSize(new Dimension(320, 320));
        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int gi = e.getX() / CS, gj = e.getY() / CS;
                if (gi < 0 || gi >= 16 || gj < 0 || gj >= 16) return;
                if (tool == Tool.PICK || SwingUtilities.isRightMouseButton(e)) {
                    int v = colors[vx(gi,gj)][vy(gi,gj)][vz(gi,gj)];
                    if (v != 0) curCol = v;
                    return;
                }
                sx = gi; sy = gj; drag = true;
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (!drag) return;
                drag = false;
                if (tool == Tool.LINE || tool == Tool.RECT) {
                    int gi = Math.max(0, Math.min(15, e.getX() / CS));
                    int gj = Math.max(0, Math.min(15, e.getY() / CS));
                    if (tool == Tool.LINE) line(sx, sy, gi, gj);
                    else rect(sx, sy, gi, gj);
                    rep();
                }
            }
        };
        grid.addMouseListener(ma);
        grid.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (tool == Tool.PENCIL && drag) {
                    int gi = e.getX() / CS, gj = e.getY() / CS;
                    if (gi >= 0 && gi < 16 && gj >= 0 && gj < 16)
                        colors[vx(gi,gj)][vy(gi,gj)][vz(gi,gj)] = curCol;
                    grid.repaint();
                }
            }
        });
        add(grid, BorderLayout.WEST);

        preview = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                render((Graphics2D) g, getWidth(), getHeight());
            }
        };
        preview.setPreferredSize(new Dimension(320, 320));
        preview.setBackground(new Color(15, 15, 30));
        add(preview, BorderLayout.CENTER);

        JPanel bot = new JPanel(new FlowLayout());
        info = new JLabel("Filled: 0/4096");
        bot.add(info);
        upd();

        JPanel cp = new JPanel(new GridLayout(2, 6, 2, 2));
        for (int c : PAL) { JButton b = new JButton(); b.setBackground(new Color(c)); b.setPreferredSize(new Dimension(18, 18));
            b.addActionListener(e -> curCol = c); cp.add(b); }
        JButton cc = new JButton("\u00B7\u00B7\u00B7");
        cc.addActionListener(e -> { Color cl = JColorChooser.showDialog(this, "Color", new Color(curCol));
            if (cl != null) curCol = cl.getRGB() | 0xFF000000; });
        cp.add(cc);
        bot.add(cp);

        JButton cl = new JButton("Clear");
        cl.addActionListener(e -> { clear(); rep(); });
        JButton fl = new JButton("Fill All");
        fl.addActionListener(e -> { fill(); rep(); });
        JButton iv = new JButton("Invert");
        iv.addActionListener(e -> { inv(); rep(); });
        bot.add(cl); bot.add(fl); bot.add(iv);

        JButton sv = new JButton("Save TXT");
        sv.addActionListener(e -> saveTxt());
        JButton ld = new JButton("Load TXT");
        ld.addActionListener(e -> loadTxt());
        bot.add(sv); bot.add(ld);

        JButton cn = new JButton("Cancel");
        cn.addActionListener(e -> dispose());
        JButton ok = new JButton("Save & Close");
        ok.addActionListener(e -> {
            ok.setEnabled(false);
            cn.setEnabled(false);
            try {
                BlockShape s = new BlockShape();
                for (int x = 0; x < 16; x++) for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++)
                    if (colors[x][y][z] != 0) s.set(x, y, z);
                if (onSave != null) onSave.accept(s);
                dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
                ok.setEnabled(true);
                cn.setEnabled(true);
            }
        });
        bot.add(cn); bot.add(ok);
        add(bot, BorderLayout.SOUTH);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_UP) { slice = Math.max(0, slice-1); upd(); rep(); }
                if (e.getKeyCode() == KeyEvent.VK_DOWN) { slice = Math.min(15, slice+1); upd(); rep(); }
            }
        });
        setFocusable(true);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private int vx(int i, int j) { return axis == 0 ? slice : i; }
    private int vy(int i, int j) { return axis == 1 ? slice : (axis == 0 ? j : j); }
    private int vz(int i, int j) { return axis == 2 ? slice : (axis == 1 ? j : i); }

    private void line(int x0, int y0, int x1, int y1) {
        int dx = Math.abs(x1 - x0), dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1, err = dx - dy;
        while (true) {
            colors[vx(x0,y0)][vy(x0,y0)][vz(x0,y0)] = curCol;
            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x0 += sx; }
            if (e2 < dx) { err += dx; y0 += sy; }
        }
    }

    private void rect(int x0, int y0, int x1, int y1) {
        int a = Math.min(x0, x1), b = Math.max(x0, x1), c = Math.min(y0, y1), d = Math.max(y0, y1);
        for (int i = a; i <= b; i++) for (int j = c; j <= d; j++)
            colors[vx(i,j)][vy(i,j)][vz(i,j)] = curCol;
    }

    private void clear() { for (int x = 0; x < 16; x++) for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) colors[x][y][z] = 0; }
    private void fill() { for (int x = 0; x < 16; x++) for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) colors[x][y][z] = curCol; }
    private void inv() { for (int x = 0; x < 16; x++) for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) colors[x][y][z] = colors[x][y][z] != 0 ? 0 : curCol; }

    private void render(Graphics2D g, int w, int h) {
        int S = 10, cx = w / 2, cy = h / 2;
        int[][] fs = {{3,2,6,7},{1,5,6,2},{0,4,7,3}};
        double[] fn = {0,0,0, 1,0,0, 0,0,1};

        java.util.ArrayList<Integer> xs = new java.util.ArrayList<>(), ys = new java.util.ArrayList<>(), zs = new java.util.ArrayList<>();
        double[] dp = new double[4096];
        int n = 0;
        for (int x = 0; x < 16; x++) for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) {
            if (colors[x][y][z] == 0) continue;
            xs.add(x); ys.add(y); zs.add(z);
            dp[n++] = x + y + z;
        }

        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) order[i] = i;
        java.util.Arrays.sort(order, (a, b) -> Double.compare(dp[a], dp[b]));

        for (int si = 0; si < n; si++) {
            int i = order[si], x = xs.get(i), y = ys.get(i), z = zs.get(i);
            int vc = colors[x][y][z];
            int r = (vc >> 16) & 0xFF, gr = (vc >> 8) & 0xFF, bl = vc & 0xFF;
            int[][] cr = new int[8][2];
            for (int ci = 0; ci < 8; ci++) {
                int cx2 = ci == 0 || ci == 3 || ci == 4 || ci == 7 ? x : x + 1;
                int cy2 = ci == 0 || ci == 1 || ci == 4 || ci == 5 ? y : y + 1;
                int cz2 = ci == 0 || ci == 1 || ci == 2 || ci == 3 ? z : z + 1;
                cr[ci][0] = (int) ((cx2 - cz2) * S + cx);
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

    private void loadTxt() {
        JFileChooser fc = new JFileChooser(".");
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            java.util.List<String> lines = java.nio.file.Files.readAllLines(fc.getSelectedFile().toPath());
            int idx = 0;
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                for (String p : line.split("\\s+")) {
                    if (idx >= 4096) break;
                    int x = idx % 16, z = (idx / 16) % 16, y = idx / 256;
                    colors[x][y][z] = (int) Long.parseLong(p.replace("0x", ""), 16);
                    idx++;
                }
            }
            rep();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Load failed: " + ex.getMessage());
        }
    }

    private void saveTxt() {
        JFileChooser fc = new JFileChooser(".");
        fc.setSelectedFile(new java.io.File(name + "_shape.txt"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try (java.io.PrintWriter pw = new java.io.PrintWriter(fc.getSelectedFile())) {
            pw.println("# " + name);
            for (int y = 0; y < 16; y++) {
                pw.println("# Y=" + y);
                for (int z = 0; z < 16; z++) {
                    StringBuilder sb = new StringBuilder();
                    for (int x = 0; x < 16; x++) {
                        if (x > 0) sb.append(' ');
                        sb.append(String.format("0x%08X", colors[x][y][z]));
                    }
                    pw.println(sb);
                }
                pw.println();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Save failed: " + ex.getMessage());
        }
    }

    private void upd() {
        char ac = "XYZ".charAt(axis);
        slLabel.setText("Slice " + ac + "=" + slice + "/15");
        int n = 0;
        for (int x = 0; x < 16; x++) for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++)
            if (colors[x][y][z] != 0) n++;
        info.setText("Filled: " + n + "/4096");
    }

    private void rep() { grid.repaint(); preview.repaint(); upd(); }
}
