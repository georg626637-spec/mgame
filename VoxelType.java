public enum VoxelType {
    AIR(0, ' ', null),
    DIRT(1, '#', "\033[0;33m"),
    GRASS(2, '#', "\033[0;32m"),
    STONE(3, '#', "\033[0;37m"),
    WOOD(4, '#', "\033[0;33m"),
    LEAVES(5, '#', "\033[0;32m"),
    WATER(6, '~', "\033[0;34m"),
    SAND(7, '#', "\033[1;33m");

    public final int id;
    public final char displayChar;
    public final String color;

    VoxelType(int id, char displayChar, String color) {
        this.id = id;
        this.displayChar = displayChar;
        this.color = color;
    }

    public static VoxelType fromId(int id) {
        for (VoxelType t : values()) {
            if (t.id == id) return t;
        }
        return AIR;
    }
}
