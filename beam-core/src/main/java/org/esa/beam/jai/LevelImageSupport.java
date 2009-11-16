package org.esa.beam.jai;


/**
 * Supports the development of images, which are returned by implementations of the
 * {@link com.bc.ceres.glevel.MultiLevelSource MultiLevelSource} interface.
 */
public final class LevelImageSupport {

    private final int sourceWidth;
    private final int sourceHeight;
    private final int level;
    private final double scale;

    public LevelImageSupport(int sourceWidth, int sourceHeight, ResolutionLevel level) {
        this.sourceWidth = sourceWidth;
        this.sourceHeight = sourceHeight;
        this.level = level.getIndex();
        this.scale = level.getScale();
    }

    public int getSourceWidth() {
        return sourceWidth;
    }

    public int getSourceHeight() {
        return sourceHeight;
    }

    public int getLevel() {
        return level;
    }

    public double getScale() {
        return scale;
    }

    public final int getSourceX(int tx) {
        return getSourceCoord(tx, 0, getSourceWidth() - 1);
    }

    public final int getSourceY(int ty) {
        return getSourceCoord(ty, 0, getSourceHeight() - 1);
    }

    public final int getSourceWidth(int destWidth) {
        return getSourceCoord(destWidth, 1, getSourceWidth() - 1);
    }

    public final int getSourceHeight(int destHeight) {
        return getSourceCoord(destHeight, 1, getSourceHeight() - 1);
    }

    public final int getSourceCoord(double destCoord, int min, int max) {
        return double2int(getScale() * destCoord, min, max);
    }

    public int getDestCoord(double sourceCoord, int min, int max) {
        return double2int(sourceCoord / getScale(), min, max);
    }

    private static int double2int(double v, int min, int max) {
        int sc = (int) Math.floor(v);
        if (sc < min) {
            sc = min;
        }
        if (sc > max) {
            sc = max;
        }
        return sc;
    }
}
