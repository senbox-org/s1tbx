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

    private static final String ORIGINAL_NAME = "${ORIGINAL_NAME}";
    private static final String NEAREST_NEIGHBOUR = "NEAREST_NEIGHBOUR"; 
    private static final String BILINEAR_INTERPOLATION = "BILINEAR_INTERPOLATION"; 
    private static final String CUBIC_CONVOLUTION = "CUBIC_CONVOLUTION"; 

    @SourceProduct(alias = "master")
    private Product masterProduct;
    @SourceProduct(alias = "slave")
    private Product slaveProduct;

    @TargetProduct
    private Product targetProduct;
    @Parameter
    private String targetProductName;
    @Parameter(defaultValue = "true")
    private boolean createNewProduct = true;
    @Parameter
    private boolean renameMasterComponents;
    @Parameter
    private boolean renameSlaveComponents;
    @Parameter
    private String masterComponentPattern;
    @Parameter
    private String slaveComponentPattern;
    @Parameter(valueSet= {NEAREST_NEIGHBOUR, BILINEAR_INTERPOLATION, CUBIC_CONVOLUTION}, defaultValue=NEAREST_NEIGHBOUR)
    private ResamplingType resamplingType;

    private transient Map<Band, Band> sourceBandMap;

    @Override
    public void initialize() throws OperatorException {
        // todo - product type
        sourceBandMap = new HashMap<Band, Band>();

        if (createNewProduct) {
            targetProduct = new Product(targetProductName,
                    masterProduct.getProductType(),
                    masterProduct.getSceneRasterWidth(),
                    masterProduct.getSceneRasterHeight());

            targetProduct.setStartTime(masterProduct.getStartTime());
            targetProduct.setEndTime(masterProduct.getEndTime());

            ProductUtils.copyMetadata(masterProduct, targetProduct);
            ProductUtils.copyTiePointGrids(masterProduct, targetProduct);
            ProductUtils.copyGeoCoding(masterProduct, targetProduct);

            for (final Band sourceBand : masterProduct.getBands()) {
                final Band targetBand = ProductUtils.copyBand(sourceBand.getName(), masterProduct, targetProduct);
                setFlagCoding(targetBand, sourceBand.getFlagCoding(), renameMasterComponents, masterComponentPattern);
                sourceBandMap.put(targetBand, sourceBand);
            }
            if (renameMasterComponents) {
                for (final Band band : targetProduct.getBands()) {
                    band.setName(masterComponentPattern.replace(ORIGINAL_NAME, band.getName()));
                }
            }
            copyBitmaskDefs(masterProduct, renameMasterComponents, masterComponentPattern);
        } else {
            // todo - implement
            targetProduct = masterProduct;
        }

        for (final Band sourceBand : slaveProduct.getBands()) {
            String targetBandName = sourceBand.getName();
            if (renameSlaveComponents) {
                targetBandName = slaveComponentPattern.replace(ORIGINAL_NAME, targetBandName);
            }
            final Band targetBand = targetProduct.addBand(targetBandName, sourceBand.getDataType());

            targetBand.setDescription(sourceBand.getDescription());
            targetBand.setUnit(sourceBand.getUnit());
            targetBand.setScalingFactor(sourceBand.getScalingFactor());
            targetBand.setScalingOffset(sourceBand.getScalingOffset());
            targetBand.setLog10Scaled(sourceBand.isLog10Scaled());

            ProductUtils.copySpectralAttributes(sourceBand, targetBand);
            targetBand.setNoDataValueUsed(sourceBand.isNoDataValueUsed());
            targetBand.setNoDataValue(sourceBand.getNoDataValue());
            targetBand.setValidPixelExpression(sourceBand.getValidPixelExpression());

            setFlagCoding(targetBand, sourceBand.getFlagCoding(), renameSlaveComponents, slaveComponentPattern);
            sourceBandMap.put(targetBand, sourceBand);
        }
        for (final Band targetBand : targetProduct.getBands()) {
            for (final Band band : targetProduct.getBands()) {
                final Band sourceBand = sourceBandMap.get(band);
                if (sourceBand != null) {
                    if (sourceBand.getProduct() == slaveProduct) {
                        targetBand.updateExpression(sourceBand.getName(), band.getName());
                    }
                }
            }
        }
        copyBitmaskDefs(slaveProduct, renameSlaveComponents, slaveComponentPattern);

        // todo - slave metadata
        // todo - slave tie point grids
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

            final Resampling resampling = resamplingType.getResampling();
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

            final RasterDataNode targetBand = targetTile.getRasterDataNode();
            final float noDataValue = (float) targetBand.getGeophysicalNoDataValue();

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
                                float sample = resampling.resample(resamplingRaster, resamplingIndex);
                                if (Float.isNaN(sample)) {
                                    sample = noDataValue;
                                }
                                targetTile.setSample(x, y, sample);
                            } catch (Exception e) {
                                throw new OperatorException(e.getMessage());
                            }
                        } else {
                            targetTile.setSample(x, y, noDataValue);
                        }
                    }
                    pm.worked(1);
                }
            } else {
                for (int y = targetRectangle.y, index = 0; y < targetRectangle.y + targetRectangle.height; ++y) {
                    for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; ++x, ++index) {
                        targetTile.setSample(x, y, noDataValue);
                    }
                }
            }
        } finally {
            pm.done();
        }
    }

    private void copyBitmaskDefs(Product sourceProduct, boolean rename, String pattern) {
        for (final BitmaskDef sourceBitmaskDef : sourceProduct.getBitmaskDefs()) {
            final BitmaskDef targetBitmaskDef = sourceBitmaskDef.createCopy();
            if (rename) {
                targetBitmaskDef.setName(pattern.replace(ORIGINAL_NAME, sourceBitmaskDef.getName()));
                for (final Band targetBand : targetProduct.getBands()) {
                    targetBitmaskDef.updateExpression(sourceBandMap.get(targetBand).getName(), targetBand.getName());
                }
            }
            targetProduct.addBitmaskDef(targetBitmaskDef);
        }
    }

    private static void setFlagCoding(Band band, FlagCoding flagCoding, boolean rename, String pattern) {
        if (flagCoding != null) {
            String flagCodingName = flagCoding.getName();
            if (rename) {
                flagCodingName = pattern.replace(ORIGINAL_NAME, flagCodingName);
            }
            final Product product = band.getProduct();
            if (!product.containsFlagCoding(flagCodingName)) {
                addFlagCoding(product, flagCoding, flagCodingName);
            }
            band.setFlagCoding(product.getFlagCoding(flagCodingName));
        }
    }

    private static void addFlagCoding(Product product, FlagCoding flagCoding, String flagCodingName) {
        final FlagCoding targetFlagCoding = new FlagCoding(flagCodingName);

        targetFlagCoding.setDescription(flagCoding.getDescription());
        ProductUtils.copyMetadata(flagCoding, targetFlagCoding);
        product.addFlagCoding(targetFlagCoding);
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
                if (rasterDataNode.isScalingApplied()) {
                    return rasterDataNode.getGeophysicalNoDataValue() == sample;
                } else {
                    return rasterDataNode.getNoDataValue() == sample;
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
