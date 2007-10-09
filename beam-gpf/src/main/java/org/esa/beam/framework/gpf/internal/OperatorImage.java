package org.esa.beam.framework.gpf.internal;

import com.bc.ceres.core.Assert;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.util.jai.RasterDataNodeOpImage;

import javax.media.jai.PlanarImage;
import java.awt.Rectangle;
import java.awt.image.WritableRaster;
import java.util.HashMap;
import java.util.Map;

public class OperatorImage extends RasterDataNodeOpImage {

    private OperatorContext operatorContext;

    protected OperatorImage(Band targetBand, OperatorContext operatorContext) {
        super(targetBand, createSingleBandedImageLayout(targetBand));
        this.operatorContext = operatorContext;
    }

    public Band getTargetBand() {
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

    private void executeOperator(WritableRaster targetTileRaster, Rectangle targetRectangle) throws OperatorException {
        if (operatorMustComputeTileStack()) {
            Map<Band, Tile> targetTiles = createTargetTileStack(targetTileRaster, targetRectangle);
            operatorContext.getOperator().computeTileStack(targetTiles, targetRectangle);
        } else {
            Tile targetTile = createTargetTile(targetRectangle, targetTileRaster);
            operatorContext.getOperator().computeTile(getTargetBand(), targetTile);
        }
    }

    private Tile createTargetTile(Rectangle targetRectangle, WritableRaster targetTileRaster) {
        Tile targetTile;
        if (operatorContext.isPassThrough()) {
            targetTile = operatorContext.getSourceTile(getTargetBand(), targetRectangle);
        } else {
            targetTile = createTargetTile(getTargetBand(), targetTileRaster, targetRectangle);
        }
        return targetTile;
    }

    private Map<Band, Tile> createTargetTileStack(WritableRaster targetTileRaster, Rectangle targetRectangle) {
        Band[] bands = operatorContext.getTargetProduct().getBands();
        Map<Band, Tile> targetTiles = new HashMap<Band, Tile>(bands.length * 2);
        if (operatorContext.isPassThrough()) {
            for (Band band : bands) {
                targetTiles.put(band, operatorContext.getSourceTile(band, targetRectangle));
            }
        } else {
            for (Band band : bands) {
                WritableRaster tileRaster = getTargetTileRaster(band, targetTileRaster, targetRectangle);
                targetTiles.put(band, createTargetTile(band, tileRaster, targetRectangle));
            }
        }
        return targetTiles;
    }

    private WritableRaster getTargetTileRaster(Band band, WritableRaster targetTileRaster, Rectangle targetRectangle) {
        WritableRaster tileRaster;
        if (band == getTargetBand()) {
            tileRaster = targetTileRaster;
        } else {
            OperatorImage image = operatorContext.getTargetImage(band);
            Assert.notNull(image);
            Assert.state(image != this);
            tileRaster = image.getWritableRaster(targetRectangle);
        }
        return tileRaster;
    }

    private static TileImpl createTargetTile(Band band, WritableRaster targetTileRaster, Rectangle targetRectangle) {
        return new TileImpl(band, targetTileRaster, targetRectangle, true);
    }

    private boolean operatorMustComputeTileStack() {
        return operatorContext.canComputeTileStack()
                && !operatorContext.canComputeTile();
    }
}
