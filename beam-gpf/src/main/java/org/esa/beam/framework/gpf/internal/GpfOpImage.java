package org.esa.beam.framework.gpf.internal;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.util.jai.RasterDataNodeOpImage;

import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFactory;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.HashMap;
import java.util.Map;

public class GpfOpImage extends RasterDataNodeOpImage {

    private OperatorContext operatorContext;

    protected GpfOpImage(Band band, OperatorContext operatorContext) {
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

    private void executeOperator(WritableRaster targetTileRaster, Rectangle targetRectangle) throws OperatorException {
        // todo - handle case: single sourceProduct == targetProduct
        if (operatorMustComputeTileStack()) {
            WritableRaster[] targetRasters = getTargetTileRasters(targetTileRaster);
            Map<Band, org.esa.beam.framework.gpf.Tile> targetTiles = new HashMap<Band, org.esa.beam.framework.gpf.Tile>(targetRasters.length * 2);
            for (int i = 0; i < targetRasters.length; i++) {
                Band targetBand = operatorContext.getTargetProduct().getBandAt(i);
                Tile targetTile = createTargetTile(targetBand, targetRasters[i], targetRectangle);
                targetTiles.put(targetBand, targetTile);
            }
            operatorContext.getOperator().computeTileStack(targetTiles, targetRectangle);
        } else {
            Tile targetTile = createTargetTile(getRasterDataNode(), targetTileRaster, targetRectangle);
            operatorContext.getOperator().computeTile(getBand(), targetTile);
        }
    }

    private static TileImpl createTargetTile(RasterDataNode rasterDataNode, WritableRaster targetTileRaster, Rectangle targetRectangle) {
        return new TileImpl(rasterDataNode, targetTileRaster, targetRectangle, true);
    }

    private WritableRaster[] getTargetTileRasters(WritableRaster targetTileRaster) {
        // Note: an array of Product.getNumBands() opImages are returned, one for each product band
        RenderedImage[] images = operatorContext.getTargetImages();
        WritableRaster[] tileRasters = new WritableRaster[images.length];
        final Rectangle rectangle = targetTileRaster.getBounds();
        for (int i = 0; i < images.length; i++) {
            RenderedImage image = images[i];
            WritableRaster tileRaster;
            if (image != this) {
                if (image instanceof RasterDataNodeOpImage) {
                    // This is the usual and expected case.
                    tileRaster = ((RasterDataNodeOpImage) image).getWritableRaster(rectangle);
                } else {
                    // Should only occur in the rare case in which a client explicitely
                    // set an image which is not a RasterDataNodeOpImage.
                    tileRaster = RasterFactory.createWritableRaster(image.getSampleModel().createCompatibleSampleModel(rectangle.width, rectangle.height), new Point(rectangle.x, rectangle.y));
                }
            } else {
                tileRaster = targetTileRaster;
            }
            tileRasters[i] = tileRaster;
        }
        return tileRasters;
    }

    private boolean operatorMustComputeTileStack() {
        return operatorContext.canComputeTileStack()
                && !operatorContext.canComputeTile();
    }
}
