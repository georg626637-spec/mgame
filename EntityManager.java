import java.util.*;

public class EntityManager {
    public final List<float[]> itemDrops = new ArrayList<>();
    public final List<float[]> npcs = new ArrayList<>();

    public void spawnNPCs(World world, java.util.Random rng) {
        npcs.clear();
        for (int i = 0; i < 5; i++) {
            int nx = 10 + rng.nextInt(World.WX - 20);
            int nz = 10 + rng.nextInt(World.WZ - 20);
            int ny = 0;
            for (int y = World.WY - 1; y >= 0; y--) {
                if (world.get(nx, y, nz) != 0) { ny = y + 1; break; }
            }
            if (ny > 0 && ny < World.WY - 2) {
                double angle = rng.nextDouble() * Math.PI * 2;
                npcs.add(new float[]{nx + 0.5f, ny, nz + 0.5f, 0, 1, 0.3f, 2.0f, (float)Math.cos(angle), (float)Math.sin(angle)});
            }
        }
    }

    public void update(double dt, World world) {
        for (float[] n : npcs) {
            double npx = n[0], npy = n[1], npz = n[2];
            double nvy = n[3];
            boolean onG = n[4] > 0.5;
            double hw = n[5], h = n[6];
            double wx = n[7], wz = n[8];
            double len = Math.sqrt(wx * wx + wz * wz);
            if (len > 0) { wx /= len; wz /= len; }

            nvy -= 9.8 * dt;
            double dx = wx * 1.2 * dt;
            double dz = wz * 1.2 * dt;

            npx = world.resolveEntityAxis(npx, npx + dx, npy, npz, hw, h, 0, npx);
            if (Math.abs(npx - n[0]) < 0.0001) { wx = (Math.random() - 0.5) * 2; wz = (Math.random() - 0.5) * 2; }
            npz = world.resolveEntityAxis(npz, npz + dz, npy, npz, hw, h, 2, npx);
            if (Math.abs(npz - n[2]) < 0.0001) { wx = (Math.random() - 0.5) * 2; wz = (Math.random() - 0.5) * 2; }

            double oldY = npy;
            npy += nvy * dt;
            if (world.entityCollides(npx, npy, npz, hw, h)) {
                npy = world.resolveEntityAxis(oldY, npy, npy, npz, hw, h, 1, npx);
                if (nvy < 0) nvy = 0;
                onG = true;
            } else {
                onG = false;
            }

            if (npy < 1) { npy = 1; nvy = 0; onG = true; }

            n[0] = (float)npx; n[1] = (float)npy; n[2] = (float)npz;
            n[3] = (float)nvy; n[4] = onG ? 1 : 0;
            n[7] = (float)wx; n[8] = (float)wz;
        }

        for (int i = itemDrops.size() - 1; i >= 0; i--) {
            float[] d = itemDrops.get(i);
            d[4] -= 9.8f * (float)dt;
            d[1] += d[4] * (float)dt;
            int ix = (int) Math.floor(d[0]);
            int iz = (int) Math.floor(d[2]);
            int iy = (int) Math.floor(d[1]);
            if (iy >= 0 && iy < World.WY && world.get(ix, iy, iz) != 0) {
                d[1] = iy + 1;
                d[4] = 0;
            }
            if (d[1] < 0) { d[1] = 0; d[4] = 0; }
        }
    }

    public void dropItem(float x, float y, float z, int type) {
        itemDrops.add(new float[]{x, y, z, type, 0f});
    }

    public void placeSaw(World world) {
        int sawX = 8, sawZ = 10;
        for (int y = World.WY - 1; y >= 0; y--) {
            if (world.get(sawX, y, sawZ) != 0) {
                if (y + 1 < World.WY) world.set(sawX, y + 1, sawZ, (byte)11);
                break;
            }
        }
    }

    public void placeFurnace(World world) {
        int furX = 8, furZ = 12;
        for (int y = World.WY - 1; y >= 0; y--) {
            if (world.get(furX, y, furZ) != 0) {
                if (y + 1 < World.WY) world.set(furX, y + 1, furZ, (byte)13);
                break;
            }
        }
    }

    public void placeChest(World world) {
        int cx = 7, cz = 11;
        for (int y = World.WY - 1; y >= 0; y--) {
            if (world.get(cx, y, cz) != 0) {
                if (y + 1 < World.WY) world.set(cx, y + 1, cz, (byte)16);
                GuiSystem.fillChest(cx, y + 1, cz, new int[]{
                    12,12,12,12,12,12,12,12,12,
                    4,4,4, 3,3,3, 8,8,8, 9,9,9,
                    15,15,15, 7,7,7
                });
                break;
            }
        }
    }

    public void placeCraftingTable(World world) {
        int cx = 6, cz = 11;
        for (int y = World.WY - 1; y >= 0; y--) {
            if (world.get(cx, y, cz) != 0) {
                if (y + 1 < World.WY) world.set(cx, y + 1, cz, (byte)17);
                break;
            }
        }
    }
}
