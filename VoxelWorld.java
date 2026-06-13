import java.util.HashMap;
import java.util.Map;

public class VoxelWorld {
    private final Map<Long, VoxelChunk> chunks = new HashMap<>();
    public static final int RENDER_DISTANCE = 4;

    private long chunkKey(int cx, int cz) {
        return ((long) cx << 32) | (cz & 0xFFFFFFFFL);
    }

    public VoxelChunk getOrCreateChunk(int cx, int cz) {
        long key = chunkKey(cx, cz);
        return chunks.computeIfAbsent(key, k -> new VoxelChunk(cx, cz));
    }

    public VoxelChunk getChunk(int cx, int cz) {
        return chunks.get(chunkKey(cx, cz));
    }

    public VoxelType getVoxel(int wx, int wy, int wz) {
        int cx = Math.floorDiv(wx, VoxelChunk.SIZE);
        int cz = Math.floorDiv(wz, VoxelChunk.SIZE);
        int lx = Math.floorMod(wx, VoxelChunk.SIZE);
        int lz = Math.floorMod(wz, VoxelChunk.SIZE);
        VoxelChunk c = getOrCreateChunk(cx, cz);
        return c.get(lx, wy, lz);
    }

    public void ensureSurroundingChunks(int px, int pz) {
        int ccx = Math.floorDiv(px, VoxelChunk.SIZE);
        int ccz = Math.floorDiv(pz, VoxelChunk.SIZE);
        for (int dx = -RENDER_DISTANCE; dx <= RENDER_DISTANCE; dx++) {
            for (int dz = -RENDER_DISTANCE; dz <= RENDER_DISTANCE; dz++) {
                getOrCreateChunk(ccx + dx, ccz + dz);
            }
        }
    }
}
