import java.util.Arrays;

public class BlockTexture {
    public static final int SIZE = 16;
    public String name;
    public int[] pixels;

    public BlockTexture(String name, int solidColor) {
        this.name = name;
        this.pixels = new int[SIZE * SIZE];
        Arrays.fill(pixels, solidColor);
    }

    public BlockTexture(String name, int[] pixels) {
        this.name = name;
        this.pixels = pixels;
    }

    public int getARGB(int x, int y) {
        return pixels[y * SIZE + x];
    }

    public static int rgb(int r, int g, int b) {
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
