import java.awt.*;
import java.util.Arrays;

public class CraftingScreen extends GuiScreen {
    private static final int GRID = 4;
    private final int[] craftSlots = new int[GRID * GRID];
    private int craftResultType, craftResultCount, craftRecipeIdx = -1;

    @Override
    public void draw(Graphics2D g, int winW, int winH, BlockRegistry reg, int[] playerInv, int selectedBlock) {
        clickBounds.clear();
        updateContext(playerInv, selectedBlock, reg);

        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(0, 0, winW, winH);

        int cx = winW / 2, cy = winH / 2;
        int slotSize = 44, gap = 4;
        int totalW = GRID * (slotSize + gap) - gap;
        int totalH = GRID * (slotSize + gap) - gap;
        int startX = cx - totalW / 2 - 40;
        int startY = cy - totalH / 2 - 10;

        g.setFont(new Font("Monospaced", Font.BOLD, 14));
        g.setColor(new Color(180, 180, 255));
        g.drawString("Crafting", startX, startY - 8);

        for (int row = 0; row < GRID; row++) {
            for (int col = 0; col < GRID; col++) {
                int idx = row * GRID + col;
                int sx = startX + col * (slotSize + gap);
                int sy = startY + row * (slotSize + gap);
                drawSlot(g, sx, sy, slotSize, slotSize, craftSlots[idx], colors, names);
                clickBounds.add(new int[]{sx, sy, slotSize, slotSize, 30, idx});
            }
        }

        int arrowX = startX + totalW + 10;
        g.setFont(new Font("Monospaced", Font.BOLD, 24));
        g.setColor(new Color(200, 200, 100));
        g.drawString("\u2192", arrowX, cy + 8);

        int rsX = arrowX + 30;
        int rsY = cy - slotSize / 2;
        drawSlot(g, rsX, rsY, slotSize + 10, slotSize + 10, craftResultType, colors, names);
        clickBounds.add(new int[]{rsX, rsY, slotSize + 10, slotSize + 10, 31, 0});

        if (craftRecipeIdx >= 0) {
            String s = "Craft: " + craftResultCount + "x " + (craftResultType <= blockCount ? names[craftResultType] : "?");
            g.setFont(new Font("Monospaced", Font.PLAIN, 11));
            g.setColor(new Color(100, 220, 100));
            g.drawString(s, cx - g.getFontMetrics().stringWidth(s) / 2, startY + totalH + 28);
        }

        drawPlayerInv(g, 10, 120);

        int btnY = startY + totalH + 40;
        int btnW = 80, btnH = 24;
        g.setColor(new Color(50, 50, 100));
        g.fillRect(cx - btnW / 2, btnY, btnW, btnH);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Monospaced", Font.BOLD, 12));
        g.drawString("Close", cx - 18, btnY + 17);
        clickBounds.add(new int[]{cx - btnW / 2, btnY, btnW, btnH, -1, 0});
    }

    @Override
    protected void handleAction(int action, int data) {
        if (action == 30) {
            int slot = data;
            if (craftSlots[slot] > 0) {
                playerInv[craftSlots[slot]]++;
                craftSlots[slot] = 0;
                updateCraftResult();
            } else if (selectedBlock > 0 && playerInv[selectedBlock] > 0) {
                playerInv[selectedBlock]--;
                craftSlots[slot] = selectedBlock;
                updateCraftResult();
            }
        } else if (action == 31) {
            if (craftRecipeIdx >= 0) {
                craftItem(playerInv);
            }
        } else if (action == 40) {
            int type = data;
            if (playerInv[type] > 0) {
                for (int i = 0; i < GRID * GRID; i++) {
                    if (craftSlots[i] == 0) {
                        playerInv[type]--;
                        craftSlots[i] = type;
                        updateCraftResult();
                        return;
                    }
                }
            }
        }
    }

    private void updateCraftResult() {
        craftRecipeIdx = -1;
        java.util.List<int[][]> recipes = CraftingTable.getRecipes();
        for (int r = 0; r < recipes.size(); r++) {
            if (matchesRecipe(recipes.get(r)[0])) {
                craftRecipeIdx = r;
                craftResultType = recipes.get(r)[1][0];
                craftResultCount = recipes.get(r)[1][1];
                return;
            }
        }
        craftResultType = 0;
        craftResultCount = 0;
    }

    private boolean matchesRecipe(int[] pattern) {
        for (int i = 0; i < GRID * GRID; i++) {
            if (pattern[i] == 0 && craftSlots[i] != 0) return false;
            if (pattern[i] != 0 && craftSlots[i] != pattern[i]) return false;
        }
        return true;
    }

    private void craftItem(int[] playerInv) {
        java.util.List<int[][]> recipes = CraftingTable.getRecipes();
        if (craftRecipeIdx < 0 || craftRecipeIdx >= recipes.size()) return;
        int[] pattern = recipes.get(craftRecipeIdx)[0];
        for (int i = 0; i < GRID * GRID; i++) {
            if (pattern[i] != 0 && craftSlots[i] != pattern[i]) return;
        }
        for (int i = 0; i < GRID * GRID; i++) {
            if (pattern[i] != 0) {
                craftSlots[i] = 0;
            }
        }
        playerInv[craftResultType] += craftResultCount;
        updateCraftResult();
    }
}
