import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.*;

public class Server {
    private ServerSocket serverSocket;
    private int nextId = 1;
    private final Map<Integer, PrintWriter> clients = new ConcurrentHashMap<>();
    private final Map<Integer, float[]> playerPositions = new ConcurrentHashMap<>();
    private volatile boolean running;
    private World world;
    public final ConcurrentLinkedQueue<int[]> incoming = new ConcurrentLinkedQueue<>();

    public void start(int port, World w) {
        world = w;
        running = true;
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                System.out.println("[Server] Listening on port " + port);
                while (running) {
                    Socket s = serverSocket.accept();
                    int id = nextId++;
                    new Thread(() -> handleClient(s, id)).start();
                }
            } catch (IOException e) {
                if (running) System.err.println("[Server] " + e.getMessage());
            }
        }).start();
    }

    private void handleClient(Socket s, int clientId) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
            PrintWriter out = new PrintWriter(s.getOutputStream(), true);
            clients.put(clientId, out);

            out.println("ID " + clientId);

            ByteBuffer buf = world.buffer.duplicate();
            buf.rewind();
            out.println("WORLD " + World.WX + " " + World.WY + " " + World.WZ);
            int total = World.WX * World.WY * World.WZ;
            for (int i = 0; i < total; i++) {
                int type = buf.get() & 0xFF;
                if (type != 0) {
                    int z = i / (World.WX * World.WY);
                    int y = (i % (World.WX * World.WY)) / World.WX;
                    int x = i % World.WX;
                    out.println("B " + x + " " + y + " " + z + " " + type);
                }
            }
            out.println("END");

            // Send existing player positions to the new client
            for (Map.Entry<Integer, float[]> e : playerPositions.entrySet()) {
                if (e.getKey() != clientId) {
                    float[] pos = e.getValue();
                    out.println("P " + e.getKey() + " " + pos[0] + " " + pos[1] + " " + pos[2]);
                }
            }

            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("B ")) {
                    String[] p = line.split(" ");
                    int x = Integer.parseInt(p[1]);
                    int y = Integer.parseInt(p[2]);
                    int z = Integer.parseInt(p[3]);
                    int type = Integer.parseInt(p[4]);
                    incoming.add(new int[]{x, y, z, type});
                    for (Map.Entry<Integer, PrintWriter> e : clients.entrySet())
                        if (e.getKey() != clientId) e.getValue().println(line);
                } else if (line.startsWith("P ")) {
                    String[] p = line.split(" ");
                    float px = Float.parseFloat(p[1]);
                    float py = Float.parseFloat(p[2]);
                    float pz = Float.parseFloat(p[3]);
                    playerPositions.put(clientId, new float[]{px, py, pz});
                    for (Map.Entry<Integer, PrintWriter> e : clients.entrySet())
                        if (e.getKey() != clientId) e.getValue().println("P " + clientId + " " + px + " " + py + " " + pz);
                }
            }
        } catch (IOException e) {
            // disconnected
        } finally {
            clients.remove(clientId);
            playerPositions.remove(clientId);
            try { s.close(); } catch (IOException e) {}
            for (Map.Entry<Integer, PrintWriter> e : clients.entrySet())
                e.getValue().println("R " + clientId);
        }
    }

    public void broadcastBlock(int x, int y, int z, int type) {
        String msg = "B " + x + " " + y + " " + z + " " + type;
        for (PrintWriter pw : clients.values())
            pw.println(msg);
    }

    public void broadcastPosition(int id, float x, float y, float z) {
        playerPositions.put(id, new float[]{x, y, z});
        String msg = "P " + id + " " + x + " " + y + " " + z;
        for (PrintWriter pw : clients.values())
            pw.println(msg);
    }

    public void stop() {
        running = false;
        try { if (serverSocket != null) serverSocket.close(); } catch (IOException e) {}
    }

    public boolean isRunning() { return running; }
}
