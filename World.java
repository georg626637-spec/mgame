import java.nio.*;
import java.util.*;
import org.lwjgl.BufferUtils;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;

public class World {
    public static final int WX = 128, WY = 32, WZ = 128;
    public ByteBuffer buffer;
    public int tex3D;
    public BlockRegistry reg;

    public void createBuffer() {
        buffer = BufferUtils.createByteBuffer(WX * WY * WZ);
    }

    public byte get(int x, int y, int z) {
        if (x < 0 || x >= WX || y < 0 || y >= WY || z < 0 || z >= WZ) return 0;
        return buffer.get(x + y * WX + z * WX * WY);
    }

    public byte getUnsafe(int x, int y, int z) {
        return buffer.get(x + y * WX + z * WX * WY);
    }

    public void set(int x, int y, int z, byte t) {
        if (x < 0 || x >= WX || y < 0 || y >= WY || z < 0 || z >= WZ) return;
        buffer.put(x + y * WX + z * WX * WY, t);
    }

    public void upload() {
        buffer.rewind();
        glBindTexture(GL_TEXTURE_3D, tex3D);
        glTexSubImage3D(GL_TEXTURE_3D, 0, 0, 0, 0, WX, WY, WZ, GL_RED_INTEGER, GL_UNSIGNED_BYTE, buffer);
    }

    public int[] raycast(double px, double py, double pz, double yaw, double pitch, double maxDist) {
        double dx = Math.sin(yaw) * Math.cos(pitch);
        double dy = Math.sin(pitch);
        double dz = Math.cos(yaw) * Math.cos(pitch);
        int mx = (int) Math.floor(px), my = (int) Math.floor(py), mz = (int) Math.floor(pz);
        int sx = dx > 0 ? 1 : -1, sy = dy > 0 ? 1 : -1, sz = dz > 0 ? 1 : -1;
        double tMaxX = dx != 0 ? (mx + (dx > 0 ? 1 : 0) - px) / dx : 1e30;
        double tMaxY = dy != 0 ? (my + (dy > 0 ? 1 : 0) - py) / dy : 1e30;
        double tMaxZ = dz != 0 ? (mz + (dz > 0 ? 1 : 0) - pz) / dz : 1e30;
        double tdX = dx != 0 ? Math.abs(1.0 / dx) : 1e30;
        double tdY = dy != 0 ? Math.abs(1.0 / dy) : 1e30;
        double tdZ = dz != 0 ? Math.abs(1.0 / dz) : 1e30;
        double dist = 0;
        int face = -1, maxSteps = (int) (maxDist * 3);
        for (int i = 0; i < maxSteps; i++) {
            if (get(mx, my, mz) != 0) {
                if (reg != null) {
                    int type = get(mx, my, mz) & 0xFF;
                    if (type > 0 && type <= reg.shapes.size()) {
                        BlockShape shape = reg.shapes.get(type - 1);
                        double ex = px + dx * dist, ey = py + dy * dist, ez = pz + dz * dist;
                        int spx = (int)((ex - mx) * 16);
                        int spy = (int)((ey - my) * 16);
                        int spz = (int)((ez - mz) * 16);
                        spx = Math.max(0, Math.min(15, spx));
                        spy = Math.max(0, Math.min(15, spy));
                        spz = Math.max(0, Math.min(15, spz));
                        if (shape.get(spx, spy, spz) != 0)
                            return new int[]{mx, my, mz, face};
                    } else {
                        return new int[]{mx, my, mz, face};
                    }
                } else {
                    return new int[]{mx, my, mz, face};
                }
            }
            if (tMaxX < tMaxY) {
                if (tMaxX < tMaxZ) { dist = tMaxX; mx += sx; tMaxX += tdX; face = sx > 0 ? 1 : 0; }
                else { dist = tMaxZ; mz += sz; tMaxZ += tdZ; face = sz > 0 ? 5 : 4; }
            } else {
                if (tMaxY < tMaxZ) { dist = tMaxY; my += sy; tMaxY += tdY; face = sy > 0 ? 3 : 2; }
                else { dist = tMaxZ; mz += sz; tMaxZ += tdZ; face = sz > 0 ? 5 : 4; }
            }
        }
        return null;
    }

    public boolean playerCollides(double px, double py, double pz) {
        int minX = (int)Math.floor(px - 0.3);
        int maxX = (int)Math.floor(px + 0.3);
        int minY = (int)Math.floor(py - 2.7);
        int maxY = (int)Math.floor(py + 0.3);
        int minZ = (int)Math.floor(pz - 0.3);
        int maxZ = (int)Math.floor(pz + 0.3);
        for (int x = minX; x <= maxX; x++)
            for (int y = minY; y <= maxY; y++)
                for (int z = minZ; z <= maxZ; z++)
                    if (get(x, y, z) != 0) return true;
        return false;
    }

    public double resolvePlayerAxis(double from, double to, double fixX, double fixY, double fixZ, int axis) {
        if (from == to) return from;
        double tx = axis == 0 ? to : fixX;
        double ty = axis == 1 ? to : fixY;
        double tz = axis == 2 ? to : fixZ;
        if (!playerCollides(tx, ty, tz)) return to;
        double lo = Math.min(from, to);
        double hi = Math.max(from, to);
        boolean pos = to > from;
        for (int i = 0; i < 12; i++) {
            double mid = (lo + hi) / 2;
            double mx = axis == 0 ? mid : fixX;
            double my = axis == 1 ? mid : fixY;
            double mz = axis == 2 ? mid : fixZ;
            if (playerCollides(mx, my, mz)) {
                if (pos) hi = mid; else lo = mid;
            } else {
                if (pos) lo = mid; else hi = mid;
            }
        }
        return pos ? lo : hi;
    }

    public boolean entityCollides(double px, double py, double pz, double hw, double h) {
        int minX = (int)Math.floor(px - hw);
        int maxX = (int)Math.floor(px + hw);
        int minY = (int)Math.floor(py);
        int maxY = (int)Math.floor(py + h);
        int minZ = (int)Math.floor(pz - hw);
        int maxZ = (int)Math.floor(pz + hw);
        for (int x = minX; x <= maxX; x++)
            for (int y = minY; y <= maxY; y++)
                for (int z = minZ; z <= maxZ; z++)
                    if (get(x, y, z) != 0) return true;
        return false;
    }

    public double resolveEntityAxis(double from, double to, double fixY, double fixZ, double hw, double h, int axis, double fixX) {
        if (from == to) return from;
        double tx = axis == 0 ? to : fixX;
        double ty = axis == 1 ? to : fixY;
        double tz = axis == 2 ? to : fixZ;
        if (!entityCollides(tx, ty, tz, hw, h)) return to;
        double lo = Math.min(from, to);
        double hi = Math.max(from, to);
        boolean pos = to > from;
        for (int i = 0; i < 12; i++) {
            double mid = (lo + hi) / 2;
            double mx = axis == 0 ? mid : fixX;
            double my = axis == 1 ? mid : fixY;
            double mz = axis == 2 ? mid : fixZ;
            if (entityCollides(mx, my, mz, hw, h)) {
                if (pos) hi = mid; else lo = mid;
            } else {
                if (pos) lo = mid; else hi = mid;
            }
        }
        return pos ? lo : hi;
    }

    public void generate(long seed) {
        createBuffer();
        java.util.Random rng = new java.util.Random(seed);
        for (int x = 0; x < WX; x++) {
            for (int z = 0; z < WZ; z++) {
                double h = 0, amp = 1, freq = 0.018;
                for (int oct = 0; oct < 5; oct++) {
                    double nx = x * freq + oct * 1.7, nz = z * freq + oct * 2.3;
                    h += (Math.sin(nx) * Math.cos(nz) * 1.0 +
                          Math.sin(x * freq * 1.7 + z * freq * 0.9 + oct * 3.1) * 0.6) * amp;
                    amp *= 0.45;
                    freq *= 2.3;
                }
                int height = (int)(h * 5 + 12);
                if (height < 1) height = 1;
                if (height >= WY) height = WY - 1;
                for (int y = 0; y < WY; y++) {
                    byte type = 0;
                    if (y < height - 3) type = 3;
                    else if (y < height) type = 1;
                    else if (y == height) type = height < 6 ? (byte)7 : (byte)2;
                    else if (y < 4) type = 6;
                    set(x, y, z, type);
                }
                if (height > 10 && rng.nextDouble() < 0.07) {
                    int trunk = 4 + rng.nextInt(2);
                    for (int ty = 1; ty <= trunk; ty++)
                        if (height + ty < WY) set(x, height + ty, z, (byte)4);
                    for (int lx = -2; lx <= 2; lx++)
                        for (int lz = -2; lz <= 2; lz++)
                            for (int ly = 0; ly <= 2; ly++) {
                                if (lx * lx + lz * lz > 4) continue;
                                if (ly == 2 && (Math.abs(lx) == 2 || Math.abs(lz) == 2)) continue;
                                if (Math.abs(lx) == 2 && Math.abs(lz) == 2) continue;
                                set(x + lx, height + trunk + ly, z + lz, (byte)5);
                            }
                }
                if (height > 8) {
                    for (int y = 5; y < height - 2; y++) {
                        double cn = Math.sin(x * 0.15 + y * 0.12) * Math.cos(z * 0.14 + y * 0.08) * 0.5
                                  + Math.sin(x * 0.1 + z * 0.18 + y * 0.15) * 0.3;
                        if (cn > 0.38) set(x, y, z, (byte)0);
                    }
                }
            }
        }
        for (int i = 0; i < 25; i++) {
            int ox = rng.nextInt(WX), oz = rng.nextInt(WZ), oy = 5 + rng.nextInt(15);
            if (get(ox, oy, oz) == 3) {
                for (int rx = -1; rx <= 1; rx++)
                    for (int ry = -1; ry <= 1; ry++)
                        for (int rz = -1; rz <= 1; rz++) {
                            if (rng.nextDouble() < 0.5) continue;
                            int dx = ox + rx, dy = oy + ry, dz = oz + rz;
                            if (dx >= 0 && dx < WX && dy >= 0 && dy < WY && dz >= 0 && dz < WZ)
                                if (get(dx, dy, dz) == 3) set(dx, dy, dz, (byte)8);
                        }
            }
        }
        for (int i = 0; i < 12; i++) {
            int ox = rng.nextInt(WX), oz = rng.nextInt(WZ), oy = 5 + rng.nextInt(18);
            if (get(ox, oy, oz) == 3) {
                for (int rx = -1; rx <= 1; rx++)
                    for (int ry = -1; ry <= 1; ry++)
                        for (int rz = -1; rz <= 1; rz++) {
                            if (rng.nextDouble() < 0.6) continue;
                            int dx = ox + rx, dy = oy + ry, dz = oz + rz;
                            if (dx >= 0 && dx < WX && dy >= 0 && dy < WY && dz >= 0 && dz < WZ)
                                if (get(dx, dy, dz) == 3) set(dx, dy, dz, (byte)9);
                        }
            }
        }
    }

    public void fellTree(int sx, int sy, int sz, java.util.Random rng, List<float[]> itemDrops) {
        java.util.ArrayDeque<int[]> stack = new java.util.ArrayDeque<>();
        java.util.HashSet<Long> visited = new java.util.HashSet<>();
        stack.push(new int[]{sx, sy, sz});
        visited.add((long)sx << 42 | (long)sy << 21 | sz);
        int count = 0;
        while (!stack.isEmpty() && count < 40) {
            int[] p = stack.pop();
            int x = p[0], y = p[1], z = p[2];
            int idx = x + y * WX + z * WX * WY;
            int type = buffer.get(idx) & 0xFF;
            if (type != 4 && type != 5) continue;
            buffer.put(idx, (byte) 0);
            itemDrops.add(new float[]{x + 0.5f, y + 0.5f, z + 0.5f, type, 0f});
            count++;
            int[][] dirs = {{0,1,0},{0,-1,0},{1,0,0},{-1,0,0},{0,0,1},{0,0,-1}};
            for (int[] d : dirs) {
                int nx = x + d[0], ny = y + d[1], nz = z + d[2];
                if (nx < 0 || nx >= WX || ny < 0 || ny >= WY || nz < 0 || nz >= WZ) continue;
                long key = (long)nx << 42 | (long)ny << 21 | nz;
                if (visited.add(key)) stack.push(new int[]{nx, ny, nz});
            }
        }
        upload();
    }
}
