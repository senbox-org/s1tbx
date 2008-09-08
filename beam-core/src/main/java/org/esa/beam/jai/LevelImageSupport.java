package org.esa.beam.jai;

import com.bc.ceres.core.Assert;


/**
 * Supports the development of images, which are returned by implementations of the
 * {@link com.bc.ceres.glevel.MultiLevelSource MultiLevelSource} interface.
 */
public class LevelImageSupport {

    private final int sourceWidth;
    private final int sourceHeight;
    private final int level;
    private final double scale;

    protected LevelImageSupport(int sourceWidth, int sourceHeight, int level) {
        this.sourceWidth = sourceWidth;
        this.sourceHeight = sourceHeight;
        Assert.argument(level >= 0, "level >= 0");
        this.level = level;
        this.scale = ImageManager.computeScale(level);
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

    public int getSourceX(int tx) {
        return getSourceCoord(tx, 0, getSourceWidth() - 1);
    }

    public int getSourceY(int ty) {
        return getSourceCoord(ty, 0, getSourceHeight() - 1);
    }

    // TODO - wrong impl, replace by getSourceRect(destRect)
    public int getSourceWidth(int destWidth) {
        return getSourceCoord(destWidth, 1, getSourceWidth() - 1);
    }

    public int getSourceCoord(double destCoord, int min, int max) {
        return double2int(destCoord / getScale(), min, max);
    }

    public int getDestCoord(double sourceCoord, int min, int max) {
        return double2int(getScale() * sourceCoord, min, max);
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