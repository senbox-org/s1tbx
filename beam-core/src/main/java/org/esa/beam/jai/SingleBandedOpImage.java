package org.esa.beam.jai;

import javax.media.jai.ImageLayout;
import javax.media.jai.SourcelessOpImage;
import javax.media.jai.JAI;
import java.awt.*;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.Map;


/**
 * A base class for single-band {@code OpImages} retrieving data at a given pyramid level.
 */
public abstract class SingleBandedOpImage extends SourcelessOpImage {

    private LevelImageSupport levelImageSupport;

    /**
     * Used to construct an image.
     *
     * @param dataBufferType The data type.
     * @param sourceWidth    The width of the level 0 image.
     * @param sourceHeight   The height of the level 0 image.
     * @param tileSize       The tile size for this image.
     * @param configuration  The configuration map (can be null).
     * @param level          The resolution level.
     */
    protected SingleBandedOpImage(int dataBufferType,
                                  int sourceWidth,
                                  int sourceHeight,
                                  Dimension tileSize,
                                  Map configuration,
                                  ResolutionLevel level) {
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
                                ResolutionLevel level) {
        super(layout,
              configuration,
              layout.getSampleModel(null),
              layout.getMinX(null),
              layout.getMinY(null),
              layout.getWidth(null),
              layout.getHeight(null));
        setTileCache(JAI.getDefaultInstance().getTileCache());
        levelImageSupport = new LevelImageSupport(sourceWidth,
                                                  sourceHeight,
                                                  level);
    }

    public final int getLevel() {
        return levelImageSupport.getLevel();
    }

    protected final double getScale() {
        return levelImageSupport.getScale();
    }

    protected final int getSourceX(int tx) {
        return levelImageSupport.getSourceX(tx);
    }

    protected final int getSourceY(int ty) {
        return levelImageSupport.getSourceY(ty);
    }

    protected final int getSourceWidth(int destWidth) {
        return levelImageSupport.getSourceWidth(destWidth);
    }

    protected final int getSourceCoord(double destCoord, int min, int max) {
        return levelImageSupport.getSourceCoord(destCoord, min, max);
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
