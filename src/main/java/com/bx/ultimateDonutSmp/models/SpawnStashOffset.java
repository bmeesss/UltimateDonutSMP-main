package com.bx.ultimateDonutSmp.models;

public final class SpawnStashOffset {
    private final int x;
    private final int y;
    private final int z;

    public SpawnStashOffset(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int x() { return x; }
    public int y() { return y; }
    public int z() { return z; }


    public static final SpawnStashOffset ZERO = new SpawnStashOffset(0, 0, 0);
    @Override public String toString() {
        return "SpawnStashOffset[x=+x, y=+y, z=+z]";
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SpawnStashOffset that = (SpawnStashOffset) o;
        return java.util.Objects.equals(x, that.x) && java.util.Objects.equals(y, that.y) && java.util.Objects.equals(z, that.z);
    }
    @Override public int hashCode() {
        return java.util.Objects.hash(x, y, z);
    }
}
