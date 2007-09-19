package org.esa.beam.framework.gpf.internal;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.OperatorContext;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.util.jai.RasterDataNodeOpImage;

import javax.media.jai.ImageLayout;
import javax.media.jai.PlanarImage;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.HashMap;
import java.util.Map;

public class GpfOpImage extends RasterDataNodeOpImage {

    private DefaultOperatorContext operatorContext;

    protected GpfOpImage(Band band,
                         DefaultOperatorContext operatorContext,
                         ImageLayout imageLayout) {
        super(band, imageLayout);
        this.operatorContext = operatorContext;
    }

    public static GpfOpImage create(Band band, DefaultOperatorContext operatorContext) {
        ImageLayout imageLayout = createSingleBandedImageLayout(band);
        return new GpfOpImage(band, operatorContext, imageLayout);
    }

    public Band getBand() {
        return (Band) getRasterDataNode();
    }

    public OperatorContext getOperatorContext() {
        return operatorContext;
    }

    @Override
    protected void computeRect(PlanarImage[] ignored, WritableRaster tile, Rectangle destRect) {
        try {
            executeOperator(tile, destRect);
        } catch (OperatorException e) {
            throw new RuntimeException(e);
        }
    }

    private void executeOperator(WritableRaster tile, Rectangle destRect) throws OperatorException {
        if (operatorMustComputeAllBands()) {
            // Provide target GPF rasters and associated AWT tiles
            WritableRaster[] targetTiles = getTargetTiles(tile);
            Map<Band, org.esa.beam.framework.gpf.Raster> targetRasters = new HashMap<Band, org.esa.beam.framework.gpf.Raster>(targetTiles.length * 2);
            for (int i = 0; i < targetTiles.length; i++) {
                Band targetBand = operatorContext.getTargetProduct().getBandAt(i);
                ProductData targetData = targetBand.createCompatibleRasterData(destRect.width, destRect.height);
                org.esa.beam.framework.gpf.Raster targetRaster = new RasterImpl(getBand(), destRect, targetData);
                targetRasters.put(targetBand, targetRaster);
            }
            // Compute target GPF rasters
            operatorContext.getOperator().computeAllBands(targetRasters, destRect, ProgressMonitor.NULL);
            // Write computed target GPF rasters into associated AWT tiles
            for (int i = 0; i < targetTiles.length; i++) {
                Band targetBand = operatorContext.getTargetProduct().getBandAt(i);
                WritableRaster targetTile = targetTiles[i];
                ProductData targetData = targetRasters.get(targetBand).getDataBuffer();
                targetTile.setDataElements(destRect.x, destRect.y, destRect.width, destRect.height, targetData.getElems());
            }
        } else {
            // Provide target GPF raster
            ProductData targetData = getBand().createCompatibleRasterData(destRect.width, destRect.height);
            org.esa.beam.framework.gpf.Raster targetRaster = new RasterImpl(getBand(), destRect, targetData);
            // Compute target GPF raster
            operatorContext.getOperator().computeBand(getBand(), targetRaster, ProgressMonitor.NULL);
            // Write computed target GPF raster into associated AWT tile
            tile.setDataElements(destRect.x, destRect.y, destRect.width, destRect.height, targetData.getElems());
        }
    }

    private WritableRaster[] getTargetTiles(WritableRaster tile) {
        // Note: an array of Product.getNumBands() opImages are returned, one for each product band
        PlanarImage[] images = operatorContext.getTargetImages();
        WritableRaster[] tiles = new WritableRaster[images.length];
        for (int i = 0; i < images.length; i++) {
            PlanarImage image = images[i];
            if (image != this) {
                tiles[i] = ((GpfOpImage) image).getTargetTile(tile.getBounds());
            } else {
                tiles[i] = tile;
            }
        }
        return tiles;
    }

    private WritableRaster getTargetTile(Rectangle tileBounds) {
        final int tileX = XToTileX(tileBounds.x);
        final int tileY = YToTileY(tileBounds.y);
        Raster tileFromCache = getTileFromCache(tileX, tileY);
        WritableRaster writableRaster;
        if (tileFromCache != null) {
            // we already put a WritableRaster into the cache
            writableRaster = (WritableRaster) tileFromCache;
        } else {
            writableRaster = createWritableRaster(tileBounds);
            addTileToCache(tileX, tileY, writableRaster);
        }
        return writableRaster;
    }

    private boolean operatorMustComputeAllBands() {
        return operatorContext.getOperatorImplementationInfo().isComputeAllBandsMethodImplemented()
                && !operatorContext.getOperatorImplementationInfo().isComputeBandMethodImplemented();
    }
}
