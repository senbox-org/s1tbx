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
import java.util.HashMap;
import java.util.Map;

/**
 * Generate polarimetric covariance or coherency matrix for a given full pol product
 */

@OperatorMetadata(alias="Polarimetric-Matrices",
                  category = "Polarimetric Tools",
                  description="Generates covariance or coherency matrix for given product")
public final class PolarimetricMatricesOp extends Operator {

    @SourceProduct(alias="source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(valueSet = {C3, C4, T3, T4}, description = "The covariance or coherency matrix",
               defaultValue=T3, label="Polarimetric Matrix")
    private String matrix = T3;

    private PolBandUtils.QuadSourceBand[] srcBandList;
    private final Map<Band, MatrixElem> matrixBandMap = new HashMap<Band, MatrixElem>(8);

    private PolBandUtils.MATRIX matrixType = PolBandUtils.MATRIX.C3;

    public static final String C3 = "C3"; // set to public because unit tests need to use it
    public static final String C4 = "C4";
    public static final String T3 = "T3";
    public static final String T4 = "T4";

    /**
     * Set matrix type. This function is used by unit test only.
     * @param s The matrix type.
     */
    public void SetMatrixType(final String s) {

        if (s.equals(C3) || s.equals(C4) || s.equals(T3) || s.equals(T4)) {
            matrix = s;
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
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs during operator initialisation.
     * @see #getTargetProduct()
     */
    @Override
    public void initialize() throws OperatorException {

        try {
            srcBandList = PolBandUtils.getSourceBands(sourceProduct,
                    PolBandUtils.getSourceProductType(sourceProduct));

            createTargetProduct();

            updateTargetProductMetadata();
        } catch(Throwable e) {
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

        OperatorUtils.copyProductNodes(sourceProduct, targetProduct);
    }

    /**
     * Add bands to the target product.
     * @throws OperatorException The exception.
     */
    private void addSelectedBands() throws OperatorException {

        String[] bandNames;
        if (matrix.equals(C3)) {

            bandNames = PolBandUtils.getC3BandNames();
            matrixType = PolBandUtils.MATRIX.C3;

        } else if (matrix.equals(C4)) {

            bandNames = PolBandUtils.getC4BandNames();
            matrixType = PolBandUtils.MATRIX.C4;

        } else if (matrix.equals(T3)) {

            bandNames = PolBandUtils.getT3BandNames();
            matrixType = PolBandUtils.MATRIX.T3;

        } else if (matrix.equals(T4)) {

            bandNames = PolBandUtils.getT4BandNames();
            matrixType = PolBandUtils.MATRIX.T4;

        } else {
            throw new OperatorException("Unknown matrix type: " + matrix);
        }

        for(PolBandUtils.QuadSourceBand bandList : srcBandList) {
            final Band[] targetBands = PolBandUtils.addBands(targetProduct, bandNames, bandList.suffix);
            bandList.addTargetBands(targetBands);
        }

        mapMatrixElemToBands();
    }

    private void mapMatrixElemToBands() {
        final Band[] bands = targetProduct.getBands();
        for (Band band : bands){
            final String targetBandName = band.getName();

            if (targetBandName.contains("11")) {
                matrixBandMap.put(band, new MatrixElem(0,0, false));
            } else if (targetBandName.contains("12_real")) {
                matrixBandMap.put(band, new MatrixElem(0,1, false));
            } else if (targetBandName.contains("12_imag")) {
                matrixBandMap.put(band, new MatrixElem(0,1, true));
            } else if (targetBandName.contains("13_real")) {
                matrixBandMap.put(band, new MatrixElem(0,2, false));
            } else if (targetBandName.contains("13_imag")) {
                matrixBandMap.put(band, new MatrixElem(0,2, true));
            } else if (targetBandName.contains("14_real")) {
                matrixBandMap.put(band, new MatrixElem(0,3, false));
            } else if (targetBandName.contains("14_imag")) {
                matrixBandMap.put(band, new MatrixElem(0,3, true));
            } else if (targetBandName.contains("22")) {
                matrixBandMap.put(band, new MatrixElem(1,1, false));
            } else if (targetBandName.contains("23_real")) {
                matrixBandMap.put(band, new MatrixElem(1,2, false));
            } else if (targetBandName.contains("23_imag")) {
                matrixBandMap.put(band, new MatrixElem(1,2, true));
            } else if (targetBandName.contains("24_real")) {
                matrixBandMap.put(band, new MatrixElem(1,3, false));
            } else if (targetBandName.contains("24_imag")) {
                matrixBandMap.put(band, new MatrixElem(1,3, true));
            } else if (targetBandName.contains("33")) {
                matrixBandMap.put(band, new MatrixElem(2,2, false));
            } else if (targetBandName.contains("34_real")) {
                matrixBandMap.put(band, new MatrixElem(2,3, false));
            } else if (targetBandName.contains("34_imag")) {
                matrixBandMap.put(band, new MatrixElem(2,3, true));
            } else if (targetBandName.contains("44")) {
                matrixBandMap.put(band, new MatrixElem(3,3, false));
            }
        }
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
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          if an error occurs during computation of the target rasters.
     */
    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {

        final double[][] Sr = new double[2][2];
        final double[][] Si = new double[2][2];
        final double[][] tempRe;
        final double[][] tempIm;

        if (matrixType.equals(PolBandUtils.MATRIX.C3) || matrixType.equals(PolBandUtils.MATRIX.T3)) {
            tempRe = new double[3][3];
            tempIm = new double[3][3];
        } else { // matrixType.equals(MATRIX.C4) || matrixType.equals(MATRIX.T4)
            tempRe = new double[4][4];
            tempIm = new double[4][4];
        }

        for(final PolBandUtils.QuadSourceBand bandList : srcBandList) {
            try {
                // save tile data for quicker access
                final TileData[] tileDataList = new TileData[bandList.targetBands.length];
                int i = 0;
                for (Band targetBand : bandList.targetBands){
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

                final int numElems = tileDataList[0].dataBuffer.getNumElems();
                for(int idx=0; idx<numElems; ++idx) {

                    PolOpUtils.getComplexScatterMatrix(idx, dataBuffers, Sr, Si);

                    if (matrixType.equals(PolBandUtils.MATRIX.C3)) {
                        PolOpUtils.computeCovarianceMatrixC3(Sr, Si, tempRe, tempIm);
                    } else if (matrixType.equals(PolBandUtils.MATRIX.C4)) {
                        PolOpUtils.computeCovarianceMatrixC4(Sr, Si, tempRe, tempIm);
                    } else if (matrixType.equals(PolBandUtils.MATRIX.T3)) {
                        PolOpUtils.computeCoherencyMatrixT3(Sr, Si, tempRe, tempIm);
                    } else if (matrixType.equals(PolBandUtils.MATRIX.T4)) {
                        PolOpUtils.computeCoherencyMatrixT4(Sr, Si, tempRe, tempIm);
                    }

                    for (final TileData tileData : tileDataList){

                        if(tileData.elem.isImaginary) {
                            tileData.dataBuffer.setElemFloatAt(idx, (float)tempIm[tileData.elem.i][tileData.elem.j]);
                        } else {
                            tileData.dataBuffer.setElemFloatAt(idx, (float)tempRe[tileData.elem.i][tileData.elem.j]);
                        }
                    }
                }

            } catch(Throwable e) {
                OperatorUtils.catchOperatorException(getId(), e);
            }
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
     * {@code META-INF/services/org.esa.beam.framework.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     * @see org.esa.beam.framework.gpf.OperatorSpi#createOperator()
     * @see org.esa.beam.framework.gpf.OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(PolarimetricMatricesOp.class);
        }
    }
}