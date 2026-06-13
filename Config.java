import java.io.*;
import java.nio.file.*;

public class Config {
    public int winW = 1280, winH = 720;
    public boolean fullscreen;
    public float maxDist = 64f;

    public Config() {
        Path p = Paths.get("voxelengine.cfg");
        if (!Files.exists(p)) p = Paths.get(System.getProperty("user.dir"), "voxelengine.cfg");
        if (!Files.exists(p)) return;
        try {
            for (String line : Files.readAllLines(p)) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split("=", 2);
                if (parts.length != 2) continue;
                String key = parts[0].trim().toLowerCase(), val = parts[1].trim();
                switch (key) {
                    case "width": winW = Integer.parseInt(val); break;
                    case "height": winH = Integer.parseInt(val); break;
                    case "fullscreen": fullscreen = Boolean.parseBoolean(val); break;
                    case "maxdist": maxDist = Float.parseFloat(val); break;
                }
            }
        } catch (IOException e) {
            System.err.println("[Config] " + e.getMessage());
        }
    }
}
