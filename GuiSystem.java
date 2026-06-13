import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class GuiSystem {
    public enum State { NONE, CHEST, FURNACE, SAW, CRAFTING }
    public static State state = State.NONE;

    // Cached data for click handling
    static int[] cachedPlayerInv;
    static int cachedSelectedBlock;
    static int[] cachedColors;
    static String[] cachedNames;
    static int cachedBlockCount;

    // Chest
    private static final Map<String, int[]> chestInventories = new HashMap<>();
    private static String chestKey;
    private static int[] chestInv;

    // Furnace
    private static int furInput, furFuel, furFuelCount, furOutput;

    // Saw
    private static int sawInput, sawOutput;

    // Crafting
    private static final int GRID = 4;
    private static final int[] craftSlots = new int[GRID * GRID];
    private static int craftResultType, craftResultCount, craftRecipeIdx = -1;

    // Shared
    public static List<int[]> clickBounds = new ArrayList<>();

    // Keeping the old Swing classes working for addRecipe calls
    public static void addRecipe(int[] pattern, int resultType, int resultCount) {
        CraftingTable.addRecipe(pattern, resultType, resultCount);
    }

    public static void clearChest(int cx, int cy, int cz) {
        chestInventories.remove(cx + "," + cy + "," + cz);
    }

    public static void fillChest(int cx, int cy, int cz, int[] items) {
        int[] inv = chestInventories.computeIfAbsent(cx + "," + cy + "," + cz, k -> new int[27]);
        for (int i = 0; i < items.length && i < inv.length; i++) {
            if (items[i] > 0) inv[i] = items[i];
        }
    }

    public static void openChest(int cx, int cy, int cz, int[] playerInv, int selectedBlock) {
        chestKey = cx + "," + cy + "," + cz;
        chestInv = chestInventories.computeIfAbsent(chestKey, k -> new int[27]);
        state = State.CHEST;
    }

    public static void openFurnace(int[] playerInv) {
        furInput = 0; furFuel = 0; furFuelCount = 0; furOutput = 0;
        state = State.FURNACE;
    }

    public static void openSaw(int[] playerInv) {
        sawInput = 0; sawOutput = 0;
        state = State.SAW;
    }

    public static void openCrafting(int[] playerInv) {
        Arrays.fill(craftSlots, 0);
        craftResultType = 0; craftResultCount = 0; craftRecipeIdx = -1;
        state = State.CRAFTING;
    }

    public static void close() {
        state = State.NONE;
        clickBounds.clear();
    }

    public static boolean isOpen() { return state != State.NONE; }

    // ---- DRAW ----
    public static void draw(Graphics2D g, int winW, int winH, BlockRegistry reg, int[] playerInv, int selectedBlock) {
        if (state == State.NONE) return;
        clickBounds.clear();
        cachedPlayerInv = playerInv;
        cachedSelectedBlock = selectedBlock;

        int blockCount = reg.size();
        int[] colors = new int[blockCount + 1];
        String[] names = new String[blockCount + 1];
        for (int i = 1; i <= blockCount; i++) {
            colors[i] = reg.color(i);
            names[i] = reg.name(i);
        }

        int cx = winW / 2, cy = winH / 2;

        // Dim background
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(0, 0, winW, winH);

        switch (state) {
            case CHEST: drawChest(g, cx, cy, colors, names, blockCount, playerInv, selectedBlock); break;
            case FURNACE: drawFurnace(g, cx, cy, colors, names, blockCount, playerInv, selectedBlock); break;
            case SAW: drawSaw(g, cx, cy, colors, names, blockCount, playerInv, selectedBlock); break;
            case CRAFTING: drawCrafting(g, cx, cy, colors, names, blockCount, playerInv, selectedBlock); break;
        }
    }

    // ---- CHEST ----
    private static void drawChest(Graphics2D g, int cx, int cy, int[] colors, String[] names, int blockCount, int[] playerInv, int selectedBlock) {
        int rows = 3, cols = 9;
        int slotSize = 50, gap = 4;
        int totalW = cols * (slotSize + gap) - gap;
        int totalH = rows * (slotSize + gap) - gap;
        int startX = cx - totalW / 2;
        int startY = cy - totalH / 2 - 20;

        // Title
        g.setFont(new Font("Monospaced", Font.BOLD, 14));
        g.setColor(new Color(255, 215, 0));
        g.drawString("Chest", cx - 20, startY - 8);

        // Slots
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int idx = row * cols + col;
                int sx = startX + col * (slotSize + gap);
                int sy = startY + row * (slotSize + gap);
                int val = chestInv[idx];
                drawSlot(g, sx, sy, slotSize, slotSize, val, colors, names);
                clickBounds.add(new int[]{sx, sy, slotSize, slotSize, 0, idx}); // type=0 chest slot
            }
        }

        // Close button
        int btnY = startY + totalH + 12;
        int btnW = 80, btnH = 24;
        g.setColor(new Color(100, 40, 20));
        g.fillRect(cx - btnW/2, btnY, btnW, btnH);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Monospaced", Font.BOLD, 12));
        g.drawString("Close", cx - 18, btnY + 17);
        clickBounds.add(new int[]{cx - btnW/2, btnY, btnW, btnH, -1, 0}); // -1 = close
    }

    // ---- FURNACE ----
    private static void drawFurnace(Graphics2D g, int cx, int cy, int[] colors, String[] names, int blockCount, int[] playerInv, int selectedBlock) {
        int slotSize = 54;
        int totalW = slotSize * 3 + 20 + 30;
        int startX = cx - totalW / 2;
        int startY = cy - slotSize / 2 - 30;

        g.setFont(new Font("Monospaced", Font.BOLD, 14));
        g.setColor(new Color(200, 120, 60));
        g.drawString("Furnace", cx - 30, startY - 8);

        // Input
        g.setColor(new Color(200, 200, 220));
        g.setFont(new Font("Monospaced", Font.PLAIN, 10));
        g.drawString("Input", startX + 10, startY + slotSize + 14);
        drawSlot(g, startX, startY, slotSize, slotSize, furInput, colors, names);
        clickBounds.add(new int[]{startX, startY, slotSize, slotSize, 10, 0});

        // Arrow
        g.setFont(new Font("Monospaced", Font.BOLD, 24));
        g.setColor(new Color(200, 200, 100));
        g.drawString("→", startX + slotSize + 8, startY + slotSize / 2 + 8);

        // Fuel
        int fy = startY + slotSize + 30;
        g.setColor(new Color(200, 200, 220));
        g.setFont(new Font("Monospaced", Font.PLAIN, 10));
        String fuelLabel = "Fuel" + (furFuelCount > 0 ? " x" + furFuelCount : "");
        g.drawString(fuelLabel, startX + 10, fy + 14);
        drawSlot(g, startX, fy, slotSize, slotSize, furFuel, colors, names);
        clickBounds.add(new int[]{startX, fy, slotSize, slotSize, 11, 0});

        // Arrow 2
        g.setFont(new Font("Monospaced", Font.BOLD, 24));
        g.setColor(new Color(200, 200, 100));
        g.drawString("→", startX + slotSize + 8, fy + slotSize / 2 + 8);

        // Output
        int ox = startX + slotSize + 20 + slotSize;
        int oy = startY;
        g.setColor(new Color(200, 200, 220));
        g.setFont(new Font("Monospaced", Font.PLAIN, 10));
        g.drawString("Output", ox + 5, startY + slotSize + 14);
        drawSlot(g, ox, oy, slotSize, slotSize, furOutput, colors, names);
        clickBounds.add(new int[]{ox, oy, slotSize, slotSize, 12, 0});

        // Status
        String status;
        if (furOutput > 0 && furFuel > 0) {
            status = "Click output to smelt 1x " + (furOutput <= blockCount ? names[furOutput] : "?");
            g.setColor(new Color(100, 220, 100));
        } else if (furInput == 0) {
            status = "Add smeltable item to Input slot from inventory";
            g.setColor(new Color(200, 200, 200));
        } else if (furFuel == 0) {
            status = "Add fuel (Wood or Coal) to Fuel slot";
            g.setColor(new Color(200, 200, 200));
        } else {
            status = "";
        }
        g.setFont(new Font("Monospaced", Font.PLAIN, 11));
        g.drawString(status, cx - g.getFontMetrics().stringWidth(status) / 2, startY + 80);

        // Player inventory draw
        drawPlayerInv(g, 10, 120, colors, names, blockCount, playerInv, selectedBlock);

        // Close button
        int btnY = startY + slotSize + 30 + slotSize + 30;
        int btnW = 80, btnH = 24;
        g.setColor(new Color(80, 40, 20));
        g.fillRect(cx - btnW/2, btnY, btnW, btnH);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Monospaced", Font.BOLD, 12));
        g.drawString("Close", cx - 18, btnY + 17);
        clickBounds.add(new int[]{cx - btnW/2, btnY, btnW, btnH, -1, 0});
    }

    // ---- SAW ----
    private static void drawSaw(Graphics2D g, int cx, int cy, int[] colors, String[] names, int blockCount, int[] playerInv, int selectedBlock) {
        int slotSize = 54;
        int totalW = slotSize * 2 + 30;
        int startX = cx - totalW / 2;
        int startY = cy - slotSize / 2 - 20;

        g.setFont(new Font("Monospaced", Font.BOLD, 14));
        g.setColor(new Color(180, 140, 80));
        g.drawString("Saw", cx - 15, startY - 8);

        // Input
        g.setColor(new Color(200, 200, 220));
        g.setFont(new Font("Monospaced", Font.PLAIN, 10));
        g.drawString("Input", startX + 10, startY + slotSize + 14);
        drawSlot(g, startX, startY, slotSize, slotSize, sawInput, colors, names);
        clickBounds.add(new int[]{startX, startY, slotSize, slotSize, 20, 0});

        // Arrow
        g.setFont(new Font("Monospaced", Font.BOLD, 24));
        g.setColor(new Color(200, 200, 100));
        g.drawString("→", startX + slotSize + 8, startY + slotSize / 2 + 8);

        // Output
        int ox = startX + slotSize + 30;
        g.setColor(new Color(200, 200, 220));
        g.setFont(new Font("Monospaced", Font.PLAIN, 10));
        g.drawString("Output", ox + 5, startY + slotSize + 14);
        drawSlot(g, ox, startY, slotSize, slotSize, sawOutput, colors, names);
        clickBounds.add(new int[]{ox, startY, slotSize, slotSize, 21, 0});

        // Status
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

        drawPlayerInv(g, 10, 120, colors, names, blockCount, playerInv, selectedBlock);

        // Close
        int btnY = startY + slotSize + 80;
        int btnW = 80, btnH = 24;
        g.setColor(new Color(80, 50, 20));
        g.fillRect(cx - btnW/2, btnY, btnW, btnH);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Monospaced", Font.BOLD, 12));
        g.drawString("Close", cx - 18, btnY + 17);
        clickBounds.add(new int[]{cx - btnW/2, btnY, btnW, btnH, -1, 0});
    }

    // ---- CRAFTING ----
    private static void drawCrafting(Graphics2D g, int cx, int cy, int[] colors, String[] names, int blockCount, int[] playerInv, int selectedBlock) {
        int slotSize = 44, gap = 4;
        int totalW = GRID * (slotSize + gap) - gap;
        int totalH = GRID * (slotSize + gap) - gap;
        int startX = cx - totalW / 2 - 40;
        int startY = cy - totalH / 2 - 10;

        g.setFont(new Font("Monospaced", Font.BOLD, 14));
        g.setColor(new Color(180, 180, 255));
        g.drawString("Crafting", startX, startY - 8);

        // Grid
        for (int row = 0; row < GRID; row++) {
            for (int col = 0; col < GRID; col++) {
                int idx = row * GRID + col;
                int sx = startX + col * (slotSize + gap);
                int sy = startY + row * (slotSize + gap);
                drawSlot(g, sx, sy, slotSize, slotSize, craftSlots[idx], colors, names);
                clickBounds.add(new int[]{sx, sy, slotSize, slotSize, 30, idx});
            }
        }

        // Arrow to result
        int arrowX = startX + totalW + 10;
        g.setFont(new Font("Monospaced", Font.BOLD, 24));
        g.setColor(new Color(200, 200, 100));
        g.drawString("→", arrowX, cy + 8);

        // Result
        int rsX = arrowX + 30;
        int rsY = cy - slotSize / 2;
        drawSlot(g, rsX, rsY, slotSize + 10, slotSize + 10, craftResultType, colors, names);
        clickBounds.add(new int[]{rsX, rsY, slotSize + 10, slotSize + 10, 31, 0});

        // Status
        if (craftRecipeIdx >= 0) {
            String s = "Craft: " + craftResultCount + "x " + (craftResultType <= blockCount ? names[craftResultType] : "?");
            g.setFont(new Font("Monospaced", Font.PLAIN, 11));
            g.setColor(new Color(100, 220, 100));
            g.drawString(s, cx - g.getFontMetrics().stringWidth(s) / 2, startY + totalH + 28);
        }

        drawPlayerInv(g, 10, 120, colors, names, blockCount, playerInv, selectedBlock);

        // Close
        int btnY = startY + totalH + 40;
        int btnW = 80, btnH = 24;
        g.setColor(new Color(50, 50, 100));
        g.fillRect(cx - btnW/2, btnY, btnW, btnH);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Monospaced", Font.BOLD, 12));
        g.drawString("Close", cx - 18, btnY + 17);
        clickBounds.add(new int[]{cx - btnW/2, btnY, btnW, btnH, -1, 0});
    }

    // ---- HELPERS ----
    private static void drawSlot(Graphics2D g, int x, int y, int w, int h, int type, int[] colors, String[] names) {
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

    private static void drawPlayerInv(Graphics2D g, int x, int y, int[] colors, String[] names, int blockCount, int[] playerInv, int selectedBlock) {
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

    // ---- CLICK HANDLER ----
    public static boolean handleClick(int mx, int my) {
        if (state == State.NONE) return false;

        for (int[] b : clickBounds) {
            int bx = b[0], by = b[1], bw = b[2], bh = b[3], action = b[4], data = b[5];
            if (mx >= bx && mx <= bx + bw && my >= by && my <= by + bh) {
                if (action == -1) { close(); return true; }
                handleAction(action, data);
                return true;
            }
        }
        close();
        return true;
    }

    private static void handleAction(int action, int data) {
        switch (state) {
            case CHEST: handleChestClick(action, data); break;
            case FURNACE: handleFurnaceClick(action, data); break;
            case SAW: handleSawClick(action, data); break;
            case CRAFTING: handleCraftingClick(action, data); break;
        }
    }

    // ---- CHEST CLICK ----
    private static void handleChestClick(int action, int slot) {
        int stack = chestInv[slot];
        if (stack > 0) {
            if (cachedPlayerInv[stack] < 64) {
                cachedPlayerInv[stack]++;
                chestInv[slot] = 0;
            }
        } else if (cachedSelectedBlock > 0 && cachedPlayerInv[cachedSelectedBlock] > 0) {
            cachedPlayerInv[cachedSelectedBlock]--;
            chestInv[slot] = cachedSelectedBlock;
        }
    }

    // ---- FURNACE CLICK ----
    private static void handleFurnaceClick(int action, int data) {
        if (action == 10) {
            if (furInput > 0) { cachedPlayerInv[furInput]++; furInput = 0; furOutput = 0; }
        } else if (action == 11) {
            if (furFuel > 0 && furFuelCount > 0) {
                cachedPlayerInv[furFuel]++;
                furFuelCount--;
                if (furFuelCount <= 0) { furFuel = 0; furFuelCount = 0; }
            }
        } else if (action == 12) {
            if (furOutput > 0 && furFuel > 0 && furFuelCount > 0) {
                int cost = (furInput == 3) ? 2 : 1;
                if (furFuelCount >= cost) {
                    cachedPlayerInv[furOutput]++;
                    furFuelCount -= cost;
                    if (furFuelCount <= 0) { furFuel = 0; furFuelCount = 0; }
                    furInput = 0;
                    furOutput = 0;
                }
            }
        } else if (action == 40) {
            int type = data;
            if (cachedPlayerInv[type] <= 0) return;
            if (furInput == 0 && isSmeltable(type)) {
                cachedPlayerInv[type]--;
                furInput = type;
                furOutput = getSmeltResult(type);
            } else if (type == 4 || type == 8) {
                if (furFuel == 0) {
                    cachedPlayerInv[type]--;
                    furFuel = type;
                    furFuelCount = 1;
                } else if (furFuel == type && furFuelCount < 64) {
                    cachedPlayerInv[type]--;
                    furFuelCount++;
                }
            }
        }
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

    // ---- SAW CLICK ----
    private static void handleSawClick(int action, int data) {
        if (action == 20) {
            if (sawInput > 0) { cachedPlayerInv[sawInput]++; sawInput = 0; sawOutput = 0; }
        } else if (action == 21) {
            if (sawOutput > 0) {
                int outType = (sawInput == 12) ? 15 : 12;
                int count = (sawInput == 12) ? 4 : 6;
                cachedPlayerInv[outType] += count;
                sawInput = 0;
                sawOutput = 0;
            }
        } else if (action == 40) {
            int type = data;
            if (type == 4 && cachedPlayerInv[type] > 0) {
                cachedPlayerInv[type]--;
                sawInput = 4;
                sawOutput = 12;
            } else if (type == 12 && cachedPlayerInv[type] > 0) {
                cachedPlayerInv[type]--;
                sawInput = 12;
                sawOutput = 15;
            }
        }
    }

    // ---- CRAFTING CLICK ----
    private static void handleCraftingClick(int action, int slot) {
        if (action == 30) {
            if (craftSlots[slot] > 0) {
                cachedPlayerInv[craftSlots[slot]]++;
                craftSlots[slot] = 0;
                updateCraftResult();
            }
        } else if (action == 31) {
            if (craftRecipeIdx >= 0) {
                craftItem(cachedPlayerInv);
            }
        } else if (action == 40) {
            int type = slot;
            if (cachedPlayerInv[type] <= 0) return;
            for (int i = 0; i < GRID * GRID; i++) {
                if (craftSlots[i] == 0) {
                    cachedPlayerInv[type]--;
                    craftSlots[i] = type;
                    updateCraftResult();
                    return;
                }
            }
        }
    }

    private static void updateCraftResult() {
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

    private static boolean matchesRecipe(int[] pattern) {
        for (int i = 0; i < GRID * GRID; i++) {
            if (pattern[i] == 0 && craftSlots[i] != 0) return false;
            if (pattern[i] != 0 && craftSlots[i] != pattern[i]) return false;
        }
        return true;
    }

    private static void craftItem(int[] playerInv) {
        java.util.List<int[][]> recipes = CraftingTable.getRecipes();
        if (craftRecipeIdx < 0 || craftRecipeIdx >= recipes.size()) return;
        int[] pattern = recipes.get(craftRecipeIdx)[0];
        for (int i = 0; i < GRID * GRID; i++) {
            if (pattern[i] != 0 && playerInv[pattern[i]] <= 0) return;
        }
        for (int i = 0; i < GRID * GRID; i++) {
            if (pattern[i] != 0) {
                playerInv[pattern[i]]--;
                craftSlots[i] = 0;
            }
        }
        playerInv[craftResultType] += craftResultCount;
        updateCraftResult();
    }

    // Expose recipes for GuiSystem access
    public static java.util.List<int[][]> getRecipes() { return CraftingTable.getRecipes(); }

    // ---- PORTABLE CRAFTING (2x1 in inventory screen) ----
    private static int[] portableSlots = new int[3]; // [in1, in2, out]

    public static void drawPortableCraft(Graphics2D g, int winW, int winH, int[] playerInv, BlockRegistry reg, int selectedBlock) {
        cachedPlayerInv = playerInv;
        cachedSelectedBlock = selectedBlock;
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

        // Input 1
        drawSlot(g, startX, startY, slotSize, slotSize, portableSlots[0], colors, names);
        clickBounds.add(new int[]{startX, startY, slotSize, slotSize, 50, 0});
        // Input 2
        drawSlot(g, startX + slotSize + gap, startY, slotSize, slotSize, portableSlots[1], colors, names);
        clickBounds.add(new int[]{startX + slotSize + gap, startY, slotSize, slotSize, 50, 1});
        // Arrow
        g.setFont(new Font("Monospaced", Font.BOLD, 22));
        g.setColor(new Color(200, 200, 100));
        g.drawString("→", startX + slotSize * 2 + gap + 4, startY + slotSize / 2 + 6);
        // Output
        int ox = startX + slotSize * 2 + gap + 20;
        drawSlot(g, ox, startY, slotSize, slotSize, portableSlots[2], colors, names);
        clickBounds.add(new int[]{ox, startY, slotSize, slotSize, 51, 0});
        // Result name
        if (portableSlots[2] > 0 && portableSlots[2] <= n) {
            g.setFont(new Font("Monospaced", Font.PLAIN, 9));
            g.setColor(new Color(100, 220, 100));
            g.drawString(names[portableSlots[2]], ox + 4, startY + slotSize + 14);
        }
    }

    public static void handlePortableCraftSlotClick(int slot, int selectedBlock) {
        if (slot == 0 || slot == 1) {
            int type = portableSlots[slot];
            if (type > 0) {
                cachedPlayerInv[type]++;
                portableSlots[slot] = 0;
                updatePortableResult();
            } else if (selectedBlock > 0 && cachedPlayerInv[selectedBlock] > 0) {
                cachedPlayerInv[selectedBlock]--;
                portableSlots[slot] = selectedBlock;
                updatePortableResult();
            }
        } else if (slot == 2) { // Output clicked → craft
            if (portableSlots[2] > 0) {
                int result = portableSlots[2];
                if (result == 12 || result == 15 || result == 8) {
                    cachedPlayerInv[result] += (result == 12) ? 4 : (result == 15) ? 2 : 3;
                    portableSlots[0] = 0;
                    portableSlots[1] = 0;
                    portableSlots[2] = 0;
                } else if (result == 10) {
                    cachedPlayerInv[10]++;
                    portableSlots[0] = 0;
                    portableSlots[1] = 0;
                    portableSlots[2] = 0;
                }
            }
        }
    }

    public static void addToPortableCraft(int type) {
        if (portableSlots[0] == 0) {
            portableSlots[0] = type;
        } else if (portableSlots[1] == 0) {
            portableSlots[1] = type;
        }
        updatePortableResult();
    }

    private static void updatePortableResult() {
        int a = portableSlots[0], b = portableSlots[1];
        // Wood + Wood → 4 Planks
        if (a == 4 && b == 4) { portableSlots[2] = 12; return; }
        // Planks + Planks → 2 Stick
        if (a == 12 && b == 12) { portableSlots[2] = 15; return; }
        // Stone + Stick → Axe
        if (a == 3 && b == 15) { portableSlots[2] = 10; return; }
        // Coal + Coal → 3 Coal
        if (a == 8 && b == 8) { portableSlots[2] = 8; return; }
        // Single Wood → 4 Planks
        if (a == 4 && b == 0) { portableSlots[2] = 12; return; }
        // Single Planks → 2 Stick
        if (a == 12 && b == 0) { portableSlots[2] = 15; return; }
        portableSlots[2] = 0;
    }
}
