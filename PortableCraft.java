import java.awt.*;
import java.util.List;

public class PortableCraft {
    private int[] slots = new int[3]; // [in1, in2, out]
    public final List<int[]> clickBounds = new java.util.ArrayList<>();

    public void clearSlots() {
        slots[0] = slots[1] = slots[2] = 0;
    }

    public void addType(int type) {
        if (slots[0] == 0) {
            slots[0] = type;
        } else if (slots[1] == 0) {
            slots[1] = type;
        }
        updateResult();
    }

    public void draw(Graphics2D g, int winW, int winH, int[] playerInv, BlockRegistry reg, int selectedBlock) {
        clickBounds.clear();
        int n = reg.size();
        int[] colors = new int[n + 1];
        String[] names = new String[n + 1];
        for (int i = 1; i <= n; i++) {
            colors[i] = reg.color(i);
            names[i] = reg.name(i);
        }

        int slotSize = 44, gap = 6;
        int totalW = slotSize * 2 + gap + 20 + slotSize;
        int startX = winW / 2 - totalW / 2;
        int startY = winH / 2 + 70;

        g.setColor(new Color(30, 30, 60, 200));
        g.fillRect(startX - 8, startY - 22, totalW + 16, slotSize + 36);
        g.setFont(new Font("Monospaced", Font.BOLD, 10));
        g.setColor(new Color(180, 180, 255));
        g.drawString("Portable Craft", startX, startY - 8);

        GuiScreen.drawSlot(g, startX, startY, slotSize, slotSize, slots[0], colors, names);
        clickBounds.add(new int[]{startX, startY, slotSize, slotSize, 50, 0});
        GuiScreen.drawSlot(g, startX + slotSize + gap, startY, slotSize, slotSize, slots[1], colors, names);
        clickBounds.add(new int[]{startX + slotSize + gap, startY, slotSize, slotSize, 50, 1});
        g.setFont(new Font("Monospaced", Font.BOLD, 22));
        g.setColor(new Color(200, 200, 100));
        g.drawString("\u2192", startX + slotSize * 2 + gap + 4, startY + slotSize / 2 + 6);
        int ox = startX + slotSize * 2 + gap + 20;
        GuiScreen.drawSlot(g, ox, startY, slotSize, slotSize, slots[2], colors, names);
        clickBounds.add(new int[]{ox, startY, slotSize, slotSize, 51, 0});
        if (slots[2] > 0 && slots[2] <= n) {
            g.setFont(new Font("Monospaced", Font.PLAIN, 9));
            g.setColor(new Color(100, 220, 100));
            g.drawString(names[slots[2]], ox + 4, startY + slotSize + 14);
        }
    }

    public void handleClick(int slot, int selectedBlock, int[] playerInv) {
        if (slot == 0 || slot == 1) {
            int type = slots[slot];
            if (type > 0) {
                playerInv[type]++;
                slots[slot] = 0;
                updateResult();
            } else if (selectedBlock > 0 && playerInv[selectedBlock] > 0) {
                playerInv[selectedBlock]--;
                slots[slot] = selectedBlock;
                updateResult();
            }
        } else if (slot == 2) {
            if (slots[2] > 0) {
                int result = slots[2];
                if (result == 12 || result == 15 || result == 8) {
                    playerInv[result] += (result == 12) ? 4 : (result == 15) ? 2 : 3;
                    slots[0] = 0;
                    slots[1] = 0;
                    slots[2] = 0;
                } else if (result == 10) {
                    playerInv[10]++;
                    slots[0] = 0;
                    slots[1] = 0;
                    slots[2] = 0;
                }
            }
        }
    }

    private void updateResult() {
        int a = slots[0], b = slots[1];
        if (a == 4 && b == 4) { slots[2] = 12; return; }
        if (a == 12 && b == 12) { slots[2] = 15; return; }
        if (a == 3 && b == 15) { slots[2] = 10; return; }
        if (a == 8 && b == 8) { slots[2] = 8; return; }
        if (a == 4 && b == 0) { slots[2] = 12; return; }
        if (a == 12 && b == 0) { slots[2] = 15; return; }
        slots[2] = 0;
    }
}
