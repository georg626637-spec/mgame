import java.util.*;

public class Player {
    public double px = 8, py = 14 + EYE_OFFSET, pz = 8;
    public double vy;
    public boolean onGround;
    public int gameMode = 1;
    public int selectedBlock = 1;
    public int[] inventory = new int[BlockRegistry.MAX_BLOCK_TYPES];
    public static final double HEIGHT = 3.0;
    public static final double EYE_OFFSET = 2.7;
    private static final double HALF = 0.3;

    public double getReach() { return gameMode == 1 ? 5.0 : 64.0; }

    public boolean isPlaceable(int type) {
        return type != 10 && type != 14 && type != 15;
    }

    public int[] getHitBlock(World world, double yaw, double pitch) {
        return world.raycast(px, py, pz, yaw, pitch, getReach());
    }

    public void update(double dt, World world, boolean[] keys, double yaw) {
        double spd = 5.0 * dt;
        double ff = 0, ss = 0;
        if (keys[0x57]) ff += spd;
        if (keys[0x53]) ff -= spd;
        if (keys[0x41]) ss -= spd;
        if (keys[0x44]) ss += spd;
        double len = Math.sqrt(ff * ff + ss * ss);
        if (len > spd) { ff = ff / len * spd; ss = ss / len * spd; }

        double cosYaw = Math.cos(yaw), sinYaw = Math.sin(yaw);

        if (gameMode == 2) {
            px += ff * sinYaw + ss * cosYaw;
            pz += ff * cosYaw - ss * sinYaw;
            double v = 0;
            if (keys[32]) v += spd;
            if (keys[340]) v -= spd;
            py += v;
            if (py < EYE_OFFSET + 1) py = EYE_OFFSET + 1;
        } else {
            double mx = ff * sinYaw + ss * cosYaw;
            double mz = ff * cosYaw - ss * sinYaw;

            px = world.resolvePlayerAxis(px, px + mx, px, py, pz, 0);
            pz = world.resolvePlayerAxis(pz, pz + mz, px, py, pz, 2);

            vy -= 9.8 * dt;
            if (keys[0x20] && onGround) vy = 4.5;

            double oldPy = py;
            py += vy * dt;

            if (world.playerCollides(px, py, pz)) {
                py = world.resolvePlayerAxis(oldPy, py, px, py, pz, 1);
                if (vy < 0) vy = 0;
            }

            double feet = py - EYE_OFFSET;
            int bx = (int) Math.floor(px);
            int bz = (int) Math.floor(pz);
            int feetY = (int) Math.floor(feet - 0.1);
            onGround = feetY >= 0 && feetY < World.WY && world.get(bx, feetY, bz) != 0;

            if (py < EYE_OFFSET + 1) { py = EYE_OFFSET + 1; vy = 0; onGround = true; }
        }
    }

    public void destroyBlock(World world, double yaw, double pitch, List<float[]> itemDrops) {
        int[] hit = getHitBlock(world, yaw, pitch);
        if (hit == null) return;
        int fx = hit[0], fy = hit[1], fz = hit[2];
        int idx = fx + fy * World.WX + fz * World.WX * World.WY;
        int oldType = world.buffer.get(idx) & 0xFF;
        if (oldType == 4 && selectedBlock == 10) {
            world.fellTree(fx, fy, fz, new java.util.Random(), itemDrops);
        } else {
            world.buffer.put(idx, (byte) 0);
            world.upload();
            if (gameMode == 1 && oldType > 0)
                itemDrops.add(new float[]{fx + 0.5f, fy + 0.5f, fz + 0.5f, oldType, 0f});
        }
    }

    public boolean placeBlock(World world, double yaw, double pitch) {
        if (!isPlaceable(selectedBlock)) return false;
        if (gameMode == 1 && inventory[selectedBlock] <= 0) return false;
        int[] hit = getHitBlock(world, yaw, pitch);
        if (hit == null) return false;
        int fx = hit[0] + (hit[3] == 0 ? 1 : hit[3] == 1 ? -1 : 0);
        int fy = hit[1] + (hit[3] == 2 ? 1 : hit[3] == 3 ? -1 : 0);
        int fz = hit[2] + (hit[3] == 4 ? 1 : hit[3] == 5 ? -1 : 0);
        if (fx < 0 || fx >= World.WX || fy < 0 || fy >= World.WY || fz < 0 || fz >= World.WZ) return false;
        if (world.get(fx, fy, fz) != 0) return false;
        if (fx + 1 > px - HALF && fx < px + HALF &&
            fy + 1 > py - EYE_OFFSET && fy < py + HALF &&
            fz + 1 > pz - HALF && fz < pz + HALF) return false;
        world.set(fx, fy, fz, (byte) selectedBlock);
        world.upload();
        if (gameMode == 1) inventory[selectedBlock]--;
        return true;
    }

    public int getBlockInSight(World world, double yaw, double pitch) {
        int[] hit = getHitBlock(world, yaw, pitch);
        if (hit == null) return 0;
        return world.get(hit[0], hit[1], hit[2]) & 0xFF;
    }

    public void pickupItems(List<float[]> itemDrops) {
        for (int i = itemDrops.size() - 1; i >= 0; i--) {
            float[] d = itemDrops.get(i);
            double dp = d[0] - px, dy = d[1] - (py - EYE_OFFSET / 2), ddz = d[2] - pz;
            if (dp * dp + dy * dy + ddz * ddz < 1.5 * 1.5) {
                int type = (int) d[3];
                if (type >= 0 && type < BlockRegistry.MAX_BLOCK_TYPES) inventory[type]++;
                itemDrops.remove(i);
            }
        }
    }

    public void resetPosition() {
        px = 8; py = 14 + EYE_OFFSET; pz = 8;
        vy = 0; onGround = false;
    }
}
