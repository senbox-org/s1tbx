package org.esa.beam.framework.gpf.internal;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.ImageUtils;
import org.esa.beam.util.jai.JAIUtils;

import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.SourcelessOpImage;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.*;
import java.util.HashMap;
import java.util.Map;

public class OperatorImage extends SourcelessOpImage {

    private Band band;
    private OperatorContext operatorContext;
    private ProgressMonitor progressMonitor;


    public OperatorImage(Band targetBand, OperatorContext operatorContext) {
        this(targetBand, operatorContext, createSingleBandedImageLayout(targetBand));
    }

    private OperatorImage(Band targetBand, OperatorContext operatorContext, ImageLayout imageLayout) {
        super(imageLayout,
              null,
              imageLayout.getSampleModel(null),
              imageLayout.getMinX(null),
              imageLayout.getMinY(null),
              imageLayout.getWidth(null),
              imageLayout.getHeight(null));
        this.band = targetBand;
        this.operatorContext = operatorContext;
        // todo - use rendering hints
        setTileCache(JAI.getDefaultInstance().getTileCache());
    }

    public OperatorContext getOperatorContext() {
        return operatorContext;
    }

    public Band getTargetBand() {
        return band;
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
        return band.getSourceImage() instanceof OperatorImage;
    }

    private WritableRaster getWritableTile(Band band, WritableRaster targetTileRaster) {
        WritableRaster tileRaster;
        if (band == getTargetBand()) {
            tileRaster = targetTileRaster;
        } else {
            OperatorImage operatorImage = operatorContext.getTargetImage(band);
            Assert.state(operatorImage != this);
            tileRaster = operatorImage.getWritableTile(targetTileRaster.getBounds());
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


    public ProgressMonitor getProgressMonitor() {
        return progressMonitor != null ? progressMonitor : ProgressMonitor.NULL;
    }

    public void setProgressMonitor(ProgressMonitor pm) {
        this.progressMonitor = pm;
    }

    public WritableRaster createWritableRaster(Rectangle rectangle) {
        final int dataBufferType = getDataBufferType(band.getDataType());
        SampleModel sampleModel = ImageUtils.createSingleBandedSampleModel(dataBufferType, rectangle.width, rectangle.height);
        final Point location = new Point(rectangle.x, rectangle.y);
        return createWritableRaster(sampleModel, location);
    }

    public static ImageLayout createSingleBandedImageLayout(RasterDataNode rasterDataNode) {
        int dataBufferType = getDataBufferType(rasterDataNode.getDataType());
        return createSingleBandedImageLayout(rasterDataNode, dataBufferType);
    }

    public static ImageLayout createSingleBandedImageLayout(RasterDataNode rasterDataNode, int dataBufferType) {
        int width = rasterDataNode.getSceneRasterWidth();
        int height = rasterDataNode.getSceneRasterHeight();
        SampleModel sampleModel = ImageUtils.createSingleBandedSampleModel(dataBufferType,
                                                                           width,
                                                                           height);
        ColorModel colorModel = createColorModel(sampleModel);
        Dimension tileSize = getPreferredTileSize(rasterDataNode.getProduct());
        return new ImageLayout(0, 0,
                               width,
                               height,
                               0, 0,
                               tileSize.width, tileSize.height,
                               sampleModel,
                               colorModel);
    }

    private static Dimension getPreferredTileSize(Product product) {
        Dimension tileSize;
        final Dimension preferredTileSize = product.getPreferredTileSize();
        if (preferredTileSize != null) {
            tileSize = preferredTileSize;
        } else {
            tileSize = JAIUtils.computePreferredTileSize(product.getSceneRasterWidth(),
                                                         product.getSceneRasterHeight(), 1);
        }
        return tileSize;
    }

    private static int getDataBufferType(int productDataType) {
        return ImageManager.getDataBufferType(productDataType);
    }

    @Override
    public synchronized void dispose() {
        band = null;
        progressMonitor = null;
        super.dispose();
    }

    @Override
    public String toString() {
        String className = getClass().getSimpleName();
        String productName = "";
        if (band.getProduct() != null) {
            productName = ":" + band.getProduct().getName();
        }
        String bandName = "." + band.getName();
        return className + productName + bandName;
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
}
