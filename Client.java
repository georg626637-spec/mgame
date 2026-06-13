import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Client {
    private Socket socket;
    private PrintWriter out;
    private volatile boolean running;
    private final ConcurrentLinkedQueue<int[]> blockQueue = new ConcurrentLinkedQueue<>();
    private volatile boolean needsUpload;
    public final Map<Integer, float[]> otherPlayers = new ConcurrentHashMap<>();
    public int myId = -1;
    private long lastPosSend = 0;

    public boolean connect(String host, int port) {
        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            running = true;

            new Thread(() -> {
                try {
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    boolean sync = false;
                    String line;
                    while ((line = in.readLine()) != null) {
                        if (line.startsWith("ID ")) {
                            myId = Integer.parseInt(line.substring(3).trim());
                        } else if (line.startsWith("WORLD ")) {
                            sync = true;
                        } else if (line.equals("END")) {
                            sync = false;
                            needsUpload = true;
                        } else if (line.startsWith("B ")) {
                            String[] p = line.split(" ");
                            int x = Integer.parseInt(p[1]);
                            int y = Integer.parseInt(p[2]);
                            int z = Integer.parseInt(p[3]);
                            int type = Integer.parseInt(p[4]);
                            blockQueue.add(new int[]{x, y, z, type});
                        } else if (line.startsWith("P ")) {
                            String[] p = line.split(" ");
                            int id = Integer.parseInt(p[1]);
                            float px = Float.parseFloat(p[2]);
                            float py = Float.parseFloat(p[3]);
                            float pz = Float.parseFloat(p[4]);
                            otherPlayers.put(id, new float[]{px, py, pz});
                        } else if (line.startsWith("R ")) {
                            int id = Integer.parseInt(line.substring(2).trim());
                            otherPlayers.remove(id);
                        }
                    }
                } catch (IOException e) {
                    // disconnected
                }
                running = false;
            }).start();

            return true;
        } catch (IOException e) {
            System.err.println("[Client] " + e.getMessage());
            return false;
        }
    }

    public void sendBlock(int x, int y, int z, int type) {
        if (out != null)
            out.println("B " + x + " " + y + " " + z + " " + type);
    }

    public void sendPosition(float x, float y, float z) {
        long now = System.currentTimeMillis();
        if (now - lastPosSend < 50) return;
        lastPosSend = now;
        if (out != null)
            out.println("P " + x + " " + y + " " + z);
    }

    public void processQueue(World world) {
        int[] change;
        while ((change = blockQueue.poll()) != null)
            world.set(change[0], change[1], change[2], (byte) change[3]);
        if (needsUpload) {
            needsUpload = false;
            world.upload();
        }
    }

    public void disconnect() {
        running = false;
        try { if (socket != null) socket.close(); } catch (IOException e) {}
    }

    public boolean isConnected() { return running; }
}
