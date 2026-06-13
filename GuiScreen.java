import java.awt.*;
import java.util.List;
import java.util.ArrayList;

public abstract class GuiScreen {
    protected List<int[]> clickBounds = new ArrayList<>();
    protected int[] playerInv;
    protected int selectedBlock;
    protected int blockCount;
    protected int[] colors;
    protected String[] names;

    protected void updateContext(int[] playerInv, int selectedBlock, BlockRegistry reg) {
        this.playerInv = playerInv;
        this.selectedBlock = selectedBlock;
        if (reg != null) {
            blockCount = reg.size();
            colors = new int[blockCount + 1];
            names = new String[blockCount + 1];
            for (int i = 1; i <= blockCount; i++) {
                colors[i] = reg.color(i);
                names[i] = reg.name(i);
            }
        }
    }

    public abstract void draw(Graphics2D g, int winW, int winH, BlockRegistry reg, int[] playerInv, int selectedBlock);

    public boolean handleClick(int mx, int my, int selectedBlock) {
        this.selectedBlock = selectedBlock;
        for (int[] b : clickBounds) {
            int bx = b[0], by = b[1], bw = b[2], bh = b[3], action = b[4], data = b[5];
            if (mx >= bx && mx <= bx + bw && my >= by && my <= by + bh) {
                if (action == -1) return false;
                handleAction(action, data);
                return true;
            }
        }
        return false;
    }

    protected abstract void handleAction(int action, int data);

    public void onClose() {}

    public List<int[]> getClickBounds() { return clickBounds; }

    protected static void drawSlot(Graphics2D g, int x, int y, int w, int h, int type, int[] colors, String[] names) {
        g.setColor(new Color(30, 30, 50));
        g.fillRect(x, y, w, h);
        g.setColor(new Color(80, 80, 120));
        g.drawRect(x, y, w, h);
        if (type > 0 && type < colors.length) {
            int c = colors[type];
            g.setColor(new Color(c));
            g.fillRect(x + 2, y + 2, w - 4, h - 4);
            if (type < names.length) {
                g.setFont(new Font("Monospaced", Font.PLAIN, 9));
                g.setColor(Color.WHITE);
                String name = names[type];
                int nw = g.getFontMetrics().stringWidth(name);
                g.drawString(name, x + (w - nw) / 2, y + h - 5);
            }
        }
    }

    protected void drawPlayerInv(Graphics2D g, int x, int y) {
        g.setFont(new Font("Monospaced", Font.BOLD, 10));
        g.setColor(new Color(180, 180, 255));
        g.drawString("Inventory", x, y - 2);
        int iy = y + 4;
        g.setFont(new Font("Monospaced", Font.PLAIN, 10));
        for (int t = 1; t <= blockCount; t++) {
            if (playerInv[t] <= 0) continue;
            int c = colors[t];
            g.setColor(new Color(c));
            g.fillRect(x, iy, 10, 10);
            g.setColor(new Color(200, 200, 220));
            g.drawString(names[t] + ": " + playerInv[t], x + 14, iy + 9);
            if (t == selectedBlock) {
                g.setColor(new Color(255, 255, 100, 180));
                g.drawRect(x - 1, iy - 1, 12, 12);
            }
            clickBounds.add(new int[]{x, iy, 120, 12, 40, t});
            iy += 13;
        }
    }
}
