package org.esa.snap.core.util.grid.isin;

public class IsinPoint {

    private double x;
    private double y;
    private int tile_line;
    private int tile_col;

    public IsinPoint(double x, double y) {
        this(x, y, -1, -1);
    }

    public IsinPoint(double x, double y, int tile_col, int tile_line) {
        this.x = x;
        this.y = y;

        this.tile_line = tile_line;
        this.tile_col = tile_col;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public int getTile_line() {
        return tile_line;
    }

    public int getTile_col() {
        return tile_col;
    }
}
