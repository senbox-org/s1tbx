/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.Stx;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.math.Histogram;
import org.esa.beam.util.math.Range;

/**
 * Format-Change
 */

@OperatorMetadata(alias="Convert-Datatype",
        category = "Utilities\\Data Conversion",
        authors = "Jun Lu, Luis Veci",
        copyright = "Copyright (C) 2013 by Array Systems Computing Inc.",
        description="Convert product data type")
public class ConvertDataTypeOp extends Operator {

    @SourceProduct(alias="source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "The list of source bands.", alias = "sourceBands", itemAlias = "band",
            rasterDataNodeType = Band.class, label="Source Bands")
    private
    String[] sourceBandNames;

    @Parameter(valueSet = { ProductData.TYPESTRING_INT8,
                            ProductData.TYPESTRING_INT16,
                            ProductData.TYPESTRING_INT32,
                            ProductData.TYPESTRING_UINT8,
                            ProductData.TYPESTRING_UINT16,
                            ProductData.TYPESTRING_UINT32,
                            ProductData.TYPESTRING_FLOAT32,
                            ProductData.TYPESTRING_FLOAT64
            }, defaultValue = ProductData.TYPESTRING_UINT8 , label="Target Data Type")
    private String targetDataType = ProductData.TYPESTRING_UINT8;
    private int dataType = ProductData.TYPE_UINT8;

    @Parameter(valueSet = { SCALING_TRUNCATE, SCALING_LINEAR,
                            SCALING_LINEAR_CLIPPED, SCALING_LINEAR_PEAK_CLIPPED,
                            SCALING_LOGARITHMIC },
                            defaultValue = SCALING_LINEAR_CLIPPED, label="Scaling")
    private String targetScalingStr = SCALING_LINEAR_CLIPPED;

    protected final static String SCALING_TRUNCATE = "Truncate";
    protected final static String SCALING_LINEAR = "Linear (slope and intercept)";
    protected final static String SCALING_LINEAR_CLIPPED = "Linear (between 95% clipped histogram)";
    protected final static String SCALING_LINEAR_PEAK_CLIPPED = "Linear (peak clipped histogram)";
    protected final static String SCALING_LOGARITHMIC = "Logarithmic";

    private enum ScalingType { NONE, TRUNC, LINEAR, LINEAR_CLIPPED, LINEAR_PEAK_CLIPPED, LOGARITHMIC }
    private ScalingType targetScaling = ScalingType.LINEAR_CLIPPED;

    /**
     * Initializes this operator and sets the one and only target product.
     * <p>The target product can be either defined by a field of type {@link org.esa.beam.framework.datamodel.Product} annotated with the
     * {@link org.esa.beam.framework.gpf.annotations.TargetProduct TargetProduct} annotation or
     * by calling {@link #setTargetProduct} method.</p>
     * <p>The framework calls this method after it has created this operator.
     * Any client code that must be performed before computation of tile data
     * should be placed here.</p>
     *
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs during operator initialisation.
     * @see #getTargetProduct()
     */
    @Override
    public void initialize() throws OperatorException {

        try {
            targetProduct = new Product(sourceProduct.getName(),
                                        sourceProduct.getProductType(),
                                        sourceProduct.getSceneRasterWidth(),
                                        sourceProduct.getSceneRasterHeight());

            OperatorUtils.copyProductNodes(sourceProduct, targetProduct);

            dataType = ProductData.getType(targetDataType);
            targetScaling = getScaling(targetScalingStr);

            addSelectedBands();

        } catch(Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private static ScalingType getScaling(final String scalingStr) {
        if(scalingStr.equals(SCALING_LINEAR))
            return ScalingType.LINEAR;
        else if(scalingStr.equals(SCALING_LINEAR_CLIPPED))
            return ScalingType.LINEAR_CLIPPED;
        else if(scalingStr.equals(SCALING_LINEAR_PEAK_CLIPPED))
            return ScalingType.LINEAR_PEAK_CLIPPED;
        else if(scalingStr.equals(SCALING_LOGARITHMIC))
            return ScalingType.LOGARITHMIC;
        else if(scalingStr.equals(SCALING_TRUNCATE))
            return ScalingType.TRUNC;
        else
            return ScalingType.NONE;
    }

    private void addSelectedBands() {
        final Band[] sourceBands = OperatorUtils.getSourceBands(sourceProduct, sourceBandNames);

        for(Band srcBand : sourceBands) {
            final Band targetBand = new Band(srcBand.getName(), dataType,
                    sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight());
            targetBand.setUnit(srcBand.getUnit());
            targetBand.setDescription(srcBand.getDescription());
            targetProduct.addBand(targetBand);
        }
    }

    /**
     * Called by the framework in order to compute a tile for the given target band.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetBand The target band.
     * @param targetTile The current tile associated with the target band to be computed.
     * @param pm         A progress monitor which should be used to determine computation cancelation requests.
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs during computation of the target raster.
     */
    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {

        try {
            final Band sourceBand = sourceProduct.getBand(targetBand.getName());
            final Tile srcTile = getSourceTile(sourceBand, targetTile.getRectangle());

            final Stx stx = sourceBand.getStx();
            double origMin = stx.getMinimum();
            double origMax = stx.getMaximum();
            ScalingType scaling = verifyScaling(targetScaling, dataType);

            final double newMin = getMin(dataType);
            final double newMax = getMax(dataType);
            final double newRange = newMax - newMin;

            if(origMax <= newMax && origMin >= newMin && sourceBand.getDataType() < ProductData.TYPE_FLOAT32)
                scaling = ScalingType.NONE;
            
            final ProductData srcData = srcTile.getRawSamples();
            final ProductData dstData = targetTile.getRawSamples();

            final double srcNoDataValue = sourceBand.getNoDataValue();
            final double destNoDataValue = targetBand.getNoDataValue();

            if(scaling == ScalingType.LINEAR_PEAK_CLIPPED) {
                final Histogram histogram = new Histogram(stx.getHistogramBins(), origMin, origMax);
                final int[] bitCounts = histogram.getBinCounts();
                double rightPct = 0.025;
                for(int i=bitCounts.length-1; i > 0; --i) {
                    if(bitCounts[i] > 10) {
                        rightPct = i / (double)bitCounts.length;
                        break;
                    }
                }
                final Range autoStretchRange = histogram.findRange(0.025, rightPct);
                origMin = autoStretchRange.getMin();
                origMax = autoStretchRange.getMax();
            } else if(scaling == ScalingType.LINEAR_CLIPPED) {
                final Histogram histogram = new Histogram(stx.getHistogramBins(), origMin, origMax);
                final Range autoStretchRange = histogram.findRangeFor95Percent();
                origMin = autoStretchRange.getMin();
                origMax = autoStretchRange.getMax();
            }
            final double origRange = origMax - origMin;

            final int numElem = dstData.getNumElems();
            double srcValue;
            for(int i=0; i < numElem; ++i) {
                srcValue = srcData.getElemDoubleAt(i);
                if(srcValue == srcNoDataValue) {
                    dstData.setElemDoubleAt(i, destNoDataValue);
                } else {
                    if(scaling == ScalingType.NONE)
                        dstData.setElemDoubleAt(i, srcValue);
                    else if(scaling == ScalingType.TRUNC)
                        dstData.setElemDoubleAt(i, truncate(srcValue, newMin, newMax));
                    else if(scaling == ScalingType.LOGARITHMIC)
                        dstData.setElemDoubleAt(i, logScale(srcValue, origMin, newMin, origRange, newRange));
                    else {
                        if(srcValue > origMax)
                            srcValue = origMax;
                        if(srcValue < origMin)
                            srcValue = origMin;
                        dstData.setElemDoubleAt(i, scale(srcValue, origMin, newMin, origRange, newRange));
                    }
                }
            }

            targetTile.setRawSamples(dstData);
        } catch(Exception e) {
            throw new OperatorException(e.getMessage());
        }
    }

    private static double getMin(final int dataType) {
        switch(dataType) {
            case ProductData.TYPE_INT8:
                return Byte.MIN_VALUE;
            case ProductData.TYPE_INT16:
                return Short.MIN_VALUE;
            case ProductData.TYPE_INT32:
                return Integer.MIN_VALUE;
            case ProductData.TYPE_UINT8:
                return 0;
            case ProductData.TYPE_UINT16:
                return 0;
            case ProductData.TYPE_UINT32:
                return 0;
            case ProductData.TYPE_FLOAT32:
                return Float.MIN_VALUE;
            default:
                return Double.MIN_VALUE;
        }
    }

    private static double getMax(final int dataType) {
        switch(dataType) {
            case ProductData.TYPE_INT8:
                return Byte.MAX_VALUE;
            case ProductData.TYPE_INT16:
                return Short.MAX_VALUE;
            case ProductData.TYPE_INT32:
                return Integer.MAX_VALUE;
            case ProductData.TYPE_UINT8:
                return Byte.MAX_VALUE + Byte.MAX_VALUE +1;
            case ProductData.TYPE_UINT16:
                return Short.MAX_VALUE + Short.MAX_VALUE +1;
            case ProductData.TYPE_UINT32:
                return Long.MAX_VALUE;
            case ProductData.TYPE_FLOAT32:
                return Float.MAX_VALUE;
            default:
                return Double.MAX_VALUE;
        }
    }

    private static ScalingType verifyScaling(final ScalingType targetScaling, final int targetDataType) {
        // if converting up don't scale
        if(targetDataType == ProductData.TYPE_FLOAT32 || targetDataType == ProductData.TYPE_FLOAT64 ||
           targetDataType == ProductData.TYPE_INT32)
            return ScalingType.NONE;
        return targetScaling;
    }

    private static double truncate(final double origValue, final double newMin, final double newMax) {
        if(origValue > newMax)
            return newMax;
        else if(origValue < newMin)
            return newMin;
        return origValue;
    }

    private static double scale(final double origValue, final double origMin, final double newMin,
                                final double origRange, final double newRange) {
        return ((origValue - origMin) / origRange) * newRange + newMin;
    }

    private static double logScale(final double origValue, final double origMin, final double newMin,
                                final double origRange, final double newRange) {
        return 10*Math.log10(((origValue - origMin) / origRange) * newRange + newMin);
    }

    // for unit tests
    protected void setTargetDataType(final String newType) {
        targetDataType = newType;
    }

    protected void setScaling(final String newScaling) {
        targetScalingStr = newScaling;
    }

    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.beam.framework.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     * @see org.esa.beam.framework.gpf.OperatorSpi#createOperator()
     * @see org.esa.beam.framework.gpf.OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(ConvertDataTypeOp.class);
        }
    }
}