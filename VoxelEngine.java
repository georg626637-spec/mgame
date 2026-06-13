import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

public class VoxelEngine extends JFrame implements Runnable, KeyListener, MouseMotionListener, MouseListener, MouseWheelListener {
    private static final int RENDER_W = 400;
    private static final int RENDER_H = 225;
    private static final double FOV = Math.PI / 2.5;
    private static final double MAX_DIST = 64;

    private Canvas canvas;
    private BufferedImage img;
    private int[] pixels;

    private VoxelWorld world;
    private double px = 8, py = 14, pz = 8;
    private double yaw = 0, pitch = -0.15;
    private boolean[] keys = new boolean[256];
    private boolean mouseGrabbed = false;
    private Robot robot;
    private Thread thread;
    private volatile boolean running = true;
    private ChatSystem chatSystem = new ChatSystem();

    private static final int[] VOXEL_COLORS = {
        0x000000, 0x8B5E3C, 0x5A9E4A, 0x8A8A8A, 0x6B4423, 0x2D5A1E, 0x3366AA, 0xE8D5A0
    };
    private static final double[] FACE_LIGHT = {0.6, 0.6, 1.0, 0.35, 0.6, 0.6};
    private static final int[] SKY_TOP = {40, 80, 160};
    private static final int[] SKY_BOT = {180, 200, 230};

    public VoxelEngine() {
        super("Voxel Engine");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true);
        setSize(960, 600);
        setLocationRelativeTo(null);

        canvas = new Canvas();
        canvas.setBackground(Color.BLACK);
        canvas.setFocusable(false);
        add(canvas, BorderLayout.CENTER);

        addKeyListener(this);
        canvas.addMouseMotionListener(this);
        canvas.addMouseListener(this);
        canvas.addMouseWheelListener(this);

        try { robot = new Robot(); } catch (AWTException e) { e.printStackTrace(); }

        world = new VoxelWorld();
        world.ensureSurroundingChunks((int) px, (int) pz);

        setVisible(true);
        canvas.createBufferStrategy(2);
        img = new BufferedImage(RENDER_W, RENDER_H, BufferedImage.TYPE_INT_RGB);
        pixels = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();
        setFocusable(true);
        requestFocusInWindow();
    }

    public void start() {
        thread = new Thread(this);
        thread.start();
    }

    public void run() {
        long last = System.nanoTime();
        double ns = 1_000_000_000.0 / 60;
        double delta = 0;
        long timer = System.currentTimeMillis();
        int frames = 0;

        while (running) {
            long now = System.nanoTime();
            delta += (now - last) / ns;
            last = now;
            while (delta >= 1) { update(); delta--; }
            render();
            frames++;
            if (System.currentTimeMillis() - timer > 1000) {
                timer += 1000;
                setTitle("Voxel Engine - " + frames + " FPS - Click=grab ESC=release WASD=move SPACE/SHIFT=up/down");
                frames = 0;
            }
        }
    }

    private void update() {
        double speed = 5.0 / 60;
        double f = 0, s = 0, v = 0;
        if (keys[KeyEvent.VK_W]) f += speed;
        if (keys[KeyEvent.VK_S]) f -= speed;
        if (keys[KeyEvent.VK_A]) s -= speed;
        if (keys[KeyEvent.VK_D]) s += speed;
        if (keys[KeyEvent.VK_SPACE]) v += speed;
        if (keys[KeyEvent.VK_SHIFT]) v -= speed;
        double sin = Math.sin(yaw), cos = Math.cos(yaw);
        px += f * sin + s * cos;
        pz += f * cos - s * sin;
        py += v;
        if (py < 1) py = 1;
        world.ensureSurroundingChunks((int) px, (int) pz);
    }

    private void render() {
        BufferStrategy bs = canvas.getBufferStrategy();
        if (bs == null) return;

        double fovTan = Math.tan(FOV / 2);
        double aspect = (double) RENDER_W / RENDER_H;
        double fwdX = Math.sin(yaw) * Math.cos(pitch);
        double fwdY = Math.sin(pitch);
        double fwdZ = Math.cos(yaw) * Math.cos(pitch);
        double rightX = Math.cos(yaw);
        double rightZ = -Math.sin(yaw);
        double upX = -Math.sin(yaw) * Math.sin(pitch);
        double upY = Math.cos(pitch);
        double upZ = -Math.cos(yaw) * Math.sin(pitch);

        int cw = canvas.getWidth(), ch = canvas.getHeight();

        for (int sy = 0; sy < RENDER_H; sy++) {
            double ndcY = 1 - (sy + 0.5) / RENDER_H * 2;
            for (int sx = 0; sx < RENDER_W; sx++) {
                double ndcX = (sx + 0.5) / RENDER_W * 2 - 1;
                double rx = ndcX * fovTan * aspect;
                double ry = ndcY * fovTan;
                double dx = fwdX + rightX * rx + upX * ry;
                double dy = fwdY + upY * ry;
                double dz = fwdZ + rightZ * rx + upZ * ry;
                double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
                dx /= len; dy /= len; dz /= len;

                int mx = (int) Math.floor(px);
                int my = (int) Math.floor(py);
                int mz = (int) Math.floor(pz);
                int stepX = dx > 0 ? 1 : -1;
                int stepY = dy > 0 ? 1 : -1;
                int stepZ = dz > 0 ? 1 : -1;
                double tMaxX = dx != 0 ? (mx + (dx > 0 ? 1 : 0) - px) / dx : Double.MAX_VALUE;
                double tMaxY = dy != 0 ? (my + (dy > 0 ? 1 : 0) - py) / dy : Double.MAX_VALUE;
                double tMaxZ = dz != 0 ? (mz + (dz > 0 ? 1 : 0) - pz) / dz : Double.MAX_VALUE;
                double tDeltaX = dx != 0 ? Math.abs(1.0 / dx) : Double.MAX_VALUE;
                double tDeltaY = dy != 0 ? Math.abs(1.0 / dy) : Double.MAX_VALUE;
                double tDeltaZ = dz != 0 ? Math.abs(1.0 / dz) : Double.MAX_VALUE;

                int face = -1, hitType = -1;
                double dist = 0;
                int maxSteps = (int) MAX_DIST * 3;

                for (int i = 0; i < maxSteps; i++) {
                    VoxelType v = world.getVoxel(mx, my, mz);
                    if (v != VoxelType.AIR) { hitType = v.ordinal(); break; }
                    if (tMaxX < tMaxY) {
                        if (tMaxX < tMaxZ) { mx += stepX; dist = tMaxX; tMaxX += tDeltaX; face = stepX > 0 ? 0 : 1; }
                        else { mz += stepZ; dist = tMaxZ; tMaxZ += tDeltaZ; face = stepZ > 0 ? 4 : 5; }
                    } else {
                        if (tMaxY < tMaxZ) { my += stepY; dist = tMaxY; tMaxY += tDeltaY; face = stepY > 0 ? 2 : 3; }
                        else { mz += stepZ; dist = tMaxZ; tMaxZ += tDeltaZ; face = stepZ > 0 ? 4 : 5; }
                    }
                }

                int color;
                if (hitType >= 0) {
                    int bc = VOXEL_COLORS[hitType];
                    double light = FACE_LIGHT[face];
                    double fog = dist / MAX_DIST;
                    fog = fog * fog;
                    light *= (1 - fog);
                    int r = (int) (((bc >> 16) & 0xFF) * light);
                    int g = (int) (((bc >> 8) & 0xFF) * light);
                    int b = (int) ((bc & 0xFF) * light);
                    double fr = 180, fg = 200, fb = 230;
                    r = (int) (r + fog * (fr - r));
                    g = (int) (g + fog * (fg - g));
                    b = (int) (b + fog * (fb - b));
                    if (r > 255) r = 255; if (g > 255) g = 255; if (b > 255) b = 255;
                    color = (r << 16) | (g << 8) | b;
                } else {
                    double t = Math.max(0, Math.min(1, (dy + 1) / 2));
                    int r = (int) (SKY_TOP[0] + (SKY_BOT[0] - SKY_TOP[0]) * t);
                    int g = (int) (SKY_TOP[1] + (SKY_BOT[1] - SKY_TOP[1]) * t);
                    int b = (int) (SKY_TOP[2] + (SKY_BOT[2] - SKY_TOP[2]) * t);
                    color = (r << 16) | (g << 8) | b;
                }
                pixels[sy * RENDER_W + sx] = color;
            }
        }

        Graphics2D g = (Graphics2D) bs.getDrawGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, cw, ch);
        double scale = Math.min((double) cw / RENDER_W, (double) ch / RENDER_H);
        int dw = (int) (RENDER_W * scale);
        int dh = (int) (RENDER_H * scale);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(img, (cw - dw) / 2, (ch - dh) / 2, dw, dh, null);
        g.setColor(Color.WHITE);
        g.drawLine(cw / 2 - 6, ch / 2, cw / 2 + 6, ch / 2);
        g.drawLine(cw / 2, ch / 2 - 6, cw / 2, ch / 2 + 6);
        chatSystem.render(g, cw, ch);
        g.dispose();
        bs.show();
    }

    public void keyPressed(KeyEvent e) {
        if (chatSystem.isActive()) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER) chatSystem.onEnter();
            else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) chatSystem.toggle();
            else if (e.getKeyCode() == KeyEvent.VK_UP) chatSystem.scrollUp();
            else if (e.getKeyCode() == KeyEvent.VK_DOWN) chatSystem.scrollDown();
            else if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) chatSystem.onBackspace();
        } else {
            if (e.getKeyCode() == KeyEvent.VK_T) chatSystem.toggle();
            else if (e.getKeyCode() < 256) keys[e.getKeyCode()] = true;
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) releaseMouse();
        }
    }

    public void keyReleased(KeyEvent e) {
        if (!chatSystem.isActive() && e.getKeyCode() < 256)
            keys[e.getKeyCode()] = false;
    }

    public void keyTyped(KeyEvent e) {
        if (chatSystem.isActive()) chatSystem.onChar(e.getKeyChar());
    }

    public void mouseWheelMoved(MouseWheelEvent e) {
        if (chatSystem.isActive()) {
            int notches = e.getWheelRotation();
            if (notches < 0) chatSystem.scrollUp();
            else chatSystem.scrollDown();
        }
    }

    public void mouseClicked(MouseEvent e) {
        if (!mouseGrabbed) grabMouse();
    }

    public void mousePressed(MouseEvent e) {}
    public void mouseReleased(MouseEvent e) {}
    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}

    public void mouseMoved(MouseEvent e) {
        if (mouseGrabbed && robot != null && !chatSystem.isActive()) {
            int cx = getX() + getWidth() / 2;
            int cy = getY() + getHeight() / 2 + getInsets().top;
            int dx = e.getXOnScreen() - cx;
            int dy = e.getYOnScreen() - cy;
            if (dx != 0 || dy != 0) {
                yaw += dx * 0.003;
                pitch -= dy * 0.003;
                pitch = Math.max(-Math.PI / 2 + 0.05, Math.min(Math.PI / 2 - 0.05, pitch));
                robot.mouseMove(cx, cy);
            }
        }
    }

    public void mouseDragged(MouseEvent e) { mouseMoved(e); }

    private void grabMouse() {
        if (!mouseGrabbed && robot != null) {
            mouseGrabbed = true;
            BufferedImage blank = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            Cursor cursor = Toolkit.getDefaultToolkit().createCustomCursor(blank, new Point(0, 0), "blank");
            setCursor(cursor);
            Point loc = getLocationOnScreen();
            robot.mouseMove(loc.x + getWidth() / 2, loc.y + getHeight() / 2 + getInsets().top);
        }
    }

    private void releaseMouse() {
        if (mouseGrabbed) { mouseGrabbed = false; setCursor(Cursor.getDefaultCursor()); }
    }

    public static void main(String[] args) {
        VoxelEngine engine = new VoxelEngine();
        engine.start();
    }
}
