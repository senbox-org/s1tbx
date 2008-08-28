package org.esa.beam.jai;

import com.bc.ceres.glevel.DownscalableImage;

import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.SourcelessOpImage;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.Map;


/**
 * A base class for single-band {@code OpImages} retrieving data at a given pyramid level.
 */
public abstract class SingleBandedOpImage extends SourcelessOpImage implements DownscalableImage, DownscalableImageFactory {
    private DownscalableImageSupport downscalableImageSupport;

    /**
     * Used to construct a level-0 image.
     *
     * @param dataBufferType The data type.
     * @param width          The width of the level 0 image.
     * @param height         The height of the level 0 image.
     * @param tileSize       The tile size for this image.
     * @param configuration  The configuration map (can be null).
     */
    protected SingleBandedOpImage(int dataBufferType,
                                  int width,
                                  int height,
                                  Dimension tileSize,
                                  Map configuration) {
        this(ImageManager.createSingleBandedImageLayout(dataBufferType,
                                                        width,
                                                        height,
                                                        tileSize,
                                                        0),
             configuration);
        downscalableImageSupport = new DownscalableImageSupport.Level0(this, this);
    }

    /**
     * Used to construct a level-N image.
     *
     * @param level0        The DownscalableImageSupport for Level 0.
     * @param level         The level of this image.
     * @param configuration The configuration map (can be null).
     */
    protected SingleBandedOpImage(DownscalableImageSupport level0,
                                  int level,
                                  Map configuration) {
        this(ImageManager.createSingleBandedImageLayout(level0.getImage().getSampleModel().getDataType(),
                                                        level0.getSourceWidth(),
                                                        level0.getSourceHeight(),
                                                        new Dimension(level0.getImage().getTileWidth(),
                                                                      level0.getImage().getTileHeight()),
                                                        level),
             configuration);
        downscalableImageSupport = new DownscalableImageSupport.LevelN(level0, this, level);
    }

    private SingleBandedOpImage(ImageLayout layout,
                                Map configuration) {
        super(layout,
              configuration,
              layout.getSampleModel(null),
              layout.getMinX(null),
              layout.getMinY(null),
              layout.getWidth(null),
              layout.getHeight(null));
        setTileCache(JAI.getDefaultInstance().getTileCache());
    }

    protected DownscalableImageSupport getDownscalableImageSupport() {
        return downscalableImageSupport;
    }

    protected final int getLevel() {
        return downscalableImageSupport.getLevel();
    }

    protected final double getScale() {
        return downscalableImageSupport.getScale();
    }

    protected int getSourceX(int tx) {
        return downscalableImageSupport.getSourceX(tx);
    }

    protected int getSourceY(int ty) {
        return downscalableImageSupport.getSourceY(ty);
    }

    // TODO - wrong impl, replace by getSourceRect(destRect)
    protected int getSourceWidth(int destWidth) {
        return downscalableImageSupport.getSourceWidth(destWidth);
    }

    protected int getSourceCoord(double destCoord, int min, int max) {
        return downscalableImageSupport.getSourceCoord(destCoord, min, max);
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

    public DownscalableImage downscale(int level) {
        return downscalableImageSupport.getDownscaledImage(level);
    }

    public synchronized void dispose() {
        downscalableImageSupport.dispose();
    }
}
