/*
 * Copyright (C) 2016 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.sar.gpf.filtering;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s1tbx.sar.gpf.filtering.SpeckleFilters.*;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
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
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.gpf.FilterWindow;
import org.esa.snap.engine_utilities.gpf.InputProductValidator;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Applies a Speckle Filter to the data
 */
@OperatorMetadata(alias = "Speckle-Filter",
        category = "Radar/Speckle Filtering",
        authors = "Jun Lu, Luis Veci",
        version = "1.0",
        copyright = "Copyright (C) 2014 by Array Systems Computing Inc.",
        description = "Speckle Reduction")
public class SpeckleFilterOp extends Operator {

    @SourceProduct(alias = "source")
    private Product sourceProduct = null;
    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "The list of source bands.", alias = "sourceBands",
            rasterDataNodeType = Band.class, label = "Source Bands")
    private String[] sourceBandNames;

    @Parameter(valueSet = {NONE, BOXCAR_SPECKLE_FILTER, MEDIAN_SPECKLE_FILTER, FROST_SPECKLE_FILTER,
            GAMMA_MAP_SPECKLE_FILTER, LEE_SPECKLE_FILTER, LEE_REFINED_FILTER, LEE_SIGMA_FILTER, IDAN_FILTER},
            defaultValue = LEE_SIGMA_FILTER,
            label = "Filter")
    private String filter = LEE_SIGMA_FILTER;

    @Parameter(description = "The kernel x dimension", interval = "(1, 100]", defaultValue = "3", label = "Size X")
    private int filterSizeX = 3;

    @Parameter(description = "The kernel y dimension", interval = "(1, 100]", defaultValue = "3", label = "Size Y")
    private int filterSizeY = 3;

    @Parameter(description = "The damping factor (Frost filter only)", interval = "(0, 100]", defaultValue = "2",
            label = "Frost Damping Factor")
    private int dampingFactor = 2;

//    @Parameter(description = "The edge threshold (Refined Lee filter only)", interval = "(0, *)", defaultValue = "5000",
//            label = "Edge detection threshold")
//    private double edgeThreshold = 5000.0;

    @Parameter(defaultValue = "false", label = "Estimate Eqivalent Number of Looks")
    private boolean estimateENL = true;

    @Parameter(description = "The number of looks", interval = "(0, *)", defaultValue = "1.0",
            label = "Number of looks")
    private double enl = 1.0;

    @Parameter(valueSet = {NUM_LOOKS_1, NUM_LOOKS_2, NUM_LOOKS_3, NUM_LOOKS_4},
            defaultValue = NUM_LOOKS_1, label = "Number of Looks")
    private String numLooksStr = NUM_LOOKS_1;

    @Parameter(valueSet = {FilterWindow.SIZE_5x5, FilterWindow.SIZE_7x7, FilterWindow.SIZE_9x9, FilterWindow.SIZE_11x11,
            FilterWindow.SIZE_13x13, FilterWindow.SIZE_15x15, FilterWindow.SIZE_17x17},
            defaultValue = FilterWindow.SIZE_7x7, label = "Window Size")
    private String windowSize = FilterWindow.SIZE_7x7; // window size for all filters

    @Parameter(valueSet = {FilterWindow.SIZE_3x3, FilterWindow.SIZE_5x5}, defaultValue = FilterWindow.SIZE_3x3,
            label = "Point target window Size")
    private String targetWindowSizeStr = FilterWindow.SIZE_3x3; // window size for point target determination in Lee sigma

    @Parameter(valueSet = {SIGMA_50_PERCENT, SIGMA_60_PERCENT, SIGMA_70_PERCENT, SIGMA_80_PERCENT, SIGMA_90_PERCENT},
            defaultValue = SIGMA_90_PERCENT, label = "Point target window Size")
    private String sigmaStr = SIGMA_90_PERCENT; // sigma value in Lee sigma

    @Parameter(description = "The Adaptive Neighbourhood size", interval = "(1, 200]", defaultValue = "50",
            label = "Adaptive Neighbourhood Size")
    private int anSize = 50;

    private final Map<String, String[]> targetBandNameToSourceBandName = new HashMap<>();

    public static final String NONE = "None";
    public static final String BOXCAR_SPECKLE_FILTER = "Boxcar";
    public static final String MEDIAN_SPECKLE_FILTER = "Median";
    public static final String FROST_SPECKLE_FILTER = "Frost";
    public static final String GAMMA_MAP_SPECKLE_FILTER = "Gamma Map";
    public static final String LEE_SPECKLE_FILTER = "Lee";
    public static final String LEE_REFINED_FILTER = "Refined Lee";
    public static final String LEE_SIGMA_FILTER = "Lee Sigma";
    public static final String IDAN_FILTER = "IDAN";
    public static final String MEAN_SPECKLE_FILTER = "Mean";

    public static final String NUM_LOOKS_1 = "1";
    public static final String NUM_LOOKS_2 = "2";
    public static final String NUM_LOOKS_3 = "3";
    public static final String NUM_LOOKS_4 = "4";

    public static final String SIGMA_50_PERCENT = "0.5";
    public static final String SIGMA_60_PERCENT = "0.6";
    public static final String SIGMA_70_PERCENT = "0.7";
    public static final String SIGMA_80_PERCENT = "0.8";
    public static final String SIGMA_90_PERCENT = "0.9";

    private SpeckleFilter speckleFilter;
    private static final String PRODUCT_SUFFIX = "_Spk";

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public SpeckleFilterOp() {
    }

    /**
     * Set speckle filter. This function is used by unit test only.
     *
     * @param s The filter name.
     */
    public void SetFilter(String s) {

        if (s.equals(BOXCAR_SPECKLE_FILTER) ||
                s.equals(MEDIAN_SPECKLE_FILTER) ||
                s.equals(FROST_SPECKLE_FILTER) ||
                s.equals(GAMMA_MAP_SPECKLE_FILTER) ||
                s.equals(LEE_SPECKLE_FILTER) ||
                s.equals(LEE_REFINED_FILTER) ||
                s.equals(LEE_SIGMA_FILTER) ||
                s.equals(IDAN_FILTER)) {
            filter = s;
        } else {
            throw new OperatorException(s + " is an invalid filter name.");
        }
    }

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

        try {
            final InputProductValidator validator = new InputProductValidator(sourceProduct);
            validator.checkIfTOPSARBurstProduct(false);

            if (filter.equals(MEAN_SPECKLE_FILTER)) {
                filter = BOXCAR_SPECKLE_FILTER;
            }

            createTargetProduct();

            speckleFilter = createFilter();

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    /**
     * Create target product.
     *
     * @throws Exception The exception.
     */
    private void createTargetProduct() throws Exception {

        targetProduct = new Product(sourceProduct.getName() + PRODUCT_SUFFIX,
                sourceProduct.getProductType(),
                sourceProduct.getSceneRasterWidth(),
                sourceProduct.getSceneRasterHeight());

        ProductUtils.copyProductNodes(sourceProduct, targetProduct);

        if(filter.equals(NONE)) {
            final Band[] selectedBands = OperatorUtils.getSourceBands(sourceProduct, sourceBandNames, false);
            for (Band srcBand : selectedBands) {
                if (srcBand instanceof VirtualBand) {
                    ProductUtils.copyVirtualBand(targetProduct, (VirtualBand) srcBand, srcBand.getName());
                } else {
                    ProductUtils.copyBand(srcBand.getName(), sourceProduct, targetProduct, true);
                }
            }
        } else {
            OperatorUtils.addSelectedBands(
                    sourceProduct, sourceBandNames, targetProduct, targetBandNameToSourceBandName, true, true);
        }

        updateTargetProductMetadata();
    }

    /**
     * Update metadata in the target product.
     */
    private void updateTargetProductMetadata() {

        final MetadataElement absTgt = AbstractMetadata.getAbstractedMetadata(targetProduct);
        if(absTgt != null) {
            absTgt.setAttributeString(AbstractMetadata.SAMPLE_TYPE, "DETECTED");
        }
    }

    private SpeckleFilter createFilter() {

        switch (filter) {
            case BOXCAR_SPECKLE_FILTER:
                return new Boxcar(this, sourceProduct, targetProduct, filterSizeX, filterSizeY,
                        targetBandNameToSourceBandName);

            case MEDIAN_SPECKLE_FILTER:
                return new Median(this, sourceProduct, targetProduct, filterSizeX, filterSizeY,
                        targetBandNameToSourceBandName);

            case FROST_SPECKLE_FILTER:
                return new Frost(this, sourceProduct, targetProduct, filterSizeX, filterSizeY,
                        targetBandNameToSourceBandName, dampingFactor);

            case GAMMA_MAP_SPECKLE_FILTER:
                return new GammaMap(this, sourceProduct, targetProduct, filterSizeX, filterSizeY,
                        targetBandNameToSourceBandName, estimateENL, enl);

            case LEE_SPECKLE_FILTER:
                return new Lee(this, sourceProduct, targetProduct, filterSizeX, filterSizeY,
                        targetBandNameToSourceBandName, estimateENL, enl);

            case LEE_REFINED_FILTER:
                return new RefinedLee(this, sourceProduct, targetProduct, targetBandNameToSourceBandName);

            case LEE_SIGMA_FILTER:
                return new LeeSigma(this, sourceProduct, targetProduct, targetBandNameToSourceBandName,
                        numLooksStr, windowSize, targetWindowSizeStr, sigmaStr);

            case IDAN_FILTER:
                return new IDAN(this, sourceProduct, targetProduct, targetBandNameToSourceBandName,
                        numLooksStr, anSize);

            default:
                return null;
        }
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
            speckleFilter.computeTile(targetBand, targetTile, pm);
        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        } finally {
            pm.done();
        }
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
            super(SpeckleFilterOp.class);
        }
    }
}

