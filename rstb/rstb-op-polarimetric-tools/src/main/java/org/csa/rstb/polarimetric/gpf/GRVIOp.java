/*
 * Copyright (C) 2020 by Microwave Remote Sensing Lab, IITBombay http://www.mrslab.in
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

import org.apache.commons.math3.util.FastMath;
import com.bc.ceres.core.ProgressMonitor;
import org.csa.rstb.polarimetric.gpf.decompositions.Decomposition;
import org.csa.rstb.polarimetric.gpf.decompositions.DecompositionBase;
import org.csa.rstb.polarimetric.gpf.support.QuadPolProcessor;
import org.csa.rstb.polarimetric.gpf.support.MatrixMath;
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
import java.util.Arrays;
import java.util.Collections;

/**
 * Generate Radar Vegetation Indices for compact-pol product
 *
 * Note: 1. This operator currently computes only one vegetation index (CpRVI). But it can be modified to compute
 *          multiple vegetation indies.
 *       2. The input to the operator is assumed to be C2 product, i.e. product with 4 bands: c11, c12_real, c12_imag
 *          and c22.
 *       3. The operator provides window size as an input parameter for averaging the input covariance matrix C2.
 *          If the input C2 matrix has already averaged, then no further averaging is needed, line 58-59, 194-197
 *          should be modified. See comments embedded.
 *
 * Reference:
 * [1] D. Mandal et al., "A Radar Vegetation Index for Crop Monitoring Using Compact Polarimetric SAR Data," 
		in IEEE Transactions on Geoscience and Remote Sensing, vol. 58, no. 9, pp. 6321-6335, Sept. 2020, 
		doi: 10.1109/TGRS.2020.2976661.
 */

@OperatorMetadata(alias = "Generalized-Radar-Vegetation-Index",
        category = "Radar/Polarimetric",
        authors = "Dipankar Mandal at al.",
        version = "1.0",
        copyright = "Copyright (C) 2020 by Microwave Remote Sensing Lab, IITBombay",
        description = "Generalized Radar Vegetation Indices generation")
public final class GRVIOp extends Operator implements QuadPolProcessor {

    @SourceProduct(alias = "source")
    private Product sourceProduct;

    @TargetProduct
    private Product targetProduct;

    // If the input covariance matrix T3 has already been averaged, then comment out the following two lines and
    // set windowSize to 0
    @Parameter(description = "The sliding window size", interval = "[1, 100]", defaultValue = "5", label = "Window Size")
    private int windowSize = 5;

    private int halfWindowSize = 0;
    private int sourceImageWidth = 0;
    private int sourceImageHeight = 0;

    private PolBandUtils.MATRIX sourceProductType = null;
    private PolBandUtils.PolSourceBand[] srcBandList;

    private final static String G_RVI = "GRVI";

    @Override
    public void initialize() throws OperatorException {

        try {
            final InputProductValidator validator = new InputProductValidator(sourceProduct);
            validator.checkIfSARProduct();

            sourceProductType = PolBandUtils.getSourceProductType(sourceProduct);
            if (sourceProductType != PolBandUtils.MATRIX.T3 && sourceProductType != PolBandUtils.MATRIX.C3 && sourceProductType != PolBandUtils.MATRIX.FULL) {
                throw new OperatorException("Quad pol source product Full or C3 or T3 coherency matrix product is expected.");
            }

            srcBandList = PolBandUtils.getSourceBands(sourceProduct, sourceProductType);
            halfWindowSize = windowSize / 2;
            sourceImageWidth = sourceProduct.getSceneRasterWidth();
            sourceImageHeight = sourceProduct.getSceneRasterHeight();

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
     * Add bands to the target product.
     *
     * @throws OperatorException The exception.
     */
    private void addSelectedBands() throws OperatorException {

        // If more indices are computed, add their names here
        final String[] targetBandNames = new String[]{G_RVI};

        for (PolBandUtils.PolSourceBand bandList : srcBandList) {
            final Band[] targetBands = OperatorUtils.addBands(targetProduct, targetBandNames, bandList.suffix);
            bandList.addTargetBands(targetBands);
        }
    }

    /**
     * Update metadata in the target product.
     */
    private void updateTargetProductMetadata() {

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(targetProduct);
        if (absRoot != null) {
            absRoot.setAttributeInt(AbstractMetadata.polsarData, 1);
        }
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

        final int x0 = targetRectangle.x;
        final int y0 = targetRectangle.y;
        final int w = targetRectangle.width;
        final int h = targetRectangle.height;
        final int maxY = y0 + h;
        final int maxX = x0 + w;
        //System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

        final double[][] Tr = new double[3][3]; // real part of coherency matrix
        final double[][] Ti = new double[3][3]; // imaginary part of coherency matrix
		final double[][] Tvr = new double[3][3]; // real part of coherency matrix of GVSM
        final double[][] Tvi = new double[3][3]; // imaginary part of coherency matrix of GVSM
		final double[][] K_T = new double[4][4]; // 4x4 Kennaugh matrix
		final double[][] K_rv = new double[4][4]; // 4x4 Kennaugh matrix of GVSM
		// Kennaugh matrices for Elementary targets
        final double[][] K_d = { //dihedral
							{ 1, 0, 0, 0 },
							{ 0, 1, 0, 0 },
							{ 0, 0, -1, 0 },
							{ 0, 0, 0, 1 }
						};
						
		final double[][] K_nd = { //narrow dihedral
							{ 0.625, 0.375, 0, 0 },
							{ 0.375, 0.625, 0, 0 },
							{ 0, 0, -0.5, 0 },
							{ 0, 0, 0, 0.5 }
						};		

		final double[][] K_t = { //trihedral
							{ 1, 0, 0, 0 },
							{ 0, 1, 0, 0 },
							{ 0, 0, 1, 0 },
							{ 0, 0, 0, -1 }
						};
		
		final double[][] K_c = { //cylinder
							{ 0.625, 0.375, 0, 0 },
							{ 0.375, 0.625, 0, 0 },
							{ 0, 0, 0.5, 0 },
							{ 0, 0, 0, -0.5 }
						};
						
						
     				

        for (final PolBandUtils.PolSourceBand bandList : srcBandList) {
            try {
                final TileData[] tileDataList = new TileData[bandList.targetBands.length];
                int i = 0;
                for (Band targetBand : bandList.targetBands) {
                    final Tile targetTile = targetTiles.get(targetBand);
                    tileDataList[i++] = new TileData(targetTile, targetBand.getName());
                }
                final TileIndex tgtIndex = new TileIndex(tileDataList[0].tile);

                final Tile[] sourceTiles = new Tile[bandList.srcBands.length];
                final ProductData[] dataBuffers = new ProductData[bandList.srcBands.length];
                final Rectangle sourceRectangle = getSourceRectangle(x0, y0, w, h);

                for (int j = 0; j < bandList.srcBands.length; j++) {
                    final Band srcBand = bandList.srcBands[j];
                    sourceTiles[j] = getSourceTile(srcBand, sourceRectangle);
                    dataBuffers[j] = sourceTiles[j].getDataBuffer();
                }
                final TileIndex srcIndex = new TileIndex(sourceTiles[0]);

                for (int y = y0; y < maxY; ++y) {
                    tgtIndex.calculateStride(y);
                    srcIndex.calculateStride(y);

                    for (int x = x0; x < maxX; ++x) {
                        final int tgtIdx = tgtIndex.getIndex(x);
                        final int srcIdx = srcIndex.getIndex(x);

                        // If input covariance matrix has been average then don't need to average it again.
                        // Comment out the following two lines and use the line below them, i.e. getCovarianceMatrixC2
                        getMeanCoherencyMatrix(x, y, halfWindowSize, halfWindowSize, sourceImageWidth,
                                sourceImageHeight, sourceProductType, srcIndex, dataBuffers, Tr, Ti);
								
											      
						// Compute Kennaugh Matrix
						K_T[0][0] = 0.5*(Tr[0][0] + Tr[1][1] + Tr[2][2]); 		  //m11
						K_T[0][1] = 0.5*(Tr[0][1] + Ti[0][1] + Tr[1][0] + Ti[1][0]); //m12
						K_T[0][2] = 0.5*(Tr[0][2] + Ti[0][2] + Tr[2][0] + Ti[2][0]); //m13
						K_T[0][3] = 0.5*(Tr[1][2] + Ti[1][2] - Tr[2][1] - Ti[2][1]); //m14
						K_T[1][0] = 0.5*(Tr[0][1] + Ti[0][1] + Tr[1][0] + Ti[1][0]); //m21
						K_T[1][1] = 0.5*(Tr[0][0] + Tr[1][1] - Tr[2][2]); 			  //m22
						K_T[1][2] = 0.5*(Tr[1][2] + Ti[1][2] + Tr[2][1] + Ti[2][1]); //m23
						K_T[1][3] = 0.5*(Tr[0][2] + Ti[1][2] - Tr[2][0] - Ti[2][0]); //m24
						K_T[2][0] = 0.5*(Tr[0][2] + Ti[0][2] + Tr[2][0] + Ti[2][0]); //m31
						K_T[2][1] = 0.5*(Tr[1][2] + Ti[1][2] + Tr[2][1] + Ti[2][1]); //m32
						K_T[2][2] = 0.5*(Tr[0][0] - Tr[1][1] + Tr[2][2]); 			  //m33
						K_T[2][3] = 0.5*((-1.0)*(Tr[0][1] + Ti[0][1] - Tr[1][0] - Ti[1][0])); //m34
						K_T[3][0] = 0.5*(Tr[1][2] + Ti[1][2] - Tr[2][1] - Ti[2][1]); //m41
						K_T[3][1] = 0.5*(Tr[0][2] + Ti[1][2] - Tr[2][0] - Ti[2][0]); //m42
						K_T[3][2] = 0.5*((-1.0)*(Tr[0][1] + Ti[0][1] - Tr[1][0] - Ti[1][0])); //m43
						K_T[3][3] = 0.5*(-Tr[0][0] + Tr[1][1] + Tr[2][2]); 		  //m44
						//K_T = 0.5*K_T;
        
						/* //Elements of Kennaugh matrix
						m11 = t11+t22+t33; m12 = t12+t21; m13 = t13+t31; m14 = -1i*(t23 - t32);
						m21 = t12+t21; m22 = t11+t22-t33; m23 = t23+t32; m24 = -1i*(t13-t31);
						m31 = t13+t31; m32 = t23+t32; m33 = t11-t22+t33; m34 = 1i*(t12-t21);
						m41 = -1i*(t23-t32); m42 = -1i*(t13-t31); m43 = 1i*(t12-t21); m44 = -t11+t22+t33;
						M_T = 0.5.*[m11 m12 m13 m14; m21 m22 m23 m24; m31 m32 m33 m34; m41 m42 m43 m44]; */
						
						// Generalized volume scattering model GVSM
						final double C11 = 0.5*(Tr[0][0]+Tr[0][1]+Tr[1][0]+Tr[1][1]);
						final double C33 = 0.5*(Tr[0][0]-Tr[0][1]-Tr[1][0]+Tr[1][1]);
						final double gamma = C11/C33;
						final double rho = 1.0/3.0;
						final double R = 1.0/((3.0/2.0)*(1 + gamma) - 0.5*Math.sqrt(gamma));
						
						Tvr[0][0] = R*(0.5*(1 + gamma) + (1.0/3.0)*Math.sqrt(gamma));
						Tvi[0][0] = 0;
						Tvr[0][1] = R*(0.5*(gamma-1));
						Tvi[0][1] = 0;
						Tvr[0][2] = 0;
						Tvi[0][2] = 0;
						Tvr[1][0] = R*(0.5*(gamma-1));
						Tvi[1][0] = 0;
						Tvr[1][1] = R*(0.5*(1 + gamma) - (1.0/3.0)*Math.sqrt(gamma));
						Tvi[1][1] = 0;
						Tvr[1][2] = 0;
						Tvi[1][2] = 0;
						Tvr[2][0] = 0;
						Tvi[2][0] = 0;
						Tvr[2][1] = 0;
						Tvi[2][1] = 0;
						Tvr[2][2] = R*(0.5*(1 + gamma) - (1.0/3.0)*Math.sqrt(gamma));
						Tvi[2][2] = 0;
						
						// Compute Kennaugh Matrix of GVSM
						K_rv[0][0] = 0.5*(Tvr[0][0] + Tvr[1][1] + Tvr[2][2]); 		  //m11
						K_rv[0][1] = 0.5*(Tvr[0][1] + Tvi[0][1] + Tvr[1][0] + Tvi[1][0]); //m12
						K_rv[0][2] = 0.5*(Tvr[0][2] + Tvi[0][2] + Tvr[2][0] + Tvi[2][0]); //m13
						K_rv[0][3] = 0.5*(Tvr[1][2] + Tvi[1][2] - Tvr[2][1] - Tvi[2][1]); //m14
						K_rv[1][0] = 0.5*(Tvr[0][1] + Tvi[0][1] + Tvr[1][0] + Tvi[1][0]); //m21
						K_rv[1][1] = 0.5*(Tvr[0][0] + Tvr[1][1] - Tvr[2][2]); 			  //m22
						K_rv[1][2] = 0.5*(Tvr[1][2] + Tvi[1][2] + Tvr[2][1] + Tvi[2][1]); //m23
						K_rv[1][3] = 0.5*(Tvr[0][2] + Tvi[1][2] - Tvr[2][0] - Tvi[2][0]); //m24
						K_rv[2][0] = 0.5*(Tvr[0][2] + Tvi[0][2] + Tvr[2][0] + Tvi[2][0]); //m31
						K_rv[2][1] = 0.5*(Tvr[1][2] + Tvi[1][2] + Tvr[2][1] + Tvi[2][1]); //m32
						K_rv[2][2] = 0.5*(Tvr[0][0] - Tvr[1][1] + Tvr[2][2]); 			  //m33
						K_rv[2][3] = 0.5*((-1.0)*(Tvr[0][1] + Tvi[0][1] - Tvr[1][0] - Tvi[1][0])); //m34
						K_rv[3][0] = 0.5*(Tvr[1][2] + Tvi[1][2] - Tvr[2][1] - Tvi[2][1]); //m41
						K_rv[3][1] = 0.5*(Tvr[0][2] + Tvi[1][2] - Tvr[2][0] - Tvi[2][0]); //m42
						K_rv[3][2] = 0.5*((-1.0)*(Tvr[0][1] + Tvi[0][1] - Tvr[1][0] - Tvi[1][0])); //m43
						K_rv[3][3] = 0.5*(-Tvr[0][0] + Tvr[1][1] + Tvr[2][2]); 		  //m44
						//K_rv = 0.5*K_rv;
						
						// Compute Geodesic distances from K_T
						// Compute Geodesic distance between K_T and K_rv
						final double[][] K_TT = new double[4][4];
						final double[][] K_rvT = new double[4][4];
						final double[][] num1 = new double[4][4];
						matrixtranspose(K_T,K_TT);
						matrixmultiply(K_TT,K_rv,num1);
						final double num = num1[0][0] + num1[1][1] + num1[2][2] + num1[3][3]; //trace of matrix
						final double[][] num2 = new double[4][4];
						matrixmultiply(K_TT,K_T,num2);
						final double num3 = num2[0][0] + num2[1][1] + num2[2][2] + num2[3][3]; //trace of matrix
						final double den1 = Math.sqrt(Math.abs(num3));
						final double[][] num4 = new double[4][4];
						matrixtranspose(K_rv,K_rvT);
						matrixmultiply(K_rvT,K_rv,num4);
						final double num5 = num4[0][0] + num4[1][1] + num4[2][2] + num4[3][3]; //trace of matrix
						final double den2 = Math.sqrt(Math.abs(num5));
						final double den = den1*den2;
						final double tempaa = 2*FastMath.acos(num/den)*180/Math.PI;
						final double GD_rv = tempaa/180;
						
						// Compute Geodesic distance between K_T and K_d
						final double[][] K_dT = new double[4][4];
						final double[][] num6 = new double[4][4];
						matrixmultiply(K_TT,K_d,num6);
						final double num7 = num6[0][0] + num6[1][1] + num6[2][2] + num6[3][3]; //trace of matrix
						final double[][] num8 = new double[4][4];
						matrixtranspose(K_d,K_dT);
						matrixmultiply(K_dT,K_d,num8);
						final double num9 = num8[0][0] + num8[1][1] + num8[2][2] + num8[3][3]; //trace of matrix
						final double den3 = Math.sqrt(Math.abs(num9));
						final double den4 = den1*den3;
						final double tempaa1 = 2*FastMath.acos(num7/den4)*180/Math.PI;
						final double GD_d = tempaa1/180;
						
						// Compute Geodesic distance between K_T and K_nd
						final double[][] K_ndT = new double[4][4];
						final double[][] num6nd = new double[4][4];
						matrixmultiply(K_TT,K_nd,num6nd);
						final double num7nd = num6nd[0][0] + num6nd[1][1] + num6nd[2][2] + num6nd[3][3]; //trace of matrix
						final double[][] num8nd = new double[4][4];
						matrixtranspose(K_nd,K_ndT);
						matrixmultiply(K_ndT,K_nd,num8nd);
						final double num9nd = num8nd[0][0] + num8nd[1][1] + num8nd[2][2] + num8nd[3][3]; //trace of matrix
						final double den3nd = Math.sqrt(Math.abs(num9nd));
						final double den4nd = den1*den3nd;
						final double tempaa1nd = 2*FastMath.acos(num7nd/den4nd)*180/Math.PI;
						final double GD_nd = tempaa1nd/180;
						
						// Compute Geodesic distance between K_T and K_c
						final double[][] K_cT = new double[4][4];
						final double[][] num6c = new double[4][4];
						matrixmultiply(K_TT,K_c,num6c);
						final double num7c = num6c[0][0] + num6c[1][1] + num6c[2][2] + num6c[3][3]; //trace of matrix
						final double[][] num8c = new double[4][4];
						matrixtranspose(K_c,K_cT);
						matrixmultiply(K_cT,K_c,num8c);
						final double num9c = num8c[0][0] + num8c[1][1] + num8c[2][2] + num8c[3][3]; //trace of matrix
						final double den3c = Math.sqrt(Math.abs(num9c));
						final double den4c = den1*den3c;
						final double tempaa1c = 2*FastMath.acos(num7c/den4c)*180/Math.PI;
						final double GD_c = tempaa1c/180;
						
						// Compute Geodesic distance between K_T and K_t
						final double[][] K_tT = new double[4][4];
						final double[][] num6t = new double[4][4];
						matrixmultiply(K_TT,K_t,num6t);
						final double num7t = num6t[0][0] + num6t[1][1] + num6t[2][2] + num6t[3][3]; //trace of matrix
						final double[][] num8t = new double[4][4];
						matrixtranspose(K_t,K_tT);
						matrixmultiply(K_tT,K_t,num8t);
						final double num9t = num8t[0][0] + num8t[1][1] + num8t[2][2] + num8t[3][3]; //trace of matrix
						final double den3t = Math.sqrt(Math.abs(num9t));
						final double den4t = den1*den3t;
						final double tempaa1t = 2*FastMath.acos(num7t/den4t)*180/Math.PI;
						final double GD_t = tempaa1t/180;
						
						
						// Compute Modulating factor 
						final Double[] gds = { GD_t, GD_c, GD_nd, GD_d };
						final double a = Collections.max(Arrays.asList(gds));
						final double b = Collections.min(Arrays.asList(gds));
						final double beta = FastMath.pow(b/a, 2);
						
        						
						// GRVI final calculation
						final double gRVI = (1 - GD_rv) * FastMath.pow(beta, GD_rv);
						
						
		
		
                        for (final TileData tileData : tileDataList) {
                            // Can add more indices here
                            if (tileData.bandName.contains(G_RVI)) {
                                tileData.dataBuffer.setElemFloatAt(tgtIdx, (float) gRVI);
                            }
                        }
                    }
                }

            } catch (Throwable e) {
                OperatorUtils.catchOperatorException(getId(), e);
            }
        }
    }

    private Rectangle getSourceRectangle(final int tx0, final int ty0, final int tw, final int th) {
        final int x0 = Math.max(0, tx0 - halfWindowSize);
        final int y0 = Math.max(0, ty0 - halfWindowSize);
        final int xMax = Math.min(tx0 + tw - 1 + halfWindowSize, sourceImageWidth - 1);
        final int yMax = Math.min(ty0 + th - 1 + halfWindowSize, sourceImageHeight - 1);
        final int w = xMax - x0 + 1;
        final int h = yMax - y0 + 1;
        return new Rectangle(x0, y0, w, h);
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
     * @see OperatorSpi#createOperator()
     * @see OperatorSpi#createOperator(Map, Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(GRVIOp.class);
        }
    }
}