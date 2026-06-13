import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

public class ModLoader {
    private static final File MODS_DIR = new File("mods");

    public static void loadMods(ModContext ctx) {
        System.out.println("[ModLoader] Scanning mods/...");
        if (!MODS_DIR.isDirectory()) {
            MODS_DIR.mkdirs();
            System.out.println("[ModLoader] Created mods/ folder");
            return;
        }
        File[] entries = MODS_DIR.listFiles();
        if (entries == null || entries.length == 0) {
            System.out.println("[ModLoader] No mods found in mods/");
            return;
        }
        for (File entry : entries) {
            String name = entry.getName();
            if (name.endsWith(".jar")) {
                loadJar(entry, ctx);
            } else if (entry.isDirectory()) {
                loadDir(entry, ctx);
            }
        }
    }

    private static void loadJar(File jar, ModContext ctx) {
        System.out.println("[ModLoader] Found JAR: " + jar.getName());
        try {
            URL[] urls = {jar.toURI().toURL()};
            try (URLClassLoader loader = new URLClassLoader(urls, ModLoader.class.getClassLoader())) {
                ServiceLoader<Mod> sl = ServiceLoader.load(Mod.class, loader);
                int count = 0;
                for (Mod mod : sl) {
                    ctx.addMod(mod);
                    mod.init(ctx);
                    count++;
                }
                if (count == 0) {
                    ctx.addModEntry(jar.getName(), "no Mod impl (check META-INF/services/Mod)");
                } else {
                    ctx.addModEntry(jar.getName(), "loaded (" + count + ")");
                }
            }
        } catch (Throwable e) {
            ctx.addModEntry(jar.getName(), "error: " + e.getClass().getSimpleName());
            System.err.println("[ModLoader] Failed to load " + jar.getName() + ": " + e);
            e.printStackTrace();
        }
    }

    private static void loadDir(File dir, ModContext ctx) {
        System.out.println("[ModLoader] Found mod directory: " + dir.getName());
        File servicesFile = new File(dir, "META-INF/services/Mod");
        if (!servicesFile.isFile()) {
            ctx.addModEntry(dir.getName(), "missing META-INF/services/Mod");
            return;
        }
        try {
            URL[] urls = {dir.toURI().toURL()};
            try (URLClassLoader loader = new URLClassLoader(urls, ModLoader.class.getClassLoader())) {
                ServiceLoader<Mod> sl = ServiceLoader.load(Mod.class, loader);
                int count = 0;
                for (Mod mod : sl) {
                    ctx.addMod(mod);
                    mod.init(ctx);
                    count++;
                }
                if (count == 0) {
                    ctx.addModEntry(dir.getName(), "no Mod impl found");
                } else {
                    ctx.addModEntry(dir.getName(), "loaded (" + count + ")");
                }
            }
        } catch (Throwable e) {
            ctx.addModEntry(dir.getName(), "error: " + e.getClass().getSimpleName());
            System.err.println("[ModLoader] Failed to load " + dir.getName() + ": " + e);
            e.printStackTrace();
        }
    }
}
