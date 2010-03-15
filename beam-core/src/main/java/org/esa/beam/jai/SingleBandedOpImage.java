package org.esa.beam.jai;

import com.bc.ceres.jai.NoDataRaster;

import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.SourcelessOpImage;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
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

    protected final int getSourceHeight(int destHeight) {
        return levelImageSupport.getSourceHeight(destHeight);
    }

    protected final int getSourceCoord(double destCoord, int min, int max) {
        return levelImageSupport.getSourceCoord(destCoord, min, max);
    }

    /**
     * Creates a new raster containing solely no-data (non-interpretable data, missing data) samples. The raster's
     * data buffer is filled with the given no-data value.
     * <p/>
     * The raster's origin is (0, 0). In order to translate the raster,
     * use {@link Raster#createTranslatedChild(int x, int y)}.
     *
     * @param noDataValue The no-data value used to fill the data buffer
     *                    of the raster created.
     *
     * @return the raster created.
     *
     * @see {@link NoDataRaster}.
     */
    protected NoDataRaster createNoDataRaster(double noDataValue) {
        final Raster raster = createWritableRaster(getSampleModel(), new Point(0, 0));
        final DataBuffer buffer = raster.getDataBuffer();

        for (int i = 0; i < buffer.getSize(); i++) {
            buffer.setElemDouble(i, noDataValue);
        }

        return new NoDataRaster(raster);
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
