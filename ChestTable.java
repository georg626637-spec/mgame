import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;

public class ChestTable extends JFrame {
    private static final Map<String, int[]> chestInventories = new HashMap<>();
    private static final int ROWS = 3, COLS = 9;
    private JLabel[] slotLabels = new JLabel[ROWS * COLS];
    private int[] inventory;
    private String posKey;
    private int[] blockColors;
    private String[] blockNames;
    private int blockCount;
    private int[] playerInv;
    private int selectedBlock;

    public ChestTable(int[] playerInv, int selectedBlock, int[] colors, String[] names, int blockCount, int cx, int cy, int cz) {
        this.playerInv = playerInv;
        this.selectedBlock = selectedBlock;
        posKey = cx + "," + cy + "," + cz;
        inventory = chestInventories.computeIfAbsent(posKey, k -> new int[ROWS * COLS]);
        this.blockColors = colors;
        this.blockNames = names;
        this.blockCount = blockCount;

        setTitle("Chest");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel grid = new JPanel(new GridLayout(ROWS, COLS, 2, 2));
        grid.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        grid.setBackground(new Color(40, 30, 15));
        for (int i = 0; i < ROWS * COLS; i++) {
            final int slot = i;
            JLabel lbl = new JLabel("", JLabel.CENTER);
            lbl.setPreferredSize(new Dimension(56, 56));
            lbl.setOpaque(true);
            lbl.setBackground(new Color(60, 40, 20));
            lbl.setFont(new Font("Monospaced", Font.PLAIN, 10));
            updateSlot(lbl, slot);
            lbl.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    int stack = inventory[slot];
                    if (stack > 0) {
                        if (playerInv[stack] < 64) {
                            playerInv[stack]++;
                            inventory[slot] = 0;
                        }
                    } else if (selectedBlock > 0 && playerInv[selectedBlock] > 0) {
                        playerInv[selectedBlock]--;
                        inventory[slot] = selectedBlock;
                    }
                    for (int j = 0; j < ROWS * COLS; j++) updateSlot(slotLabels[j], j);
                }
            });
            slotLabels[i] = lbl;
            grid.add(lbl);
        }
        add(grid, BorderLayout.CENTER);

        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dispose());
        add(closeBtn, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
    }

    private void updateSlot(JLabel lbl, int slot) {
        int val = inventory[slot];
        if (val > 0 && val <= blockCount) {
            int c = blockColors[val];
            lbl.setBackground(new Color(c));
            int[] pix = new int[256];
            Arrays.fill(pix, c);
            BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            img.setRGB(0, 0, 16, 16, pix, 0, 16);
            lbl.setIcon(new ImageIcon(img.getScaledInstance(32, 32, Image.SCALE_FAST)));
            lbl.setText(blockNames[val]);
            lbl.setToolTipText(blockNames[val]);
            lbl.setForeground(Color.WHITE);
            lbl.setVerticalTextPosition(JLabel.BOTTOM);
            lbl.setHorizontalTextPosition(JLabel.CENTER);
        } else {
            lbl.setIcon(null);
            lbl.setText("");
            lbl.setToolTipText(null);
            lbl.setBackground(new Color(60, 40, 20));
        }
    }

    public static void clearChest(int cx, int cy, int cz) {
        chestInventories.remove(cx + "," + cy + "," + cz);
    }

    public static void fillChest(int cx, int cy, int cz, int[] items) {
        int[] inv = chestInventories.computeIfAbsent(cx + "," + cy + "," + cz, k -> new int[ROWS * COLS]);
        for (int i = 0; i < items.length && i < inv.length; i++) {
            if (items[i] > 0) inv[i] = items[i];
        }
    }
}
