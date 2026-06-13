import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;
import javax.swing.*;

public class SawTable extends JFrame {
    private int input = 0;
    private int output = 0;
    private final int[] inventory;
    private final int[] blockColors;
    private final String[] blockNames;
    private final int blockCount;
    private final JLabel inputLabel = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel outputLabel = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel statusLabel = new JLabel(" ", SwingConstants.CENTER);
    private final JPanel invPanel = new JPanel();

    public SawTable(int[] inventory, int[] blockColors, String[] blockNames, int blockCount) {
        super("Circular Saw");
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
        invPanel.setPreferredSize(new Dimension(155, 160));
        rebuildInventory();
        add(invPanel, BorderLayout.WEST);

        JPanel center = new JPanel(new GridBagLayout());
        center.setBackground(new Color(30, 30, 50));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);

        JPanel inPanel = new JPanel(new BorderLayout());
        inPanel.setBackground(new Color(40, 40, 70));
        JLabel inTitle = new JLabel("Input", SwingConstants.CENTER);
        inTitle.setFont(new Font("Monospaced", Font.BOLD, 11));
        inTitle.setForeground(new Color(200, 200, 200));
        inPanel.add(inTitle, BorderLayout.NORTH);
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
                updateSlot();
                updateOutput();
                rebuildInventory();
            }
        });
        inPanel.add(inputLabel, BorderLayout.CENTER);
        inPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        JPanel outPanel = new JPanel(new BorderLayout());
        outPanel.setBackground(new Color(40, 40, 70));
        JLabel outTitle = new JLabel("Output", SwingConstants.CENTER);
        outTitle.setFont(new Font("Monospaced", Font.BOLD, 11));
        outTitle.setForeground(new Color(200, 200, 200));
        outPanel.add(outTitle, BorderLayout.NORTH);
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
        outPanel.add(outputLabel, BorderLayout.CENTER);
        outPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        JLabel arrow = new JLabel(" \u2192 ", SwingConstants.CENTER);
        arrow.setFont(new Font("Monospaced", Font.BOLD, 32));
        arrow.setForeground(new Color(200, 200, 100));

        gbc.gridx = 0; gbc.gridy = 0; center.add(inPanel, gbc);
        gbc.gridx = 1; center.add(arrow, gbc);
        gbc.gridx = 2; center.add(outPanel, gbc);

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
                    if (input == 0 && inventory[type] > 0 && type == 4) {
                        inventory[type]--;
                        input = type;
                        output = 12;
                        updateSlot();
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

    private void updateSlot() {
        if (input == 0) {
            inputLabel.setIcon(null);
            inputLabel.setBackground(new Color(20, 20, 40));
            inputLabel.setToolTipText("");
        } else if (input <= blockCount) {
            int c = blockColors[input];
            inputLabel.setBackground(new Color(c));
            int[] pix = new int[256]; Arrays.fill(pix, c);
            BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            img.setRGB(0, 0, 16, 16, pix, 0, 16);
            inputLabel.setIcon(new ImageIcon(img.getScaledInstance(46, 46, Image.SCALE_FAST)));
            inputLabel.setToolTipText(blockNames[input]);
        }
    }

    private void updateOutput() {
        if (output == 0) {
            outputLabel.setIcon(null);
            outputLabel.setBackground(new Color(20, 20, 40));
            outputLabel.setToolTipText("");
            statusLabel.setText(" ");
        } else if (output <= blockCount) {
            int c = blockColors[output];
            outputLabel.setBackground(new Color(c));
            int[] pix = new int[256]; Arrays.fill(pix, c);
            BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            img.setRGB(0, 0, 16, 16, pix, 0, 16);
            outputLabel.setIcon(new ImageIcon(img.getScaledInstance(46, 46, Image.SCALE_FAST)));
            outputLabel.setToolTipText(blockNames[output]);
            statusLabel.setText("Click output to take " + blockNames[output]);
        }
    }

    private void takeOutput() {
        if (output == 0) return;
        inventory[output]++;
        input = 0;
        output = 0;
        updateSlot();
        updateOutput();
        rebuildInventory();
    }
}
