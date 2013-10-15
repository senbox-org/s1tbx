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
package org.csa.rstb.gpf;

import org.csa.rstb.gpf.decompositions.*;
import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.gpf.OperatorUtils;
import org.esa.nest.gpf.PolBandUtils;

import java.awt.*;
import java.util.Map;

/**
 * Perform Polarimetric decomposition of a given polarimetric product
 */

@OperatorMetadata(alias="Polarimetric-Decomposition",
                  category = "Polarimetric",
                  description="Perform Polarimetric decomposition of a given product")
public final class PolarimetricDecompositionOp extends Operator {

    @SourceProduct(alias="source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(valueSet = {SINCLAIR_DECOMPOSITION, PAULI_DECOMPOSITION, FREEMAN_DURDEN_DECOMPOSITION,
            YAMAGUCHI_DECOMPOSITION, VANZYL_DECOMPOSITION, H_A_ALPHA_DECOMPOSITION, CLOUDE_DECOMPOSITION,
            TOUZI_DECOMPOSITION}, defaultValue = SINCLAIR_DECOMPOSITION, label="Decomposition")
    private String decomposition = SINCLAIR_DECOMPOSITION;

    @Parameter(description = "The sliding window size", interval = "[1, 100]", defaultValue = "5", label="Window Size")
    private int windowSize = 5;

    // H-A-Alpha flags
    @Parameter(description = "Output entropy, anisotropy, alpha", defaultValue = "false",
            label="Entropy (H), Anisotropy (A), Alpha")
    private boolean outputHAAlpha = false;

    @Parameter(description = "Output beta, delta, gamma, lambda", defaultValue = "false",
            label="Beta, Delta, Gamma, Lambda")
    private boolean outputBetaDeltaGammaLambda = false;

    @Parameter(description = "Output alpha 1, 2, 3", defaultValue = "false", label="Alpha 1, Alpha 2, Alpha 3")
    private boolean outputAlpha123 = false;

    @Parameter(description = "Output lambda 1, 2, 3", defaultValue = "false", label="Lambda 1, Lambda 2, Lambda 3")
    private boolean outputLambda123 = false;

    // Touzi flags
    @Parameter(description = "Output psi, tau, alpha, phi", defaultValue = "false", label="Psi, Tau, Alpha, Phi")
    private boolean outputTouziParamSet0 = false;

    @Parameter(description = "Output psi1, tau1, alpha1, phi1", defaultValue = "false", label="Psi 1, Tau 1, Alpha 1, Phi 1")
    private boolean outputTouziParamSet1 = false;

    @Parameter(description = "Output psi2, tau2, alpha2, phi2", defaultValue = "false", label="Psi 2, Tau 2, Alpha 2, Phi 2")
    private boolean outputTouziParamSet2 = false;

    @Parameter(description = "Output psi3, tau3, alpha3, phi3", defaultValue = "false", label="Psi 3, Tau 3, Alpha 3, Phi 3")
    private boolean outputTouziParamSet3 = false;

    static final String SINCLAIR_DECOMPOSITION = "Sinclair Decomposition";
    static final String PAULI_DECOMPOSITION = "Pauli Decomposition";
    static final String FREEMAN_DURDEN_DECOMPOSITION = "Freeman-Durden Decomposition";
    static final String YAMAGUCHI_DECOMPOSITION = "Yamaguchi Decomposition";
    static final String VANZYL_DECOMPOSITION = "van Zyl Decomposition";
    static final String H_A_ALPHA_DECOMPOSITION = "H-A-Alpha Decomposition";
    static final String CLOUDE_DECOMPOSITION = "Cloude Decomposition";
    static final String TOUZI_DECOMPOSITION = "Touzi Decomposition";

    private PolBandUtils.QuadSourceBand[] srcBandList;
    private PolBandUtils.MATRIX sourceProductType = null;
    private Decomposition polDecomp;

    /**
     * Set decomposition. This function is used by unit test only.
     * @param s The decomposition name.
     */
    protected void SetDecomposition(final String s) {

        if (s.equals(SINCLAIR_DECOMPOSITION) || s.equals(PAULI_DECOMPOSITION) ||
            s.equals(FREEMAN_DURDEN_DECOMPOSITION) || s.equals(YAMAGUCHI_DECOMPOSITION) ||
            s.equals(VANZYL_DECOMPOSITION) || s.equals(H_A_ALPHA_DECOMPOSITION) ||
            s.equals(CLOUDE_DECOMPOSITION) || s.equals(TOUZI_DECOMPOSITION)) {
                decomposition = s;
        } else {
            throw new OperatorException(s + " is an invalid decomposition name.");
        }
    }

    protected void setTouziParameters(final boolean set0, final boolean set1,
                                      final boolean set2, final boolean set3) {
        outputTouziParamSet0 = set0;
        outputTouziParamSet1 = set1;
        outputTouziParamSet2 = set2;
        outputTouziParamSet3 = set3;
    }

    protected void setHAAlphaParameters(final boolean HAAlpha, final boolean BetaDeltaGammaLambda,
                                      final boolean Alpha123, final boolean Lambda123) {
        outputHAAlpha = HAAlpha;
        outputBetaDeltaGammaLambda = BetaDeltaGammaLambda;
        outputAlpha123 = Alpha123;
        outputLambda123 = Lambda123;
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
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs during operator initialisation.
     * @see #getTargetProduct()
     */
    @Override
    public void initialize() throws OperatorException {

        try {
            sourceProductType = PolBandUtils.getSourceProductType(sourceProduct);

            srcBandList = PolBandUtils.getSourceBands(sourceProduct, sourceProductType);

            polDecomp = createDecomposition();

            createTargetProduct();

            updateTargetProductMetadata();
        } catch(Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private Decomposition createDecomposition() throws OperatorException {
        int sourceImageWidth = sourceProduct.getSceneRasterWidth();
        int sourceImageHeight = sourceProduct.getSceneRasterHeight();

        if(sourceProductType == null) {
            throw new OperatorException("Source product type is unknown");
        }
        if(sourceImageWidth == 0 || sourceImageHeight == 0) {
            throw new OperatorException("Source image dimensions unknown");
        }

        if(decomposition.equals(SINCLAIR_DECOMPOSITION)) {
            return new Sinclair(srcBandList, sourceProductType,
                                windowSize, sourceImageWidth, sourceImageHeight);
        } else if(decomposition.equals(PAULI_DECOMPOSITION)) {
            return new Pauli(srcBandList, sourceProductType,
                                windowSize, sourceImageWidth, sourceImageHeight);
        } else if(decomposition.equals(FREEMAN_DURDEN_DECOMPOSITION)) {
            return new FreemanDurden(srcBandList, sourceProductType,
                                windowSize, sourceImageWidth, sourceImageHeight);
        } else if(decomposition.equals(YAMAGUCHI_DECOMPOSITION)) {
            return new Yamaguchi(srcBandList, sourceProductType,
                                windowSize, sourceImageWidth, sourceImageHeight);
        } else if(decomposition.equals(VANZYL_DECOMPOSITION)) {
            return new vanZyl(srcBandList, sourceProductType,
                              windowSize, sourceImageWidth, sourceImageHeight);
        } else if(decomposition.equals(CLOUDE_DECOMPOSITION)) {
            return new Cloude(srcBandList, sourceProductType,
                                windowSize, sourceImageWidth, sourceImageHeight);
        } else if(decomposition.equals(H_A_ALPHA_DECOMPOSITION)) {
            return new hAAlpha(srcBandList, sourceProductType,
                                windowSize, sourceImageWidth, sourceImageHeight,
                                outputHAAlpha,
                                outputBetaDeltaGammaLambda,
                                outputAlpha123,
                                outputLambda123);
        } else if(decomposition.equals(TOUZI_DECOMPOSITION)) {
            return new Touzi(srcBandList, sourceProductType,
                                windowSize, sourceImageWidth, sourceImageHeight,
                                outputTouziParamSet0,
                                outputTouziParamSet1,
                                outputTouziParamSet2,
                                outputTouziParamSet3);
        }
        return null;
    }

    /**
     * Create target product.
     */
    private void createTargetProduct() {

        targetProduct = new Product(sourceProduct.getName(),
                                    sourceProduct.getProductType(),
                                    sourceProduct.getSceneRasterWidth(),
                                    sourceProduct.getSceneRasterHeight());

        addSelectedBands();

        OperatorUtils.copyProductNodes(sourceProduct, targetProduct);
    }

    /**
     * Update metadata in the target product.
     */
    private void updateTargetProductMetadata() {
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(targetProduct);

        absRoot.setAttributeInt(AbstractMetadata.polsarData, 1);

        // Save new slave band names
        PolBandUtils.saveNewBandNames(targetProduct, srcBandList);
    }

    /**
     * Add bands to the target product.
     * @throws OperatorException The exception.
     */
    private void addSelectedBands() throws OperatorException {

        final String[] targetBandNames = polDecomp.getTargetBandNames();

        for(final PolBandUtils.QuadSourceBand bandList :srcBandList) {
            final Band[] targetBands = new Band[targetBandNames.length];
            int i = 0;
            for (String targetBandName : targetBandNames) {

                final Band targetBand = new Band(targetBandName + bandList.suffix,
                                                 ProductData.TYPE_FLOAT32,
                                                 targetProduct.getSceneRasterWidth(),
                                                 targetProduct.getSceneRasterHeight());

                polDecomp.setBandUnit(targetBandName + bandList.suffix, targetBand);

                targetProduct.addBand(targetBand);
                targetBands[i++] = targetBand;
            }
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
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          if an error occurs during computation of the target rasters.
     */
    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {
        try {

            polDecomp.computeTile(targetTiles, targetRectangle, this);

        } catch(Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        } finally {
            pm.done();
        }
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
            super(PolarimetricDecompositionOp.class);
            setOperatorUI(PolarimetricDecompositionOpUI.class);
        }
    }
}