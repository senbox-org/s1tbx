/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.snap.collocation;

import com.bc.ceres.binding.Property;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.IndexCoding;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.dataop.barithm.BandArithmetic;
import org.esa.snap.core.dataop.resamp.Resampling;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.StringUtils;

import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Map;

import static java.text.MessageFormat.*;

/**
 * This operator is used to spatially collocate two data products. It requires two source products,
 * a {@code master} product which provides the Coordinate reference system and grid into which
 * the raster data sets of the {@code slave} product are resampled to.
 *
 * @author Ralf Quast
 * @author Norman Fomferra
 * @since BEAM 4.1
 */
@OperatorMetadata(alias = "Collocate",
        category = "Raster/Geometric",
        version = "1.2",
        authors = "Ralf Quast, Norman Fomferra",
        copyright = "(c) 2007-2011 by Brockmann Consult",
        description = "Collocates two products based on their geo-codings.")
public class CollocateOp extends Operator {

    public static final String SOURCE_NAME_REFERENCE = "${ORIGINAL_NAME}";
    public static final String DEFAULT_MASTER_COMPONENT_PATTERN = "${ORIGINAL_NAME}_M";
    public static final String DEFAULT_SLAVE_COMPONENT_PATTERN = "${ORIGINAL_NAME}_S";
    private static final String NEAREST_NEIGHBOUR = "NEAREST_NEIGHBOUR";
    private static final String BILINEAR_INTERPOLATION = "BILINEAR_INTERPOLATION";
    private static final String CUBIC_CONVOLUTION = "CUBIC_CONVOLUTION";

    @SourceProduct(alias = "master", description = "The source product which serves as master.")
    private Product masterProduct;

    @SourceProduct(alias = "slave", description = "The source product which serves as slave.")
    private Product slaveProduct;

    @TargetProduct(description = "The target product which will use the master's grid.")
    private Product targetProduct;

    @Parameter(defaultValue = "_collocated",
            description = "The name of the target product")
    @Deprecated
    private String targetProductName;

    @Parameter(defaultValue = "COLLOCATED",
            description = "The product type string for the target product (informal)")
    private String targetProductType;

    @Parameter(defaultValue = "true",
            description = "Whether or not components of the master product shall be renamed in the target product.")
    private boolean renameMasterComponents;

    @Parameter(defaultValue = "true",
            description = "Whether or not components of the slave product shall be renamed in the target product.")
    private boolean renameSlaveComponents;

    @Parameter(defaultValue = DEFAULT_MASTER_COMPONENT_PATTERN,
            description = "The text pattern to be used when renaming master components.")
    private String masterComponentPattern;

    @Parameter(defaultValue = DEFAULT_SLAVE_COMPONENT_PATTERN,
            description = "The text pattern to be used when renaming slave components.")
    private String slaveComponentPattern;

//    @Parameter(defaultValue = "true",
//               description = "If true, slave tie-point grids will become bands in the target product, otherwise they will be resampled to tiepoint grids again.")
//    private boolean slaveTiePointGridsBecomeBands;

    @Parameter(defaultValue = NEAREST_NEIGHBOUR,
            description = "The method to be used when resampling the slave grid onto the master grid.")
    private ResamplingType resamplingType;

    private transient Map<Band, RasterDataNode> sourceRasterMap;

    public Product getMasterProduct() {
        return masterProduct;
    }

    public void setMasterProduct(Product masterProduct) {
        this.masterProduct = masterProduct;
    }

    public Product getSlaveProduct() {
        return slaveProduct;
    }

    public void setSlaveProduct(Product slaveProduct) {
        this.slaveProduct = slaveProduct;
    }

    public String getTargetProductType() {
        return targetProductType;
    }

    public void setTargetProductType(String targetProductType) {
        this.targetProductType = targetProductType;
    }

    public boolean getRenameMasterComponents() {
        return renameMasterComponents;
    }

    public void setRenameMasterComponents(boolean renameMasterComponents) {
        this.renameMasterComponents = renameMasterComponents;
    }

    public boolean getRenameSlaveComponents() {
        return renameSlaveComponents;
    }

    public void setRenameSlaveComponents(boolean renameSlaveComponents) {
        this.renameSlaveComponents = renameSlaveComponents;
    }

    public String getMasterComponentPattern() {
        return masterComponentPattern;
    }

    public void setMasterComponentPattern(String masterComponentPattern) {
        this.masterComponentPattern = masterComponentPattern;
    }

    public String getSlaveComponentPattern() {
        return slaveComponentPattern;
    }

    public void setSlaveComponentPattern(String slaveComponentPattern) {
        this.slaveComponentPattern = slaveComponentPattern;
    }

    public ResamplingType getResamplingType() {
        return resamplingType;
    }

    public void setResamplingType(ResamplingType resamplingType) {
        this.resamplingType = resamplingType;
    }

    @Override
    public void initialize() throws OperatorException {
        validateProduct(masterProduct);
        validateProduct(slaveProduct);
        if (renameMasterComponents && StringUtils.isNullOrEmpty(masterComponentPattern)) {
            throw new OperatorException(format("Parameter ''{0}'' must be set to a non-empty string pattern.", "masterComponentPattern"));
        }
        if (renameSlaveComponents && StringUtils.isNullOrEmpty(slaveComponentPattern)) {
            throw new OperatorException(format("Parameter ''{0}'' must be set to a non-empty string pattern.", "slaveComponentPattern"));
        }

        sourceRasterMap = new HashMap<>(31);

        targetProduct = new Product(
                targetProductName != null ? targetProductName : masterProduct.getName() + "_" + slaveProduct.getName(),
                targetProductType != null ? targetProductType : masterProduct.getProductType(),
                masterProduct.getSceneRasterWidth(),
                masterProduct.getSceneRasterHeight());

        final ProductData.UTC utc1 = masterProduct.getStartTime();
        if (utc1 != null) {
            targetProduct.setStartTime(new ProductData.UTC(utc1.getMJD()));
        }
        final ProductData.UTC utc2 = masterProduct.getEndTime();
        if (utc2 != null) {
            targetProduct.setEndTime(new ProductData.UTC(utc2.getMJD()));
        }

        ProductUtils.copyMetadata(masterProduct, targetProduct);
        ProductUtils.copyTiePointGrids(masterProduct, targetProduct);

        for (final Band sourceBand : masterProduct.getBands()) {
            final Band targetBand = ProductUtils.copyBand(sourceBand.getName(), masterProduct, targetProduct, true);
            handleSampleCodings(sourceBand, targetBand, renameMasterComponents, masterComponentPattern);
            sourceRasterMap.put(targetBand, sourceBand);
        }

        if (renameMasterComponents) {
            for (final Band band : targetProduct.getBands()) {
                band.setName(masterComponentPattern.replace(SOURCE_NAME_REFERENCE, band.getName()));
            }
        }


        copyMasks(masterProduct, renameMasterComponents, masterComponentPattern);

        for (final Band sourceBand : slaveProduct.getBands()) {
            String targetBandName = sourceBand.getName();
            if (renameSlaveComponents) {
                targetBandName = slaveComponentPattern.replace(SOURCE_NAME_REFERENCE, targetBandName);
            }
            final Band targetBand = targetProduct.addBand(targetBandName, sourceBand.getDataType());
            ProductUtils.copyRasterDataNodeProperties(sourceBand, targetBand);
            handleSampleCodings(sourceBand, targetBand, renameSlaveComponents, slaveComponentPattern);
            sourceRasterMap.put(targetBand, sourceBand);
        }

        for (final TiePointGrid sourceGrid : slaveProduct.getTiePointGrids()) {
            String targetBandName = sourceGrid.getName();
            if (renameSlaveComponents) {
                targetBandName = slaveComponentPattern.replace(SOURCE_NAME_REFERENCE, targetBandName);
            }
            final Band targetBand = targetProduct.addBand(targetBandName, sourceGrid.getDataType());
            ProductUtils.copyRasterDataNodeProperties(sourceGrid, targetBand);
            sourceRasterMap.put(targetBand, sourceGrid);
        }

        for (final Band targetBandOuter : targetProduct.getBands()) {
            for (final Band targetBandInner : targetProduct.getBands()) {
                final RasterDataNode sourceRaster = sourceRasterMap.get(targetBandInner);
                if (sourceRaster != null) {
                    if (sourceRaster.getProduct() == slaveProduct) {
                        targetBandOuter.updateExpression(
                                BandArithmetic.createExternalName(sourceRaster.getName()),
                                BandArithmetic.createExternalName(targetBandInner.getName()));
                    }
                }
            }
        }

        ProductUtils.copyGeoCoding(masterProduct, targetProduct);
        copyMasks(slaveProduct, renameSlaveComponents, slaveComponentPattern);

        // todo - slave metadata!?
    }

    private void validateProduct(Product product) {
        if (product.getSceneGeoCoding() == null) {
            throw new OperatorException(format("Product ''{0}'' has no geo-coding.", product.getName()));
        }
        if (product.isMultiSizeProduct()) {
            throw new OperatorException(format("Product ''{0}'' has rasters of different sizes.", product.getName()));
        }
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTileMap, Rectangle targetRectangle, ProgressMonitor pm) throws
                                                                                                               OperatorException {
        pm.beginTask("Collocating bands...", targetProduct.getNumBands() + 1);
        try {
            final PixelPos[] sourcePixelPositions = ProductUtils.computeSourcePixelCoordinates(
                    slaveProduct.getSceneGeoCoding(),
                    slaveProduct.getSceneRasterWidth(),
                    slaveProduct.getSceneRasterHeight(),
                    masterProduct.getSceneGeoCoding(),
                    targetRectangle);
            final Rectangle sourceRectangle = getBoundingBox(
                    sourcePixelPositions,
                    slaveProduct.getSceneRasterWidth(),
                    slaveProduct.getSceneRasterHeight());
            pm.worked(1);

            for (final Band targetBand : targetProduct.getBands()) {
                checkForCancellation();
                final RasterDataNode sourceRaster = sourceRasterMap.get(targetBand);
                final Tile targetTile = targetTileMap.get(targetBand);

                collocateSourceBand(sourceRaster, sourceRectangle, sourcePixelPositions, targetTile,
                                    SubProgressMonitor.create(pm, 1));
            }
        } finally {
            pm.done();
        }
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        final RasterDataNode sourceRaster = sourceRasterMap.get(targetBand);

        if (sourceRaster.getProduct() == slaveProduct) {
            final PixelPos[] sourcePixelPositions = ProductUtils.computeSourcePixelCoordinates(
                    slaveProduct.getSceneGeoCoding(),
                    slaveProduct.getSceneRasterWidth(),
                    slaveProduct.getSceneRasterHeight(),
                    masterProduct.getSceneGeoCoding(),
                    targetTile.getRectangle());
            final Rectangle sourceRectangle = getBoundingBox(
                    sourcePixelPositions,
                    slaveProduct.getSceneRasterWidth(),
                    slaveProduct.getSceneRasterHeight());

            collocateSourceBand(sourceRaster, sourceRectangle, sourcePixelPositions, targetTile, pm);
        } else {
            targetTile.setRawSamples(getSourceTile(sourceRaster, targetTile.getRectangle()).getRawSamples());
        }
    }

    @Override
    public void dispose() {
        sourceRasterMap = null;
        super.dispose();
    }

    private void collocateSourceBand(RasterDataNode sourceBand, Rectangle sourceRectangle,
                                     PixelPos[] sourcePixelPositions,
                                     Tile targetTile, ProgressMonitor pm) throws OperatorException {
        pm.beginTask(format("collocating band {0}", sourceBand.getName()), targetTile.getHeight());
        try {
            final RasterDataNode targetBand = targetTile.getRasterDataNode();
            final Rectangle targetRectangle = targetTile.getRectangle();

            final int sourceRasterHeight = slaveProduct.getSceneRasterHeight();
            final int sourceRasterWidth = slaveProduct.getSceneRasterWidth();

            final Resampling resampling;
            if (isFlagBand(sourceBand) || isValidPixelExpressionUsed(sourceBand)) {
                resampling = ResamplingType.NEAREST_NEIGHBOUR.getResampling();
            } else {
                resampling = resamplingType.getResampling();
            }
            final Resampling.Index resamplingIndex = resampling.createIndex();
            final double noDataValue = targetBand.getGeophysicalNoDataValue();

            if (sourceRectangle != null) {
                final Tile sourceTile = getSourceTile(sourceBand, sourceRectangle);
                final ResamplingRaster resamplingRaster = new ResamplingRaster(sourceTile);

                for (int y = targetRectangle.y, index = 0; y < targetRectangle.y + targetRectangle.height; ++y) {
                    for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; ++x, ++index) {
                        final PixelPos sourcePixelPos = sourcePixelPositions[index];

                        if (sourcePixelPos != null) {
                            resampling.computeIndex(sourcePixelPos.x, sourcePixelPos.y,
                                                    sourceRasterWidth, sourceRasterHeight, resamplingIndex);
                            double sample;
                            if (resampling == Resampling.NEAREST_NEIGHBOUR) {
                                sample = sourceTile.getSampleDouble((int) resamplingIndex.i0, (int) resamplingIndex.j0);
                            } else {
                                try {
                                    sample = resampling.resample(resamplingRaster, resamplingIndex);
                                } catch (Exception e) {
                                    throw new OperatorException(e.getMessage());
                                }
                            }
                            if (Double.isNaN(sample)) {
                                sample = noDataValue;
                            }
                            targetTile.setSample(x, y, sample);
                        } else {
                            targetTile.setSample(x, y, noDataValue);
                        }
                    }
                    checkForCancellation();
                    pm.worked(1);
                }
            } else {
                for (int y = targetRectangle.y, index = 0; y < targetRectangle.y + targetRectangle.height; ++y) {
                    for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; ++x, ++index) {
                        targetTile.setSample(x, y, noDataValue);
                    }
                    checkForCancellation();
                    pm.worked(1);
                }
            }
        } finally {
            pm.done();
        }
    }

    private void copyMasks(Product sourceProduct, boolean rename, String pattern) {
        ProductNodeGroup<Mask> maskGroup = sourceProduct.getMaskGroup();
        final Mask[] masks = maskGroup.toArray(new Mask[maskGroup.getNodeCount()]);
        for (Mask mask : masks) {
            Mask.ImageType imageType = mask.getImageType();
            final Mask newmask = new Mask(mask.getName(),
                                          targetProduct.getSceneRasterWidth(),
                                          targetProduct.getSceneRasterHeight(),
                                          imageType);
            newmask.setDescription(mask.getDescription());
            for (Property property : mask.getImageConfig().getProperties()) {
                newmask.getImageConfig().setValue(property.getDescriptor().getName(), property.getValue());
            }
            if (rename) {
                newmask.setName(pattern.replace(SOURCE_NAME_REFERENCE, mask.getName()));
                for (final Band targetBand : targetProduct.getBands()) {
                    RasterDataNode srcRDN = sourceRasterMap.get(targetBand);
                    if (srcRDN != null) {
                        newmask.updateExpression(
                                BandArithmetic.createExternalName(srcRDN.getName()),
                                BandArithmetic.createExternalName(targetBand.getName()));
                    }
                }
            }
            targetProduct.getMaskGroup().add(newmask);

        }
    }

    private void handleSampleCodings(Band sourceBand, Band targetBand, boolean renameComponents, String renamePattern) {
        handleFlagCoding(sourceBand, targetBand, renameComponents, renamePattern);
        handleIndexCoding(sourceBand, targetBand, renameComponents, renamePattern);
    }

    private void handleFlagCoding(Band sourceBand, Band targetBand, boolean renameComponents, String renamePattern) {
        if (sourceBand.getFlagCoding() != null) {
            targetBand.getProduct().getFlagCodingGroup().remove(targetBand.getFlagCoding());
            targetBand.setSampleCoding(null);
        }
        setFlagCoding(targetBand, sourceBand.getFlagCoding(), renameComponents, renamePattern);
    }

    private void handleIndexCoding(Band sourceBand, Band targetBand, boolean renameComponents, String renamePattern) {
        if (sourceBand.getIndexCoding() != null) {
            targetBand.getProduct().getIndexCodingGroup().remove(targetBand.getIndexCoding());
            targetBand.setSampleCoding(null);
        }
        setIndexCoding(targetBand, sourceBand.getIndexCoding(), renameComponents, renamePattern);
    }

    private static void setFlagCoding(Band band, FlagCoding flagCoding, boolean rename, String pattern) {
        if (flagCoding != null) {
            String flagCodingName = flagCoding.getName();
            if (rename) {
                flagCodingName = pattern.replace(SOURCE_NAME_REFERENCE, flagCodingName);
            }
            final Product product = band.getProduct();
            if (!product.getFlagCodingGroup().contains(flagCodingName)) {
                addFlagCoding(product, flagCoding, flagCodingName);
            }
            band.setSampleCoding(product.getFlagCodingGroup().get(flagCodingName));
        }
    }

    private static void setIndexCoding(Band band, IndexCoding indexCoding, boolean rename, String pattern) {
        if (indexCoding != null) {
            String indexCodingName = indexCoding.getName();
            if (rename) {
                indexCodingName = pattern.replace(SOURCE_NAME_REFERENCE, indexCodingName);
            }
            final Product product = band.getProduct();
            if (!product.getIndexCodingGroup().contains(indexCodingName)) {
                addIndexCoding(product, indexCoding, indexCodingName);
            }
            band.setSampleCoding(product.getIndexCodingGroup().get(indexCodingName));
        }
    }

    private static void addFlagCoding(Product product, FlagCoding flagCoding, String flagCodingName) {
        final FlagCoding targetFlagCoding = new FlagCoding(flagCodingName);

        targetFlagCoding.setDescription(flagCoding.getDescription());
        ProductUtils.copyMetadata(flagCoding, targetFlagCoding);
        product.getFlagCodingGroup().add(targetFlagCoding);
    }

    private static void addIndexCoding(Product product, IndexCoding indexCoding, String indexCodingName) {
        final IndexCoding targetIndexCoding = new IndexCoding(indexCodingName);

        targetIndexCoding.setDescription(indexCoding.getDescription());
        ProductUtils.copyMetadata(indexCoding, targetIndexCoding);
        product.getIndexCodingGroup().add(targetIndexCoding);
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

    private static boolean isFlagBand(RasterDataNode sourceRaster) {
        return (sourceRaster instanceof Band && ((Band) sourceRaster).isFlagBand());
    }

    private static boolean isValidPixelExpressionUsed(RasterDataNode sourceRaster) {
        final String validPixelExpression = sourceRaster.getValidPixelExpression();
        return validPixelExpression != null && !validPixelExpression.trim().isEmpty();
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

        public boolean getSamples(int[] x, int[] y, double[][] samples) {
            boolean allValid = true;
            for (int i = 0; i < y.length; i++) {
                for (int j = 0; j < x.length; j++) {
                    samples[i][j] = tile.getSampleDouble(x[j], y[i]);
                    if (isNoDataValue(samples[i][j])) {
                        allValid = false;
                    }
                }
            }
            return allValid;
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
