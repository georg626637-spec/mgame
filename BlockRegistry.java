import java.nio.*;
import java.util.*;
import org.lwjgl.BufferUtils;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL42.*;

public class BlockRegistry {
    public static final int MAX_BLOCK_TYPES = 32;
    public final List<BlockTexture> textures = new ArrayList<>();
    public final List<BlockShape> shapes = new ArrayList<>();
    public int texArray;
    public int shapeTex;
    public int uBlockTexLoc;
    public int uShapeTexLoc;
    public Map<String, BlockTexture> pendingTextures = new HashMap<>();

    public void initDefaults() {
        int[] colors = {0, 0xFF8B5E3C, 0xFF5A9E4A, 0xFF8A8A8A, 0xFF6B4423, 0xFF2D5A1E, 0xFF3366AA, 0xFFE8D5A0, 0xFF333333, 0xFFCC9966, 0xFFAAAAAA, 0xFF886644, 0xFFC8A96E, 0xFF555555, 0xFFCC8888, 0xFF8B6B3C, 0xFFB8860B};
        String[] names = {"", "Dirt", "Grass", "Stone", "Wood", "Leaves", "Water", "Sand", "Coal", "Iron", "Axe", "Saw", "Planks", "Furnace", "Cheek", "Stick", "Chest"};
        for (int i = 1; i <= 15; i++)
            textures.add(new BlockTexture(names[i], colors[i]));
        int[] chestPixels = new int[256];
        int gold = BlockTexture.rgb(204, 153, 0);
        int body = BlockTexture.rgb(139, 69, 19);
        int lockC = BlockTexture.rgb(255, 215, 0);
        int hole = BlockTexture.rgb(30, 30, 30);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                if (y < 2 || y >= 14 || x < 2 || x >= 14) {
                    chestPixels[y * 16 + x] = gold;
                } else if (y >= 6 && y <= 8 && x >= 5 && x <= 10) {
                    chestPixels[y * 16 + x] = (y == 7 && x >= 6 && x <= 9) ? hole : lockC;
                } else {
                    chestPixels[y * 16 + x] = body;
                }
            }
        }
        textures.add(new BlockTexture("Chest", chestPixels));
        int[] ctPixels = new int[256];
        int woodC = BlockTexture.rgb(160, 120, 60);
        int gridC = BlockTexture.rgb(80, 60, 30);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                if (x % 4 == 0 || y % 4 == 0) {
                    ctPixels[y * 16 + x] = gridC;
                } else if (x == 15 || y == 15) {
                    ctPixels[y * 16 + x] = gridC;
                } else {
                    ctPixels[y * 16 + x] = woodC;
                }
            }
        }
        textures.add(new BlockTexture("CraftingTable", ctPixels));
        for (int i = 0; i < textures.size(); i++)
            shapes.add(BlockShape.fullBlock());
    }

    public int size() { return textures.size(); }

    public String name(int type) {
        if (type <= 0 || type > textures.size()) return "";
        return textures.get(type - 1).name;
    }

    public int color(int type) {
        if (type <= 0 || type > textures.size()) return 0;
        return textures.get(type - 1).pixels[0];
    }

    public void upload() {
        int layers = textures.size();
        int sz = BlockTexture.SIZE;
        ByteBuffer buf = BufferUtils.createByteBuffer(sz * sz * 4 * layers);
        for (BlockTexture tex : textures) {
            for (int y = 0; y < sz; y++)
                for (int x = 0; x < sz; x++) {
                    int argb = tex.pixels[y * sz + x];
                    buf.put((byte) ((argb >> 16) & 0xFF));
                    buf.put((byte) ((argb >> 8) & 0xFF));
                    buf.put((byte) (argb & 0xFF));
                    buf.put((byte) 255);
                }
        }
        buf.flip();
        if (texArray == 0) {
            texArray = glGenTextures();
            glBindTexture(GL_TEXTURE_2D_ARRAY, texArray);
            glTexImage3D(GL_TEXTURE_2D_ARRAY, 0, GL_RGBA8, sz, sz, MAX_BLOCK_TYPES, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer)null);
            glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        }
        glBindTexture(GL_TEXTURE_2D_ARRAY, texArray);
        glTexSubImage3D(GL_TEXTURE_2D_ARRAY, 0, 0, 0, 0, sz, sz, layers, GL_RGBA, GL_UNSIGNED_BYTE, buf);
    }

    public void uploadShapes() {
        int layers = shapes.size();
        int r = BlockShape.RES;
        ByteBuffer buf = BufferUtils.createByteBuffer(r * r * r * MAX_BLOCK_TYPES);
        for (int i = 0; i < MAX_BLOCK_TYPES; i++) {
            if (i < shapes.size()) {
                buf.put(shapes.get(i).toBuffer());
            } else {
                for (int j = 0; j < r * r * r; j++) buf.put((byte)0);
            }
        }
        buf.flip();
        if (shapeTex == 0) {
            shapeTex = glGenTextures();
            glBindTexture(GL_TEXTURE_3D, shapeTex);
            glTexImage3D(GL_TEXTURE_3D, 0, GL_R8UI, r, r, r * MAX_BLOCK_TYPES, 0, GL_RED_INTEGER, GL_UNSIGNED_BYTE, (ByteBuffer)null);
            glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);
        }
        glBindTexture(GL_TEXTURE_3D, shapeTex);
        glTexSubImage3D(GL_TEXTURE_3D, 0, 0, 0, 0, r, r, r * MAX_BLOCK_TYPES, GL_RED_INTEGER, GL_UNSIGNED_BYTE, buf);
    }

    public void setBlockShape(int type, BlockShape shape) {
        if (type <= 0 || type > shapes.size()) return;
        shapes.set(type - 1, shape);
        uploadShapes();
    }

    public void addFromPending(String name) {
        BlockTexture tex = pendingTextures.remove(name);
        if (tex == null) tex = new BlockTexture(name, 0xFF888888);
        textures.add(tex);
        shapes.add(BlockShape.fullBlock());
        upload();
        uploadShapes();
    }

    public int addFromMod(String name, int argbColor) {
        if (textures.size() >= MAX_BLOCK_TYPES) return 0;
        textures.add(new BlockTexture(name, argbColor));
        shapes.add(BlockShape.fullBlock());
        upload();
        uploadShapes();
        return textures.size();
    }

    public int addFromMod(String name, int[] texPixels) {
        if (textures.size() >= MAX_BLOCK_TYPES) return 0;
        textures.add(new BlockTexture(name, texPixels));
        shapes.add(BlockShape.fullBlock());
        upload();
        uploadShapes();
        return textures.size();
    }
}
