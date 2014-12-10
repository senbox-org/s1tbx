/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.csa.rstb.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.csa.rstb.gpf.specklefilters.BoxCar;
import org.csa.rstb.gpf.specklefilters.IDAN;
import org.csa.rstb.gpf.specklefilters.LeeSigma;
import org.csa.rstb.gpf.specklefilters.RefinedLee;
import org.csa.rstb.gpf.specklefilters.SpeckleFilter;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;
import org.esa.nest.dataio.PolBandUtils;
import org.esa.snap.datamodel.AbstractMetadata;
import org.esa.snap.gpf.OperatorUtils;

import java.awt.*;
import java.util.*;

/**
 * Applies a Polarimetric Speckle Filter to the data (covariance/coherency matrix data)
 */
@OperatorMetadata(alias = "Polarimetric-Speckle-Filter",
        category = "SAR Processing/Polarimetric",
        authors = "Jun Lu, Luis Veci",
        copyright = "Copyright (C) 2014 by Array Systems Computing Inc.",
        description = "Polarimetric Speckle Reduction")
public class PolarimetricSpeckleFilterOp extends Operator {

    @SourceProduct(alias = "source")
    private Product sourceProduct = null;
    @TargetProduct
    private Product targetProduct;

    @Parameter(valueSet = {BOXCAR_SPECKLE_FILTER, IDAN_FILTER, REFINED_LEE_FILTER, LEE_SIGMA_FILTER},
            defaultValue = REFINED_LEE_FILTER, label = "Filter")
    private String filter;

    @Parameter(description = "The filter size", interval = "(1, 100]", defaultValue = "5", label = "Filter Size")
    private int filterSize = 5;

    @Parameter(valueSet = {NUM_LOOKS_1, NUM_LOOKS_2, NUM_LOOKS_3, NUM_LOOKS_4},
            defaultValue = NUM_LOOKS_1, label = "Number of Looks")
    private String numLooksStr = NUM_LOOKS_1;

    @Parameter(valueSet = {WINDOW_SIZE_5x5, WINDOW_SIZE_7x7, WINDOW_SIZE_9x9, WINDOW_SIZE_11x11},
            defaultValue = WINDOW_SIZE_7x7, label = "Window Size")
    private String windowSize = WINDOW_SIZE_7x7; // window size for all filters

    @Parameter(valueSet = {WINDOW_SIZE_3x3, WINDOW_SIZE_5x5}, defaultValue = WINDOW_SIZE_3x3,
            label = "Point target window Size")
    private String targetWindowSizeStr = WINDOW_SIZE_3x3; // window size for point target determination in Lee sigma

    @Parameter(description = "The Adaptive Neighbourhood size", interval = "(1, 200]", defaultValue = "50",
            label = "Adaptive Neighbourhood Size")
    private int anSize = 50;

    @Parameter(valueSet = {LeeSigma.SIGMA_50_PERCENT, LeeSigma.SIGMA_60_PERCENT, LeeSigma.SIGMA_70_PERCENT, LeeSigma.SIGMA_80_PERCENT, LeeSigma.SIGMA_90_PERCENT},
            defaultValue = LeeSigma.SIGMA_90_PERCENT, label = "Point target window Size")
    private String sigmaStr = LeeSigma.SIGMA_90_PERCENT; // sigma value in Lee sigma

    private PolBandUtils.PolSourceBand[] srcBandList;

    private int sourceImageWidth = 0;
    private int sourceImageHeight = 0;
    private PolBandUtils.MATRIX sourceProductType = null;

    public static final String BOXCAR_SPECKLE_FILTER = "Box Car Filter";
    public static final String REFINED_LEE_FILTER = "Refined Lee Filter";
    public static final String IDAN_FILTER = "IDAN Filter";
    public static final String LEE_SIGMA_FILTER = "Improved Lee Sigma Filter";

    public static final String WINDOW_SIZE_3x3 = "3x3";
    public static final String WINDOW_SIZE_5x5 = "5x5";
    public static final String WINDOW_SIZE_7x7 = "7x7";
    public static final String WINDOW_SIZE_9x9 = "9x9";
    public static final String WINDOW_SIZE_11x11 = "11x11";

    public static final String NUM_LOOKS_1 = "1";
    public static final String NUM_LOOKS_2 = "2";
    public static final String NUM_LOOKS_3 = "3";
    public static final String NUM_LOOKS_4 = "4";

    private SpeckleFilter speckleFilter;

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public PolarimetricSpeckleFilterOp() {
    }

    /**
     * Set speckle filter. This function is used by unit test only.
     *
     * @param s The filter name.
     */
    public void SetFilter(final String s) {

        if (s.equals(BOXCAR_SPECKLE_FILTER) || s.equals(IDAN_FILTER) ||
                s.equals(REFINED_LEE_FILTER) || s.equals(LEE_SIGMA_FILTER)) {
            filter = s;
        } else {
            throw new OperatorException(s + " is an invalid filter name.");
        }
    }

    /**
     * Initializes this operator and sets the one and only target product.
     * <p>The target product can be either defined by a field of type {@link org.esa.beam.framework.datamodel.Product} annotated with the
     * {@link org.esa.beam.framework.gpf.annotations.TargetProduct TargetProduct} annotation or
     * by calling {@link #setTargetProduct} method.</p>
     * <p>The framework calls this method after it has created this operator.
     * Any client code that must be performed before computation of tile data
     * should be placed here.</p>
     *
     * @throws org.esa.beam.framework.gpf.OperatorException If an error occurs during operator initialisation.
     * @see #getTargetProduct()
     */
    @Override
    public void initialize() throws OperatorException {

        try {
            getSourceImageDimension();

            sourceProductType = PolBandUtils.getSourceProductType(sourceProduct);

            srcBandList = PolBandUtils.getSourceBands(sourceProduct, sourceProductType);

            createTargetProduct();

            getWindowSize();

            speckleFilter = createFilter();

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private void getWindowSize() {
        switch (windowSize) {
            case WINDOW_SIZE_5x5:
                filterSize = 5;
                break;
            case WINDOW_SIZE_7x7:
                filterSize = 7;
                break;
            case WINDOW_SIZE_9x9:
                filterSize = 9;
                break;
            case WINDOW_SIZE_11x11:
                filterSize = 11;
                break;
            default:
                throw new OperatorException("Unknown window size: " + windowSize);
        }
    }

    private SpeckleFilter createFilter() {
        final int numLooks = Integer.parseInt(numLooksStr);

        switch (filter) {
            case BOXCAR_SPECKLE_FILTER:
                return new BoxCar(this, sourceProduct, targetProduct, sourceProductType, srcBandList, filterSize);
            case REFINED_LEE_FILTER:
                return new RefinedLee(this, sourceProduct, targetProduct, sourceProductType, srcBandList, filterSize,
                                      numLooks);
            case IDAN_FILTER:
                return new IDAN(this, sourceProduct, targetProduct, sourceProductType, srcBandList, anSize,
                                numLooks);
            case LEE_SIGMA_FILTER:
                return new LeeSigma(this, sourceProduct, targetProduct, sourceProductType, srcBandList, filterSize,
                                    numLooks, sigmaStr, targetWindowSizeStr);
            default:
                return null;
        }
    }

    /**
     * Get source image dimension.
     */
    private void getSourceImageDimension() {
        sourceImageWidth = sourceProduct.getSceneRasterWidth();
        sourceImageHeight = sourceProduct.getSceneRasterHeight();
    }

    /**
     * Create target product.
     */
    private void createTargetProduct() {

        targetProduct = new Product(sourceProduct.getName(), sourceProduct.getProductType(),
                                    sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight());

        ProductUtils.copyProductNodes(sourceProduct, targetProduct);

        addSelectedBands();

        AbstractMetadata.getAbstractedMetadata(targetProduct).setAttributeInt(AbstractMetadata.polsarData, 1);
    }

    private void addSelectedBands() throws OperatorException {

        String[] bandNames = null;
        boolean copyInputBands = false;
        if (sourceProductType == PolBandUtils.MATRIX.FULL) {
            bandNames = PolBandUtils.getT3BandNames();
        } else {
            copyInputBands = true;
        }

        for (PolBandUtils.PolSourceBand bandList : srcBandList) {
            String suffix = bandList.suffix;
            if (copyInputBands) {
                bandNames = new String[bandList.srcBands.length];
                int i = 0;
                for (Band band : bandList.srcBands) {
                    bandNames[i++] = band.getName();
                }
                suffix = "";
            }
            final Band[] targetBands = OperatorUtils.addBands(targetProduct, bandNames, suffix);
            bandList.addTargetBands(targetBands);
        }
    }

    /**
     * Called by the framework in order to compute the stack of tiles for the given target bands.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetTiles     The current tiles to be computed for each target band.
     * @param targetRectangle The area in pixel coordinates to be computed (same for all rasters in <code>targetRasters</code>).
     * @param pm              A progress monitor which should be used to determine computation cancelation requests.
     * @throws org.esa.beam.framework.gpf.OperatorException if an error occurs during computation of the target rasters.
     */
    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm)
            throws OperatorException {

        try {
            speckleFilter.computeTiles(targetTiles, targetRectangle, getSourceTileRectangle(targetRectangle));

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        } finally {
            pm.done();
        }
    }

    /**
     * Get source tile rectangle.
     *
     * @param rect X target tile rectangle.
     * @return The source tile rectangle.
     */
    private Rectangle getSourceTileRectangle(final Rectangle rect) {

        int sx0 = rect.x;
        int sy0 = rect.y;
        int sw = rect.width;
        int sh = rect.height;
        final int halfFilterSize = filterSize / 2;

        if (rect.x >= halfFilterSize) {
            sx0 -= halfFilterSize;
            sw += halfFilterSize;
        }
        if (rect.y >= halfFilterSize) {
            sy0 -= halfFilterSize;
            sh += halfFilterSize;
        }
        if (rect.x + rect.width + halfFilterSize <= sourceImageWidth) {
            sw += halfFilterSize;
        }
        if (rect.y + rect.height + halfFilterSize <= sourceImageHeight) {
            sh += halfFilterSize;
        }

        return new Rectangle(sx0, sy0, sw, sh);
    }

    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.beam.framework.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     *
     * @see OperatorSpi#createOperator()
     * @see OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(PolarimetricSpeckleFilterOp.class);
        }
    }
}

