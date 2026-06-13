import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;
import javax.swing.*;

public class CraftingTable extends JFrame {
    private static final int GRID = 4;
    private final int[] slots = new int[GRID * GRID];
    private final int[] inventory;
    private final int[] blockColors;
    private final String[] blockNames;
    private final int blockCount;
    private final JLabel[] slotLabels = new JLabel[GRID * GRID];
    private final JLabel resultLabel = new JLabel();
    private final JLabel statusLabel = new JLabel(" ", SwingConstants.CENTER);
    private final JPanel invPanel = new JPanel();
    private final JPanel gridPanel = new JPanel(new GridLayout(GRID, GRID, 3, 3));
    private int currentRecipe = -1;
    private int resultType;
    private int resultCount;
    private int dragType = -1;
    private Cursor dragCursor;

    private static final java.util.List<int[][]> recipes = new java.util.ArrayList<>();
    static {
        addRecipe(new int[]{3,3,0,0, 3,3,0,0, 0,0,0,0, 0,0,0,0}, 3, 4);
        addRecipe(new int[]{4,4,0,0, 4,4,0,0, 0,0,0,0, 0,0,0,0}, 4, 4);
        addRecipe(new int[]{8,8,0,0, 8,8,0,0, 0,0,0,0, 0,0,0,0}, 8, 2);
        addRecipe(new int[]{9,9,0,0, 9,9,0,0, 0,0,0,0, 0,0,0,0}, 9, 2);
        addRecipe(new int[]{3,3,3,0, 3,3,3,0, 3,3,3,0, 0,0,0,0}, 3, 9);
        addRecipe(new int[]{4,4,4,0, 4,4,4,0, 4,4,4,0, 0,0,0,0}, 4, 9);
        addRecipe(new int[]{8,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0}, 8, 3);
        addRecipe(new int[]{14,0,0,0, 15,0,0,0, 15,0,0,0, 15,0,0,0}, 10, 1);
        addRecipe(new int[]{9,3,0,0, 9,3,0,0, 0,0,0,0, 0,0,0,0}, 11, 1);
        addRecipe(new int[]{3,3,3,0, 3,0,3,0, 3,3,3,0, 0,0,0,0}, 13, 1);
        addRecipe(new int[]{12,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0}, 15, 2);
    }

    public static void addRecipe(int[] pattern, int resultType, int resultCount) {
        recipes.add(new int[][]{pattern, new int[]{resultType, resultCount}});
    }

    public static java.util.List<int[][]> getRecipes() { return recipes; }

    public CraftingTable(int[] inventory, int[] blockColors, String[] blockNames, int blockCount) {
        super("Crafting Table 4x4");
        this.inventory = inventory;
        this.blockColors = blockColors;
        this.blockNames = blockNames;
        this.blockCount = blockCount;
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(new Color(30, 30, 50));

        invPanel.setLayout(new BoxLayout(invPanel, BoxLayout.Y_AXIS));
        invPanel.setBackground(new Color(25, 25, 50));
        invPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(80, 80, 140), 1),
            "Inventory (drag to grid)", javax.swing.border.TitledBorder.LEFT,
            javax.swing.border.TitledBorder.TOP,
            new Font("Monospaced", Font.BOLD, 10), new Color(180, 180, 255)));
        invPanel.setPreferredSize(new Dimension(155, 300));
        rebuildInventory();
        add(invPanel, BorderLayout.WEST);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(new Color(30, 30, 50));

        gridPanel.setBackground(new Color(40, 40, 70));
        gridPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        for (int i = 0; i < GRID * GRID; i++) {
            final int idx = i;
            JLabel lb = new JLabel(" ", SwingConstants.CENTER);
            lb.setPreferredSize(new Dimension(48, 48));
            lb.setOpaque(true);
            lb.setBackground(new Color(20, 20, 40));
            lb.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 100), 1));
            lb.setFont(new Font("Monospaced", Font.PLAIN, 18));
            lb.addMouseListener(new MouseAdapter() {
                public void mouseReleased(MouseEvent e) {
                    if (dragType > 0) {
                        slots[idx] = dragType;
                        updateSlot(idx);
                        updateResult();
                        dragType = -1;
                        setCursor(Cursor.getDefaultCursor());
                    } else {
                        slots[idx] = 0;
                        updateSlot(idx);
                        updateResult();
                    }
                }
            });
            slotLabels[i] = lb;
            gridPanel.add(lb);
        }
        centerPanel.add(gridPanel, BorderLayout.CENTER);

        JPanel resultPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        resultPanel.setBackground(new Color(40, 40, 70));
        resultLabel.setPreferredSize(new Dimension(64, 64));
        resultLabel.setOpaque(true);
        resultLabel.setBackground(new Color(20, 20, 40));
        resultLabel.setHorizontalAlignment(SwingConstants.CENTER);
        resultLabel.setFont(new Font("Monospaced", Font.BOLD, 22));
        resultLabel.setBorder(BorderFactory.createLineBorder(new Color(100, 100, 180), 2));
        resultLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (dragType > 0) return;
                craftItem();
            }
        });
        JLabel arrow = new JLabel(" \u2192 ", SwingConstants.CENTER);
        arrow.setFont(new Font("Monospaced", Font.BOLD, 28));
        arrow.setForeground(new Color(200, 200, 100));
        resultPanel.add(arrow);
        resultPanel.add(resultLabel);
        centerPanel.add(resultPanel, BorderLayout.SOUTH);
        add(centerPanel, BorderLayout.CENTER);

        statusLabel.setFont(new Font("Monospaced", Font.PLAIN, 12));
        statusLabel.setForeground(new Color(200, 200, 200));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        add(statusLabel, BorderLayout.NORTH);

        pack();
        setResizable(false);
        setLocationRelativeTo(null);
        getRootPane().addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                if (dragType > 0) {
                    dragType = -1;
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        });
        updateResult();
    }

    private void rebuildInventory() {
        invPanel.removeAll();
        Font f = new Font("Monospaced", Font.PLAIN, 11);
        for (int t = 1; t <= blockCount; t++) {
            if (inventory[t] <= 0) continue;
            final int type = t;
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
            row.setBackground(new Color(30, 30, 60));
            row.setMaximumSize(new Dimension(155, 30));
            JLabel icon = new JLabel();
            icon.setPreferredSize(new Dimension(18, 18));
            int c = blockColors[t];
            int[] pix = new int[256]; Arrays.fill(pix, c);
            BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            img.setRGB(0, 0, 16, 16, pix, 0, 16);
            icon.setIcon(new ImageIcon(img.getScaledInstance(16, 16, Image.SCALE_FAST)));
            JLabel text = new JLabel(blockNames[t] + ": " + inventory[t]);
            text.setFont(f);
            text.setForeground(new Color(200, 200, 220));
            row.add(icon);
            row.add(text);
            row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            row.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    dragType = type;
                    dragCursor = makeDragCursor(type);
                    setCursor(dragCursor);
                }
                public void mouseClicked(MouseEvent e) {
                    if (dragType > 0) {
                        addToGrid(type);
                        dragType = -1;
                        setCursor(Cursor.getDefaultCursor());
                    }
                }
            });
            invPanel.add(row);
        }
        invPanel.revalidate();
        invPanel.repaint();
    }

    private void addToGrid(int type) {
        for (int i = 0; i < GRID * GRID; i++) {
            if (slots[i] == 0) {
                slots[i] = type;
                updateSlot(i);
                updateResult();
                return;
            }
        }
        slots[GRID * GRID - 1] = type;
        updateSlot(GRID * GRID - 1);
        updateResult();
    }

    private void updateSlot(int idx) {
        int t = slots[idx];
        if (t == 0) {
            slotLabels[idx].setIcon(null);
            slotLabels[idx].setBackground(new Color(20, 20, 40));
            slotLabels[idx].setToolTipText("");
        } else if (t <= blockCount) {
            int c = blockColors[t];
            slotLabels[idx].setBackground(new Color(c));
            int[] pix = new int[256]; Arrays.fill(pix, c);
            BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            img.setRGB(0, 0, 16, 16, pix, 0, 16);
            slotLabels[idx].setIcon(new ImageIcon(img.getScaledInstance(40, 40, Image.SCALE_FAST)));
            slotLabels[idx].setToolTipText(blockNames[t]);
        }
    }

    private void updateResult() {
        currentRecipe = -1;
        resultLabel.setIcon(null);
        resultLabel.setBackground(new Color(20, 20, 40));
        for (int r = 0; r < recipes.size(); r++) {
            if (matchesRecipe(recipes.get(r)[0])) {
                currentRecipe = r;
                resultType = recipes.get(r)[1][0];
                resultCount = recipes.get(r)[1][1];
                int c = resultType <= blockCount ? blockColors[resultType] : 0xFF888888;
                resultLabel.setBackground(new Color(c));
                int[] pix = new int[256]; Arrays.fill(pix, c);
                BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
                img.setRGB(0, 0, 16, 16, pix, 0, 16);
                resultLabel.setIcon(new ImageIcon(img.getScaledInstance(54, 54, Image.SCALE_FAST)));
                String name = resultType <= blockCount ? blockNames[resultType] : ("Type " + resultType);
                statusLabel.setText("Craft: " + resultCount + "x " + name + "  (click result)");
                return;
            }
        }
        statusLabel.setText(" ");
    }

    private Cursor makeDragCursor(int type) {
        int c = type <= blockCount ? blockColors[type] : 0xFF888888;
        BufferedImage img = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(c, true));
        g.fillRect(2, 2, 20, 20);
        g.setColor(new Color(255, 255, 255, 180));
        g.drawRect(1, 1, 21, 21);
        g.dispose();
        return Toolkit.getDefaultToolkit().createCustomCursor(img, new Point(12, 12), "block");
    }

    private boolean matchesRecipe(int[] pattern) {
        for (int i = 0; i < GRID * GRID; i++) {
            if (pattern[i] == 0 && slots[i] != 0) return false;
            if (pattern[i] != 0 && slots[i] != pattern[i]) return false;
        }
        return true;
    }

    private void craftItem() {
        if (currentRecipe < 0) return;
        int[] pattern = recipes.get(currentRecipe)[0];
        for (int i = 0; i < GRID * GRID; i++) {
            if (pattern[i] != 0) {
                if (inventory[pattern[i]] <= 0) {
                    statusLabel.setText("Not enough materials!");
                    return;
                }
            }
        }
        for (int i = 0; i < GRID * GRID; i++) {
            if (pattern[i] != 0) {
                inventory[pattern[i]]--;
                slots[i] = 0;
                updateSlot(i);
            }
        }
        inventory[resultType] += resultCount;
        statusLabel.setText("Crafted " + resultCount + "x " + (resultType <= blockCount ? blockNames[resultType] : ("Type " + resultType)));
        rebuildInventory();
        updateResult();
    }
}
