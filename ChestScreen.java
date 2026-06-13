import java.awt.*;

public class ChestScreen extends GuiScreen {
    private String chestKey;
    private int[] chestInv;

    public ChestScreen(String chestKey, int[] chestInv) {
        this.chestKey = chestKey;
        this.chestInv = chestInv;
    }

    @Override
    public void draw(Graphics2D g, int winW, int winH, BlockRegistry reg, int[] playerInv, int selectedBlock) {
        clickBounds.clear();
        updateContext(playerInv, selectedBlock, reg);

        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(0, 0, winW, winH);

        int cx = winW / 2, cy = winH / 2;
        int rows = 3, cols = 9;
        int slotSize = 50, gap = 4;
        int totalW = cols * (slotSize + gap) - gap;
        int totalH = rows * (slotSize + gap) - gap;
        int startX = cx - totalW / 2;
        int startY = cy - totalH / 2 - 20;

        g.setFont(new Font("Monospaced", Font.BOLD, 14));
        g.setColor(new Color(255, 215, 0));
        g.drawString("Chest", cx - 20, startY - 8);

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int idx = row * cols + col;
                int sx = startX + col * (slotSize + gap);
                int sy = startY + row * (slotSize + gap);
                drawSlot(g, sx, sy, slotSize, slotSize, chestInv[idx], colors, names);
                clickBounds.add(new int[]{sx, sy, slotSize, slotSize, 0, idx});
            }
        }

        int btnY = startY + totalH + 12;
        int btnW = 80, btnH = 24;
        g.setColor(new Color(100, 40, 20));
        g.fillRect(cx - btnW / 2, btnY, btnW, btnH);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Monospaced", Font.BOLD, 12));
        g.drawString("Close", cx - 18, btnY + 17);
        clickBounds.add(new int[]{cx - btnW / 2, btnY, btnW, btnH, -1, 0});
    }

    @Override
    protected void handleAction(int action, int data) {
        int slot = data;
        int stack = chestInv[slot];
        if (stack > 0) {
            if (playerInv[stack] < 64) {
                playerInv[stack]++;
                chestInv[slot] = 0;
            }
        } else if (selectedBlock > 0 && playerInv[selectedBlock] > 0) {
            playerInv[selectedBlock]--;
            chestInv[slot] = selectedBlock;
        }
    }
}
