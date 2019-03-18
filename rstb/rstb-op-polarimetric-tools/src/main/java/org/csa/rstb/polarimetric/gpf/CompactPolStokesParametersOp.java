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

import com.bc.ceres.core.ProgressMonitor;
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
import org.esa.snap.engine_utilities.gpf.TileIndex;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Generate compact polarimetric Stokes child parameters.
 */

@OperatorMetadata(alias = "CP-Stokes-Parameters",
        category = "Radar/Polarimetric/Compact Polarimetry",
        authors = "Jun Lu, Luis Veci",
        copyright = "Copyright (C) 2018 SkyWatch Space Applications Inc.",
        description = "Generates compact polarimetric Stokes child parameters")
public final class CompactPolStokesParametersOp extends Operator implements CompactPolProcessor {

    private static final String PRODUCT_SUFFIX = "_Stokes";
    @SourceProduct(alias = "source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;
    //    @Parameter(description = "The sliding window size", interval = "[1, 100]", defaultValue = "5", label="Window Size")
//    private int windowSize = 5;
    @Parameter(valueSet = {"3", "5", "7", "9", "11", "13", "15", "17", "19"}, defaultValue = "5", label = "Window Size X")
    private String windowSizeXStr = "5";
    @Parameter(valueSet = {"3", "5", "7", "9", "11", "13", "15", "17", "19"}, defaultValue = "5", label = "Window Size Y")
    private String windowSizeYStr = "5";
    @Parameter(description = "Output Stokes vector", defaultValue = "false",
            label = "Stokes vector")
    private boolean outputStokesVector = false;
    @Parameter(description = "Output degree of polarization", defaultValue = "true",
            label = "Degree of polarization")
    private boolean outputDegreeOfPolarization = true;
    @Parameter(description = "Output degree of depolarization", defaultValue = "true",
            label = "Degree of depolarization")
    private boolean outputDegreeOfDepolarization = true;
    @Parameter(description = "Output degree of circularity", defaultValue = "true",
            label = "Degree of circularity")
    private boolean outputDegreeOfCircularity = true;
    @Parameter(description = "Output degree of ellipticity", defaultValue = "true",
            label = "Degree of ellipticity")
    private boolean outputDegreeOfEllipticity = true;
    @Parameter(description = "Output circular polarization ratio", defaultValue = "true",
            label = "Circular polarization ratio")
    private boolean outputCPR = true;
    @Parameter(description = "Output linear polarization ratio", defaultValue = "true",
            label = "Linear polarization ratio")
    private boolean outputLPR = true;
    @Parameter(description = "Output relative phase", defaultValue = "true",
            label = "Linear relative phase")
    private boolean outputRelativePhase = true;
    @Parameter(description = "Output alphas", defaultValue = "true",
            label = "Alphas")
    private boolean outputAlphas = true;
    @Parameter(description = "Output conformity coefficient", defaultValue = "true",
            label = "Conformity coefficient")
    private boolean outputConformity = true;
    @Parameter(description = "Output phase phi", defaultValue = "true",
            label = "Phase Phi")
    private boolean outputPhasePhi = true;
    private int windowSizeX = 0;
    private int windowSizeY = 0;
    private int sourceImageWidth = 0;
    private int sourceImageHeight = 0;
    private String compactMode = null;
    private boolean useRCMConvention = false;
    private PolBandUtils.MATRIX sourceProductType = null;
    private PolBandUtils.PolSourceBand[] srcBandList;

    /**
     * Set output parameter. This function is used by unit test only.
     *
     * @param s The output Stokes child parameter string.
     */
    public void SetOutputParameter(final String s) {

        switch (s) {
            case "Stokes Vector":
                outputStokesVector = true;
                break;
            case "Degree Of Polarization":
                outputDegreeOfPolarization = true;
                break;
            case "Degree Of Depolarization":
                outputDegreeOfDepolarization = true;
                break;
            case "Degree Of Circularity":
                outputDegreeOfCircularity = true;
                break;
            case "Degree Of Ellipticity":
                outputDegreeOfEllipticity = true;
                break;
            case "Circular Polarization Ratio":
                outputCPR = true;
                break;
            case "Linear Polarization Ratio":
                outputLPR = true;
                break;
            case "Relative Phase":
                outputRelativePhase = true;
                break;
            case "Alphas":
                outputAlphas = true;
                break;
            case "Conformity Coefficient":
                outputConformity = true;
                break;
            default:
                throw new OperatorException(s + " is an invalid parameter name.");
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
            if (sourceProductType != PolBandUtils.MATRIX.LCHCP && sourceProductType != PolBandUtils.MATRIX.RCHCP &&
                    sourceProductType != PolBandUtils.MATRIX.C2) {
                throw new OperatorException("Compact pol source product or C2 covariance matrix product is expected.");
            }

            if (!outputDegreeOfPolarization && !outputDegreeOfDepolarization && !outputDegreeOfCircularity &&
                    !outputDegreeOfEllipticity && !outputCPR && !outputLPR && !outputRelativePhase && !outputAlphas &&
                    !outputConformity && !outputPhasePhi && !outputStokesVector) {
                throw new OperatorException("Please select output parameters.");
            }

            srcBandList = PolBandUtils.getSourceBands(sourceProduct, sourceProductType);

            windowSizeX = Integer.parseInt(windowSizeXStr);
            windowSizeY = Integer.parseInt(windowSizeYStr);
            sourceImageWidth = sourceProduct.getSceneRasterWidth();
            sourceImageHeight = sourceProduct.getSceneRasterHeight();

            getCompactMode();

            createTargetProduct();

            updateTargetProductMetadata();
        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private void getCompactMode() {
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);
        compactMode = absRoot.getAttributeString(AbstractMetadata.compact_mode, CompactPolProcessor.rch);
        //System.out.println("compactMode = " + compactMode);
        if (!compactMode.equals(CompactPolProcessor.rch) && !compactMode.equals(CompactPolProcessor.lch)) {
            throw new OperatorException("Right/Left Circular Hybrid Mode is expected.");
        }

        useRCMConvention = PolBandUtils.useRCMConvention();
    }

    /**
     * Create target product.
     */
    private void createTargetProduct() {

        targetProduct = new Product(sourceProduct.getName() + PRODUCT_SUFFIX,
                sourceProduct.getProductType(),
                sourceProduct.getSceneRasterWidth(),
                sourceProduct.getSceneRasterHeight());

        addSelectedBands();

        ProductUtils.copyProductNodes(sourceProduct, targetProduct);
    }

    /**
     * Add bands to the target product.
     *
     * @throws OperatorException The exception.
     */
    private void addSelectedBands() throws OperatorException {

        final String[] targetBandNames = getTargetBandNames();

        for (PolBandUtils.PolSourceBand bandList : srcBandList) {
            final Band[] targetBands = OperatorUtils.addBands(targetProduct, targetBandNames, bandList.suffix);
            bandList.addTargetBands(targetBands);
        }
    }

    private String[] getTargetBandNames() {
        final List<String> targetBandNameList = new ArrayList<>(13);

        if (outputStokesVector) {
            targetBandNameList.add("g0");
            targetBandNameList.add("g1");
            targetBandNameList.add("g2");
            targetBandNameList.add("g3");
        }

        if (outputDegreeOfPolarization) {
            targetBandNameList.add("DegreeOfPolarization");
        }

        if (outputDegreeOfDepolarization) {
            targetBandNameList.add("DegreeOfDepolarization");
        }

        if (outputDegreeOfCircularity) {
            targetBandNameList.add("DegreeOfCircularity");
        }

        if (outputDegreeOfEllipticity) {
            targetBandNameList.add("DegreeOfEllipticity");
        }

        if (outputCPR) {
            targetBandNameList.add("CPR");
        }

        if (outputLPR) {
            targetBandNameList.add("LPR");
        }

        if (outputRelativePhase) {
            targetBandNameList.add("RelativePhase");
        }

        if (outputAlphas) {
            targetBandNameList.add("Alphas");
        }

        if (outputConformity) {
            targetBandNameList.add("ConformityCoefficient");
        }

        if (outputPhasePhi) {
            targetBandNameList.add("PhasePhi");
        }

        return targetBandNameList.toArray(new String[targetBandNameList.size()]);
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

        final int x0 = targetRectangle.x;
        final int y0 = targetRectangle.y;
        final int w = targetRectangle.width;
        final int h = targetRectangle.height;
        final int maxY = y0 + h;
        final int maxX = x0 + w;
        //System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

        final TileIndex trgIndex = new TileIndex(targetTiles.get(targetProduct.getBandAt(0)));

        final double[][] Cr = new double[2][2];
        final double[][] Ci = new double[2][2];
        final double[] g = new double[4];

        final Rectangle sourceRectangle = getSourceTileRectangle(x0, y0, w, h, windowSizeX, windowSizeY);
        final int halfWindowSizeX = windowSizeX / 2;
        final int halfWindowSizeY = windowSizeY / 2;

        for (final PolBandUtils.PolSourceBand bandList : srcBandList) {
            try {
                // save tile data for quicker access
                final TileData[] tileDataList = new TileData[bandList.targetBands.length];
                int i = 0;
                for (Band targetBand : bandList.targetBands) {
                    final Tile targetTile = targetTiles.get(targetBand);
                    tileDataList[i++] = new TileData(targetTile, targetBand.getName());
                }

                final Tile[] sourceTiles = new Tile[bandList.srcBands.length];
                final ProductData[] dataBuffers = new ProductData[bandList.srcBands.length];
                for (int j = 0; j < bandList.srcBands.length; j++) {
                    sourceTiles[j] = getSourceTile(bandList.srcBands[j], sourceRectangle);
                    dataBuffers[j] = sourceTiles[j].getDataBuffer();
                }

                for (int y = y0; y < maxY; ++y) {
                    trgIndex.calculateStride(y);
                    for (int x = x0; x < maxX; ++x) {
                        final int idx = trgIndex.getIndex(x);

                        getMeanCovarianceMatrixC2(x, y, halfWindowSizeX, halfWindowSizeY, sourceImageWidth,
                                sourceImageHeight, sourceProductType, sourceTiles, dataBuffers, Cr, Ci);

                        StokesParameters.computeCompactPolStokesVector(Cr, Ci, g);

                        StokesParameters sp = StokesParameters.computeStokesParameters(g, compactMode, useRCMConvention);

                        for (final TileData tileData : tileDataList) {

                            if (outputStokesVector && tileData.bandName.startsWith("g0")) {
                                tileData.dataBuffer.setElemFloatAt(idx, (float) g[0]);
                            }
                            if (outputStokesVector && tileData.bandName.startsWith("g1")) {
                                tileData.dataBuffer.setElemFloatAt(idx, (float) g[1]);
                            }
                            if (outputStokesVector && tileData.bandName.startsWith("g2")) {
                                tileData.dataBuffer.setElemFloatAt(idx, (float) g[2]);
                            }
                            if (outputStokesVector && tileData.bandName.startsWith("g3")) {
                                tileData.dataBuffer.setElemFloatAt(idx, (float) g[3]);
                            }
                            if (outputDegreeOfPolarization && tileData.bandName.startsWith("DegreeOfPolarization")) {
                                tileData.dataBuffer.setElemFloatAt(idx, (float) sp.DegreeOfPolarization);
                            }
                            if (outputDegreeOfDepolarization && tileData.bandName.startsWith("DegreeOfDepolarization")) {
                                tileData.dataBuffer.setElemFloatAt(idx, (float) sp.DegreeOfDepolarization);
                            }
                            if (outputDegreeOfCircularity && tileData.bandName.startsWith("DegreeOfCircularity")) {
                                tileData.dataBuffer.setElemFloatAt(idx, (float) sp.DegreeOfCircularity);
                            }
                            if (outputDegreeOfEllipticity && tileData.bandName.startsWith("DegreeOfEllipticity")) {
                                tileData.dataBuffer.setElemFloatAt(idx, (float) sp.DegreeOfEllipticity);
                            }
                            if (outputCPR && tileData.bandName.startsWith("CPR")) {
                                tileData.dataBuffer.setElemFloatAt(idx, (float) sp.circularPolarizationRatio);
                            }
                            if (outputLPR && tileData.bandName.startsWith("LPR")) {
                                tileData.dataBuffer.setElemFloatAt(idx, (float) sp.linearPolarizationRatio);
                            }
                            if (outputRelativePhase && tileData.bandName.startsWith("RelativePhase")) {
                                tileData.dataBuffer.setElemFloatAt(idx, (float) sp.RelativePhase);
                            }
                            if (outputAlphas && tileData.bandName.startsWith("Alphas")) {
                                tileData.dataBuffer.setElemFloatAt(idx, (float) sp.Alphas);
                            }
                            if (outputPhasePhi && tileData.bandName.startsWith("PhasePhi")) {
                                tileData.dataBuffer.setElemFloatAt(idx, (float) sp.PhasePhi);
                            }
                            if (outputConformity && tileData.bandName.startsWith("ConformityCoefficient")) {
                                tileData.dataBuffer.setElemFloatAt(idx, (float) sp.Conformity);
                            }
                        }

                    }
                }

            } catch (Throwable e) {
                OperatorUtils.catchOperatorException(getId(), e);
            }
        }
    }

    /**
     * Get source tile rectangle.
     *
     * @param x0          X coordinate of pixel at the upper left corner of the target tile.
     * @param y0          Y coordinate of pixel at the upper left corner of the target tile.
     * @param w           The width of the target tile.
     * @param h           The height of the target tile.
     * @param windowSizeX The sliding window width.
     * @param windowSizeY The sliding window height.
     * @return The source tile rectangle.
     */
    private Rectangle getSourceTileRectangle(final int x0, final int y0, final int w, final int h,
                                             final int windowSizeX, final int windowSizeY) {

        int sx0 = x0;
        int sy0 = y0;
        int sw = w;
        int sh = h;
        final int halfWindowSizeX = windowSizeX / 2;
        final int halfWindowSizeY = windowSizeY / 2;

        if (x0 >= halfWindowSizeX) {
            sx0 -= halfWindowSizeX;
            sw += halfWindowSizeX;
        }

        if (y0 >= halfWindowSizeY) {
            sy0 -= halfWindowSizeY;
            sh += halfWindowSizeY;
        }

        if (x0 + w + halfWindowSizeX <= sourceImageWidth) {
            sw += halfWindowSizeX;
        }

        if (y0 + h + halfWindowSizeY <= sourceImageHeight) {
            sh += halfWindowSizeY;
        }

        return new Rectangle(sx0, sy0, sw, sh);
    }

    private static class TileData {
        final Tile tile;
        final ProductData dataBuffer;
        final String bandName;

        public TileData(final Tile tile, final String bandName) {
            this.tile = tile;
            this.dataBuffer = tile.getDataBuffer();
            this.bandName = bandName;
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
            super(CompactPolStokesParametersOp.class);
        }
    }
}