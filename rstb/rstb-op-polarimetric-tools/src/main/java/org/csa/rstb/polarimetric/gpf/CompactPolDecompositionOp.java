/*
 * Copyright (C) 2018 Skywatch Space Applications Inc. https://www.skywatch.co
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
package org.csa.rstb.polarimetric.gpf;

import org.csa.rstb.polarimetric.gpf.decompositions_cp.CP_HAlpha;
import org.csa.rstb.polarimetric.gpf.decompositions_cp.CP_MChi;
import org.csa.rstb.polarimetric.gpf.decompositions_cp.CP_MDelta;
import org.csa.rstb.polarimetric.gpf.decompositions_cp.CP_RVOG;
import com.bc.ceres.core.ProgressMonitor;
import org.csa.rstb.polarimetric.gpf.decompositions.Decomposition;
import org.esa.s1tbx.commons.polsar.PolBandUtils;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
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
import org.esa.snap.engine_utilities.gpf.InputProductValidator;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;

import java.awt.*;
import java.util.Map;

/**
 * Perform Compact Polarimetric decomposition of a given polarimetric product
 */

@OperatorMetadata(alias = "CP-Decomposition",
        category = "Radar/Polarimetric/Compact Polarimetry",
        authors = "Jun Lu, Luis Veci",
        copyright = "Copyright (C) 2018 SkyWatch Space Applications Inc.",
        description = "Perform Compact Polarimetric decomposition of a given product")
public final class CompactPolDecompositionOp extends Operator {

    public static final String M_CHI_DECOMPOSITION = "M-Chi Decomposition";
    public static final String M_DELTA_DECOMPOSITION = "M-Delta Decomposition";
    public static final String H_ALPHA_DECOMPOSITION = "H-Alpha Decomposition";
    public static final String RVOG_DECOMPOSITION = "2 Layer RVOG Model Based Decomposition";

    @SourceProduct(alias = "source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;
    @Parameter(valueSet = {M_CHI_DECOMPOSITION, M_DELTA_DECOMPOSITION, H_ALPHA_DECOMPOSITION, RVOG_DECOMPOSITION},
            defaultValue = M_CHI_DECOMPOSITION, label = "Decomposition")
    private String decomposition = M_CHI_DECOMPOSITION;
    @Parameter(valueSet = {"3", "5", "7", "9", "11", "13", "15", "17", "19"}, defaultValue = "5", label = "Window Size X")
    private String windowSizeXStr = "5";
    @Parameter(valueSet = {"3", "5", "7", "9", "11", "13", "15", "17", "19"}, defaultValue = "5", label = "Window Size Y")
    private String windowSizeYStr = "5";
    @Parameter(description = "Compute alpha by coherency matrix T3", defaultValue = "true",
            label = "Compute Alpha By T3")
    private boolean computeAlphaByT3 = true;
    private int windowSizeX = 0;
    private int windowSizeY = 0;
    private PolBandUtils.PolSourceBand[] srcBandList;
    private PolBandUtils.MATRIX sourceProductType = null;
    private Decomposition polDecomp;
    private String compactMode = null;

    /**
     * Set decomposition. This function is used by unit test only.
     *
     * @param s The decomposition name.
     */
    protected void SetDecomposition(final String s) {

        if (s.equals(M_CHI_DECOMPOSITION) || s.equals(M_DELTA_DECOMPOSITION) ||
                s.equals(H_ALPHA_DECOMPOSITION) || s.equals(RVOG_DECOMPOSITION)) {
            decomposition = s;
        } else {
            throw new OperatorException(s + " is an invalid decomposition name.");
        }
    }

    /**
     * Initializes this operator and sets the one and only target product.
     * <p>The target product can be either defined by a field of type {@link org.esa.snap.core.datamodel.Product} annotated with the
     * {@link org.esa.snap.core.gpf.annotations.TargetProduct TargetProduct} annotation or
     * by calling {@link #setTargetProduct} method.</p>
     * <p>The framework calls this method after it has created this operator.
     * Any client code that must be performed before computation of tile data
     * should be placed here.</p>
     *
     * @throws org.esa.snap.core.gpf.OperatorException If an error occurs during operator initialisation.
     * @see #getTargetProduct()
     */
    @Override
    public void initialize() throws OperatorException {

        try {
            final InputProductValidator validator = new InputProductValidator(sourceProduct);
            validator.checkIfSARProduct();
            validator.checkIfSLC();

            sourceProductType = PolBandUtils.getSourceProductType(sourceProduct);
            if (sourceProductType != PolBandUtils.MATRIX.LCHCP &&
                    sourceProductType != PolBandUtils.MATRIX.RCHCP &&
                    sourceProductType != PolBandUtils.MATRIX.C2) {
                throw new OperatorException("Compact pol source product or C2 covariance matrix product is expected.");
            }

            srcBandList = PolBandUtils.getSourceBands(sourceProduct, sourceProductType);

            getCompactPolMode();

            windowSizeX = Integer.parseInt(windowSizeXStr);
            windowSizeY = Integer.parseInt(windowSizeYStr);

            polDecomp = createDecomposition();

            createTargetProduct();

            updateTargetProductMetadata();
        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private void getCompactPolMode() {
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);
        compactMode = absRoot.getAttributeString(AbstractMetadata.compact_mode, CompactPolProcessor.rch);
        if (!compactMode.equals(CompactPolProcessor.rch) && !compactMode.equals(CompactPolProcessor.lch)) {
            throw new OperatorException("Right/Left Circular Hybrid Mode is expected.");
        }
    }

    private Decomposition createDecomposition() throws OperatorException {

        final int sourceImageWidth = sourceProduct.getSceneRasterWidth();
        final int sourceImageHeight = sourceProduct.getSceneRasterHeight();

        if (sourceProductType == null) {
            throw new OperatorException("Source product type is unknown");
        }
        if (sourceImageWidth == 0 || sourceImageHeight == 0) {
            throw new OperatorException("Source image dimensions unknown");
        }

        switch (decomposition) {
            case M_CHI_DECOMPOSITION:
                return new CP_MChi(srcBandList, sourceProductType, compactMode, windowSizeX, windowSizeY,
                        sourceImageWidth, sourceImageHeight);
            case M_DELTA_DECOMPOSITION:
                return new CP_MDelta(srcBandList, sourceProductType, compactMode, windowSizeX, windowSizeY,
                        sourceImageWidth, sourceImageHeight);
            case H_ALPHA_DECOMPOSITION:
                return new CP_HAlpha(srcBandList, sourceProductType, compactMode, windowSizeX, windowSizeY, computeAlphaByT3,
                        sourceImageWidth, sourceImageHeight);
            case RVOG_DECOMPOSITION:
                return new CP_RVOG(srcBandList, sourceProductType, compactMode, windowSizeX, windowSizeY,
                        sourceImageWidth, sourceImageHeight);
        }
        return null;
    }

    /**
     * Create target product.
     */
    private void createTargetProduct() {

        targetProduct = new Product(sourceProduct.getName() + polDecomp.getSuffix(),
                sourceProduct.getProductType(),
                sourceProduct.getSceneRasterWidth(),
                sourceProduct.getSceneRasterHeight());

        addSelectedBands();

        ProductUtils.copyProductNodes(sourceProduct, targetProduct);
    }

    /**
     * Update metadata in the target product.
     */
    private void updateTargetProductMetadata() {
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(targetProduct);
        absRoot.setAttributeInt(AbstractMetadata.polsarData, 1);
        absRoot.setAttributeString(AbstractMetadata.compact_mode, compactMode);

        // Save new slave band names
        PolBandUtils.saveNewBandNames(targetProduct, srcBandList);
    }

    /**
     * Add bands to the target product.
     *
     * @throws OperatorException The exception.
     */
    private void addSelectedBands() throws OperatorException {

        final String[] targetBandNames = polDecomp.getTargetBandNames();

        for (final PolBandUtils.PolSourceBand bandList : srcBandList) {
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

    private String getCompactMode() {
        return compactMode;
    }

    /**
     * Called by the framework in order to compute the stack of tiles for the given target bands.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetTiles     The current tiles to be computed for each target band.
     * @param targetRectangle The area in pixel coordinates to be computed (same for all rasters in <code>targetRasters</code>).
     * @param pm              A progress monitor which should be used to determine computation cancelation requests.
     * @throws org.esa.snap.core.gpf.OperatorException if an error occurs during computation of the target rasters.
     */
    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {
        try {

            polDecomp.computeTile(targetTiles, targetRectangle, this);

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
     * @see org.esa.snap.core.gpf.OperatorSpi#createOperator()
     * @see org.esa.snap.core.gpf.OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(CompactPolDecompositionOp.class);
        }
    }
}