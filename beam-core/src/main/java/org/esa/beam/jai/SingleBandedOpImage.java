package org.esa.beam.jai;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.Map;

import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.SourcelessOpImage;

import com.bc.ceres.glevel.LevelImage;


/**
 * A base class for single-band {@code OpImages} retrieving data at a given pyramid level.
 */
public abstract class SingleBandedOpImage extends SourcelessOpImage implements LevelImage {
    
    private ScalableImageSupport scalableImageSupport;
    private final int level;
    
    /**
     * Used to construct an image.
     *
     * @param dataBufferType The data type.
     * @param sourceWidth    The width of the level 0 image.
     * @param sourceHeight   The height of the level 0 image.
     * @param tileSize       The tile size for this image.
     * @param configuration  The configuration map (can be null).
     * @param lrImageFactory 
     */
    protected SingleBandedOpImage(int dataBufferType,
                                  int sourceWidth,
                                  int sourceHeight,
                                  Dimension tileSize,
                                  Map configuration, 
                                  int level) {
        this(ImageManager.createSingleBandedImageLayout(dataBufferType,
                                                        sourceWidth,
                                                        sourceHeight,
                                                        tileSize,
                                                        level),
             sourceWidth,
             sourceHeight,
             configuration,
             level);
    }

    private SingleBandedOpImage(ImageLayout layout,
                                int sourceWidth,
                                int sourceHeight,
                                Map configuration,
                                int level) {
        super(layout,
              configuration,
              layout.getSampleModel(null),
              layout.getMinX(null),
              layout.getMinY(null),
              layout.getWidth(null),
              layout.getHeight(null));
        this.level = level;
        setTileCache(JAI.getDefaultInstance().getTileCache());
        scalableImageSupport = new ScalableImageSupport(sourceWidth,
                                                        sourceHeight,
                                                        level);
    }

    public final int getLevel() {
        return level;
    }

    protected final double getScale() {
        return scalableImageSupport.getScale();
    }

    protected int getSourceX(int tx) {
        return scalableImageSupport.getSourceX(tx);
    }

    protected int getSourceY(int ty) {
        return scalableImageSupport.getSourceY(ty);
    }

    // TODO - wrong impl, replace by getSourceRect(destRect)
    protected int getSourceWidth(int destWidth) {
        return scalableImageSupport.getSourceWidth(destWidth);
    }

    protected int getSourceCoord(double destCoord, int min, int max) {
        return scalableImageSupport.getSourceCoord(destCoord, min, max);
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
}
