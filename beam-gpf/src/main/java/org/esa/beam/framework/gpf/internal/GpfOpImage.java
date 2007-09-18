package org.esa.beam.framework.gpf.internal;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.OperatorContext;
import org.esa.beam.framework.gpf.OperatorException;

import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.HashMap;
import java.util.Map;

public class GpfOpImage extends RasterDataNodeOpImage {

    private OperatorContext operatorContext;

    protected GpfOpImage(OperatorContext operatorContext,
                         ImageLayout imageLayout,
                         SampleModel sampleModel,
                         Band band,
                         ProgressMonitor progressMonitor) {
        super(imageLayout, sampleModel, band);
        this.operatorContext = operatorContext;
        setTileCache(JAI.getDefaultInstance().getTileCache());
    }

    public static GpfOpImage create(OperatorContext operatorContext,
                                    Band band,
                                    ProgressMonitor progressMonitor) {
        SampleModel sampleModel = ImageHelpers.createSingleBandSampleModel(band);
        ImageLayout imageLayout = ImageHelpers.createSingleBandImageLayout(band, sampleModel);
        return new GpfOpImage(operatorContext,
                              imageLayout,
                              sampleModel,
                              band,
                              progressMonitor);
    }

    public OperatorContext getOperatorContext() {
        return operatorContext;
    }

    public Band getBand() {
        return (Band) getRasterDataNode();
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
            WritableRaster[] targetTiles = getTargetTiles(tile);
            Map<Band, org.esa.beam.framework.gpf.Raster> targetRasters = new HashMap<Band, org.esa.beam.framework.gpf.Raster>(targetTiles.length * 2);
            for (int i = 0; i < targetTiles.length; i++) {
                Band targetBand = operatorContext.getTargetProduct().getBandAt(i);
                ProductData targetData = targetBand.createCompatibleRasterData(destRect.width, destRect.height);
                org.esa.beam.framework.gpf.Raster targetRaster = new RasterImpl(getBand(), destRect, targetData);
                targetRasters.put(targetBand, targetRaster);
            }
            operatorContext.getOperator().computeAllBands(targetRasters, destRect, ProgressMonitor.NULL);
            for (int i = 0; i < targetTiles.length; i++) {
                Band targetBand = operatorContext.getTargetProduct().getBandAt(i);
                WritableRaster targetTile = targetTiles[i];
                ProductData targetData = targetRasters.get(targetBand).getDataBuffer();
                targetTile.setDataElements(destRect.x, destRect.y, destRect.width, destRect.height, targetData.getElems());
            }
        } else {
            ProductData targetData = getBand().createCompatibleRasterData(destRect.width, destRect.height);
            org.esa.beam.framework.gpf.Raster targetRaster = new RasterImpl(getBand(), destRect, targetData);
            operatorContext.getOperator().computeBand(getBand(), targetRaster, ProgressMonitor.NULL);
            tile.setDataElements(destRect.x, destRect.y, destRect.width, destRect.height, targetData.getElems());
        }
    }

    private WritableRaster[] getTargetTiles(WritableRaster tile) {
        // Note: an array of Product.getNumBands() opImages are returned, one for each product band
        GpfOpImage[] opImages = operatorContext.getOpImages();
        WritableRaster[] tiles = new WritableRaster[opImages.length];
        for (int i = 0; i < opImages.length; i++) {
            GpfOpImage opImage = opImages[i];
            if (opImage != this) {
                tiles[i] = opImage.getTargetTile(tile.getBounds());
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
        return operatorContext.getClassInfo().isAllBandsMethodImplemented()
                && !operatorContext.getClassInfo().isBandMethodImplemented();
    }
}
