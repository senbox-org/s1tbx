package org.esa.beam.framework.gpf.internal;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.OperatorContext;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.util.jai.RasterDataNodeOpImage;

import javax.media.jai.PlanarImage;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.HashMap;
import java.util.Map;

public class GpfOpImage extends RasterDataNodeOpImage {

    private DefaultOperatorContext operatorContext;

    protected GpfOpImage(Band band, DefaultOperatorContext operatorContext) {
        super(band, createSingleBandedImageLayout(band));
        this.operatorContext = operatorContext;
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
        // todo - handle case: single sourceProduct == targetProduct
        if (operatorMustComputeTileStack()) {
            // Provide target GPF rasters and associated AWT tiles
            WritableRaster[] targetRasters = getTargetTiles(tile);
            Map<Band, org.esa.beam.framework.gpf.Tile> targetTiles = new HashMap<Band, org.esa.beam.framework.gpf.Tile>(targetRasters.length * 2);
            for (int i = 0; i < targetRasters.length; i++) {
                Band targetBand = operatorContext.getTargetProduct().getBandAt(i);
                ProductData targetData = targetBand.createCompatibleRasterData(destRect.width, destRect.height);
                org.esa.beam.framework.gpf.Tile targetTile = createTargetTile(destRect, targetData);
                targetTiles.put(targetBand, targetTile);
            }
            // Compute target GPF rasters
            System.out.println(">> computeAllBands: this = " + this + ", destRect" + destRect);
            operatorContext.getOperator().computeTileStack(targetTiles, destRect, ProgressMonitor.NULL);
            System.out.println("<< computeAllBands: this = " + this + ", destRect" + destRect);

            // Write computed target GPF rasters into associated AWT tiles
            for (int i = 0; i < targetRasters.length; i++) {
                Band targetBand = operatorContext.getTargetProduct().getBandAt(i);
                WritableRaster targetRaster = targetRasters[i];
                ProductData targetData = targetTiles.get(targetBand).getRawSampleData();
                targetRaster.setDataElements(destRect.x, destRect.y, destRect.width, destRect.height, targetData.getElems());
            }
        } else {
            // Provide target GPF raster
            ProductData targetData = getBand().createCompatibleRasterData(destRect.width, destRect.height);
            org.esa.beam.framework.gpf.Tile targetTile = createTargetTile(destRect, targetData);

            // Compute target GPF raster
            final long t0 = System.currentTimeMillis();
            System.out.println(">> computeBand: this = " + this + ", targetRectangle" + targetTile.getRectangle());
            operatorContext.getOperator().computeTile(getBand(), targetTile, ProgressMonitor.NULL);
            System.out.println("<< computeBand: time = " + (System.currentTimeMillis() - t0) + " ms");

            // Write computed target GPF raster into associated AWT tile
            tile.setDataElements(destRect.x, destRect.y, destRect.width, destRect.height, targetData.getElems());
        }
    }

    private RasterImpl createTargetTile(Rectangle destRect, ProductData targetData) {
        return new RasterImpl(getBand(), destRect, targetData);
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

    private boolean operatorMustComputeTileStack() {
        return operatorContext.canComputeTileStack()
                && !operatorContext.canComputeTile();
    }
}
