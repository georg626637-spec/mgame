import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;
import javax.swing.*;

public class FurnaceTable extends JFrame {
    private int input = 0;
    private int fuel = 0;
    private int output = 0;
    private final int[] inventory;
    private final int[] blockColors;
    private final String[] blockNames;
    private final int blockCount;
    private final JLabel inputLabel = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel fuelLabel = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel outputLabel = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel statusLabel = new JLabel(" ", SwingConstants.CENTER);
    private final JPanel invPanel = new JPanel();

    private static final int[][] recipes = {
        {9, 9},  // iron ore → iron
        {4, 8},  // wood → coal
        {7, 14}, // sand → glass (placeholder if added)
    };

    public FurnaceTable(int[] inventory, int[] blockColors, String[] blockNames, int blockCount) {
        super("Furnace");
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
            "Inventory", javax.swing.border.TitledBorder.LEFT,
            javax.swing.border.TitledBorder.TOP,
            new Font("Monospaced", Font.BOLD, 10), new Color(180, 180, 255)));
        invPanel.setPreferredSize(new Dimension(155, 200));
        rebuildInventory();
        add(invPanel, BorderLayout.WEST);

        JPanel center = new JPanel(new GridBagLayout());
        center.setBackground(new Color(30, 30, 50));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);

        inputLabel.setPreferredSize(new Dimension(56, 56));
        inputLabel.setOpaque(true);
        inputLabel.setBackground(new Color(20, 20, 40));
        inputLabel.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 100), 2));
        inputLabel.setFont(new Font("Monospaced", Font.PLAIN, 18));
        inputLabel.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                if (input == 0) return;
                inventory[input]++;
                input = 0;
                output = 0;
                updateSlot(inputLabel, input);
                updateSlot(outputLabel, output);
                updateStatus();
                rebuildInventory();
            }
        });

        fuelLabel.setPreferredSize(new Dimension(56, 56));
        fuelLabel.setOpaque(true);
        fuelLabel.setBackground(new Color(20, 20, 40));
        fuelLabel.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 100), 2));
        fuelLabel.setFont(new Font("Monospaced", Font.PLAIN, 18));
        fuelLabel.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                if (fuel == 0) return;
                inventory[fuel]++;
                fuel = 0;
                updateSlot(fuelLabel, fuel);
                updateStatus();
                rebuildInventory();
            }
        });

        outputLabel.setPreferredSize(new Dimension(56, 56));
        outputLabel.setOpaque(true);
        outputLabel.setBackground(new Color(20, 20, 40));
        outputLabel.setBorder(BorderFactory.createLineBorder(new Color(100, 180, 100), 2));
        outputLabel.setFont(new Font("Monospaced", Font.BOLD, 22));
        outputLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                takeOutput();
            }
        });

        JLabel arrow = new JLabel(" \u2192 ", SwingConstants.CENTER);
        arrow.setFont(new Font("Monospaced", Font.BOLD, 28));
        arrow.setForeground(new Color(200, 200, 100));

        JPanel inputPane = new JPanel(new BorderLayout());
        inputPane.setBackground(new Color(40, 40, 70));
        JLabel inT = new JLabel("Input", SwingConstants.CENTER);
        inT.setFont(new Font("Monospaced", Font.BOLD, 11));
        inT.setForeground(new Color(200, 200, 200));
        inputPane.add(inT, BorderLayout.NORTH);
        inputPane.add(inputLabel, BorderLayout.CENTER);
        inputPane.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        JPanel fuelPane = new JPanel(new BorderLayout());
        fuelPane.setBackground(new Color(40, 40, 70));
        JLabel fT = new JLabel("Fuel", SwingConstants.CENTER);
        fT.setFont(new Font("Monospaced", Font.BOLD, 11));
        fT.setForeground(new Color(200, 200, 200));
        fuelPane.add(fT, BorderLayout.NORTH);
        fuelPane.add(fuelLabel, BorderLayout.CENTER);
        fuelPane.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        JPanel outPane = new JPanel(new BorderLayout());
        outPane.setBackground(new Color(40, 40, 70));
        JLabel oT = new JLabel("Output", SwingConstants.CENTER);
        oT.setFont(new Font("Monospaced", Font.BOLD, 11));
        oT.setForeground(new Color(200, 200, 200));
        outPane.add(oT, BorderLayout.NORTH);
        outPane.add(outputLabel, BorderLayout.CENTER);
        outPane.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        gbc.gridx = 0; gbc.gridy = 0; center.add(inputPane, gbc);
        gbc.gridy = 1; center.add(fuelPane, gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.gridheight = 2; center.add(arrow, gbc);
        gbc.gridx = 2; gbc.gridy = 0; gbc.gridheight = 2; center.add(outPane, gbc);

        add(center, BorderLayout.CENTER);

        statusLabel.setFont(new Font("Monospaced", Font.PLAIN, 12));
        statusLabel.setForeground(new Color(200, 200, 200));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        add(statusLabel, BorderLayout.NORTH);

        pack();
        setResizable(false);
        setLocationRelativeTo(null);
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
                public void mouseClicked(MouseEvent e) {
                    if (fuel == 0 && (type == 4 || type == 8) && inventory[type] > 0) {
                        inventory[type]--;
                        fuel = type;
                        updateSlot(fuelLabel, fuel);
                        updateStatus();
                        rebuildInventory();
                        return;
                    }
                    if (input == 0 && inventory[type] > 0 && isSmeltable(type)) {
                        inventory[type]--;
                        input = type;
                        updateSlot(inputLabel, input);
                        updateOutput();
                        rebuildInventory();
                    }
                }
            });
            invPanel.add(row);
        }
        invPanel.revalidate();
        invPanel.repaint();
    }

    private boolean isSmeltable(int type) {
        for (int[] r : recipes) if (r[0] == type) return true;
        return false;
    }

    private int getSmeltResult(int type) {
        for (int[] r : recipes) if (r[0] == type) return r[1];
        return 0;
    }

    private void updateSlot(JLabel label, int t) {
        if (t == 0) {
            label.setIcon(null);
            label.setBackground(new Color(20, 20, 40));
            label.setToolTipText("");
        } else if (t <= blockCount) {
            int c = blockColors[t];
            label.setBackground(new Color(c));
            int[] pix = new int[256]; Arrays.fill(pix, c);
            BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            img.setRGB(0, 0, 16, 16, pix, 0, 16);
            label.setIcon(new ImageIcon(img.getScaledInstance(46, 46, Image.SCALE_FAST)));
            label.setToolTipText(blockNames[t]);
        }
    }

    private void updateOutput() {
        int result = input > 0 ? getSmeltResult(input) : 0;
        if (result > 0 && fuel > 0) {
            output = result;
        } else {
            output = 0;
        }
        updateSlot(outputLabel, output);
        updateStatus();
    }

    private void updateStatus() {
        if (output > 0 && fuel > 0) {
            statusLabel.setFont(new Font("Monospaced", Font.PLAIN, 12));
            statusLabel.setText("Click output to smelt 1x " + blockNames[output]);
        } else if (input == 0) {
            statusLabel.setText("Add smeltable item to Input slot");
        } else if (fuel == 0) {
            statusLabel.setText("Add fuel (Wood or Coal) to Fuel slot");
        } else {
            statusLabel.setFont(new Font("Monospaced", Font.PLAIN, 12));
            statusLabel.setText(" ");
        }
    }

    private void takeOutput() {
        if (output == 0) return;
        inventory[output]++;
        if (fuel > 0) fuel--;
        input = 0;
        output = 0;
        updateSlot(inputLabel, input);
        updateSlot(fuelLabel, fuel);
        updateSlot(outputLabel, output);
        updateStatus();
        rebuildInventory();
    }
}
