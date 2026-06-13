public class VoxelChunk {
    public static final int SIZE = 16;
    public static final int HEIGHT = 32;
    private final VoxelType[][][] voxels;
    private final int chunkX, chunkZ;

    public VoxelChunk(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.voxels = new VoxelType[SIZE][HEIGHT][SIZE];
        fillAir();
        generateTerrain();
    }

    private void fillAir() {
        for (int x = 0; x < SIZE; x++)
            for (int y = 0; y < HEIGHT; y++)
                for (int z = 0; z < SIZE; z++)
                    voxels[x][y][z] = VoxelType.AIR;
    }

    private void generateTerrain() {
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                int worldX = chunkX * SIZE + x;
                int worldZ = chunkZ * SIZE + z;
                int height = getHeight(worldX, worldZ);
                for (int y = 0; y < HEIGHT; y++) {
                    if (y < height - 3) {
                        voxels[x][y][z] = VoxelType.STONE;
                    } else if (y < height) {
                        voxels[x][y][z] = VoxelType.DIRT;
                    } else if (y == height) {
                        if (height < 6) {
                            voxels[x][y][z] = VoxelType.SAND;
                        } else {
                            voxels[x][y][z] = VoxelType.GRASS;
                            if (height > 14 && Math.random() < 0.1) {
                                for (int trunk = 1; trunk <= 4; trunk++) {
                                    if (y + trunk < HEIGHT)
                                        voxels[x][y + trunk][z] = VoxelType.WOOD;
                                }
                                int leafY = y + 5;
                                if (leafY < HEIGHT) {
                                    for (int lx = -2; lx <= 2; lx++)
                                        for (int lz = -2; lz <= 2; lz++)
                                            for (int ly = 0; ly <= 2; ly++) {
                                                int px = x + lx, py = leafY + ly, pz = z + lz;
                                                if (px >= 0 && px < SIZE && pz >= 0 && pz < SIZE && py < HEIGHT) {
                                                    if (Math.abs(lx) == 2 && Math.abs(lz) == 2 && Math.random() < 0.5) continue;
                                                    if (ly == 2 && Math.abs(lx) == 2 && Math.random() < 0.5) continue;
                                                    if (voxels[px][py][pz] == VoxelType.AIR)
                                                        voxels[px][py][pz] = VoxelType.LEAVES;
                                                }
                                            }
                                }
                            }
                        }
                    } else if (y < 4 && voxels[x][y][z] == VoxelType.AIR) {
                        voxels[x][y][z] = VoxelType.WATER;
                    }
                }
            }
        }
    }

    private int getHeight(int wx, int wz) {
        double nx = wx * 0.05;
        double nz = wz * 0.05;
        double h = Math.sin(nx) * Math.cos(nz) * 4
                 + Math.sin(nx * 2 + 1.3) * Math.cos(nz * 2 + 0.7) * 2
                 + Math.cos(nx * 0.5 + 2.1) * Math.sin(nz * 0.5 + 1.7) * 3;
        return (int) (h + 10);
    }

    public VoxelType get(int x, int y, int z) {
        if (x < 0 || x >= SIZE || y < 0 || y >= HEIGHT || z < 0 || z >= SIZE)
            return VoxelType.AIR;
        return voxels[x][y][z];
    }

    public void set(int x, int y, int z, VoxelType type) {
        if (x >= 0 && x < SIZE && y >= 0 && y < HEIGHT && z >= 0 && z < SIZE)
            voxels[x][y][z] = type;
    }

    public int getChunkX() { return chunkX; }
    public int getChunkZ() { return chunkZ; }
}
