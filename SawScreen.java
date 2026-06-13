import java.awt.*;

public class SawScreen extends GuiScreen {
    private int sawInput, sawOutput;

    @Override
    public void draw(Graphics2D g, int winW, int winH, BlockRegistry reg, int[] playerInv, int selectedBlock) {
        clickBounds.clear();
        updateContext(playerInv, selectedBlock, reg);

        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(0, 0, winW, winH);

        int cx = winW / 2, cy = winH / 2;
        int slotSize = 54;
        int totalW = slotSize * 2 + 30;
        int startX = cx - totalW / 2;
        int startY = cy - slotSize / 2 - 20;

        g.setFont(new Font("Monospaced", Font.BOLD, 14));
        g.setColor(new Color(180, 140, 80));
        g.drawString("Saw", cx - 15, startY - 8);

        g.setColor(new Color(200, 200, 220));
        g.setFont(new Font("Monospaced", Font.PLAIN, 10));
        g.drawString("Input", startX + 10, startY + slotSize + 14);
        drawSlot(g, startX, startY, slotSize, slotSize, sawInput, colors, names);
        clickBounds.add(new int[]{startX, startY, slotSize, slotSize, 20, 0});

        g.setFont(new Font("Monospaced", Font.BOLD, 24));
        g.setColor(new Color(200, 200, 100));
        g.drawString("\u2192", startX + slotSize + 8, startY + slotSize / 2 + 8);

        int ox = startX + slotSize + 30;
        g.setColor(new Color(200, 200, 220));
        g.setFont(new Font("Monospaced", Font.PLAIN, 10));
        g.drawString("Output", ox + 5, startY + slotSize + 14);
        drawSlot(g, ox, startY, slotSize, slotSize, sawOutput, colors, names);
        clickBounds.add(new int[]{ox, startY, slotSize, slotSize, 21, 0});

        String status;
        if (sawOutput > 0) {
            String outName = (sawInput == 12) ? "4x Sticks" : "6x Planks";
            status = "Click output to craft " + outName;
            g.setColor(new Color(100, 220, 100));
        } else if (sawInput == 0) {
            status = "Add Wood (type 4) or Planks (type 12) to Input from inventory";
            g.setColor(new Color(200, 200, 200));
        } else {
            status = "";
        }
        g.setFont(new Font("Monospaced", Font.PLAIN, 11));
        g.drawString(status, cx - g.getFontMetrics().stringWidth(status) / 2, startY + 80);

        drawPlayerInv(g, 10, 120);

        int btnY = startY + slotSize + 80;
        int btnW = 80, btnH = 24;
        g.setColor(new Color(80, 50, 20));
        g.fillRect(cx - btnW / 2, btnY, btnW, btnH);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Monospaced", Font.BOLD, 12));
        g.drawString("Close", cx - 18, btnY + 17);
        clickBounds.add(new int[]{cx - btnW / 2, btnY, btnW, btnH, -1, 0});
    }

    @Override
    protected void handleAction(int action, int data) {
        if (action == 20) {
            if (sawInput > 0) { playerInv[sawInput]++; sawInput = 0; sawOutput = 0; }
        } else if (action == 21) {
            if (sawOutput > 0) {
                int outType = (sawInput == 12) ? 15 : 12;
                int count = (sawInput == 12) ? 4 : 6;
                playerInv[outType] += count;
                sawInput = 0;
                sawOutput = 0;
            }
        } else if (action == 40) {
            int type = data;
            if (type == 4 && playerInv[type] > 0) {
                playerInv[type]--;
                sawInput = 4;
                sawOutput = 12;
            } else if (type == 12 && playerInv[type] > 0) {
                playerInv[type]--;
                sawInput = 12;
                sawOutput = 15;
            }
        }
    }
}
