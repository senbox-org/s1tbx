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
import org.apache.commons.math3.util.FastMath;
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
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.eo.Constants;
import org.esa.snap.engine_utilities.gpf.InputProductValidator;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.gpf.TileIndex;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Simulation of Compact Pol data from RADARSAT-2 Quad Pol data
 */

@OperatorMetadata(alias = "CP-Simulation",
        category = "Radar/Polarimetric/Compact Polarimetry",
        authors = "Jun Lu, Luis Veci",
        copyright = "Copyright (C) 2018 SkyWatch Space Applications Inc.",
        description = "Simulation of Compact Pol data from Quad Pol data")
public final class CompactPolDataSimulationOp extends Operator implements QuadPolProcessor, CompactPolProcessor {

    public static final String C2 = "Covariance Matrix C2"; // set to public because unit tests need to use it
    public static final String S2 = "Scatter Vector S2";
    private final Map<Band, MatrixElem> matrixBandMap = new HashMap<>(8);
    @SourceProduct(alias = "source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;
    @Parameter(valueSet = {CompactPolProcessor.rch, CompactPolProcessor.lch},
            description = "The compact mode", defaultValue = CompactPolProcessor.rch, label = "Compact Mode")
    private String compactMode = CompactPolProcessor.rch;
    @Parameter(valueSet = {C2, S2}, description = "The output simulated compact pol data format",
            defaultValue = C2, label = "Output Format")
    private String outputFormat = C2;
    @Parameter(description = "The noise power", interval = "(-35, -15)", defaultValue = "-25",
            label = "Noise Power (dB)")
    private double noisePower = -25;
    @Parameter(description = "Simulate noise floor", defaultValue = "false", label = "Simulate noise floor")
    private Boolean simulateNoiseFloor = false;
    private PolBandUtils.PolSourceBand[] srcBandList;
    private PolBandUtils.MATRIX sourceProductType = null;
    private double sigma = 0.0;
    private Random random = new Random();
    private boolean useRCMConvention = false;

    private static final String quarterPi = "PI/4 Mode";
    private static final String dcp = "Dual Circular Polarimetric Mode";
    private static final String dualHHHV = "Dual HH/HV";
    private static final String dualVVVH = "Dual VV/VH";

    /**
     * Set compact mode. This function is used by unit test only.
     *
     * @param m The compact mode.
     */
    public void SetCompactMode(final String m) {

        if (m.equals(quarterPi) || m.equals(CompactPolProcessor.rch) || m.equals(CompactPolProcessor.lch) ||
                m.equals(dcp) || m.equals(dualHHHV) || m.equals(dualVVVH)) {
            compactMode = m;
        } else {
            throw new OperatorException(m + " is an invalid compact mode.");
        }
    }

    public void SetOutputFormat(final String f) {

        if (f.equals(C2) || f.equals(S2)) {
            outputFormat = f;
        } else {
            throw new OperatorException(f + " is an invalid output format.");
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
            srcBandList = PolBandUtils.getSourceBands(sourceProduct, sourceProductType);
            useRCMConvention = Boolean.getBoolean(SystemUtils.getApplicationContextId() + ".hybridmode.useRCMConvention");

            if (simulateNoiseFloor) {
                sigma = Math.sqrt(FastMath.pow(10.0, noisePower / 10.0) / 2.0);
            }
            createTargetProduct();

            updateTargetProductMetadata();
        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
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

        ProductUtils.copyProductNodes(sourceProduct, targetProduct);
    }

    /**
     * Add C2 or S2 bands to the target product.
     *
     * @throws OperatorException The exception.
     */
    private void addSelectedBands() throws OperatorException {

        if (sourceProductType != PolBandUtils.MATRIX.FULL) {
            throw new OperatorException("Full polarimetric product is expected.");
        }

        String[] bandNames = null;
        if (outputFormat.equals(C2)) {
            bandNames = PolBandUtils.getC2BandNames();
            addBands(bandNames);
            mapMatrixElemToBandsC2();
        } else {
            if (compactMode.equals(CompactPolProcessor.lch)) {
                bandNames = PolBandUtils.getLCHModeS2BandNames();
            } else {
                bandNames = PolBandUtils.getRCHModeS2BandNames();
            }
            addBands(bandNames);
            mapMatrixElemToBandsS2();
        }
    }

    private void addBands(final String[] bandNames) {

        for (PolBandUtils.PolSourceBand bandList : srcBandList) {
            final java.util.List<Band> tgtBandList = new ArrayList<>(bandNames.length);
            for (String targetBandName : bandNames) {

                final Band targetBand = new Band(targetBandName + bandList.suffix,
                        ProductData.TYPE_FLOAT32,
                        targetProduct.getSceneRasterWidth(),
                        targetProduct.getSceneRasterHeight());
                if (targetBandName.startsWith("i_") || targetBandName.contains("_real")) {
                    targetBand.setUnit(Unit.REAL);
                } else if (targetBandName.startsWith("q_") || targetBandName.contains("_imag")) {
                    targetBand.setUnit(Unit.IMAGINARY);
                } else {
                    targetBand.setUnit(Unit.INTENSITY);
                }
                tgtBandList.add(targetBand);
                targetProduct.addBand(targetBand);
            }
            final Band[] targetBands = tgtBandList.toArray(new Band[tgtBandList.size()]);

            bandList.addTargetBands(targetBands);
        }
    }

    private void mapMatrixElemToBandsC2() {

        final Band[] bands = targetProduct.getBands();
        for (Band band : bands) {
            final String targetBandName = band.getName();

            if (PolBandUtils.isBandForMatrixElement(targetBandName, "11")) {
                matrixBandMap.put(band, new MatrixElem(0, 0, false));
            } else if (PolBandUtils.isBandForMatrixElement(targetBandName, "12_real")) {
                matrixBandMap.put(band, new MatrixElem(0, 1, false));
            } else if (PolBandUtils.isBandForMatrixElement(targetBandName, "12_imag")) {
                matrixBandMap.put(band, new MatrixElem(0, 1, true));
            } else if (PolBandUtils.isBandForMatrixElement(targetBandName, "13_real")) {
                matrixBandMap.put(band, new MatrixElem(0, 2, false));
            } else if (PolBandUtils.isBandForMatrixElement(targetBandName, "13_imag")) {
                matrixBandMap.put(band, new MatrixElem(0, 2, true));
            } else if (PolBandUtils.isBandForMatrixElement(targetBandName, "14_real")) {
                matrixBandMap.put(band, new MatrixElem(0, 3, false));
            } else if (PolBandUtils.isBandForMatrixElement(targetBandName, "14_imag")) {
                matrixBandMap.put(band, new MatrixElem(0, 3, true));
            } else if (PolBandUtils.isBandForMatrixElement(targetBandName, "22")) {
                matrixBandMap.put(band, new MatrixElem(1, 1, false));
            } else if (PolBandUtils.isBandForMatrixElement(targetBandName, "23_real")) {
                matrixBandMap.put(band, new MatrixElem(1, 2, false));
            } else if (PolBandUtils.isBandForMatrixElement(targetBandName, "23_imag")) {
                matrixBandMap.put(band, new MatrixElem(1, 2, true));
            } else if (PolBandUtils.isBandForMatrixElement(targetBandName, "24_real")) {
                matrixBandMap.put(band, new MatrixElem(1, 3, false));
            } else if (PolBandUtils.isBandForMatrixElement(targetBandName, "24_imag")) {
                matrixBandMap.put(band, new MatrixElem(1, 3, true));
            } else if (PolBandUtils.isBandForMatrixElement(targetBandName, "33")) {
                matrixBandMap.put(band, new MatrixElem(2, 2, false));
            } else if (PolBandUtils.isBandForMatrixElement(targetBandName, "34_real")) {
                matrixBandMap.put(band, new MatrixElem(2, 3, false));
            } else if (PolBandUtils.isBandForMatrixElement(targetBandName, "34_imag")) {
                matrixBandMap.put(band, new MatrixElem(2, 3, true));
            } else if (PolBandUtils.isBandForMatrixElement(targetBandName, "44")) {
                matrixBandMap.put(band, new MatrixElem(3, 3, false));
            }
        }
    }

    private void mapMatrixElemToBandsS2() {

        final Band[] bands = targetProduct.getBands();
        for (Band band : bands) {
            final String targetBandName = band.getName();

            if (targetBandName.contains("i_45H") || targetBandName.contains("i_RH") ||
                    targetBandName.contains("i_RCH") ||
                    targetBandName.contains("i_LH") || targetBandName.contains("i_RR") ||
                    targetBandName.contains("i_HH") || targetBandName.contains("i_VH")) {
                matrixBandMap.put(band, new MatrixElem(0, 0, false));

            } else if (targetBandName.contains("q_45H") || targetBandName.contains("q_RH") ||
                    targetBandName.contains("q_RCH") ||
                    targetBandName.contains("q_LH") || targetBandName.contains("q_RR") ||
                    targetBandName.contains("q_HH") || targetBandName.contains("q_VH")) {
                matrixBandMap.put(band, new MatrixElem(0, 0, true));

            } else if (targetBandName.contains("i_45V") || targetBandName.contains("i_RV") ||
                    targetBandName.contains("i_RCV") ||
                    targetBandName.contains("i_LV") || targetBandName.contains("i_RL") ||
                    targetBandName.contains("i_HV") || targetBandName.contains("i_VV")) {
                matrixBandMap.put(band, new MatrixElem(1, 0, false));

            } else if (targetBandName.contains("q_45V") || targetBandName.contains("q_RV") ||
                    targetBandName.contains("q_RCV") ||
                    targetBandName.contains("q_LV") || targetBandName.contains("q_RL") ||
                    targetBandName.contains("q_HV") || targetBandName.contains("q_VV")) {
                matrixBandMap.put(band, new MatrixElem(1, 0, true));
            }
        }
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
     * Called by the framework in order to compute the stack of tiles for the given target bands.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetTiles     The current tiles to be computed for each target band.
     * @param targetRectangle The area in pixel coordinates to be computed (same for all rasters in <code>targetRasters</code>).
     * @param pm              A progress monitor which should be used to determine computation cancelation requests.
     * @throws org.esa.snap.core.gpf.OperatorException if an error occurs during computation of the target rasters.
     */
    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm)
            throws OperatorException {

        if (outputFormat.equals(C2)) {
            computeTileStackC2(targetTiles, targetRectangle, pm);
        } else {
            computeTileStackS2(targetTiles, targetRectangle, pm);
        }
    }

    private void computeTileStackC2(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) {

        final int x0 = targetRectangle.x;
        final int y0 = targetRectangle.y;
        final int w = targetRectangle.width;
        final int h = targetRectangle.height;
        final int maxY = y0 + h;
        final int maxX = x0 + w;

        final double[][] Sr = new double[2][2];
        final double[][] Si = new double[2][2];
        final double[][] Cr = new double[2][2];
        final double[][] Ci = new double[2][2];

        for (final PolBandUtils.PolSourceBand bandList : srcBandList) {
            try {
                // save tile data for quicker access
                final TileData[] tileDataList = new TileData[bandList.targetBands.length];
                int i = 0;
                for (Band targetBand : bandList.targetBands) {
                    final Tile targetTile = targetTiles.get(targetBand);
                    final MatrixElem elem = matrixBandMap.get(targetBand);

                    tileDataList[i++] = new TileData(targetTile, elem);
                }

                final Tile[] sourceTiles = new Tile[bandList.srcBands.length];
                final ProductData[] dataBuffers = new ProductData[bandList.srcBands.length];
                for (int j = 0; j < bandList.srcBands.length; j++) {
                    sourceTiles[j] = getSourceTile(bandList.srcBands[j], targetRectangle);
                    dataBuffers[j] = sourceTiles[j].getDataBuffer();
                }

                final TileIndex srcIndex = new TileIndex(sourceTiles[0]);
                final TileIndex tgtIndex = new TileIndex(tileDataList[0].tile);

                int srcIdx, tgtIdx;
                for (int y = y0, yy = 0; y < maxY; ++y, ++yy) {
                    srcIndex.calculateStride(y);
                    tgtIndex.calculateStride(y);
                    for (int x = x0, xx = 0; x < maxX; ++x, ++xx) {
                        srcIdx = srcIndex.getIndex(x);
                        tgtIdx = tgtIndex.getIndex(x);

                        getComplexScatterMatrix(srcIdx, dataBuffers, Sr, Si);

                        if (simulateNoiseFloor) {
                            Sr[0][0] += getGaussianRandom();
                            Si[0][0] += getGaussianRandom();
                            Sr[0][1] = 0.5 * (Sr[0][1] + Sr[1][0]) + getGaussianRandom();
                            Sr[1][0] = Sr[0][1];
                            Si[0][1] = 0.5 * (Si[0][1] + Si[1][0]) + +getGaussianRandom();
                            Si[1][0] = Si[0][1];
                            Sr[1][1] += getGaussianRandom();
                            Si[1][1] += getGaussianRandom();
                        } else {
                            Sr[0][1] = 0.5 * (Sr[0][1] + Sr[1][0]);
                            Sr[1][0] = Sr[0][1];
                            Si[0][1] = 0.5 * (Si[0][1] + Si[1][0]);
                            Si[1][0] = Si[0][1];
                        }

                        computeCompactPolCovarianceMatrixC2(Sr, Si, Cr, Ci);

                        for (final TileData tileData : tileDataList) {

                            if (tileData.elem.isImaginary) {
                                tileData.dataBuffer.setElemFloatAt(tgtIdx, (float) Ci[tileData.elem.i][tileData.elem.j]);
                            } else {
                                tileData.dataBuffer.setElemFloatAt(tgtIdx, (float) Cr[tileData.elem.i][tileData.elem.j]);
                            }
                        }
                    }
                }
                /*
                final int numElems = tileDataList[0].dataBuffer.getNumElems();
                for(int idx = 0; idx < numElems; ++idx) {

                    PolOpUtils.getComplexScatterMatrix(idx, dataBuffers, Sr, Si);

                    Sr[0][1] = 0.5*(Sr[0][1] + Sr[1][0]);
                    Sr[1][0] = Sr[0][1];
                    Si[0][1] = 0.5*(Si[0][1] + Si[1][0]);
                    Si[1][0] = Si[0][1];

                    CP.computeCompactPolCovarianceMatrixC2(compactMode, useRCMConvention, Sr, Si, Cr, Ci);

                    for (final TileData tileData : tileDataList){

                        if(tileData.elem.isImaginary) {
                            tileData.dataBuffer.setElemFloatAt(idx, (float)Ci[tileData.elem.i][tileData.elem.j]);
                        } else {
                            tileData.dataBuffer.setElemFloatAt(idx, (float)Cr[tileData.elem.i][tileData.elem.j]);
                        }
                    }
                }*/
            } catch (Throwable e) {
                OperatorUtils.catchOperatorException(getId(), e);
            }
        }
    }

    private double getGaussianRandom() {
        return random.nextGaussian() * sigma;
    }

    private void computeTileStackS2(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) {

        final int x0 = targetRectangle.x;
        final int y0 = targetRectangle.y;
        final int w = targetRectangle.width;
        final int h = targetRectangle.height;
        final int maxY = y0 + h;
        final int maxX = x0 + w;

        final double[][] Sr = new double[2][2];
        final double[][] Si = new double[2][2];
        final double[] kr = new double[2];
        final double[] ki = new double[2];

        for (final PolBandUtils.PolSourceBand bandList : srcBandList) {
            try {
                // save tile data for quicker access
                final TileData[] tileDataList = new TileData[bandList.targetBands.length];
                int i = 0;
                for (Band targetBand : bandList.targetBands) {
                    final Tile targetTile = targetTiles.get(targetBand);
                    final MatrixElem elem = matrixBandMap.get(targetBand);

                    tileDataList[i++] = new TileData(targetTile, elem);
                }

                final Tile[] sourceTiles = new Tile[bandList.srcBands.length];
                final ProductData[] dataBuffers = new ProductData[bandList.srcBands.length];
                for (int j = 0; j < bandList.srcBands.length; j++) {
                    sourceTiles[j] = getSourceTile(bandList.srcBands[j], targetRectangle);
                    dataBuffers[j] = sourceTiles[j].getDataBuffer();
                }

                final TileIndex srcIndex = new TileIndex(sourceTiles[0]);
                final TileIndex tgtIndex = new TileIndex(tileDataList[0].tile);

                int srcIdx, tgtIdx;
                for (int y = y0, yy = 0; y < maxY; ++y, ++yy) {
                    srcIndex.calculateStride(y);
                    tgtIndex.calculateStride(y);
                    for (int x = x0, xx = 0; x < maxX; ++x, ++xx) {
                        srcIdx = srcIndex.getIndex(x);
                        tgtIdx = tgtIndex.getIndex(x);

                        getComplexScatterMatrix(srcIdx, dataBuffers, Sr, Si);

                        if (simulateNoiseFloor) {
                            Sr[0][0] += getGaussianRandom();
                            Si[0][0] += getGaussianRandom();
                            Sr[0][1] = 0.5 * (Sr[0][1] + Sr[1][0]) + getGaussianRandom();
                            Sr[1][0] = Sr[0][1];
                            Si[0][1] = 0.5 * (Si[0][1] + Si[1][0]) + +getGaussianRandom();
                            Si[1][0] = Si[0][1];
                            Sr[1][1] += getGaussianRandom();
                            Si[1][1] += getGaussianRandom();
                        } else {
                            Sr[0][1] = 0.5 * (Sr[0][1] + Sr[1][0]);
                            Sr[1][0] = Sr[0][1];
                            Si[0][1] = 0.5 * (Si[0][1] + Si[1][0]);
                            Si[1][0] = Si[0][1];
                        }

                        computeCompactPolScatteringVector(compactMode, useRCMConvention, Sr, Si, kr, ki);

                        for (final TileData tileData : tileDataList) {

                            if (tileData.elem.isImaginary) {
                                tileData.dataBuffer.setElemFloatAt(tgtIdx, (float) ki[tileData.elem.i]);
                            } else {
                                tileData.dataBuffer.setElemFloatAt(tgtIdx, (float) kr[tileData.elem.i]);
                            }
                        }
                    }
                }

                /*
                final int numElems = tileDataList[0].dataBuffer.getNumElems();
                for(int idx = 0; idx < numElems; ++idx) {

                    PolOpUtils.getComplexScatterMatrix(idx, dataBuffers, Sr, Si);

                    Sr[0][1] = 0.5*(Sr[0][1] + Sr[1][0]);
                    Sr[1][0] = Sr[0][1];
                    Si[0][1] = 0.5*(Si[0][1] + Si[1][0]);
                    Si[1][0] = Si[0][1];

                    CP.computeCompactPolScatteringVector(compactMode, useRCMConvention, Sr, Si, kr, ki);

                    for (final TileData tileData : tileDataList){

                        if(tileData.elem.isImaginary) {
                            tileData.dataBuffer.setElemFloatAt(idx, (float)ki[tileData.elem.i]);
                        } else {
                            tileData.dataBuffer.setElemFloatAt(idx, (float)kr[tileData.elem.i]);
                        }
                    }
                }
                */
            } catch (Throwable e) {
                OperatorUtils.catchOperatorException(getId(), e);
            }
        }
    }

    /**
     * Simulate 2x2 compact pol covariance matrix using scatter matrix for a given pixel in a full pol product.
     *
     * @param scatterRe Real part of the scatter matrix
     * @param scatterIm Imaginary part of the scatter matrix
     * @param Cr        Real part of the covariance matrix
     * @param Ci        Imaginary part of the covariance matrix
     */
    private void computeCompactPolCovarianceMatrixC2(
            final double[][] scatterRe, final double[][] scatterIm,
            final double[][] Cr, final double[][] Ci) {

        final double[] kr = new double[2];
        final double[] ki = new double[2];

        computeCompactPolScatteringVector(compactMode, useRCMConvention, scatterRe, scatterIm, kr, ki);

        computeCovarianceMatrixC2(kr, ki, Cr, Ci);
    }

    /**
     * Simulate compact pol 2x1 complex scattering vector using scatter matrix for a given pixel in a full pol product.
     *
     * @param compactMode Compact polarimetric mode
     * @param scatterRe   Real part of the scatter matrix
     * @param scatterIm   Imaginary part of the scatter matrix
     * @param kr          Real part of the scattering vector
     * @param ki          Imaginary part of the scattering vector
     */
    public static void computeCompactPolScatteringVector(
            final String compactMode, final boolean useRCMConvention,
            final double[][] scatterRe, final double[][] scatterIm,
            final double[] kr, final double[] ki) {

        final double sHHr = scatterRe[0][0];
        final double sHHi = scatterIm[0][0];
        final double sHVr = scatterRe[0][1];
        final double sHVi = scatterIm[0][1];
        final double sVHr = scatterRe[1][0];
        final double sVHi = scatterIm[1][0];
        final double sVVr = scatterRe[1][1];
        final double sVVi = scatterIm[1][1];

        if (compactMode.equals(quarterPi)) {
            kr[0] = (sHHr + sHVr) / Constants.sqrt2;
            ki[0] = (sHHi + sHVi) / Constants.sqrt2;
            kr[1] = (sVVr + sVHr) / Constants.sqrt2;
            ki[1] = (sVVi + sVHi) / Constants.sqrt2;
        } else if (compactMode.equals(CompactPolProcessor.rch) && !useRCMConvention || compactMode.equals(CompactPolProcessor.lch) && useRCMConvention) {
            kr[0] = (sHHr + sHVi) / Constants.sqrt2;
            ki[0] = (sHHi - sHVr) / Constants.sqrt2;
            kr[1] = (sVHr + sVVi) / Constants.sqrt2;
            ki[1] = (sVHi - sVVr) / Constants.sqrt2;
        } else if (compactMode.equals(CompactPolProcessor.lch) && !useRCMConvention || compactMode.equals(CompactPolProcessor.rch) && useRCMConvention) {
            kr[0] = (sHHr - sHVi) / Constants.sqrt2;
            ki[0] = (sHHi + sHVr) / Constants.sqrt2;
            kr[1] = (sVHr - sVVi) / Constants.sqrt2;
            ki[1] = (sVHi + sVVr) / Constants.sqrt2;
        } else if (compactMode.equals(dcp)) {
            kr[0] = (sHHr - sVVr + sHVi + sVHi) / 2.0;
            ki[0] = (sHHi - sVVi - sHVr - sVHr) / 2.0;
            kr[1] = (-sHHi - sVVi + sHVr - sVHr) / 2.0;
            ki[1] = (sHHr + sHHi + sHVi - sVHi) / 2.0;
        } else if (compactMode.equals(dualHHHV)) {
            kr[0] = sHHr;
            ki[0] = sHHi;
            kr[1] = sHVr;
            ki[1] = sHVi;
        } else if (compactMode.equals(dualVVVH)) {
            kr[0] = sVHr;
            ki[0] = sVHi;
            kr[1] = sVVr;
            ki[1] = sVVi;
        }
    }

    private static class MatrixElem {
        public final int i;
        public final int j;
        public final boolean isImaginary;

        MatrixElem(final int i, final int j, final boolean isImaginary) {
            this.i = i;
            this.j = j;
            this.isImaginary = isImaginary;
        }
    }

    private static class TileData {
        final Tile tile;
        final MatrixElem elem;
        final ProductData dataBuffer;

        public TileData(final Tile tile, final MatrixElem elem) {
            this.tile = tile;
            this.elem = elem;
            this.dataBuffer = tile.getDataBuffer();
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
            super(CompactPolDataSimulationOp.class);
        }
    }
}