import java.util.*;
import java.io.*;
import java.nio.file.*;

public class ModContext {
    private final Game engine;
    private final List<Mod> mods = new ArrayList<>();
    final List<String[]> jarEntries = new ArrayList<>();

    public ModContext(Game engine) {
        this.engine = engine;
    }

    void addModEntry(String name, String status) {
        jarEntries.add(new String[]{name, status});
    }

    void addMod(Mod mod) {
        mods.add(mod);
    }

    public int addBlock(String name, int argbColor) {
        return engine.addBlockFromMod(name, argbColor);
    }

    public int addBlock(String name, int[] texPixels) {
        return engine.addBlockFromMod(name, texPixels);
    }

    public void givePlayerBlock(int type, int count) {
        if (type > 0 && type < engine.player.inventory.length)
            engine.player.inventory[type] += count;
    }

    public void addCraftingRecipe(int[] pattern, int resultType, int resultCount) {
        CraftingTable.addRecipe(pattern, resultType, resultCount);
    }

    public void setBlockShape(int type, BlockShape shape) {
        engine.setBlockShape(type, shape);
    }

    public int loadBlockFromTxt(String filePath) {
        return loadBlockFromTxt(filePath, null);
    }

    public int loadBlockFromTxt(String filePath, String blockName) {
        try {
            java.util.List<String> lines = Files.readAllLines(Paths.get(filePath));

            String name = blockName;
            if (name == null) {
                for (String ln : lines) {
                    ln = ln.trim();
                    if (ln.startsWith("# Voxel Block:") || ln.startsWith("# Block:")) {
                        name = ln.substring(ln.indexOf(':') + 1).trim();
                        break;
                    }
                }
                if (name == null) {
                    name = new File(filePath).getName();
                    if (name.contains(".")) name = name.substring(0, name.lastIndexOf('.'));
                }
            }

            int[] colors = new int[4096];
            int idx = 0;
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                for (String p : line.split("\\s+")) {
                    if (idx >= 4096) break;
                    colors[idx++] = (int) Long.parseLong(p.replace("0x", "").replace("0X", ""), 16);
                }
            }

            if (idx == 0) {
                engine.addMessage("[Mod] No voxel data in " + filePath);
                return 0;
            }

            java.util.Map<Integer, Integer> freq = new java.util.HashMap<>();
            BlockShape shape = new BlockShape();
            int[] texPixels = new int[256];
            int bestColor = 0xFF888888;
            for (int i = 0; i < idx; i++) {
                int x = i % 16, z = (i / 16) % 16, y = i / 256;
                if (colors[i] != 0) {
                    shape.set(x, y, z);
                    freq.merge(colors[i], 1, Integer::sum);
                    int ti = z * 16 + x;
                    texPixels[ti] = colors[i];
                }
            }

            int maxFreq = 0;
            for (java.util.Map.Entry<Integer, Integer> e : freq.entrySet()) {
                if (e.getValue() > maxFreq) {
                    maxFreq = e.getValue();
                    bestColor = e.getKey();
                }
            }

            for (int i = 0; i < 256; i++) {
                if (texPixels[i] == 0) texPixels[i] = bestColor;
            }

            int type = engine.addBlockFromMod(name, texPixels);
            if (type > 0) {
                engine.setBlockShape(type, shape);
                engine.player.inventory[type] += 64;
                engine.addMessage("[Mod] Loaded block '" + name + "' (type " + type + ") from " + filePath);
            }
            return type;
        } catch (Exception e) {
            engine.addMessage("[Mod] Failed to load " + filePath + ": " + e.getMessage());
            return 0;
        }
    }

    public void log(String msg) {
        engine.addMessage("[Mod] " + msg);
    }

    public List<Mod> getMods() {
        return Collections.unmodifiableList(mods);
    }

    public List<String[]> getJarEntries() {
        return Collections.unmodifiableList(jarEntries);
    }
}
