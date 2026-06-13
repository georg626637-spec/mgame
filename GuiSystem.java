import java.awt.*;
import java.util.List;
import java.util.Map;

public class GuiSystem {
    private static GuiScreen currentScreen;
    public static List<int[]> clickBounds = new java.util.ArrayList<>();
    public static int mouseX, mouseY;

    private static final Map<String, int[]> chestInventories = new java.util.HashMap<>();

    // ---- Chest persistence ----
    public static void fillChest(int cx, int cy, int cz, int[] items) {
        int[] inv = chestInventories.computeIfAbsent(cx + "," + cy + "," + cz, k -> new int[27]);
        for (int i = 0; i < items.length && i < inv.length; i++) {
            if (items[i] > 0) inv[i] = items[i];
        }
    }

    public static void clearChest(int cx, int cy, int cz) {
        chestInventories.remove(cx + "," + cy + "," + cz);
    }

    // ---- Screen management ----
    public static boolean isOpen() { return currentScreen != null; }

    public static void close() {
        if (currentScreen != null) currentScreen.onClose();
        currentScreen = null;
        clickBounds.clear();
    }

    public static void openChest(int cx, int cy, int cz, int[] playerInv, int selectedBlock) {
        String key = cx + "," + cy + "," + cz;
        int[] inv = chestInventories.computeIfAbsent(key, k -> new int[27]);
        currentScreen = new ChestScreen(key, inv);
    }

    public static void openFurnace(int[] playerInv) {
        currentScreen = new FurnaceScreen();
    }

    public static void openSaw(int[] playerInv) {
        currentScreen = new SawScreen();
    }

    public static void openCrafting(int[] playerInv) {
        currentScreen = new CraftingScreen();
    }

    // ---- Draw ----
    public static void draw(Graphics2D g, int winW, int winH, BlockRegistry reg, int[] playerInv, int selectedBlock) {
        if (currentScreen == null) return;
        clickBounds.clear();
        currentScreen.draw(g, winW, winH, reg, playerInv, selectedBlock);
        clickBounds.addAll(currentScreen.getClickBounds());
    }

    // ---- Click ----
    public static boolean handleClick(int mx, int my, int selectedBlock) {
        if (currentScreen == null) return false;
        boolean stayOpen = currentScreen.handleClick(mx, my, selectedBlock);
        if (!stayOpen) {
            currentScreen.onClose();
            currentScreen = null;
            clickBounds.clear();
        }
        return true;
    }

    // ---- Recipe passthrough (for ModContext) ----
    public static void addRecipe(int[] pattern, int resultType, int resultCount) {
        CraftingTable.addRecipe(pattern, resultType, resultCount);
    }

    public static java.util.List<int[][]> getRecipes() {
        return CraftingTable.getRecipes();
    }
}
