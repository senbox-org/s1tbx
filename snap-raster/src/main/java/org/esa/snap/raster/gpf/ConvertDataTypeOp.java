/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.raster.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.Stx;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.math.Histogram;
import org.esa.snap.core.util.math.Range;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Format-Change
 */

@OperatorMetadata(alias = "Convert-Datatype",
        category = "Raster/Data Conversion",
        authors = "Jun Lu, Luis Veci",
        version = "1.0",
        copyright = "Copyright (C) 2015 by Array Systems Computing Inc.",
        description = "Convert product data type")
public class ConvertDataTypeOp extends Operator {

    @SourceProduct(alias = "source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "The list of source bands.", alias = "sourceBands",
            rasterDataNodeType = Band.class, label = "Source Bands")
    private
    String[] sourceBandNames;

    @Parameter(valueSet = {ProductData.TYPESTRING_INT8,
            ProductData.TYPESTRING_INT16,
            ProductData.TYPESTRING_INT32,
            ProductData.TYPESTRING_UINT8,
            ProductData.TYPESTRING_UINT16,
            ProductData.TYPESTRING_UINT32,
            ProductData.TYPESTRING_FLOAT32,
            ProductData.TYPESTRING_FLOAT64
    }, defaultValue = ProductData.TYPESTRING_UINT8, label = "Target Data Type")
    private String targetDataType = ProductData.TYPESTRING_UINT8;
    private int dataType = ProductData.TYPE_UINT8;

    @Parameter(valueSet = {SCALING_TRUNCATE, SCALING_LINEAR,
            SCALING_LINEAR_CLIPPED, SCALING_LINEAR_PEAK_CLIPPED,
            SCALING_LOGARITHMIC},
            defaultValue = SCALING_LINEAR_CLIPPED, label = "Scaling")
    private String targetScalingStr = SCALING_LINEAR_CLIPPED;

    @Parameter(label = "Target no data value", defaultValue = "0")
    private Double targetNoDataValue = 0D;

    public final static String SCALING_TRUNCATE = "Truncate";
    public final static String SCALING_LINEAR = "Linear (slope and intercept)";
    public final static String SCALING_LINEAR_CLIPPED = "Linear (between 95% clipped histogram)";
    public final static String SCALING_LINEAR_PEAK_CLIPPED = "Linear (peak clipped histogram)";
    public final static String SCALING_LOGARITHMIC = "Logarithmic";

    public enum ScalingType {NONE, TRUNC, LINEAR, LINEAR_CLIPPED, LINEAR_PEAK_CLIPPED, LOGARITHMIC}

    private ScalingType targetScaling = ScalingType.LINEAR_CLIPPED;

    private final Map<Band, Stx> stxMap = new HashMap<>();

    /**
     * Initializes this operator and sets the one and only target product.
     * <p>The target product can be either defined by a field of type {@link Product} annotated with the
     * {@link TargetProduct TargetProduct} annotation or
     * by calling {@link #setTargetProduct} method.</p>
     * <p>The framework calls this method after it has created this operator.
     * Any client code that must be performed before computation of tile data
     * should be placed here.</p>
     *
     * @throws OperatorException If an error occurs during operator initialisation.
     * @see #getTargetProduct()
     */
    @Override
    public void initialize() throws OperatorException {
        ensureSingleRasterSize(sourceProduct);

        try {
            targetProduct = new Product(sourceProduct.getName(),
                    sourceProduct.getProductType(),
                    sourceProduct.getSceneRasterWidth(),
                    sourceProduct.getSceneRasterHeight());

            ProductUtils.copyProductNodes(sourceProduct, targetProduct);

            dataType = ProductData.getType(targetDataType);
            targetScaling = getScaling(targetScalingStr);

            if(targetNoDataValue == null) {
                targetNoDataValue = 0D;
            }

            addSelectedBands();

        } catch (Throwable e) {
            throw new OperatorException(e);
        }
    }

    private static ScalingType getScaling(final String scalingStr) {
        switch (scalingStr) {
            case SCALING_LINEAR:
                return ScalingType.LINEAR;
            case SCALING_LINEAR_CLIPPED:
                return ScalingType.LINEAR_CLIPPED;
            case SCALING_LINEAR_PEAK_CLIPPED:
                return ScalingType.LINEAR_PEAK_CLIPPED;
            case SCALING_LOGARITHMIC:
                return ScalingType.LOGARITHMIC;
            case SCALING_TRUNCATE:
                return ScalingType.TRUNC;
            default:
                return ScalingType.NONE;
        }
    }

    /**
     * get the selected bands
     *
     * @param sourceProduct       the input product
     * @param sourceBandNames     the select band names
     * @param includeVirtualBands include virtual bands by default
     * @return band list
     * @throws OperatorException if source band not found
     */
    public static Band[] getSourceBands(final Product sourceProduct, String[] sourceBandNames, final boolean includeVirtualBands) throws OperatorException {

        if (sourceBandNames == null || sourceBandNames.length == 0) {
            final Band[] bands = sourceProduct.getBands();
            final List<String> bandNameList = new ArrayList<>(sourceProduct.getNumBands());
            for (Band band : bands) {
                if (!(band instanceof VirtualBand) || includeVirtualBands)
                    bandNameList.add(band.getName());
            }
            sourceBandNames = bandNameList.toArray(new String[bandNameList.size()]);
        }

        final List<Band> sourceBandList = new ArrayList<>(sourceBandNames.length);
        for (final String sourceBandName : sourceBandNames) {
            final Band sourceBand = sourceProduct.getBand(sourceBandName);
            if (sourceBand != null) {
                sourceBandList.add(sourceBand);
            }
        }
        return sourceBandList.toArray(new Band[sourceBandList.size()]);
    }

    private void addSelectedBands() {
        final Band[] sourceBands = getSourceBands(sourceProduct, sourceBandNames, false);

        for (Band srcBand : sourceBands) {
            final Band targetBand = new Band(srcBand.getName(), dataType,
                    sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight());
            targetBand.setUnit(srcBand.getUnit());
            targetBand.setNoDataValue(targetNoDataValue);
            targetBand.setNoDataValueUsed(srcBand.isNoDataValueUsed());
            targetBand.setDescription(srcBand.getDescription());
            targetProduct.addBand(targetBand);
        }
    }

    private synchronized void calculateStatistics(final Band sourceBand) {
        if(stxMap.get(sourceBand) != null)
            return;

        stxMap.put(sourceBand, sourceBand.getStx());
    }

    /**
     * Called by the framework in order to compute a tile for the given target band.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetBand The target band.
     * @param targetTile The current tile associated with the target band to be computed.
     * @param pm         A progress monitor which should be used to determine computation cancelation requests.
     * @throws OperatorException If an error occurs during computation of the target raster.
     */
    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {

        try {
            final Band sourceBand = sourceProduct.getBand(targetBand.getName());
            final Tile srcTile = getSourceTile(sourceBand, targetTile.getRectangle());

            if(stxMap.get(sourceBand) == null) {
                calculateStatistics(sourceBand);
            }

            final Stx stx = stxMap.get(sourceBand);
            double origMin = stx.getMinimum();
            double origMax = stx.getMaximum();
            ScalingType scaling = verifyScaling(targetScaling, dataType);

            final double newMin = getMin(dataType);
            final double newMax = getMax(dataType);
            final double newRange = newMax - newMin;

            if (origMax <= newMax && origMin >= newMin && sourceBand.getDataType() < ProductData.TYPE_FLOAT32) {
                scaling = ScalingType.NONE;
            }

            final ProductData srcData = srcTile.getRawSamples();
            final ProductData dstData = targetTile.getRawSamples();

            final Double srcNoDataValue = sourceBand.getNoDataValue();
            final Double destNoDataValue = targetBand.getNoDataValue();

            if (scaling == ScalingType.LINEAR_PEAK_CLIPPED) {
                final Histogram histogram = new Histogram(stx.getHistogramBins(), origMin, origMax);
                final int[] bitCounts = histogram.getBinCounts();
                double rightPct = 0.025;
                for (int i = bitCounts.length - 1; i > 0; --i) {
                    if (bitCounts[i] > 10) {
                        rightPct = i / (double) bitCounts.length;
                        break;
                    }
                }
                final Range autoStretchRange = histogram.findRange(0.025, rightPct);
                origMin = autoStretchRange.getMin();
                origMax = autoStretchRange.getMax();
            } else if (scaling == ScalingType.LINEAR_CLIPPED) {
                final Histogram histogram = new Histogram(stx.getHistogramBins(), origMin, origMax);
                final Range autoStretchRange = histogram.findRangeFor95Percent();
                origMin = autoStretchRange.getMin();
                origMax = autoStretchRange.getMax();
            }
            final double origRange = origMax - origMin;

            final int numElem = dstData.getNumElems();
            double srcValue;
            for (int i = 0; i < numElem; ++i) {
                srcValue = srcData.getElemDoubleAt(i);
                if(sourceBand.isScalingApplied()) {
                    srcValue = sourceBand.scale(srcValue);
                }

                if (srcNoDataValue.equals(srcValue)) {
                    dstData.setElemDoubleAt(i, destNoDataValue);
                } else {
                    if (ScalingType.NONE.equals(scaling))
                        dstData.setElemDoubleAt(i, srcValue);
                    else if (ScalingType.TRUNC.equals(scaling))
                        dstData.setElemDoubleAt(i, truncate(srcValue, newMin, newMax));
                    else if (ScalingType.LOGARITHMIC.equals(scaling))
                        dstData.setElemDoubleAt(i, logScale(srcValue, origMin, newMin, origRange, newRange));
                    else {
                        if (srcValue > origMax)
                            srcValue = origMax;
                        if (srcValue < origMin)
                            srcValue = origMin;
                        dstData.setElemDoubleAt(i, scale(srcValue, origMin, newMin, origRange, newRange));
                    }
                }
            }

            targetTile.setRawSamples(dstData);
        } catch (Exception e) {
            throw new OperatorException(e.getMessage());
        }
    }

    private static double getMin(final int dataType) {
        switch (dataType) {
            case ProductData.TYPE_INT8:
                return Byte.MIN_VALUE;
            case ProductData.TYPE_UINT8:
                return 0;
            case ProductData.TYPE_INT16:
                return Short.MIN_VALUE;
            case ProductData.TYPE_UINT16:
                return 0;
            case ProductData.TYPE_INT32:
                return Integer.MIN_VALUE;
            case ProductData.TYPE_UINT32:
                return 0;
            case ProductData.TYPE_INT64:
                return Long.MIN_VALUE;
            case ProductData.TYPE_FLOAT32:
                return Float.MIN_VALUE;
            default:
                return Double.MIN_VALUE;
        }
    }

    private static double getMax(final int dataType) {
        switch (dataType) {
            case ProductData.TYPE_INT8:
                return Byte.MAX_VALUE;
            case ProductData.TYPE_UINT8:
                return Byte.MAX_VALUE + Byte.MAX_VALUE + 1;
            case ProductData.TYPE_INT16:
                return Short.MAX_VALUE;
            case ProductData.TYPE_UINT16:
                return Short.MAX_VALUE + Short.MAX_VALUE + 1;
            case ProductData.TYPE_INT32:
                return Integer.MAX_VALUE;
            case ProductData.TYPE_UINT32:
                return Long.MAX_VALUE;
            case ProductData.TYPE_INT64:
                return Long.MAX_VALUE;
            case ProductData.TYPE_FLOAT32:
                return Float.MAX_VALUE;
            default:
                return Double.MAX_VALUE;
        }
    }

    private static ScalingType verifyScaling(final ScalingType targetScaling, final int targetDataType) {
        // if converting up don't scale
        if (targetDataType == ProductData.TYPE_FLOAT32 || targetDataType == ProductData.TYPE_FLOAT64 ||
                targetDataType == ProductData.TYPE_INT32)
            return ScalingType.NONE;
        return targetScaling;
    }

    private static double truncate(final double origValue, final double newMin, final double newMax) {
        if (origValue > newMax)
            return newMax;
        else if (origValue < newMin)
            return newMin;
        return origValue;
    }

    private static double scale(final double origValue, final double origMin, final double newMin,
                                final double origRange, final double newRange) {
        return ((origValue - origMin) / origRange) * newRange + newMin;
    }

    private static double logScale(final double origValue, final double origMin, final double newMin,
                                   final double origRange, final double newRange) {
        return 10 * Math.log10(((origValue - origMin) / origRange) * newRange + newMin);
    }

    // for unit tests
    public void setTargetDataType(final String newType) {
        targetDataType = newType;
    }

    public void setScaling(final String newScaling) {
        targetScalingStr = newScaling;
    }

    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.snap.core.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     *
     * @see OperatorSpi#createOperator()
     * @see OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(ConvertDataTypeOp.class);
        }
    }
}
