import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import javax.swing.SwingUtilities;
import java.util.List;
import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Game {
    long window;
    int winW, winH;

    ChatSystem chat = new ChatSystem();
    Camera camera = new Camera();
    BlockRegistry reg = new BlockRegistry();
    World world = new World();
    Player player = new Player();
    EntityManager entities = new EntityManager();
    Renderer renderer = new Renderer();
    InputHandler input = new InputHandler();

    double lastMouseX, lastMouseY;
    int[] craftBtnBounds;
    long seed = 42;
    java.util.Random rng = new java.util.Random(seed);
    ModContext modCtx;
    Server server;
    Client client;

    public void run() {
        initGLFW();
        GL.createCapabilities();
        glViewport(0, 0, winW, winH);
        renderer.setWindowSize(winW, winH);

        world.generate(seed);
        world.upload();

        entities.spawnNPCs(world, rng);
        entities.placeSaw(world);
        entities.placeFurnace(world);

        reg.initDefaults();
        world.reg = reg;

        chat.setTerrainCallback(this::regenerateWorld);
        chat.setCreateTextureCallback(this::openPixelEditor);
        chat.setAddBlockCallback(this::addBlockFromPending);
        chat.setModeCallback(m -> {
            player.gameMode = m;
            if (m == 1) player.vy = 0;
            addMessage("Mode: " + (m == 1 ? "Survival" : "Creative"));
        });
        chat.setCraftCallback(this::openCraftingTable);
        chat.setBlockShapeCallback(this::openShapeEditor);
        chat.setBlockEditCallback(this::openBlockEditor);
        chat.setModCallback(this::listMods);
        chat.setServerCallback(arg -> {
            int port = 25565;
            if (!arg.isEmpty()) {
                try { port = Integer.parseInt(arg); }
                catch (NumberFormatException e) { addMessage("Usage: /server [port]"); return; }
            }
            addMessage("Starting server on port " + port + "...");
            server = new Server();
            server.start(port, world);
        });
        chat.setClientCallback(addr -> {
            int colon = addr.lastIndexOf(':');
            if (colon < 0) { addMessage("Usage: /client <ip:port>"); return; }
            String host = addr.substring(0, colon);
            int port;
            try { port = Integer.parseInt(addr.substring(colon + 1)); }
            catch (NumberFormatException e) { addMessage("Invalid port"); return; }
            addMessage("Connecting to " + host + ":" + port + "...");
            client = new Client();
            if (client.connect(host, port)) {
                addMessage("Connected to " + host + ":" + port);
            } else {
                addMessage("Connection failed");
                client = null;
            }
        });
        chat.setSeedCallback(s -> {
            seed = s;
            rng = new java.util.Random(seed);
            regenerateWorld();
            addMessage("Seed: " + seed);
        });

        renderer.setupShaders();
        renderer.setupBuffers(reg, world);

        modCtx = new ModContext(this);
        ModLoader.loadMods(modCtx);

        double lastFrame = glfwGetTime(), lastTitle = lastFrame;
        int fps = 0;

        while (!glfwWindowShouldClose(window)) {
            double now = glfwGetTime();
            double dt = now - lastFrame;
            lastFrame = now;

            update(dt);
            renderer.render(world, player, camera, entities, chat, reg, input.selectorOpen, seed,
            client != null ? client.otherPlayers : null, client != null ? client.myId : -1);
            craftBtnBounds = renderer.craftBtnBounds;
            glfwSwapBuffers(window);
            glfwPollEvents();
            fps++;
            if (now - lastTitle >= 1.0) {
                glfwSetWindowTitle(window, "Voxel Engine GPU - " + fps + " FPS");
                fps = 0;
                lastTitle = now;
            }
        }
        cleanup();
    }

    private void initGLFW() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) throw new RuntimeException("glfwInit failed");
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

        long monitor = glfwGetPrimaryMonitor();
        GLFWVidMode vidmode = glfwGetVideoMode(monitor);
        winW = vidmode.width();
        winH = vidmode.height();
        window = glfwCreateWindow(winW, winH, "Voxel Engine GPU", 0, NULL);
        if (window == NULL) throw new RuntimeException("glfwCreateWindow failed");

        input.setupCallbacks(this, window);

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwSetWindowSizeCallback(window, (w, nw, nh) -> {
            winW = nw;
            winH = nh;
            glViewport(0, 0, winW, winH);
            renderer.setWindowSize(winW, winH);
            renderer.resizeOverlay(winW, winH);
        });
        glfwShowWindow(window);
    }

    private void update(double dt) {
        if (pendingShapeName != null) {
            reg.addFromPending(pendingShapeName);
            int type = reg.size();
            reg.setBlockShape(type, pendingShape);
            addMessage("Block '" + pendingShapeName + "' created with custom shape (type " + type + ").");
            pendingShapeName = null;
            pendingShape = null;
        }
        if (client != null && client.isConnected())
            client.processQueue(world);
        if (server != null) {
            int[] change;
            while ((change = server.incoming.poll()) != null) {
                world.set(change[0], change[1], change[2], (byte) change[3]);
                world.upload();
            }
        }
        player.update(dt, world, input.keys, camera.yaw);
        entities.update(dt, world);
        player.pickupItems(entities.itemDrops);

        if (client != null && client.isConnected())
            client.sendPosition((float)player.px, (float)player.py, (float)player.pz);
        if (server != null && server.isRunning())
            server.broadcastPosition(0, (float)player.px, (float)player.py, (float)player.pz);
    }

    private void sendBlockChange(int x, int y, int z, int type) {
        if (server != null) server.broadcastBlock(x, y, z, type);
        if (client != null && client.isConnected()) client.sendBlock(x, y, z, type);
    }

    void onLeftClick() {
        int[] hit = player.getHitBlock(world, camera.yaw, camera.pitch);
        if (hit != null) {
            int fx = hit[0], fy = hit[1], fz = hit[2];
            int oldType = world.get(fx, fy, fz) & 0xFF;
            if (oldType > 0) {
                player.destroyBlock(world, camera.yaw, camera.pitch, entities.itemDrops);
                sendBlockChange(fx, fy, fz, 0);
            }
        }
    }

    void onRightClick() {
        int type = player.getBlockInSight(world, camera.yaw, camera.pitch);
        if (type == 11) { openSawTable(); return; }
        if (type == 13) { openFurnaceTable(); return; }
        int[] hit = player.getHitBlock(world, camera.yaw, camera.pitch);
        if (hit != null) {
            int fx = hit[0] + (hit[3] == 0 ? 1 : hit[3] == 1 ? -1 : 0);
            int fy = hit[1] + (hit[3] == 2 ? 1 : hit[3] == 3 ? -1 : 0);
            int fz = hit[2] + (hit[3] == 4 ? 1 : hit[3] == 5 ? -1 : 0);
            if (player.placeBlock(world, camera.yaw, camera.pitch) && world.get(fx, fy, fz) != 0)
                sendBlockChange(fx, fy, fz, player.selectedBlock);
        }
    }

    void addMessage(String msg) {
        try {
            java.lang.reflect.Method m = chat.getClass().getDeclaredMethod("addMessage", String.class);
            m.setAccessible(true);
            m.invoke(chat, msg);
        } catch (Exception ignored) {}
    }

    private void regenerateWorld() {
        world.buffer.rewind();
        rng = new java.util.Random(seed);
        world.generate(seed);
        world.upload();
        player.resetPosition();
        camera.yaw = 0;
        camera.pitch = -0.15;
    }

    private void openPixelEditor(String name) {
        BlockTexture existing = reg.pendingTextures.get(name);
        int[] initial = existing != null ? existing.pixels : new int[BlockTexture.SIZE * BlockTexture.SIZE];
        final PixelEditor[] editorRef = new PixelEditor[1];
        editorRef[0] = new PixelEditor(name, initial, () -> {
            reg.pendingTextures.put(name, new BlockTexture(name, editorRef[0].getPixels()));
            addMessage("Texture '" + name + "' created. Use /blockadd " + name + " to add as block.");
        });
    }

    private String pendingShapeName;
    private BlockShape pendingShape;

    private void openShapeEditor(String name) {
        if (!reg.pendingTextures.containsKey(name))
            reg.pendingTextures.put(name, new BlockTexture(name, 0xFF888888));
        javax.swing.SwingUtilities.invokeLater(() -> new ShapeEditor(name, shape -> {
            pendingShapeName = name;
            pendingShape = shape;
        }));
    }

    private void openBlockEditor(String name) {
        BlockTexture existing = reg.pendingTextures.get(name);
        int[] initial = existing != null ? existing.pixels : new int[BlockTexture.SIZE * BlockTexture.SIZE];
        final BlockEditor[] editorRef = new BlockEditor[1];
        javax.swing.SwingUtilities.invokeLater(() -> {
            editorRef[0] = new BlockEditor(name, initial, () -> {
                reg.pendingTextures.put(name, new BlockTexture(name, editorRef[0].getTexPixels()));
                BlockShape shape = new BlockShape();
                int[][][] sc = editorRef[0].getShapeColors();
                for (int x = 0; x < 16; x++)
                    for (int y = 0; y < 16; y++)
                        for (int z = 0; z < 16; z++)
                            if (sc[x][y][z] != 0) shape.set(x, y, z);
                pendingShapeName = name;
                pendingShape = shape;
            });
        });
    }

    void openCraftingTable() {
        int n = reg.size();
        int[] colors = new int[n + 1];
        String[] names = new String[n + 1];
        for (int i = 1; i <= n; i++) {
            colors[i] = reg.color(i);
            names[i] = reg.name(i);
        }
        SwingUtilities.invokeLater(() -> new CraftingTable(player.inventory, colors, names, n).setVisible(true));
    }

    private void openSawTable() {
        int n = reg.size();
        int[] colors = new int[n + 1];
        String[] names = new String[n + 1];
        for (int i = 1; i <= n; i++) {
            colors[i] = reg.color(i);
            names[i] = reg.name(i);
        }
        SwingUtilities.invokeLater(() -> new SawTable(player.inventory, colors, names, n).setVisible(true));
    }

    private void openFurnaceTable() {
        int n = reg.size();
        int[] colors = new int[n + 1];
        String[] names = new String[n + 1];
        for (int i = 1; i <= n; i++) {
            colors[i] = reg.color(i);
            names[i] = reg.name(i);
        }
        SwingUtilities.invokeLater(() -> new FurnaceTable(player.inventory, colors, names, n).setVisible(true));
    }

    private void addBlockFromPending(String name) {
        reg.addFromPending(name);
        addMessage("Block '" + name + "' added (type " + reg.size() + ").");
    }

    int addBlockFromMod(String name, int argbColor) {
        int type = reg.addFromMod(name, argbColor);
        if (type > 0)
            addMessage("[Mod] Block '" + name + "' added (type " + type + ").");
        return type;
    }

    int addBlockFromMod(String name, int[] texPixels) {
        int type = reg.addFromMod(name, texPixels);
        if (type > 0)
            addMessage("[Mod] Block '" + name + "' added (type " + type + ").");
        return type;
    }

    void setBlockShape(int type, BlockShape shape) {
        reg.setBlockShape(type, shape);
    }

    private void listMods() {
        if (modCtx == null) { addMessage("Mods: (none)"); return; }
        List<Mod> mods = modCtx.getMods();
        List<String[]> jars = modCtx.getJarEntries();
        if (jars.isEmpty() && mods.isEmpty()) {
            addMessage("No mods found in mods/ folder");
            return;
        }
        if (!jars.isEmpty()) {
            addMessage("Mods folder (" + jars.size() + " JARs):");
            for (String[] j : jars) {
                String status = j[1];
                if (status.startsWith("loaded")) {
                    addMessage("  \u2713 " + j[0] + " - " + status);
                } else {
                    addMessage("  \u2717 " + j[0] + " - " + status);
                }
            }
        }
        if (!mods.isEmpty()) {
            addMessage("Loaded classes:");
            for (Mod m : mods) addMessage("  - " + m.getClass().getSimpleName());
        }
    }

    private void cleanup() {
        glfwDestroyWindow(window);
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    public static void main(String[] args) {
        new Game().run();
    }
}
