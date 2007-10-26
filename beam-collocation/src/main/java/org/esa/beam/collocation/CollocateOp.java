package org.esa.beam.collocation;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.dataop.resamp.Resampling;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;

import java.awt.*;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * Collocation operator.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
@OperatorMetadata(alias = "Collocate",
        version = "1.0",
        authors = "Ralf Quast",
        copyright = "(c) 2007 by Brockmann Consult",
        description = "Collocate two products.")
public class CollocateOp extends Operator {

    @SourceProduct(alias = "master")
    private Product masterProduct;
    @SourceProduct(alias = "slave")
    private Product slaveProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter
    private String targetProductName;
    @Parameter
    private boolean renameMasterComponents;
    @Parameter
    private boolean renameSlaveComponents;
    @Parameter
    private String masterComponentPattern;
    @Parameter
    private String slaveComponentPattern;
    @Parameter
    private Resampling resampling;

    private transient Map<Band, Band> sourceBandMap;

    @Override
    public void initialize() throws OperatorException {
        sourceBandMap = new HashMap<Band, Band>();
        targetProduct = new Product(targetProductName,
                masterProduct.getProductType(),
                masterProduct.getSceneRasterWidth(),
                masterProduct.getSceneRasterHeight());

        targetProduct.setStartTime(masterProduct.getStartTime());
        targetProduct.setEndTime(masterProduct.getEndTime());

        ProductUtils.copyFlagCodings(masterProduct, targetProduct);
        ProductUtils.copyMetadata(masterProduct, targetProduct);
        ProductUtils.copyTiePointGrids(masterProduct, targetProduct);
        ProductUtils.copyGeoCoding(masterProduct, targetProduct);

        for (final Band band : masterProduct.getBands()) {
            final Band targetBand = ProductUtils.copyBand(band.getName(), masterProduct, targetProduct);
            if (renameMasterComponents) {
                targetBand.setName(masterComponentPattern.replace("${ORIGINAL_NAME}", band.getName()));
            }
            final FlagCoding flagCoding = band.getFlagCoding();
            if (flagCoding != null) {
                targetBand.setFlagCoding(targetProduct.getFlagCoding(flagCoding.getName()));
            }
            sourceBandMap.put(targetBand, band);
        }
        ProductUtils.copyBitmaskDefs(masterProduct, targetProduct);

        for (final Band band : slaveProduct.getBands()) {
            final Band targetBand = targetProduct.addBand(band.getName(), band.getDataType());
            if (renameSlaveComponents) {
                targetBand.setName(slaveComponentPattern.replace("${ORIGINAL_NAME}", band.getName()));
            }

            targetBand.setDescription(band.getDescription());
            targetBand.setUnit(band.getUnit());
            targetBand.setScalingFactor(band.getScalingFactor());
            targetBand.setScalingOffset(band.getScalingOffset());
            targetBand.setLog10Scaled(band.isLog10Scaled());
            // todo - slaveBand.setSpectralBandIndex(band.getSpectralBandIndex());
            targetBand.setSpectralWavelength(band.getSpectralWavelength());
            targetBand.setSpectralBandwidth(band.getSpectralBandwidth());
            targetBand.setSolarFlux(band.getSolarFlux());
            targetBand.setNoDataValueUsed(band.isNoDataValueUsed());
            targetBand.setNoDataValue(band.getNoDataValue());
            // todo - slaveBand.setValidPixelExpression(band.getValidPixelExpression());

            sourceBandMap.put(targetBand, band);
        }

        // todo - slave metadata
        // todo - slave flag codings
        // todo - slave tie point grids
        // todo - slave bitmask definitions
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        final Band sourceBand = sourceBandMap.get(targetBand);

        if (sourceBand.getProduct() == slaveProduct) {
            collocateSourceBand(sourceBand, targetTile, pm);
        } else {
            targetTile.setRawSamples(getSourceTile(sourceBand, targetTile.getRectangle(), pm).getRawSamples());
        }
    }

    @Override
    public void dispose() {
        sourceBandMap = null;
    }

    private void collocateSourceBand(Band sourceBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        try {
            pm.beginTask(MessageFormat.format("collocating band {0}", sourceBand.getName()), targetTile.getHeight());

            final Resampling.Index resamplingIndex = resampling.createIndex();

            final GeoCoding sourceGeoCoding = slaveProduct.getGeoCoding();
            final GeoCoding targetGeoCoding = targetProduct.getGeoCoding();

            final int sourceRasterHeight = slaveProduct.getSceneRasterHeight();
            final int sourceRasterWidth = slaveProduct.getSceneRasterWidth();

            final Rectangle targetRectangle = targetTile.getRectangle();
            final PixelPos[] sourcePixelPositions = ProductUtils.computeSourcePixelCoordinates(sourceGeoCoding,
                    sourceRasterWidth,
                    sourceRasterHeight,
                    targetGeoCoding,
                    targetRectangle);
            final Rectangle sourceRectangle = getBoundingBox(sourcePixelPositions, sourceRasterWidth,
                    sourceRasterHeight);

            if (sourceRectangle != null) {
                final Tile sourceTile = getSourceTile(sourceBand, sourceRectangle, pm);
                final ResamplingRaster resamplingRaster = new ResamplingRaster(sourceTile);

                for (int y = targetRectangle.y, index = 0; y < targetRectangle.y + targetRectangle.height; ++y) {
                    for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; ++x, ++index) {
                        final PixelPos sourcePixelPos = sourcePixelPositions[index];

                        if (sourcePixelPos != null) {
                            resampling.computeIndex(sourcePixelPos.x, sourcePixelPos.y,
                                    sourceRasterWidth, sourceRasterHeight, resamplingIndex);
                            try {
                                targetTile.setSample(x, y, resampling.resample(resamplingRaster, resamplingIndex));
                                // todo - no data value
                            } catch (Exception e) {
                                throw new OperatorException(e.getMessage());
                            }
                        } else {
                            // todo -- no source pixel pos
                        }
                    }
                    pm.worked(1);
                }
            } else {
                // todo -- no source pixel pos for the whole target line
            }
        } finally {
            pm.done();
        }
    }

    private static Rectangle getBoundingBox(PixelPos[] pixelPositions, int maxWidth, int maxHeight) {
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (final PixelPos pixelsPos : pixelPositions) {
            if (pixelsPos != null) {
                final int x = (int) Math.floor(pixelsPos.getX());
                final int y = (int) Math.floor(pixelsPos.getY());

                if (x < minX) {
                    minX = x;
                }
                if (x > maxX) {
                    maxX = x;
                }
                if (y < minY) {
                    minY = y;
                }
                if (y > maxY) {
                    maxY = y;
                }
            }
        }
        if (minX > maxX || minY > maxY) {
            return null;
        }

        minX = Math.max(minX - 2, 0);
        maxX = Math.min(maxX + 2, maxWidth - 1);
        minY = Math.max(minY - 2, 0);
        maxY = Math.min(maxY + 2, maxHeight - 1);

        return new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    private static class ResamplingRaster implements Resampling.Raster {

        private final Tile tile;

        public ResamplingRaster(Tile tile) {
            this.tile = tile;
        }

        public final int getWidth() {
            return tile.getWidth();
        }

        public final int getHeight() {
            return tile.getHeight();
        }

        public final float getSample(int x, int y) throws Exception {
            final double sample = tile.getSampleDouble(x, y);

            if (isNoDataValue(sample)) {
                return Float.NaN;
            }

            return (float) sample;
        }

        private boolean isNoDataValue(double sample) {
            final RasterDataNode rasterDataNode = tile.getRasterDataNode();

            if (rasterDataNode.isNoDataValueUsed()) {
                if (rasterDataNode.isNoDataValueSet()) {
                    if (rasterDataNode.isScalingApplied()) {
                        return rasterDataNode.getGeophysicalNoDataValue() == sample;
                    } else {
                        return rasterDataNode.getNoDataValue() == sample;
                    }
                }
            }

            return false;
        }
    }

    /**
     * Collocation operator SPI.
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(CollocateOp.class);
        }
    }
}
