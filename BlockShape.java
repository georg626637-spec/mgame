import java.nio.*;

public class BlockShape {
    public static final int RES = 16;
    private byte[][][] data = new byte[RES][RES][RES];

    public BlockShape() {}

    public static BlockShape fullBlock() {
        BlockShape s = new BlockShape();
        for (int x = 0; x < RES; x++)
            for (int y = 0; y < RES; y++)
                for (int z = 0; z < RES; z++)
                    s.data[x][y][z] = 1;
        return s;
    }

    public void set(int x, int y, int z) { data[x][y][z] = 1; }
    public void clear(int x, int y, int z) { data[x][y][z] = 0; }
    public byte get(int x, int y, int z) { return data[x][y][z]; }

    public ByteBuffer toBuffer() {
        ByteBuffer buf = ByteBuffer.allocateDirect(RES * RES * RES);
        for (int z = 0; z < RES; z++)
            for (int y = 0; y < RES; y++)
                for (int x = 0; x < RES; x++)
                    buf.put(data[x][y][z]);
        buf.rewind();
        return buf;
    }
}
