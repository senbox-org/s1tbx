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
import org.csa.rstb.gpf.classifiers.CloudePottier;
import org.csa.rstb.gpf.classifiers.FreemanDurdenWishart;
import org.csa.rstb.gpf.classifiers.HAlphaWishart;
import org.csa.rstb.gpf.classifiers.HAlphaWishartC2;
import org.csa.rstb.gpf.classifiers.PolClassifier;
import org.esa.s1tbx.dataio.PolBandUtils;
import org.esa.snap.datamodel.AbstractMetadata;
import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.framework.datamodel.IndexCoding;
import org.esa.snap.framework.datamodel.MetadataElement;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.ProductData;
import org.esa.snap.framework.gpf.Operator;
import org.esa.snap.framework.gpf.OperatorException;
import org.esa.snap.framework.gpf.OperatorSpi;
import org.esa.snap.framework.gpf.Tile;
import org.esa.snap.framework.gpf.annotations.OperatorMetadata;
import org.esa.snap.framework.gpf.annotations.Parameter;
import org.esa.snap.framework.gpf.annotations.SourceProduct;
import org.esa.snap.framework.gpf.annotations.TargetProduct;
import org.esa.snap.gpf.InputProductValidator;
import org.esa.snap.gpf.OperatorUtils;
import org.esa.snap.util.ProductUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Perform Polarimetric classification of a given polarimetric product
 */

@OperatorMetadata(alias = "Polarimetric-Classification",
        category = "SAR Processing/Polarimetric",
        authors = "Jun Lu, Luis Veci",
        copyright = "Copyright (C) 2014 by Array Systems Computing Inc.",
        description = "Perform Polarimetric classification of a given product")
public class PolarimetricClassificationOp extends Operator {

    @SourceProduct(alias = "source")
    protected Product sourceProduct;
    @TargetProduct
    protected Product targetProduct;

    @Parameter(valueSet = {UNSUPERVISED_CLOUDE_POTTIER_CLASSIFICATION, UNSUPERVISED_HALPHA_WISHART_CLASSIFICATION,
            UNSUPERVISED_HALPHA_WISHART_DUAL_POL_CLASSIFICATION, UNSUPERVISED_FREEMAN_DURDEN_CLASSIFICATION},
            defaultValue = UNSUPERVISED_HALPHA_WISHART_CLASSIFICATION, label = "Classification")
    protected String classification = UNSUPERVISED_HALPHA_WISHART_CLASSIFICATION;

    @Parameter(description = "The sliding window size", interval = "(1, 100]", defaultValue = "5", label = "Window Size")
    private int windowSize = 5;

    @Parameter(description = "The maximum number of iterations", interval = "[1, 100]", defaultValue = "3",
            label = "Maximum Number of Iterations")
    protected int maxIterations = 3;

    @Parameter(description = "The initial number of classes", interval = "[9, 1000]", defaultValue = "90",
            label = "The Initial Number of Classes")
    private int numInitialClasses = 90;

    @Parameter(description = "The desired number of classes", interval = "[9, 100]", defaultValue = "15",
            label = "The Final Number of Classes")
    private int numFinalClasses = 15;

    @Parameter(description = "The threshold for classifying pixels to mixed category", interval = "(0, *)",
            defaultValue = "0.5", label = "Threshold for Mixed Category")
    private double mixedCategoryThreshold = 0.5;

    protected int sourceImageWidth = 0;
    protected int sourceImageHeight = 0;
    protected PolBandUtils.PolSourceBand[] srcBandList;
    protected final Map<Band, PolBandUtils.PolSourceBand> bandMap = new HashMap<>();

    public static final String UNSUPERVISED_CLOUDE_POTTIER_CLASSIFICATION = "Cloude-Pottier";
    public static final String UNSUPERVISED_HALPHA_WISHART_CLASSIFICATION = "H Alpha Wishart";
    public static final String UNSUPERVISED_HALPHA_WISHART_DUAL_POL_CLASSIFICATION = "H Alpha Wishart Dual Pol";
    public static final String UNSUPERVISED_FREEMAN_DURDEN_CLASSIFICATION = "Freeman-Durden Wishart";

    protected PolBandUtils.MATRIX sourceProductType;
    protected PolClassifier classifier;

    /**
     * Set classification. This function is used by unit test only.
     *
     * @param s The classification name.
     */
    public void SetClassification(String s) {

        if (s.equals(UNSUPERVISED_CLOUDE_POTTIER_CLASSIFICATION) ||
                s.equals(UNSUPERVISED_HALPHA_WISHART_CLASSIFICATION) ||
                s.equals(UNSUPERVISED_HALPHA_WISHART_DUAL_POL_CLASSIFICATION) ||
                s.equals(UNSUPERVISED_FREEMAN_DURDEN_CLASSIFICATION)) {
            classification = s;
        } else {
            throw new OperatorException(s + " is an invalid classification name.");
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
            final InputProductValidator validator = new InputProductValidator(sourceProduct);
            validator.checkIfSentinel1DeburstProduct();

            sourceProductType = PolBandUtils.getSourceProductType(sourceProduct);

            checkSourceProductType(sourceProductType);

            sourceImageWidth = sourceProduct.getSceneRasterWidth();
            sourceImageHeight = sourceProduct.getSceneRasterHeight();
            sourceProductType = PolBandUtils.getSourceProductType(sourceProduct);

            srcBandList = PolBandUtils.getSourceBands(sourceProduct, sourceProductType);

            classifier = createClassifier(classification);

            createTargetProduct();

            if (targetProduct.getNumBands() > 1 && !classifier.canProcessStacks()) {
                throw new OperatorException("Stack processing is not supported with this classifier.");
            }

            updateTargetProductMetadata();
        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private void checkSourceProductType(final PolBandUtils.MATRIX sourceProductType) {

        if(sourceProductType == PolBandUtils.MATRIX.UNKNOWN) {
            // This check will catch products with a single pol.
            throw new OperatorException("Input should be a polarimetric product");
        }

        switch (classification) {
            case UNSUPERVISED_CLOUDE_POTTIER_CLASSIFICATION:
            case UNSUPERVISED_HALPHA_WISHART_CLASSIFICATION:
            case UNSUPERVISED_FREEMAN_DURDEN_CLASSIFICATION:
                if (PolBandUtils.isDualPol(sourceProductType)) {
                    throw new OperatorException("Input product cannot be dual pol");
                }
                break;
            case UNSUPERVISED_HALPHA_WISHART_DUAL_POL_CLASSIFICATION:
                if (PolBandUtils.isQuadPol(sourceProductType)) {
                    throw new OperatorException("Input product cannot be quad pol");
                }
                break;
            default:
                break;
        }
    }

    private PolClassifier createClassifier(final String classification) throws OperatorException {
        switch (classification) {
            case UNSUPERVISED_CLOUDE_POTTIER_CLASSIFICATION:

                return new CloudePottier(sourceProductType, sourceImageWidth, sourceImageHeight, windowSize, bandMap, this);

            case UNSUPERVISED_HALPHA_WISHART_CLASSIFICATION:

                return new HAlphaWishart(sourceProductType, sourceImageWidth, sourceImageHeight, windowSize, bandMap,
                        maxIterations, this);

            case UNSUPERVISED_HALPHA_WISHART_DUAL_POL_CLASSIFICATION:

                return new HAlphaWishartC2(sourceProductType, sourceImageWidth, sourceImageHeight, windowSize, windowSize,
                        bandMap, maxIterations, this);

            case UNSUPERVISED_FREEMAN_DURDEN_CLASSIFICATION:

                return new FreemanDurdenWishart(sourceProductType, sourceImageWidth, sourceImageHeight, windowSize, bandMap,
                        maxIterations, numInitialClasses, numFinalClasses, mixedCategoryThreshold, this);
        }
        throw new OperatorException(classification + " is an invalid classification name.");
    }

    /**
     * Create target product.
     */
    private void createTargetProduct() {

        targetProduct = new Product(sourceProduct.getName(),
                sourceProduct.getProductType(),
                sourceImageWidth, sourceImageHeight);

        ProductUtils.copyProductNodes(sourceProduct, targetProduct);

        final String targetBandName = classifier.getTargetBandName();

        // add index coding
        final IndexCoding indexCoding = classifier.createIndexCoding();
        targetProduct.getIndexCodingGroup().add(indexCoding);

        // add a target product per source product
        for (final PolBandUtils.PolSourceBand bandList : srcBandList) {
            final Band targetBand = new Band(targetBandName + bandList.suffix,
                    ProductData.TYPE_UINT8,
                    targetProduct.getSceneRasterWidth(),
                    targetProduct.getSceneRasterHeight());

            targetBand.setUnit("zone_index");
            targetBand.setNoDataValue(HAlphaWishart.NODATACLASS);
            targetBand.setNoDataValueUsed(true);
            targetProduct.addBand(targetBand);

            bandMap.put(targetBand, bandList);
            targetBand.setSampleCoding(indexCoding);
        }
    }

    /**
     * Update metadata in the target product.
     */
    private void updateTargetProductMetadata() {
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(targetProduct);

        absRoot.setAttributeInt(AbstractMetadata.polsarData, 1);

    }

    public void checkIfCancelled() {
        checkForCancellation();
    }

    /**
     * Called by the framework in order to compute a tile for the given target band.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetBand The target band.
     * @param targetTile The current tile associated with the target band to be computed.
     * @param pm         A progress monitor which should be used to determine computation cancelation requests.
     * @throws org.esa.beam.framework.gpf.OperatorException If an error occurs during computation of the target raster.
     */
    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        try {
            classifier.computeTile(targetBand, targetTile);

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        } finally {
            pm.done();
        }
    }

    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.snap.framework.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     *
     * @see org.esa.beam.framework.gpf.OperatorSpi#createOperator()
     * @see org.esa.beam.framework.gpf.OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(PolarimetricClassificationOp.class);
        }
    }
}
