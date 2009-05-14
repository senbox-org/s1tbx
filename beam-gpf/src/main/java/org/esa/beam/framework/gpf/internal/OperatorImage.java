package org.esa.beam.framework.gpf.internal;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.ImageUtils;

import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.SourcelessOpImage;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.HashMap;
import java.util.Map;

public class OperatorImage extends SourcelessOpImage {

    private static final String DISABLE_TILE_CACHING_PROPERTY = "beam.gpf.disableOperatorTileCaching";

    private Band band;
    private OperatorContext operatorContext;
    private ProgressMonitor progressMonitor;

    public OperatorImage(Band targetBand, OperatorContext operatorContext) {
        this(targetBand, operatorContext, ImageManager.createSingleBandedImageLayout(targetBand));
    }

    private OperatorImage(Band targetBand, OperatorContext operatorContext, ImageLayout imageLayout) {
        super(imageLayout,
              operatorContext.getRenderingHints(),
              imageLayout.getSampleModel(null),
              imageLayout.getMinX(null),
              imageLayout.getMinY(null),
              imageLayout.getWidth(null),
              imageLayout.getHeight(null));
        this.band = targetBand;
        this.operatorContext = operatorContext;
        final boolean disableTileCaching = Boolean.getBoolean(DISABLE_TILE_CACHING_PROPERTY);
        if (disableTileCaching) {
            setTileCache(null);
        } else if (getTileCache() == null) {
            setTileCache(JAI.getDefaultInstance().getTileCache());
        }
    }

    public OperatorContext getOperatorContext() {
        return operatorContext;
    }

    public Band getTargetBand() {
        return band;
    }

    @Override
    protected void computeRect(PlanarImage[] ignored, WritableRaster tile, Rectangle destRect) {

        long nanos1 = System.nanoTime();

        if (operatorMustComputeTileStack()) {
            Map<Band, Tile> targetTiles = createTargetTileStack(tile, destRect);
            operatorContext.getOperator().computeTileStack(targetTiles, destRect, getProgressMonitor());
        } else {
            Tile targetTile = createTargetTile(destRect, tile);
            if (operatorContext.isComputeTileMethodUsable()) {
                operatorContext.getOperator().computeTile(getTargetBand(), targetTile, getProgressMonitor());
            }
        }

        long nanos2 = System.nanoTime();

        double targetNanosPerPixel = (double) (nanos2 - nanos1) / (double) (destRect.width * destRect.height);
        operatorContext.getPerformanceMetric().updateTarget(targetNanosPerPixel);

        double sourceNanosPerPixel = operatorContext.getSourceNanosPerPixel();
        operatorContext.getPerformanceMetric().updateSource(sourceNanosPerPixel);
    }

    WritableRaster getWritableRaster(Rectangle tileRectangle) {
        Assert.argument(tileRectangle.x % getTileWidth() == 0, "rectangle");
        Assert.argument(tileRectangle.y % getTileHeight() == 0, "rectangle");
        Assert.argument(tileRectangle.width == getTileWidth(), "rectangle");
        Assert.argument(tileRectangle.height == getTileHeight(), "rectangle");
        final int tileX = XToTileX(tileRectangle.x);
        final int tileY = YToTileY(tileRectangle.y);
        Raster tileFromCache = getTileFromCache(tileX, tileY);
        WritableRaster writableRaster;
        if (tileFromCache != null) {
            // we already put a WritableRaster into the cache
            writableRaster = (WritableRaster) tileFromCache;
        } else {
            writableRaster = createWritableRaster(tileRectangle);
            addTileToCache(tileX, tileY, writableRaster);
        }
        return writableRaster;
    }

    public ProgressMonitor getProgressMonitor() {
        return progressMonitor != null ? progressMonitor : ProgressMonitor.NULL;
    }

    public void setProgressMonitor(ProgressMonitor pm) {
        this.progressMonitor = pm;
    }

    public WritableRaster createWritableRaster(Rectangle rectangle) {
        final int dataBufferType = ImageManager.getDataBufferType(band.getDataType());
        SampleModel sampleModel = ImageUtils.createSingleBandedSampleModel(dataBufferType, rectangle.width,
                                                                           rectangle.height);
        final Point location = new Point(rectangle.x, rectangle.y);
        return createWritableRaster(sampleModel, location);
    }

    @Override
    public synchronized void dispose() {
        band = null;
        progressMonitor = null;
        super.dispose();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append("[");
        sb.append(operatorContext.getOperatorSpi().getOperatorAlias());
        sb.append(",");
        sb.append(band.getName());
        sb.append("]");
        return sb.toString();
    }

    public static ProgressMonitor setProgressMonitor(RenderedImage image, ProgressMonitor pm) {
        ProgressMonitor oldPm = ProgressMonitor.NULL;
        if (image instanceof OperatorImage) {
            final OperatorImage opImage = (OperatorImage) image;
            oldPm = opImage.getProgressMonitor();
            opImage.setProgressMonitor(pm);
        }
        return oldPm;
    }

    private Tile createTargetTile(Rectangle targetRectangle, WritableRaster targetTileRaster) {
        Tile targetTile;
        if (operatorContext.isPassThrough()) {
            targetTile = operatorContext.getSourceTile(getTargetBand(), targetRectangle, getProgressMonitor());
        } else {
            targetTile = createTargetTile(getTargetBand(), targetTileRaster, targetRectangle);
        }
        return targetTile;
    }

    private Map<Band, Tile> createTargetTileStack(WritableRaster targetTileRaster, Rectangle targetRectangle) throws OperatorException {
        Band[] bands = operatorContext.getTargetProduct().getBands();
        Map<Band, Tile> targetTiles = new HashMap<Band, Tile>(bands.length * 2);
        if (operatorContext.isPassThrough()) {
            for (Band band : bands) {
                if (isBandComputedByThisOperator(band)) {
                    targetTiles.put(band, operatorContext.getSourceTile(band, targetRectangle, getProgressMonitor()));
                }
            }
        } else {
            for (Band band : bands) {
                if (isBandComputedByThisOperator(band)) {
                    WritableRaster tileRaster = getWritableRaster(band, targetTileRaster);
                    targetTiles.put(band, createTargetTile(band, tileRaster, targetRectangle));
                }
            }
        }
        return targetTiles;
    }

    private static TileImpl createTargetTile(Band band, WritableRaster targetTileRaster, Rectangle targetRectangle) {
        return new TileImpl(band, targetTileRaster, targetRectangle, true);
    }

    private boolean operatorMustComputeTileStack() {
        return operatorContext.isComputeTileStackMethodUsable()
                && !operatorContext.isComputeTileMethodUsable();
    }

    boolean isBandComputedByThisOperator(Band targetBand) {
        if (targetBand == getTargetBand()) {
            return true;
        }
        if (!targetBand.isSourceImageSet()) {
            return false;
        }
        OperatorImage image = operatorContext.getTargetImage(targetBand);
        return image != null && image == targetBand.getSourceImage().getImage(0);
    }

    private WritableRaster getWritableRaster(Band band, WritableRaster targetTileRaster) {
        WritableRaster tileRaster;
        if (band == getTargetBand()) {
            tileRaster = targetTileRaster;
        } else {
            OperatorImage operatorImage = operatorContext.getTargetImage(band);
            Assert.state(operatorImage != this);
            tileRaster = operatorImage.getWritableRaster(targetTileRaster.getBounds());
        }
        return tileRaster;
    }


}
