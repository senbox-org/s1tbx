package org.esa.beam.framework.gpf.internal;

import com.bc.ceres.core.Assert;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.util.jai.RasterDataNodeOpImage;

import javax.media.jai.PlanarImage;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.HashMap;
import java.util.Map;

public class OperatorImage extends RasterDataNodeOpImage {

    private OperatorContext operatorContext;

    public OperatorImage(Band targetBand, OperatorContext operatorContext) {
        super(targetBand, createSingleBandedImageLayout(targetBand));
        this.operatorContext = operatorContext;
    }

    public OperatorContext getOperatorContext() {
        return operatorContext;
    }

    public Band getTargetBand() {
        return (Band) getRasterDataNode();
    }

    @Override
    protected void computeRect(PlanarImage[] ignored, WritableRaster tile, Rectangle destRect) {
        if (operatorMustComputeTileStack()) {
            Map<Band, Tile> targetTiles = createTargetTileStack(tile, destRect);
            operatorContext.getOperator().computeTileStack(targetTiles, destRect, getProgressMonitor());
        } else {
            Tile targetTile = createTargetTile(destRect, tile);
            if (operatorContext.isComputeTileMethodUsable()) {
                operatorContext.getOperator().computeTile(getTargetBand(), targetTile, getProgressMonitor());
            }
        }
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
                if (isBandComputedByOperator(band)) {
                    targetTiles.put(band, operatorContext.getSourceTile(band, targetRectangle, getProgressMonitor()));
                }
            }
        } else {
            for (Band band : bands) {
                if (isBandComputedByOperator(band)) {
                    WritableRaster tileRaster = getWritableTile(band, targetTileRaster);
                    targetTiles.put(band, createTargetTile(band, tileRaster, targetRectangle));
                }
            }
        }
        return targetTiles;
    }

    private boolean isBandComputedByOperator(Band band) {
        return band.getImage() instanceof OperatorImage;
    }

    private WritableRaster getWritableTile(Band band, WritableRaster targetTileRaster) {
        WritableRaster tileRaster;
        if (band == getTargetBand()) {
            tileRaster = targetTileRaster;
        } else {
            RasterDataNodeOpImage rasterImage = operatorContext.getTargetImage(band);
            Assert.state(rasterImage != this);
            Assert.state(rasterImage instanceof OperatorImage);
            tileRaster = ((OperatorImage) rasterImage).getWritableTile(targetTileRaster.getBounds());
        }
        return tileRaster;
    }

    public WritableRaster getWritableTile(Rectangle tileRectangle) {
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

    private static TileImpl createTargetTile(Band band, WritableRaster targetTileRaster, Rectangle targetRectangle) {
        return new TileImpl(band, targetTileRaster, targetRectangle, true);
    }

    private boolean operatorMustComputeTileStack() {
        return operatorContext.isComputeTileStackMethodUsage()
                && !operatorContext.isComputeTileMethodUsable();
    }
}
