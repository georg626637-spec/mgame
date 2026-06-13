import java.awt.*;
import java.util.List;

public class FurnaceScreen extends GuiScreen {
    private int[] inputSlots = new int[9]; // 3x3 grid
    private int furFuel, furFuelCount;
    private int recipeIdx = -1;

    private static final List<int[][]> furnaceRecipes = new java.util.ArrayList<>();
    static {
        addRecipe(new int[]{3,3,3, 3,0,0, 3,0,0}, 14, 1);
    }

    private static void addRecipe(int[] pattern, int resultType, int resultCount) {
        furnaceRecipes.add(new int[][]{pattern, new int[]{resultType, resultCount}});
    }

    @Override
    public void draw(Graphics2D g, int winW, int winH, BlockRegistry reg, int[] playerInv, int selectedBlock) {
        clickBounds.clear();
        updateContext(playerInv, selectedBlock, reg);
        recipeIdx = -1;

        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(0, 0, winW, winH);

        int cx = winW / 2, cy = winH / 2;
        int slotSize = 44, gap = 4;
        int cols = 3, rows = 3;
        int gridW = cols * (slotSize + gap) - gap;
        int gridH = rows * (slotSize + gap) - gap;
        int startX = cx - gridW / 2 - 30;
        int startY = cy - gridH / 2 - 10;

        g.setFont(new Font("Monospaced", Font.BOLD, 14));
        g.setColor(new Color(200, 120, 60));
        g.drawString("Furnace", startX, startY - 8);

        g.setFont(new Font("Monospaced", Font.PLAIN, 10));
        g.setColor(new Color(200, 200, 220));
        g.drawString("Input", startX, startY + gridH + 14);

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int idx = row * cols + col;
                int sx = startX + col * (slotSize + gap);
                int sy = startY + row * (slotSize + gap);
                drawSlot(g, sx, sy, slotSize, slotSize, inputSlots[idx], colors, names);
                clickBounds.add(new int[]{sx, sy, slotSize, slotSize, 10, idx});
            }
        }

        int arrowX = startX + gridW + 10;
        g.setFont(new Font("Monospaced", Font.BOLD, 24));
        g.setColor(new Color(200, 200, 100));
        g.drawString("\u2192", arrowX, cy + 8);

        int ox = arrowX + 30;
        int oy = cy - slotSize / 2;
        int preview = getPreviewOutput();
        drawSlot(g, ox, oy, slotSize + 6, slotSize + 6, preview, colors, names);
        clickBounds.add(new int[]{ox, oy, slotSize + 6, slotSize + 6, 12, 0});

        int totalW = gridW + 40 + slotSize + 6;
        int fuelAreaStartX = cx - totalW / 2 + gridW / 2 + 30;
        int fy = startY + gridH + 40;
        g.setFont(new Font("Monospaced", Font.PLAIN, 10));
        g.setColor(new Color(200, 200, 220));
        String fuelLabel = "Fuel" + (furFuelCount > 0 ? " x" + furFuelCount : "");
        g.drawString(fuelLabel, fuelAreaStartX, fy - 8);
        drawSlot(g, fuelAreaStartX, fy, slotSize, slotSize, furFuel, colors, names);
        clickBounds.add(new int[]{fuelAreaStartX, fy, slotSize, slotSize, 11, 0});

        String status;
        int filledCount = 0;
        for (int i = 0; i < 9; i++) if (inputSlots[i] > 0) filledCount++;
        if (filledCount > 0 && furFuel > 0 && furFuelCount > 0) {
            String resultName = (preview > 0 && preview <= blockCount) ? names[preview] : "?";
            status = "Click output to smelt 1x " + resultName;
            g.setColor(new Color(100, 220, 100));
        } else if (filledCount == 0) {
            status = "Add smeltable items to the grid from inventory";
            g.setColor(new Color(200, 200, 200));
        } else if (furFuel == 0 || furFuelCount == 0) {
            status = "Add fuel (Wood or Coal)";
            g.setColor(new Color(200, 200, 200));
        } else {
            status = "";
        }
        g.setFont(new Font("Monospaced", Font.PLAIN, 11));
        g.drawString(status, cx - g.getFontMetrics().stringWidth(status) / 2, startY + gridH + 90);

        drawPlayerInv(g, 10, 120);

        int btnY = startY + gridH + 100;
        int btnW = 80, btnH = 24;
        g.setColor(new Color(80, 40, 20));
        g.fillRect(cx - btnW / 2, btnY, btnW, btnH);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Monospaced", Font.BOLD, 12));
        g.drawString("Close", cx - 18, btnY + 17);
        clickBounds.add(new int[]{cx - btnW / 2, btnY, btnW, btnH, -1, 0});
    }

    @Override
    protected void handleAction(int action, int data) {
        if (action == 10) { // Input grid slot
            int slot = data;
            int type = inputSlots[slot];
            if (type > 0) {
                playerInv[type]++;
                inputSlots[slot] = 0;
            } else if (selectedBlock > 0 && playerInv[selectedBlock] > 0) {
                playerInv[selectedBlock]--;
                inputSlots[slot] = selectedBlock;
            }
        } else if (action == 11) { // Fuel slot
            if (furFuel > 0 && furFuelCount > 0) {
                playerInv[furFuel]++;
                furFuelCount--;
                if (furFuelCount <= 0) { furFuel = 0; furFuelCount = 0; }
            } else if (selectedBlock > 0 && playerInv[selectedBlock] > 0 && (selectedBlock == 4 || selectedBlock == 8)) {
                playerInv[selectedBlock]--;
                furFuel = selectedBlock;
                furFuelCount = 1;
            }
        } else if (action == 12) { // Output click → try pattern first, then individual smelt
            if (furFuel <= 0 || furFuelCount <= 0) return;
            // Check pattern recipes
            for (int r = 0; r < furnaceRecipes.size(); r++) {
                if (matchesPattern(furnaceRecipes.get(r)[0])) {
                    int[] result = furnaceRecipes.get(r)[1];
                    int resultType = result[0], resultCount = result[1];
                    int cost = 1;
                    if (furFuelCount >= cost) {
                        playerInv[resultType] += resultCount;
                        furFuelCount -= cost;
                        if (furFuelCount <= 0) { furFuel = 0; furFuelCount = 0; }
                        for (int i = 0; i < 9; i++) inputSlots[i] = 0;
                        return;
                    }
                }
            }
            // Fallback: individual smelting
            for (int i = 0; i < 9; i++) {
                if (inputSlots[i] > 0 && isSmeltable(inputSlots[i])) {
                    int type = inputSlots[i];
                    int cost = (type == 3) ? 2 : 1;
                    if (furFuelCount >= cost) {
                        int result = getSmeltResult(type);
                        playerInv[result]++;
                        furFuelCount -= cost;
                        if (furFuelCount <= 0) { furFuel = 0; furFuelCount = 0; }
                        inputSlots[i] = 0;
                        return;
                    }
                }
            }
        } else if (action == 40) { // Player inventory
            int type = data;
            if (playerInv[type] <= 0) return;
            if (isSmeltable(type)) {
                for (int i = 0; i < 9; i++) {
                    if (inputSlots[i] == 0) {
                        playerInv[type]--;
                        inputSlots[i] = type;
                        return;
                    }
                }
            } else if (type == 4 || type == 8) {
                if (furFuel == 0) {
                    playerInv[type]--;
                    furFuel = type;
                    furFuelCount = 1;
                } else if (furFuel == type && furFuelCount < 64) {
                    playerInv[type]--;
                    furFuelCount++;
                }
            }
        }
    }

    private boolean matchesPattern(int[] pattern) {
        for (int i = 0; i < 9; i++) {
            if (pattern[i] == 0 && inputSlots[i] != 0) return false;
            if (pattern[i] != 0 && inputSlots[i] != pattern[i]) return false;
        }
        return true;
    }

    private int getPreviewOutput() {
        for (int r = 0; r < furnaceRecipes.size(); r++) {
            if (matchesPattern(furnaceRecipes.get(r)[0])) {
                return furnaceRecipes.get(r)[1][0];
            }
        }
        for (int i = 0; i < 9; i++) {
            if (inputSlots[i] > 0 && isSmeltable(inputSlots[i]))
                return getSmeltResult(inputSlots[i]);
        }
        return 0;
    }

    private static boolean isSmeltable(int type) {
        return type == 9 || type == 4 || type == 7 || type == 3;
    }

    private static int getSmeltResult(int type) {
        if (type == 9) return 9;
        if (type == 4) return 8;
        if (type == 7) return 14;
        if (type == 3) return 14;
        return 0;
    }
}
