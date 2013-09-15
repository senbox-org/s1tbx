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

import Jama.Matrix;
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
import org.esa.beam.util.StringUtils;
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.datamodel.Unit;
import org.esa.nest.gpf.OperatorUtils;
import org.esa.nest.gpf.ReaderUtils;
import org.esa.nest.gpf.StackUtils;
import org.esa.nest.gpf.TileIndex;

import java.awt.*;
import java.util.Map;

@OperatorMetadata(alias = "CoherenceOptimizationOp",
		          category = "InSAR\\Products",
		          description = "Estimate optimum coherence from stack of coregistered images")
public class CoherenceOptimizationOp extends Operator {

    @SourceProduct
    private Product sourceProduct;

    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "The sliding window size", interval = "[1, 100]", defaultValue = "5", label="Window Size")
    private int windowSize = 5;

    @Parameter(description = "Output optimum coherence in complex", defaultValue = "false",
               label="Output optimum coherence in complex")
    private boolean outputInComplex = false;

    private int halfWindowSize = 0;
    private int sourceImageWidth = 0;
    private int sourceImageHeight = 0;
    private int numOfSlaveProducts = 0;
    private final Band[] masterBands = new Band[8];
    private Band[] slaveBands = null;
    private String[] slaveDates = null;
    private String[] targetBandNames = null;

    private static final String PRODUCT_NAME = "coherence_opt";


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
            halfWindowSize = windowSize / 2;

            getSourceBands();

            createTargetProduct();

        } catch (Exception e) {
            throw new OperatorException(e);
        }
    }

    /**
     * Check input product format, get source bands and set corresponding flag.
     */
    private void getSourceBands() {

        final String masterTag = "mst";
        final MetadataElement masterMetaData = AbstractMetadata.getAbstractedMetadata(sourceProduct);
        final String masterDate = OperatorUtils.getAcquisitionDate(masterMetaData);
        getBands(masterTag, masterDate, masterMetaData, masterBands, 0);

        final String slaveTag = "slv";
        final String slaveMetadataRoot = AbstractMetadata.SLAVE_METADATA_ROOT;
        MetadataElement[] slaveMetaData = sourceProduct.getMetadataRoot().getElement(slaveMetadataRoot).getElements();

        numOfSlaveProducts = slaveMetaData.length;
        slaveBands = new Band[numOfSlaveProducts*8];
        slaveDates = new String[numOfSlaveProducts];
        for (int i = 0; i < numOfSlaveProducts; i++) {
            slaveDates[i] = OperatorUtils.getAcquisitionDate(slaveMetaData[i]);
            getBands(slaveTag, slaveDates[i], slaveMetaData[i], slaveBands, i*8);
        }
    }

    /**
     * Get source bands for a master/slave product in the coregistered stack product.
     * @param tag Tag to distinguish master and slave products.
     * @param date Product acquisition date
     * @param root Root of the metadata of the given product
     * @param sourceBands Band array holding all source bands
     * @param idx Index indicating where the source bands should be saved in the sourceBands array
     * @throws OperatorException The exceptions
     */
    private void getBands(
            final String tag, final String date, final MetadataElement root, final Band[] sourceBands, final int idx)
            throws OperatorException {

        boolean hasHH_i = false, hasHH_q = false;
        boolean hasHV_i = false, hasHV_q = false;
        boolean hasVV_i = false, hasVV_q = false;
        boolean hasVH_i = false, hasVH_q = false;

        String[] masterBandNames = null;
        if (tag.contains("mst")) {
            masterBandNames = StackUtils.getMasterBandNames(sourceProduct);
        }

        final int numOfBands = sourceProduct.getNumBands();
        final String[] bandNames = sourceProduct.getBandNames();
        for (int i = 0; i < numOfBands; i++) {
            String bandName = bandNames[i];

            if (masterBandNames != null) {
                if (!StringUtils.contains(masterBandNames, bandName)) {
                    continue;
                }
            } else {
                if (!bandName.contains(tag) || !bandName.contains(date)) {
                    continue;
                }
            }

            final Band band = sourceProduct.getBand(bandName);
            final Unit.UnitType bandUnit = Unit.getUnitType(band);
            final String pol = OperatorUtils.getBandPolarization(band.getName(), root);

            if (pol.contains("hh") && bandUnit== Unit.UnitType.REAL) {
                sourceBands[idx] = band;
                hasHH_i = true;
            } else if (pol.contains("hh") && bandUnit== Unit.UnitType.IMAGINARY) {
                sourceBands[idx+1] = band;
                hasHH_q = true;
            } else if (pol.contains("hv") && bandUnit== Unit.UnitType.REAL) {
                sourceBands[idx+2] = band;
                hasHV_i = true;
            } else if (pol.contains("hv") && bandUnit== Unit.UnitType.IMAGINARY) {
                sourceBands[idx+3] = band;
                hasHV_q = true;
            } else if (pol.contains("vh") && bandUnit== Unit.UnitType.REAL) {
                sourceBands[idx+4] = band;
                hasVH_i = true;
            } else if (pol.contains("vh") && bandUnit== Unit.UnitType.IMAGINARY) {
                sourceBands[idx+5] = band;
                hasVH_q = true;
            } else if (pol.contains("vv") && bandUnit== Unit.UnitType.REAL) {
                sourceBands[idx+6] = band;
                hasVV_i = true;
            } else if (pol.contains("vv") && bandUnit== Unit.UnitType.IMAGINARY) {
                sourceBands[idx+7] = band;
                hasVV_q = true;
            }
        }

        if (!hasHH_i || !hasHH_q || !hasHV_i || !hasHV_q || !hasVV_i || !hasVV_q || !hasVH_i || !hasVH_q) {
            throw new OperatorException("Full polarization coregistered stack is expected.");
        }
    }

    /**
     * Create target product
     */
    private void createTargetProduct() {

        sourceImageWidth = sourceProduct.getSceneRasterWidth();
        sourceImageHeight = sourceProduct.getSceneRasterHeight();

        targetProduct = new Product(PRODUCT_NAME,
                                    sourceProduct.getProductType(),
                                    sourceImageWidth,
                                    sourceImageHeight);

        addSelectedBands();

        OperatorUtils.copyProductNodes(sourceProduct, targetProduct);
    }

    /**
     * Add bands to the target product.
     * @throws OperatorException The exception.
     */
    private void addSelectedBands() throws OperatorException {

        String[] prefix = null;

        int n; // number of target bands per slave product
        if (outputInComplex) {
            n = 6;
            prefix = new String[n];
            prefix[0] = "i_coh_opt_1";
            prefix[1] = "q_coh_opt_1";
            prefix[2] = "i_coh_opt_2";
            prefix[3] = "q_coh_opt_2";
            prefix[4] = "i_coh_opt_3";
            prefix[5] = "q_coh_opt_3";
        } else {
            n = 3;
            prefix = new String[n];
            prefix[0] = "coh_opt_1";
            prefix[1] = "coh_opt_2";
            prefix[2] = "coh_opt_3";
        }

        targetBandNames = new String[numOfSlaveProducts*n];

        Band targetBandI, targetBandQ;
        for (int i = 0; i < numOfSlaveProducts; i++) {

            for (int j = 0; j < n; j++) {
                targetBandNames[i*n+j] = prefix[j] + '_' + slaveDates[i];
            }

            if (outputInComplex) {
                for (int j = 0; j < n; j = j + 2) {
                    targetBandI = targetProduct.addBand(targetBandNames[i*n+j], ProductData.TYPE_FLOAT32);
                    targetBandI.setUnit(Unit.REAL);
                    targetBandQ = targetProduct.addBand(targetBandNames[i*n+j+1], ProductData.TYPE_FLOAT32);
                    targetBandQ.setUnit(Unit.IMAGINARY);

                    final String suffix = "_coh_opt_" + (j/2+1) + '_' + slaveDates[i];
                    ReaderUtils.createVirtualIntensityBand(targetProduct, targetBandI, targetBandQ, suffix);
                    ReaderUtils.createVirtualPhaseBand(targetProduct, targetBandI, targetBandQ, suffix);
                }
            } else {
                for (int j = 0; j < n; j++) {
                    targetBandI = targetProduct.addBand(targetBandNames[i*n+j], ProductData.TYPE_FLOAT32);
                    targetBandI.setUnit(Unit.INTENSITY);
                }
            }
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
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm)
            throws OperatorException {
        try {
            final int x0 = targetRectangle.x;
            final int y0 = targetRectangle.y;
            final int w = targetRectangle.width;
            final int h = targetRectangle.height;
            final int maxY = y0 + h;
            final int maxX = x0 + w;
            //System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

            final TileIndex trgIndex = new TileIndex(targetTiles.get(getTargetProduct().getBandAt(0)));
            final Rectangle sourceRectangle = getSourceRectangle(x0, y0, w, h);

            final Tile sourceTile = getSourceTile(masterBands[0], sourceRectangle);
            final ProductData[] masterDataBuffers = new ProductData[masterBands.length];
            for (int i = 0; i < masterBands.length; ++i) {
                masterDataBuffers[i] = getSourceTile(masterBands[i], sourceRectangle).getDataBuffer();
            }

            final int n = 8; // number of slave bands in each slave product
            int m; // number of target bands produced by each slave product
            if (outputInComplex) {
                m = 6;
            } else {
                m = 3;
            }
            for (int i = 0; i < numOfSlaveProducts; ++i) {

                final ProductData[] slaveDataBuffers = new ProductData[n];
                for (int j = 0; j < slaveDataBuffers.length; ++j) {
                    slaveDataBuffers[j] = getSourceTile(slaveBands[i*n+j], sourceRectangle).getDataBuffer();
                }

                final ProductData[] targetDataBuffers = new ProductData[m];
                for (int j = 0; j < targetDataBuffers.length; ++j) {
                    targetDataBuffers[j] = targetTiles.get(targetProduct.getBand(targetBandNames[i*m+j])).getDataBuffer();
                }

                computeOptimumCoherence(x0, y0, maxX, maxY, trgIndex, sourceTile,
                                        masterDataBuffers, slaveDataBuffers, targetDataBuffers);
            }
        } catch (Exception e) {
            throw new OperatorException(e);
        }
    }

    private void computeOptimumCoherence(final int x0, final int y0, final int maxX, final int maxY,
                                         final TileIndex trgIndex, final Tile sourceTile,
                                         final ProductData[] masterDataBuffers, final ProductData[] slaveDataBuffers,
                                         final ProductData[] targetDataBuffers) {

        final double[][] T11Re = new double[3][3];
        final double[][] T11Im = new double[3][3];
        final double[][] T12Re = new double[3][3];
        final double[][] T12Im = new double[3][3];
        final double[][] T22Re = new double[3][3];
        final double[][] T22Im = new double[3][3];

        final double[][] V1Re = new double[3][3];
        final double[][] V1Im = new double[3][3];
        final double[][] V2Re = new double[3][3];
        final double[][] V2Im = new double[3][3];

        final double[][] EigenVect1Re = new double[3][3];
        final double[][] EigenVect1Im = new double[3][3];
        final double[][] EigenVect2Re = new double[3][3];
        final double[][] EigenVect2Im = new double[3][3];
        final double[] EigenVal1 = new double[3];
        final double[] EigenVal2 = new double[3];

        boolean T11Valid = true, T22Valid = true;
        final double noDataValue = 0;
        for(int y = y0; y < maxY; ++y) {
            trgIndex.calculateStride(y);
            for(int x = x0; x < maxX; ++x) {
                final int idx = trgIndex.getIndex(x);

                getMeanMatrices(x, y, sourceTile, masterDataBuffers, slaveDataBuffers,
                                T11Re, T11Im, T12Re, T12Im, T22Re, T22Im);

                T11Valid = checkMatrixCondition(T11Re, T11Im);
                if (T11Valid) {
                    T22Valid = checkMatrixCondition(T22Re, T22Im);
                }
                if (!T11Valid || !T22Valid) {
                    for (int i = 0; i < targetDataBuffers.length; i++) {
                        targetDataBuffers[i].setElemFloatAt(idx, (float)noDataValue);
                      }
                    continue;
                }

                computeV1V2Matrices(T11Re, T11Im, T12Re, T12Im, T22Re, T22Im, V1Re, V1Im, V2Re, V2Im);

                PolOpUtils.eigenDecompGeneral(3, V1Re, V1Im, EigenVect1Re, EigenVect1Im, EigenVal1);
                PolOpUtils.eigenDecompGeneral(3, V2Re, V2Im, EigenVect2Re, EigenVect2Im, EigenVal2);

                final CohData data = computeCoherence(T11Re, T11Im, T12Re, T12Im, T22Re, T22Im,
                                                      EigenVect1Re, EigenVect1Im, EigenVect2Re, EigenVect2Im);

                if (outputInComplex) {
                    targetDataBuffers[0].setElemFloatAt(idx, (float)data.i_coh_opt_1);
                    targetDataBuffers[1].setElemFloatAt(idx, (float)data.q_coh_opt_1);
                    targetDataBuffers[2].setElemFloatAt(idx, (float)data.i_coh_opt_2);
                    targetDataBuffers[3].setElemFloatAt(idx, (float)data.q_coh_opt_2);
                    targetDataBuffers[4].setElemFloatAt(idx, (float)data.i_coh_opt_3);
                    targetDataBuffers[5].setElemFloatAt(idx, (float)data.q_coh_opt_3);
                } else {
                    targetDataBuffers[0].setElemFloatAt(idx,
                            (float)(data.i_coh_opt_1*data.i_coh_opt_1 + data.q_coh_opt_1*data.q_coh_opt_1));
                    targetDataBuffers[1].setElemFloatAt(idx,
                            (float)(data.i_coh_opt_2*data.i_coh_opt_2 + data.q_coh_opt_2*data.q_coh_opt_2));
                    targetDataBuffers[2].setElemFloatAt(idx,
                            (float)(data.i_coh_opt_3*data.i_coh_opt_3 + data.q_coh_opt_3*data.q_coh_opt_3));
                }
            }
        }
    }

    /**
     * Get source tile rectangle.
     * @param tx0 X coordinate for the upper left corner pixel in the target tile.
     * @param ty0 Y coordinate for the upper left corner pixel in the target tile.
     * @param tw The target tile width.
     * @param th The target tile height.
     * @return The source tile rectangle.
     */
    private Rectangle getSourceRectangle(final int tx0, final int ty0, final int tw, final int th) {

        final int x0 = Math.max(0, tx0 - halfWindowSize);
        final int y0 = Math.max(0, ty0 - halfWindowSize);
        final int xMax = Math.min(tx0 + tw - 1 + halfWindowSize, sourceImageWidth - 1);
        final int yMax = Math.min(ty0 + th - 1 + halfWindowSize, sourceImageHeight - 1);
        final int w = xMax - x0 + 1;
        final int h = yMax - y0 + 1;
        return new Rectangle(x0, y0, w, h);
    }

    private void getMeanMatrices(final int x, final int y, final Tile sourceTile,
                                 final ProductData[] masterDataBuffers, final ProductData[] slaveDataBuffers,
                                 double[][] T11Re, double[][] T11Im, double[][] T12Re, double[][] T12Im,
                                 double[][] T22Re, double[][] T22Im) {

        final double[][] S1Re = new double[2][2];
        final double[][] S1Im = new double[2][2];
        final double[][] S2Re = new double[2][2];
        final double[][] S2Im = new double[2][2];
        final double[][] tempTr = new double[3][3];
        final double[][] tempTi = new double[3][3];

        final Matrix T11ReMat = new Matrix(3,3);
        final Matrix T11ImMat = new Matrix(3,3);
        final Matrix T12ReMat = new Matrix(3,3);
        final Matrix T12ImMat = new Matrix(3,3);
        final Matrix T22ReMat = new Matrix(3,3);
        final Matrix T22ImMat = new Matrix(3,3);

        final TileIndex srcIndex = new TileIndex(sourceTile);

        final int xSt = Math.max(x - halfWindowSize, 0);
        final int xEd = Math.min(x + halfWindowSize, sourceImageWidth - 1);
        final int ySt = Math.max(y - halfWindowSize, 0);
        final int yEd = Math.min(y + halfWindowSize, sourceImageHeight - 1);
        final int num = (yEd - ySt + 1)*(xEd - xSt + 1);

        for (int yy = ySt; yy <= yEd; ++yy) {
            srcIndex.calculateStride(yy);
            for (int xx = xSt; xx <= xEd; ++xx) {
                final int idx = srcIndex.getIndex(xx);

                PolOpUtils.getComplexScatterMatrix(idx, masterDataBuffers, S1Re, S1Im);
                PolOpUtils.getComplexScatterMatrix(idx, slaveDataBuffers, S2Re, S2Im);

                PolOpUtils.computeCoherencyMatrixT3(S1Re, S1Im, tempTr, tempTi);
                T11ReMat.plusEquals(new Matrix(tempTr));
                T11ImMat.plusEquals(new Matrix(tempTi));

                PolOpUtils.computeCoherencyMatrixT3(S2Re, S2Im, tempTr, tempTi);
                T22ReMat.plusEquals(new Matrix(tempTr));
                T22ImMat.plusEquals(new Matrix(tempTi));

                PolOpUtils.computeCorrelationMatrix(S1Re, S1Im, S2Re, S2Im, tempTr, tempTi);
                T12ReMat.plusEquals(new Matrix(tempTr));
                T12ImMat.plusEquals(new Matrix(tempTi));
            }
        }

        T11ReMat.timesEquals(1.0/num);
        T11ImMat.timesEquals(1.0/num);
        T12ReMat.timesEquals(1.0/num);
        T12ImMat.timesEquals(1.0/num);
        T22ReMat.timesEquals(1.0/num);
        T22ImMat.timesEquals(1.0/num);

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                T11Re[i][j] = T11ReMat.get(i,j);
                T11Im[i][j] = T11ImMat.get(i,j);
                T12Re[i][j] = T12ReMat.get(i,j);
                T12Im[i][j] = T12ImMat.get(i,j);
                T22Re[i][j] = T22ReMat.get(i,j);
                T22Im[i][j] = T22ImMat.get(i,j);
            }
        }
    }

    private static boolean checkMatrixCondition(final double[][] TRe, final double[][] TIm) {

        final double[][] M = new double[6][6];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                M[i][j] = TRe[i][j];
                M[i+3][j+3] = TRe[i][j];
                M[i][j+3] = -TIm[i][j];
                M[i+3][j] = TIm[i][j];
            }
        }
        Matrix Mat = new Matrix(M);
        return (Mat.det() > 0.001);
    }

    private static void computeV1V2Matrices(final double[][] T11Re, final double[][] T11Im, final double[][] T12Re,
                                            final double[][] T12Im, final double[][] T22Re, final double[][] T22Im,
                                            final double[][] V1Re, final double[][] V1Im,
                                            final double[][] V2Re, final double[][] V2Im) {

        final double[][] invT11Re = new double[3][3];
        final double[][] invT11Im = new double[3][3];

        PolOpUtils.inverseComplexMatrix(3, T11Re, T11Im, invT11Re, invT11Im);
        final Matrix invT11ReMat = new Matrix(invT11Re);
        final Matrix invT11ImMat = new Matrix(invT11Im);

        final double[][] invT22Re = new double[3][3];
        final double[][] invT22Im = new double[3][3];

        PolOpUtils.inverseComplexMatrix(3, T22Re, T22Im, invT22Re, invT22Im);
        final Matrix invT22ReMat = new Matrix(invT22Re);
        final Matrix invT22ImMat = new Matrix(invT22Im);

        final Matrix T12ReMat = new Matrix(T12Re);
        final Matrix T12ImMat = new Matrix(T12Im);

        // compute V1
        Matrix Tmp1Mat = invT11ReMat.times(T12ReMat).minus(invT11ImMat.times(T12ImMat));
        Matrix Tmp2Mat = invT11ImMat.times(T12ReMat).plus(invT11ReMat.times(T12ImMat));
        Matrix Tmp3Mat = invT22ReMat.times(T12ReMat.transpose()).plus(invT22ImMat.times(T12ImMat.transpose()));
        Matrix Tmp4Mat = invT22ImMat.times(T12ReMat.transpose()).minus(invT22ReMat.times(T12ImMat.transpose()));

        final Matrix V1ReMat = Tmp1Mat.times(Tmp3Mat).minus(Tmp2Mat.times(Tmp4Mat));
        final Matrix V1ImMat = Tmp1Mat.times(Tmp4Mat).plus(Tmp2Mat.times(Tmp3Mat));

        for (int j = 0; j < 3; ++j) {
            for (int i = 0; i < 3; ++i) {
                V1Re[j][i] = V1ReMat.get(j,i);
                V1Im[j][i] = V1ImMat.get(j,i);
            }
        }

        // compute V2
        Tmp1Mat = invT22ReMat.times(T12ReMat.transpose()).plus(invT22ImMat.times(T12ImMat.transpose()));
        Tmp2Mat = invT22ImMat.times(T12ReMat.transpose()).minus(invT22ReMat.times(T12ImMat.transpose()));
        Tmp3Mat = invT11ReMat.times(T12ReMat).minus(invT11ImMat.times(T12ImMat));
        Tmp4Mat = invT11ImMat.times(T12ReMat).plus(invT11ReMat.times(T12ImMat));

        final Matrix V2ReMat = Tmp1Mat.times(Tmp3Mat).minus(Tmp2Mat.times(Tmp4Mat));
        final Matrix V2ImMat = Tmp1Mat.times(Tmp4Mat).plus(Tmp2Mat.times(Tmp3Mat));

        for (int j = 0; j < 3; ++j) {
            for (int i = 0; i < 3; ++i) {
                V2Re[j][i] = V2ReMat.get(j,i);
                V2Im[j][i] = V2ImMat.get(j,i);
            }
        }
    }

    /**
     * Compute optimum coherence values.
     * @param T11Re Real part of T11 matrix
     * @param T11Im Imaginary part of T11 matrix
     * @param T12Re Real part of T12 matrix
     * @param T12Im Imaginary part of T12 matrix
     * @param T22Re Real part of T22 matrix
     * @param T22Im Imaginary part of T22 matrix
     * @param EigenVect1Re Real part of eigenvectors of V1 matrix
     * @param EigenVect1Im Imaginary part of eigenvectors of V1 matrix
     * @param EigenVect2Re Real part of eigenvectors of V2 matrix
     * @param EigenVect2Im Imaginary part of eigenvectors of V2 matrix
     * @return The optimum coherence values
     */
    private static CohData computeCoherence(
            final double[][] T11Re, final double[][] T11Im, final double[][] T12Re,
            final double[][] T12Im, final double[][] T22Re, final double[][] T22Im,
            final double[][] EigenVect1Re, final double[][] EigenVect1Im,
            final double[][] EigenVect2Re, final double[][] EigenVect2Im) {

        final double[] i_coh_opt = new double[3];
        final double[] q_coh_opt = new double[3];
        double tmp1, tmp2, tmp3, tmp4, tmp5, w1T11w1, w2T22w2, w1T12w2Re, w1T12w2Im;

        final Matrix T11ReMat = new Matrix(T11Re);
        final Matrix T11ImMat = new Matrix(T11Im);
        final Matrix T12ReMat = new Matrix(T12Re);
        final Matrix T12ImMat = new Matrix(T12Im);
        final Matrix T22ReMat = new Matrix(T22Re);
        final Matrix T22ImMat = new Matrix(T22Im);

        for (int i = 0; i < 3; ++i) {
            final double[] w1Re = {EigenVect1Re[0][i], EigenVect1Re[1][i], EigenVect1Re[2][i]};
            final double[] w1Im = {EigenVect1Im[0][i], EigenVect1Im[1][i], EigenVect1Im[2][i]};
            final double[] w2Re = {EigenVect2Re[0][i], EigenVect2Re[1][i], EigenVect2Re[2][i]};
            final double[] w2Im = {EigenVect2Im[0][i], EigenVect2Im[1][i], EigenVect2Im[2][i]};

            final Matrix w1ReMat = new Matrix(w1Re, 3);
            final Matrix w1ImMat = new Matrix(w1Im, 3);
            final Matrix w2ReMat = new Matrix(w2Re, 3);
            final Matrix w2ImMat = new Matrix(w2Im, 3);

            final Matrix w1ReMatT = w1ReMat.transpose();
            final Matrix w1ImMatT = w1ImMat.transpose();
            final Matrix w2ReMatT = w2ReMat.transpose();
            final Matrix w2ImMatT = w2ImMat.transpose();

            tmp1 = w1ReMatT.times(T11ReMat).times(w1ReMat).get(0,0);
            tmp2 = w1ReMatT.times(T11ImMat).times(w1ImMat).get(0,0);
            tmp3 = w1ImMatT.times(T11ReMat).times(w1ImMat).get(0,0);
            tmp4 = w1ImMatT.times(T11ImMat).times(w1ReMat).get(0,0);
            w1T11w1 = tmp1 - tmp2 + tmp3 + tmp4;

            tmp1 = w2ReMatT.times(T22ReMat).times(w2ReMat).get(0,0);
            tmp2 = w2ReMatT.times(T22ImMat).times(w2ImMat).get(0,0);
            tmp3 = w2ImMatT.times(T22ReMat).times(w2ImMat).get(0,0);
            tmp4 = w2ImMatT.times(T22ImMat).times(w2ReMat).get(0,0);
            w2T22w2 = tmp1 - tmp2 + tmp3 + tmp4;

            tmp5 = Math.sqrt(w1T11w1*w2T22w2);

            tmp1 = w1ReMatT.times(T12ReMat).times(w2ReMat).get(0,0);
            tmp2 = w1ReMatT.times(T12ImMat).times(w2ImMat).get(0,0);
            tmp3 = w1ImMatT.times(T12ReMat).times(w2ImMat).get(0,0);
            tmp4 = w1ImMatT.times(T12ImMat).times(w2ReMat).get(0,0);
            w1T12w2Re = tmp1 - tmp2 + tmp3 + tmp4;

            tmp1 = w1ReMatT.times(T12ReMat).times(w2ImMat).get(0,0);
            tmp2 = w1ReMatT.times(T12ImMat).times(w2ReMat).get(0,0);
            tmp3 = w1ImMatT.times(T12ReMat).times(w2ReMat).get(0,0);
            tmp4 = w1ImMatT.times(T12ImMat).times(w2ImMat).get(0,0);
            w1T12w2Im = tmp1 + tmp2 - tmp3 + tmp4;

            i_coh_opt[i] = w1T12w2Re / tmp5;
            q_coh_opt[i] = w1T12w2Im / tmp5;
        }

        return new CohData(i_coh_opt[0], q_coh_opt[0], i_coh_opt[1], q_coh_opt[1], i_coh_opt[2], q_coh_opt[2]);
    }


    public static class CohData {

        public final double i_coh_opt_1;
        public final double q_coh_opt_1;
        public final double i_coh_opt_2;
        public final double q_coh_opt_2;
        public final double i_coh_opt_3;
        public final double q_coh_opt_3;

        public CohData(final double i_coh_opt_1, final double q_coh_opt_1,
                       final double i_coh_opt_2, final double q_coh_opt_2,
                       final double i_coh_opt_3, final double q_coh_opt_3) {

            this.i_coh_opt_1 = i_coh_opt_1;
            this.q_coh_opt_1 = q_coh_opt_1;
            this.i_coh_opt_2 = i_coh_opt_2;
            this.q_coh_opt_2 = q_coh_opt_2;
            this.i_coh_opt_3 = i_coh_opt_3;
            this.q_coh_opt_3 = q_coh_opt_3;
        }
    }

    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.beam.framework.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     *
     * @see org.esa.beam.framework.gpf.OperatorSpi#createOperator()
     * @see org.esa.beam.framework.gpf.OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(CoherenceOptimizationOp.class);
        }
    }
}
