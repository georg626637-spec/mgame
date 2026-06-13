import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class ServerDialog extends JFrame {
    private final JTextField addressField = new JTextField("127.0.0.1:25565");

    public ServerDialog(String initial) {
        super("Server Connect");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(new Color(30, 30, 50));
        if (!initial.isEmpty()) addressField.setText(initial);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(new Color(30, 30, 50));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel label = new JLabel("Server Address (ip:port):");
        label.setFont(new Font("Monospaced", Font.BOLD, 14));
        label.setForeground(new Color(200, 200, 220));

        addressField.setFont(new Font("Monospaced", Font.PLAIN, 16));
        addressField.setBackground(new Color(20, 20, 40));
        addressField.setForeground(new Color(100, 255, 100));
        addressField.setCaretColor(new Color(100, 255, 100));
        addressField.setBorder(BorderFactory.createLineBorder(new Color(80, 80, 140), 2));
        addressField.setPreferredSize(new Dimension(300, 35));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        btnPanel.setBackground(new Color(30, 30, 50));

        JButton connectBtn = new JButton("Connect");
        connectBtn.setFont(new Font("Monospaced", Font.BOLD, 14));
        connectBtn.setBackground(new Color(40, 80, 40));
        connectBtn.setForeground(new Color(200, 255, 200));
        connectBtn.setFocusPainted(false);
        connectBtn.addActionListener(e -> {
            String addr = addressField.getText().trim();
            if (!addr.isEmpty()) {
                addMessage("Connecting to " + addr + "...");
                setVisible(false);
                dispose();
            }
        });

        JButton closeBtn = new JButton("Close");
        closeBtn.setFont(new Font("Monospaced", Font.BOLD, 14));
        closeBtn.setBackground(new Color(80, 40, 40));
        closeBtn.setForeground(new Color(255, 200, 200));
        closeBtn.setFocusPainted(false);
        closeBtn.addActionListener(e -> {
            setVisible(false);
            dispose();
        });

        JPanel fields = new JPanel(new BorderLayout(5, 5));
        fields.setBackground(new Color(30, 30, 50));
        fields.add(label, BorderLayout.NORTH);
        fields.add(addressField, BorderLayout.SOUTH);

        btnPanel.add(connectBtn);
        btnPanel.add(closeBtn);

        mainPanel.add(fields, BorderLayout.CENTER);
        mainPanel.add(btnPanel, BorderLayout.SOUTH);

        add(mainPanel);
        pack();
        setResizable(false);
        setLocationRelativeTo(null);

        addressField.selectAll();
        addressField.requestFocusInWindow();
    }

    private void addMessage(String msg) {
        System.out.println("[Server] " + msg);
    }
}
