package org.esa.beam.jai;

import com.bc.layer.level.Downscaleable;
import com.bc.ceres.core.Assert;

import javax.media.jai.ImageLayout;
import javax.media.jai.SourcelessOpImage;
import java.awt.*;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.Map;
import java.util.HashMap;


public abstract class LevelOpImage extends SourcelessOpImage implements Downscaleable {
    private final LevelOpImage level0Image;
    private final Map<Integer, LevelOpImage> levelNImages;
    private final int sourceWidth;
    private final int sourceHeight;
    private final int level;
    private final double scale;

    protected LevelOpImage(ImageLayout imageLayout,
                           Map configuration,
                           SampleModel sampleModel,
                           int minX,
                           int minY,
                           int width,
                           int height,
                           int sourceWidth,
                           int sourceHeight) {
        this(imageLayout, configuration, sampleModel, minX, minY, width, height, sourceWidth, sourceHeight, null, 0);
    }

    protected LevelOpImage(ImageLayout imageLayout,
                           Map configuration,
                           SampleModel sampleModel,
                           int minX,
                           int minY,
                           int width,
                           int height,
                           int sourceWidth,
                           int sourceHeight,
                           LevelOpImage level0Image,
                           int level) {
        super(imageLayout, configuration, sampleModel, minX, minY, width, height);
        Assert.argument(sourceWidth >= width, "sourceWidth");
        Assert.argument(sourceHeight >= height, "sourceHeight");
        Assert.argument(level >= 0, "level");
        this.sourceWidth = sourceWidth;
        this.sourceHeight = sourceHeight;
        this.level0Image = level == 0 ? this : level0Image;
        this.levelNImages = level == 0 ? new HashMap<Integer, LevelOpImage>(16) : null;
        this.level = level;
        this.scale = ImageManager.computeScale(level);
        Assert.notNull(this.level0Image, "level0Image");
    }

    public final LevelOpImage getLevel0Image() {
        return level0Image;
    }

    public final int getSourceWidth() {
        return sourceWidth;
    }

    public final int getSourceHeight() {
        return sourceHeight;
    }

    public final int getLevel() {
        return level;
    }

    public final double getScale() {
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
        return double2int(destCoord / scale, min, max);
    }

    public int getDestCoord(double sourceCoord, int min, int max) {
        return double2int(scale * sourceCoord, min, max);
    }

    /**
     * Empty implementation. Used to prevent clients from overriding it, since
     * they shall implement {@link #computeRect(javax.media.jai.PlanarImage[], java.awt.image.WritableRaster, java.awt.Rectangle)}.
     *
     * @param sources  The sources.
     * @param dest     The destination raster.
     * @param destRect The destination rectangle.
     */
    @Override
    protected final void computeRect(Raster[] sources, WritableRaster dest, Rectangle destRect) {
    }

    public RenderedImage downscale(int level) {
        if (level == getLevel()) {
            return this;
        }
        LevelOpImage opImage = level0Image.levelNImages.get(level);
        if (opImage == null) {
            opImage = createDownscaledImage(level);
            level0Image.levelNImages.put(level, opImage);
        }
        return opImage;
    }

    protected abstract LevelOpImage createDownscaledImage(int level);

    public synchronized void dispose() {
        if (levelNImages != null) 
        levelNImages.clear();
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
