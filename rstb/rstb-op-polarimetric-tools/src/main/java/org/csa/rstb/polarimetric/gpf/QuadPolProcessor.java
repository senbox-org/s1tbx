package org.csa.rstb.polarimetric.gpf;

import Jama.Matrix;
import org.apache.commons.math3.util.FastMath;
import org.csa.rstb.polarimetric.gpf.decompositions.DecompositionBase;
import org.esa.s1tbx.commons.polsar.PolBandUtils;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.dataop.downloadable.StatusProgressMonitor;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.engine_utilities.eo.Constants;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.gpf.ThreadManager;
import org.esa.snap.engine_utilities.gpf.TileIndex;

import java.awt.*;

public interface QuadPolProcessor extends PolarimetricProcessor {

    default void getQuadPolDataBuffer(final Operator op, final Band[] srcBands, final Rectangle sourceRectangle,
                                      final PolBandUtils.MATRIX sourceProductType,
                                      final Tile[] sourceTiles, final ProductData[] dataBuffers) {

        for (Band band : srcBands) {
            final String bandName = band.getName();

            if (sourceProductType == PolBandUtils.MATRIX.FULL) {

                if (bandName.contains("i_HH")) {
                    sourceTiles[0] = op.getSourceTile(band, sourceRectangle);
                    dataBuffers[0] = sourceTiles[0].getDataBuffer();
                } else if (bandName.contains("q_HH")) {
                    sourceTiles[1] = op.getSourceTile(band, sourceRectangle);
                    dataBuffers[1] = sourceTiles[1].getDataBuffer();
                } else if (bandName.contains("i_HV")) {
                    sourceTiles[2] = op.getSourceTile(band, sourceRectangle);
                    dataBuffers[2] = sourceTiles[2].getDataBuffer();
                } else if (bandName.contains("q_HV")) {
                    sourceTiles[3] = op.getSourceTile(band, sourceRectangle);
                    dataBuffers[3] = sourceTiles[3].getDataBuffer();
                } else if (bandName.contains("i_VH")) {
                    sourceTiles[4] = op.getSourceTile(band, sourceRectangle);
                    dataBuffers[4] = sourceTiles[4].getDataBuffer();
                } else if (bandName.contains("q_VH")) {
                    sourceTiles[5] = op.getSourceTile(band, sourceRectangle);
                    dataBuffers[5] = sourceTiles[5].getDataBuffer();
                } else if (bandName.contains("i_VV")) {
                    sourceTiles[6] = op.getSourceTile(band, sourceRectangle);
                    dataBuffers[6] = sourceTiles[6].getDataBuffer();
                } else if (bandName.contains("q_VV")) {
                    sourceTiles[7] = op.getSourceTile(band, sourceRectangle);
                    dataBuffers[7] = sourceTiles[7].getDataBuffer();
                }

            } else if (sourceProductType == PolBandUtils.MATRIX.C3) {

                if (bandName.contains("C11")) {
                    sourceTiles[0] = op.getSourceTile(band, sourceRectangle);
                    dataBuffers[0] = sourceTiles[0].getDataBuffer();
                } else if (bandName.contains("C12_real")) {
                    sourceTiles[1] = op.getSourceTile(band, sourceRectangle);
                    dataBuffers[1] = sourceTiles[1].getDataBuffer();
                } else if (bandName.contains("C12_imag")) {
                    sourceTiles[2] = op.getSourceTile(band, sourceRectangle);
                    dataBuffers[2] = sourceTiles[2].getDataBuffer();
                } else if (bandName.contains("C13_real")) {
                    sourceTiles[3] = op.getSourceTile(band, sourceRectangle);
                    dataBuffers[3] = sourceTiles[3].getDataBuffer();
                } else if (bandName.contains("C13_imag")) {
                    sourceTiles[4] = op.getSourceTile(band, sourceRectangle);
                    dataBuffers[4] = sourceTiles[4].getDataBuffer();
                } else if (bandName.contains("C22")) {
                    sourceTiles[5] = op.getSourceTile(band, sourceRectangle);
                    dataBuffers[5] = sourceTiles[5].getDataBuffer();
                } else if (bandName.contains("C23_real")) {
                    sourceTiles[6] = op.getSourceTile(band, sourceRectangle);
                    dataBuffers[6] = sourceTiles[6].getDataBuffer();
                } else if (bandName.contains("C23_imag")) {
                    sourceTiles[7] = op.getSourceTile(band, sourceRectangle);
                    dataBuffers[7] = sourceTiles[7].getDataBuffer();
                } else if (bandName.contains("C33")) {
                    sourceTiles[8] = op.getSourceTile(band, sourceRectangle);
                    dataBuffers[8] = sourceTiles[8].getDataBuffer();
                }

            } else if (sourceProductType == PolBandUtils.MATRIX.T3) {

                if (bandName.contains("T11")) {
                    sourceTiles[0] = op.getSourceTile(band, sourceRectangle);
                    dataBuffers[0] = sourceTiles[0].getDataBuffer();
                } else if (bandName.contains("T12_real")) {
                    sourceTiles[1] = op.getSourceTile(band, sourceRectangle);
                    dataBuffers[1] = sourceTiles[1].getDataBuffer();
                } else if (bandName.contains("T12_imag")) {
                    sourceTiles[2] = op.getSourceTile(band, sourceRectangle);
                    dataBuffers[2] = sourceTiles[2].getDataBuffer();
                } else if (bandName.contains("T13_real")) {
                    sourceTiles[3] = op.getSourceTile(band, sourceRectangle);
                    dataBuffers[3] = sourceTiles[3].getDataBuffer();
                } else if (bandName.contains("T13_imag")) {
                    sourceTiles[4] = op.getSourceTile(band, sourceRectangle);
                    dataBuffers[4] = sourceTiles[4].getDataBuffer();
                } else if (bandName.contains("T22")) {
                    sourceTiles[5] = op.getSourceTile(band, sourceRectangle);
                    dataBuffers[5] = sourceTiles[5].getDataBuffer();
                } else if (bandName.contains("T23_real")) {
                    sourceTiles[6] = op.getSourceTile(band, sourceRectangle);
                    dataBuffers[6] = sourceTiles[6].getDataBuffer();
                } else if (bandName.contains("T23_imag")) {
                    sourceTiles[7] = op.getSourceTile(band, sourceRectangle);
                    dataBuffers[7] = sourceTiles[7].getDataBuffer();
                } else if (bandName.contains("T33")) {
                    sourceTiles[8] = op.getSourceTile(band, sourceRectangle);
                    dataBuffers[8] = sourceTiles[8].getDataBuffer();
                }

            } else if (sourceProductType == PolBandUtils.MATRIX.C4) {

                if (bandName.contains("C11")) {
                    sourceTiles[0] = op.getSourceTile(band, sourceRectangle);
                    dataBuffers[0] = sourceTiles[0].getDataBuffer();
                } else if (bandName.contains("C12_real")) {
                    sourceTiles[1] = op.getSourceTile(band, sourceRectangle);
                    dataBuffers[1] = sourceTiles[1].getDataBuffer();
                } else if (bandName.contains("C12_imag")) {
                    sourceTiles[2] = op.getSourceTile(band, sourceRectangle);
                    dataBuffers[2] = sourceTiles[2].getDataBuffer();
                } else if (bandName.contains("C13_real")) {
                    sourceTiles[3] = op.getSourceTile(band, sourceRectangle);
                    dataBuffers[3] = sourceTiles[3].getDataBuffer();
                } else if (bandName.contains("C13_imag")) {
                    sourceTiles[4] = op.getSourceTile(band, sourceRectangle);
                    dataBuffers[4] = sourceTiles[4].getDataBuffer();
                } else if (bandName.contains("C14_real")) {
                    sourceTiles[5] = op.getSourceTile(band, sourceRectangle);
                    dataBuffers[5] = sourceTiles[5].getDataBuffer();
                } else if (bandName.contains("C14_imag")) {
                    sourceTiles[6] = op.getSourceTile(band, sourceRectangle);
                    dataBuffers[6] = sourceTiles[6].getDataBuffer();
                } else if (bandName.contains("C22")) {
                    sourceTiles[7] = op.getSourceTile(band, sourceRectangle);
                    dataBuffers[7] = sourceTiles[7].getDataBuffer();
                } else if (bandName.contains("C23_real")) {
                    sourceTiles[8] = op.getSourceTile(band, sourceRectangle);
                    dataBuffers[8] = sourceTiles[8].getDataBuffer();
                } else if (bandName.contains("C23_imag")) {
                    sourceTiles[9] = op.getSourceTile(band, sourceRectangle);
                    dataBuffers[9] = sourceTiles[9].getDataBuffer();
                } else if (bandName.contains("C24_real")) {
                    sourceTiles[10] = op.getSourceTile(band, sourceRectangle);
                    dataBuffers[10] = sourceTiles[10].getDataBuffer();
                } else if (bandName.contains("C24_imag")) {
                    sourceTiles[11] = op.getSourceTile(band, sourceRectangle);
                    dataBuffers[11] = sourceTiles[11].getDataBuffer();
                } else if (bandName.contains("C33")) {
                    sourceTiles[12] = op.getSourceTile(band, sourceRectangle);
                    dataBuffers[12] = sourceTiles[12].getDataBuffer();
                } else if (bandName.contains("C34_real")) {
                    sourceTiles[13] = op.getSourceTile(band, sourceRectangle);
                    dataBuffers[13] = sourceTiles[13].getDataBuffer();
                } else if (bandName.contains("C34_imag")) {
                    sourceTiles[14] = op.getSourceTile(band, sourceRectangle);
                    dataBuffers[14] = sourceTiles[14].getDataBuffer();
                } else if (bandName.contains("C44")) {
                    sourceTiles[15] = op.getSourceTile(band, sourceRectangle);
                    dataBuffers[15] = sourceTiles[15].getDataBuffer();
                }

            } else if (sourceProductType == PolBandUtils.MATRIX.T4) {

                if (bandName.contains("T11")) {
                    sourceTiles[0] = op.getSourceTile(band, sourceRectangle);
                    dataBuffers[0] = sourceTiles[0].getDataBuffer();
                } else if (bandName.contains("T12_real")) {
                    sourceTiles[1] = op.getSourceTile(band, sourceRectangle);
                    dataBuffers[1] = sourceTiles[1].getDataBuffer();
                } else if (bandName.contains("T12_imag")) {
                    sourceTiles[2] = op.getSourceTile(band, sourceRectangle);
                    dataBuffers[2] = sourceTiles[2].getDataBuffer();
                } else if (bandName.contains("T13_real")) {
                    sourceTiles[3] = op.getSourceTile(band, sourceRectangle);
                    dataBuffers[3] = sourceTiles[3].getDataBuffer();
                } else if (bandName.contains("T13_imag")) {
                    sourceTiles[4] = op.getSourceTile(band, sourceRectangle);
                    dataBuffers[4] = sourceTiles[4].getDataBuffer();
                } else if (bandName.contains("T14_real")) {
                    sourceTiles[5] = op.getSourceTile(band, sourceRectangle);
                    dataBuffers[5] = sourceTiles[5].getDataBuffer();
                } else if (bandName.contains("T14_imag")) {
                    sourceTiles[6] = op.getSourceTile(band, sourceRectangle);
                    dataBuffers[6] = sourceTiles[6].getDataBuffer();
                } else if (bandName.contains("T22")) {
                    sourceTiles[7] = op.getSourceTile(band, sourceRectangle);
                    dataBuffers[7] = sourceTiles[7].getDataBuffer();
                } else if (bandName.contains("T23_real")) {
                    sourceTiles[8] = op.getSourceTile(band, sourceRectangle);
                    dataBuffers[8] = sourceTiles[8].getDataBuffer();
                } else if (bandName.contains("T23_imag")) {
                    sourceTiles[9] = op.getSourceTile(band, sourceRectangle);
                    dataBuffers[9] = sourceTiles[9].getDataBuffer();
                } else if (bandName.contains("T24_real")) {
                    sourceTiles[10] = op.getSourceTile(band, sourceRectangle);
                    dataBuffers[10] = sourceTiles[10].getDataBuffer();
                } else if (bandName.contains("T24_imag")) {
                    sourceTiles[11] = op.getSourceTile(band, sourceRectangle);
                    dataBuffers[11] = sourceTiles[11].getDataBuffer();
                } else if (bandName.contains("T33")) {
                    sourceTiles[12] = op.getSourceTile(band, sourceRectangle);
                    dataBuffers[12] = sourceTiles[12].getDataBuffer();
                } else if (bandName.contains("T34_real")) {
                    sourceTiles[13] = op.getSourceTile(band, sourceRectangle);
                    dataBuffers[13] = sourceTiles[13].getDataBuffer();
                } else if (bandName.contains("T34_imag")) {
                    sourceTiles[14] = op.getSourceTile(band, sourceRectangle);
                    dataBuffers[14] = sourceTiles[14].getDataBuffer();
                } else if (bandName.contains("T44")) {
                    sourceTiles[15] = op.getSourceTile(band, sourceRectangle);
                    dataBuffers[15] = sourceTiles[15].getDataBuffer();
                }
            }
        }
    }

    /**
     * Get scatter matrix for given pixel.
     *
     * @param index           X,Y coordinate of the given pixel
     * @param dataBuffers     Source tiles dataBuffers for all 8 source bands
     * @param scatterMatrix_i Real part of the scatter matrix
     * @param scatterMatrix_q Imaginary part of the scatter matrix
     */
    default void getComplexScatterMatrix(final int index, final ProductData[] dataBuffers,
                                         final double[][] scatterMatrix_i, final double[][] scatterMatrix_q) {

        scatterMatrix_i[0][0] = dataBuffers[0].getElemDoubleAt(index); // HH - real
        scatterMatrix_q[0][0] = dataBuffers[1].getElemDoubleAt(index); // HH - imag

        scatterMatrix_i[0][1] = dataBuffers[2].getElemDoubleAt(index); // HV - real
        scatterMatrix_q[0][1] = dataBuffers[3].getElemDoubleAt(index); // HV - imag

        scatterMatrix_i[1][0] = dataBuffers[4].getElemDoubleAt(index); // VH - real
        scatterMatrix_q[1][0] = dataBuffers[5].getElemDoubleAt(index); // VH - imag

        scatterMatrix_i[1][1] = dataBuffers[6].getElemDoubleAt(index); // VV - real
        scatterMatrix_q[1][1] = dataBuffers[7].getElemDoubleAt(index); // VV - imag
    }

    /**
     * Get covariance matrix C3 for given pixel.
     *
     * @param index       X,Y coordinate of the given pixel
     * @param dataBuffers Source tiles dataBuffers for all 9 source bands
     * @param Cr          Real part of the covariance matrix
     * @param Ci          Imaginary part of the covariance matrix
     */
    default void getCovarianceMatrixC3(final int index, final ProductData[] dataBuffers,
                                       final double[][] Cr, final double[][] Ci) {

        Cr[0][0] = dataBuffers[0].getElemDoubleAt(index); // C11 - real
        Ci[0][0] = 0.0;                                   // C11 - imag

        Cr[0][1] = dataBuffers[1].getElemDoubleAt(index); // C12 - real
        Ci[0][1] = dataBuffers[2].getElemDoubleAt(index); // C12 - imag

        Cr[0][2] = dataBuffers[3].getElemDoubleAt(index); // C13 - real
        Ci[0][2] = dataBuffers[4].getElemDoubleAt(index); // C13 - imag

        Cr[1][1] = dataBuffers[5].getElemDoubleAt(index); // C22 - real
        Ci[1][1] = 0.0;                                   // C22 - imag

        Cr[1][2] = dataBuffers[6].getElemDoubleAt(index); // C23 - real
        Ci[1][2] = dataBuffers[7].getElemDoubleAt(index); // C23 - imag

        Cr[2][2] = dataBuffers[8].getElemDoubleAt(index); // C33 - real
        Ci[2][2] = 0.0;                                   // C33 - imag

        Cr[1][0] = Cr[0][1];
        Ci[1][0] = -Ci[0][1];
        Cr[2][0] = Cr[0][2];
        Ci[2][0] = -Ci[0][2];
        Cr[2][1] = Cr[1][2];
        Ci[2][1] = -Ci[1][2];
    }

    /**
     * Get covariance matrix C4 for given pixel.
     *
     * @param index       X,Y coordinate of the given pixel
     * @param dataBuffers Source tiles dataBuffers for all 16 source bands
     * @param Cr          Real part of the covariance matrix
     * @param Ci          Imaginary part of the covariance matrix
     */
    default void getCovarianceMatrixC4(final int index, final ProductData[] dataBuffers,
                                       final double[][] Cr, final double[][] Ci) {

        Cr[0][0] = dataBuffers[0].getElemDoubleAt(index); // C11 - real
        Ci[0][0] = 0.0;                                   // C11 - imag

        Cr[0][1] = dataBuffers[1].getElemDoubleAt(index); // C12 - real
        Ci[0][1] = dataBuffers[2].getElemDoubleAt(index); // C12 - imag

        Cr[0][2] = dataBuffers[3].getElemDoubleAt(index); // C13 - real
        Ci[0][2] = dataBuffers[4].getElemDoubleAt(index); // C13 - imag

        Cr[0][3] = dataBuffers[5].getElemDoubleAt(index); // C14 - real
        Ci[0][3] = dataBuffers[6].getElemDoubleAt(index); // C14 - imag

        Cr[1][1] = dataBuffers[7].getElemDoubleAt(index); // C22 - real
        Ci[1][1] = 0.0;                                   // C22 - imag

        Cr[1][2] = dataBuffers[8].getElemDoubleAt(index); // C23 - real
        Ci[1][2] = dataBuffers[9].getElemDoubleAt(index); // C23 - imag

        Cr[1][3] = dataBuffers[10].getElemDoubleAt(index); // C24 - real
        Ci[1][3] = dataBuffers[11].getElemDoubleAt(index); // C24 - imag

        Cr[2][2] = dataBuffers[12].getElemDoubleAt(index); // C33 - real
        Ci[2][2] = 0.0;                                    // C33 - imag

        Cr[2][3] = dataBuffers[13].getElemDoubleAt(index); // C34 - real
        Ci[2][3] = dataBuffers[14].getElemDoubleAt(index); // C34 - imag

        Cr[3][3] = dataBuffers[15].getElemDoubleAt(index); // C44 - real
        Ci[3][3] = 0.0;                                    // C44 - imag

        Cr[1][0] = Cr[0][1];
        Ci[1][0] = -Ci[0][1];
        Cr[2][0] = Cr[0][2];
        Ci[2][0] = -Ci[0][2];
        Cr[2][1] = Cr[1][2];
        Ci[2][1] = -Ci[1][2];
        Cr[3][0] = Cr[0][3];
        Ci[3][0] = -Ci[0][3];
        Cr[3][1] = Cr[1][3];
        Ci[3][1] = -Ci[1][3];
        Cr[3][2] = Cr[2][3];
        Ci[3][2] = -Ci[2][3];
    }

    /**
     * Get coherency matrix T3 for given pixel.
     *
     * @param index       X,Y coordinate of the given pixel
     * @param dataBuffers Source tile buffers for all 9 source bands
     * @param Tr          Real part of the coherency matrix
     * @param Ti          Imaginary part of the coherency matrix
     */
    default void getCoherencyMatrixT3(final int index, final ProductData[] dataBuffers,
                                      final double[][] Tr, final double[][] Ti) {

        Tr[0][0] = dataBuffers[0].getElemDoubleAt(index); // T11 - real
        Ti[0][0] = 0.0;                                                   // T11 - imag

        Tr[0][1] = dataBuffers[1].getElemDoubleAt(index); // T12 - real
        Ti[0][1] = dataBuffers[2].getElemDoubleAt(index); // T12 - imag

        Tr[0][2] = dataBuffers[3].getElemDoubleAt(index); // T13 - real
        Ti[0][2] = dataBuffers[4].getElemDoubleAt(index); // T13 - imag

        Tr[1][1] = dataBuffers[5].getElemDoubleAt(index); // T22 - real
        Ti[1][1] = 0.0;                                                   // T22 - imag

        Tr[1][2] = dataBuffers[6].getElemDoubleAt(index); // T23 - real
        Ti[1][2] = dataBuffers[7].getElemDoubleAt(index); // T23 - imag

        Tr[2][2] = dataBuffers[8].getElemDoubleAt(index); // T33 - real
        Ti[2][2] = 0.0;                                                   // T33 - imag

        Tr[1][0] = Tr[0][1];
        Ti[1][0] = -Ti[0][1];
        Tr[2][0] = Tr[0][2];
        Ti[2][0] = -Ti[0][2];
        Tr[2][1] = Tr[1][2];
        Ti[2][1] = -Ti[1][2];
    }

    /**
     * Get coherency matrix T4 for given pixel.
     *
     * @param index       X,Y coordinate of the given pixel
     * @param dataBuffers Source tile buffers for all 16 source bands
     * @param Tr          Real part of the coherency matrix
     * @param Ti          Imaginary part of the coherency matrix
     */
    default void getCoherencyMatrixT4(final int index, final ProductData[] dataBuffers,
                                      final double[][] Tr, final double[][] Ti) {

        Tr[0][0] = dataBuffers[0].getElemDoubleAt(index); // T11 - real
        Ti[0][0] = 0.0;                                   // T11 - imag

        Tr[0][1] = dataBuffers[1].getElemDoubleAt(index); // T12 - real
        Ti[0][1] = dataBuffers[2].getElemDoubleAt(index); // T12 - imag

        Tr[0][2] = dataBuffers[3].getElemDoubleAt(index); // T13 - real
        Ti[0][2] = dataBuffers[4].getElemDoubleAt(index); // T13 - imag

        Tr[0][3] = dataBuffers[5].getElemDoubleAt(index); // T14 - real
        Ti[0][3] = dataBuffers[6].getElemDoubleAt(index); // T14 - imag

        Tr[1][1] = dataBuffers[7].getElemDoubleAt(index); // T22 - real
        Ti[1][1] = 0.0;                                   // T22 - imag

        Tr[1][2] = dataBuffers[8].getElemDoubleAt(index); // T23 - real
        Ti[1][2] = dataBuffers[9].getElemDoubleAt(index); // T23 - imag

        Tr[1][3] = dataBuffers[10].getElemDoubleAt(index); // T24 - real
        Ti[1][3] = dataBuffers[11].getElemDoubleAt(index); // T24 - imag

        Tr[2][2] = dataBuffers[12].getElemDoubleAt(index); // T33 - real
        Ti[2][2] = 0.0;                                    // T33 - imag

        Tr[2][3] = dataBuffers[13].getElemDoubleAt(index); // T34 - real
        Ti[2][3] = dataBuffers[14].getElemDoubleAt(index); // T34 - imag

        Tr[3][3] = dataBuffers[15].getElemDoubleAt(index); // T44 - real
        Ti[3][3] = 0.0;                                    // T44 - imag

        Tr[1][0] = Tr[0][1];
        Ti[1][0] = -Ti[0][1];
        Tr[2][0] = Tr[0][2];
        Ti[2][0] = -Ti[0][2];
        Tr[2][1] = Tr[1][2];
        Ti[2][1] = -Ti[1][2];
        Tr[3][0] = Tr[0][3];
        Ti[3][0] = -Ti[0][3];
        Tr[3][1] = Tr[1][3];
        Ti[3][1] = -Ti[1][3];
        Tr[3][2] = Tr[2][3];
        Ti[3][2] = -Ti[2][3];
    }

    /**
     * Compute covariance matrix for given scatter matrix.
     *
     * @param scatterRe Real part of the scatter matrix
     * @param scatterIm Imaginary part of the scatter matrix
     * @param Cr        Real part of the covariance matrix
     * @param Ci        Imaginary part of the covariance matrix
     */
    default void computeCovarianceMatrixC3(final double[][] scatterRe, final double[][] scatterIm,
                                           final double[][] Cr, final double[][] Ci) {

        final double k1r = scatterRe[0][0];
        final double k1i = scatterIm[0][0];
        final double sHVr = scatterRe[0][1];
        final double sHVi = scatterIm[0][1];
        final double sVHr = scatterRe[1][0];
        final double sVHi = scatterIm[1][0];
        final double k3r = scatterRe[1][1];
        final double k3i = scatterIm[1][1];

        final double k2r = (sHVr + sVHr) / Constants.sqrt2;
        final double k2i = (sHVi + sVHi) / Constants.sqrt2;

        Cr[0][0] = k1r * k1r + k1i * k1i;
        //Ci[0][0] = 0.0;

        Cr[0][1] = k1r * k2r + k1i * k2i;
        Ci[0][1] = k1i * k2r - k1r * k2i;

        Cr[0][2] = k1r * k3r + k1i * k3i;
        Ci[0][2] = k1i * k3r - k1r * k3i;

        Cr[1][1] = k2r * k2r + k2i * k2i;
        //Ci[1][1] = 0.0;

        Cr[1][2] = k2r * k3r + k2i * k3i;
        Ci[1][2] = k2i * k3r - k2r * k3i;

        Cr[2][2] = k3r * k3r + k3i * k3i;
        //Ci[2][2] = 0.0;

        Cr[1][0] = Cr[0][1];
        Ci[1][0] = -Ci[0][1];
        Cr[2][0] = Cr[0][2];
        Ci[2][0] = -Ci[0][2];
        Cr[2][1] = Cr[1][2];
        Ci[2][1] = -Ci[1][2];
    }

    /**
     * Compute covariance matrix C4 for given scatter matrix.
     *
     * @param scatterRe Real part of the scatter matrix
     * @param scatterIm Imaginary part of the scatter matrix
     * @param Cr        Real part of the covariance matrix
     * @param Ci        Imaginary part of the covariance matrix
     */
    default void computeCovarianceMatrixC4(final double[][] scatterRe, final double[][] scatterIm,
                                           final double[][] Cr, final double[][] Ci) {

        final double k1r = scatterRe[0][0];
        final double k1i = scatterIm[0][0];
        final double k2r = scatterRe[0][1];
        final double k2i = scatterIm[0][1];
        final double k3r = scatterRe[1][0];
        final double k3i = scatterIm[1][0];
        final double k4r = scatterRe[1][1];
        final double k4i = scatterIm[1][1];

        Cr[0][0] = k1r * k1r + k1i * k1i;
        Ci[0][0] = 0.0;

        Cr[0][1] = k1r * k2r + k1i * k2i;
        Ci[0][1] = k1i * k2r - k1r * k2i;

        Cr[0][2] = k1r * k3r + k1i * k3i;
        Ci[0][2] = k1i * k3r - k1r * k3i;

        Cr[0][3] = k1r * k4r + k1i * k4i;
        Ci[0][3] = k1i * k4r - k1r * k4i;

        Cr[1][1] = k2r * k2r + k2i * k2i;
        Ci[1][1] = 0.0;

        Cr[1][2] = k2r * k3r + k2i * k3i;
        Ci[1][2] = k2i * k3r - k2r * k3i;

        Cr[1][3] = k2r * k4r + k2i * k4i;
        Ci[1][3] = k2i * k4r - k2r * k4i;

        Cr[2][2] = k3r * k3r + k3i * k3i;
        Ci[2][2] = 0.0;

        Cr[2][3] = k3r * k4r + k3i * k4i;
        Ci[2][3] = k3i * k4r - k3r * k4i;

        Cr[3][3] = k4r * k4r + k4i * k4i;
        Ci[3][3] = 0.0;

        Cr[1][0] = Cr[0][1];
        Ci[1][0] = -Ci[0][1];
        Cr[2][0] = Cr[0][2];
        Ci[2][0] = -Ci[0][2];
        Cr[2][1] = Cr[1][2];
        Ci[2][1] = -Ci[1][2];
        Cr[3][0] = Cr[0][3];
        Ci[3][0] = -Ci[0][3];
        Cr[3][1] = Cr[1][3];
        Ci[3][1] = -Ci[1][3];
        Cr[3][2] = Cr[2][3];
        Ci[3][2] = -Ci[2][3];
    }

    /**
     * Compute coherency matrix T3 for given scatter matrix.
     *
     * @param scatterRe Real part of the scatter matrix
     * @param scatterIm Imaginary part of the scatter matrix
     * @param Tr        Real part of the coherency matrix
     * @param Ti        Imaginary part of the coherency matrix
     */
    default void computeCoherencyMatrixT3(final double[][] scatterRe, final double[][] scatterIm,
                                          final double[][] Tr, final double[][] Ti) {

        final double sHHr = scatterRe[0][0];
        final double sHHi = scatterIm[0][0];
        final double sHVr = scatterRe[0][1];
        final double sHVi = scatterIm[0][1];
        final double sVHr = scatterRe[1][0];
        final double sVHi = scatterIm[1][0];
        final double sVVr = scatterRe[1][1];
        final double sVVi = scatterIm[1][1];

        final double k1r = (sHHr + sVVr) / Constants.sqrt2;
        final double k1i = (sHHi + sVVi) / Constants.sqrt2;
        final double k2r = (sHHr - sVVr) / Constants.sqrt2;
        final double k2i = (sHHi - sVVi) / Constants.sqrt2;
        final double k3r = (sHVr + sVHr) / Constants.sqrt2;
        final double k3i = (sHVi + sVHi) / Constants.sqrt2;

        Tr[0][0] = k1r * k1r + k1i * k1i;
        Ti[0][0] = 0.0;

        Tr[0][1] = k1r * k2r + k1i * k2i;
        Ti[0][1] = k1i * k2r - k1r * k2i;

        Tr[0][2] = k1r * k3r + k1i * k3i;
        Ti[0][2] = k1i * k3r - k1r * k3i;

        Tr[1][1] = k2r * k2r + k2i * k2i;
        Ti[1][1] = 0.0;

        Tr[1][2] = k2r * k3r + k2i * k3i;
        Ti[1][2] = k2i * k3r - k2r * k3i;

        Tr[2][2] = k3r * k3r + k3i * k3i;
        Ti[2][2] = 0.0;

        Tr[1][0] = Tr[0][1];
        Ti[1][0] = -Ti[0][1];
        Tr[2][0] = Tr[0][2];
        Ti[2][0] = -Ti[0][2];
        Tr[2][1] = Tr[1][2];
        Ti[2][1] = -Ti[1][2];
    }

    /**
     * Compute coherency matrix T4 for given scatter matrix.
     *
     * @param scatterRe Real part of the scatter matrix
     * @param scatterIm Imaginary part of the scatter matrix
     * @param Tr        Real part of the coherency matrix
     * @param Ti        Imaginary part of the coherency matrix
     */
    default void computeCoherencyMatrixT4(final double[][] scatterRe, final double[][] scatterIm,
                                          final double[][] Tr, final double[][] Ti) {

        final double sHHr = scatterRe[0][0];
        final double sHHi = scatterIm[0][0];
        final double sHVr = scatterRe[0][1];
        final double sHVi = scatterIm[0][1];
        final double sVHr = scatterRe[1][0];
        final double sVHi = scatterIm[1][0];
        final double sVVr = scatterRe[1][1];
        final double sVVi = scatterIm[1][1];

        final double k1r = (sHHr + sVVr) / Constants.sqrt2;
        final double k1i = (sHHi + sVVi) / Constants.sqrt2;
        final double k2r = (sHHr - sVVr) / Constants.sqrt2;
        final double k2i = (sHHi - sVVi) / Constants.sqrt2;
        final double k3r = (sHVr + sVHr) / Constants.sqrt2;
        final double k3i = (sHVi + sVHi) / Constants.sqrt2;
        final double k4r = (sVHi - sHVi) / Constants.sqrt2;
        final double k4i = (sHVr - sVHr) / Constants.sqrt2;

        Tr[0][0] = k1r * k1r + k1i * k1i;
        Ti[0][0] = 0.0;

        Tr[0][1] = k1r * k2r + k1i * k2i;
        Ti[0][1] = k1i * k2r - k1r * k2i;

        Tr[0][2] = k1r * k3r + k1i * k3i;
        Ti[0][2] = k1i * k3r - k1r * k3i;

        Tr[0][3] = k1r * k4r + k1i * k4i;
        Ti[0][3] = k1i * k4r - k1r * k4i;

        Tr[1][1] = k2r * k2r + k2i * k2i;
        Ti[1][1] = 0.0;

        Tr[1][2] = k2r * k3r + k2i * k3i;
        Ti[1][2] = k2i * k3r - k2r * k3i;

        Tr[1][3] = k2r * k4r + k2i * k4i;
        Ti[1][3] = k2i * k4r - k2r * k4i;

        Tr[2][2] = k3r * k3r + k3i * k3i;
        Ti[2][2] = 0.0;

        Tr[2][3] = k3r * k4r + k3i * k4i;
        Ti[2][3] = k3i * k4r - k3r * k4i;

        Tr[3][3] = k4r * k4r + k4i * k4i;
        Ti[3][3] = 0.0;

        Tr[1][0] = Tr[0][1];
        Ti[1][0] = -Ti[0][1];
        Tr[2][0] = Tr[0][2];
        Ti[2][0] = -Ti[0][2];
        Tr[2][1] = Tr[1][2];
        Ti[2][1] = -Ti[1][2];
        Tr[3][0] = Tr[0][3];
        Ti[3][0] = -Ti[0][3];
        Tr[3][1] = Tr[1][3];
        Ti[3][1] = -Ti[1][3];
        Tr[3][2] = Tr[2][3];
        Ti[3][2] = -Ti[2][3];
    }

    /**
     * Convert covariance matrix C4 to coherency matrix T4
     *
     * @param c4Re Real part of C4 matrix
     * @param c4Im Imaginary part of C4 matrix
     * @param t4Re Real part of T4 matrix
     * @param t4Im Imaginary part of T4 matrix
     */
    default void c4ToT4(final double[][] c4Re, final double[][] c4Im,
                        final double[][] t4Re, final double[][] t4Im) {

        t4Re[0][0] = 0.5 * (c4Re[0][0] + 2 * c4Re[0][3] + c4Re[3][3]);
        t4Im[0][0] = 0.5 * (c4Im[0][0] + c4Im[3][3]);

        t4Re[0][1] = 0.5 * (c4Re[0][0] - c4Re[3][3]);
        t4Im[0][1] = 0.5 * (c4Im[0][0] - 2 * c4Im[0][3] - c4Im[3][3]);

        t4Re[0][2] = 0.5 * (c4Re[0][1] + c4Re[1][3] + c4Re[0][2] + c4Re[2][3]);
        t4Im[0][2] = 0.5 * (c4Im[0][1] - c4Im[1][3] + c4Im[0][2] - c4Im[2][3]);

        t4Re[0][3] = 0.5 * (c4Im[0][1] - c4Im[1][3] - c4Im[0][2] + c4Im[2][3]);
        t4Im[0][3] = 0.5 * (-c4Re[0][1] - c4Re[1][3] + c4Re[0][2] + c4Re[2][3]);

        t4Re[1][0] = t4Re[0][1];
        t4Im[1][0] = -t4Im[0][1];

        t4Re[1][1] = 0.5 * (c4Re[0][0] - 2 * c4Re[0][3] + c4Re[3][3]);
        t4Im[1][1] = 0.5 * (c4Im[0][0] + c4Im[3][3]);

        t4Re[1][2] = 0.5 * (c4Re[0][1] - c4Re[1][3] + c4Re[0][2] - c4Re[2][3]);
        t4Im[1][2] = 0.5 * (c4Im[0][1] + c4Im[1][3] + c4Im[0][2] + c4Im[2][3]);

        t4Re[1][3] = 0.5 * (c4Im[0][1] + c4Im[1][3] - c4Im[0][2] - c4Im[2][3]);
        t4Im[1][3] = 0.5 * (-c4Re[0][1] + c4Re[1][3] + c4Re[0][2] - c4Re[2][3]);

        t4Re[2][0] = t4Re[0][2];
        t4Im[2][0] = -t4Im[0][2];

        t4Re[2][1] = t4Re[1][2];
        t4Im[2][1] = -t4Im[1][2];

        t4Re[2][2] = 0.5 * (c4Re[1][1] + 2 * c4Re[1][2] + c4Re[2][2]);
        t4Im[2][2] = 0.5 * (c4Im[1][1] + c4Im[2][2]);

        t4Re[2][3] = 0.5 * (c4Im[1][1] - 2 * c4Im[1][2] - c4Im[2][2]);
        t4Im[2][3] = 0.5 * (-c4Re[1][1] + c4Re[2][2]);

        t4Re[3][0] = t4Re[0][3];
        t4Im[3][0] = -t4Im[0][3];

        t4Re[3][1] = t4Re[1][3];
        t4Im[3][1] = -t4Im[1][3];

        t4Re[3][2] = t4Re[2][3];
        t4Im[3][2] = -t4Im[2][3];

        t4Re[3][3] = 0.5 * (c4Re[1][1] - 2 * c4Re[1][2] + c4Re[2][2]);
        t4Im[3][3] = 0.5 * (c4Im[1][1] + c4Im[2][2]);
    }

    /**
     * Convert coherency matrix T4 to covariance matrix C4
     *
     * @param t4Re Real part of T4 matrix
     * @param t4Im Imaginary part of T4 matrix
     * @param c4Re Real part of C4 matrix
     * @param c4Im Imaginary part of C4 matrix
     */
    default void t4ToC4(final double[][] t4Re, final double[][] t4Im,
                        final double[][] c4Re, final double[][] c4Im) {

        c4Re[0][0] = 0.5 * (t4Re[0][0] + t4Re[0][1] + t4Re[1][0] + t4Re[1][1]);
        c4Im[0][0] = 0.0;

        c4Re[0][1] = 0.5 * (t4Re[0][2] - t4Im[0][3] + t4Re[1][2] - t4Im[1][3]);
        c4Im[0][1] = 0.5 * (t4Im[0][2] + t4Re[0][3] + t4Im[1][2] + t4Re[1][3]);

        c4Re[0][2] = 0.5 * (t4Re[0][2] + t4Im[0][3] + t4Re[1][2] + t4Im[1][3]);
        c4Im[0][2] = 0.5 * (t4Im[0][2] - t4Re[0][3] + t4Im[1][2] - t4Re[1][3]);

        c4Re[0][3] = 0.5 * (t4Re[0][0] - t4Re[0][1] + t4Re[1][0] - t4Re[1][1]);
        c4Im[0][3] = 0.5 * (t4Im[0][0] - t4Im[0][1] + t4Im[1][0] - t4Im[1][1]);

        c4Re[1][0] = c4Re[0][1];
        c4Im[1][0] = -c4Im[0][1];

        c4Re[1][1] = 0.5 * (t4Re[2][2] - t4Im[2][3] + t4Im[3][2] + t4Re[3][3]);
        c4Im[1][1] = 0.0;

        c4Re[1][2] = 0.5 * (t4Re[2][2] + t4Im[2][3] + t4Im[3][2] - t4Re[3][3]);
        c4Im[1][2] = 0.5 * (t4Im[2][2] - t4Re[2][3] - t4Re[3][2] - t4Im[3][3]);

        c4Re[1][3] = 0.5 * (t4Re[2][0] - t4Re[2][1] + t4Im[3][0] - t4Im[3][1]);
        c4Im[1][3] = 0.5 * (t4Im[2][0] - t4Im[2][1] - t4Re[3][0] + t4Re[3][1]);

        c4Re[2][0] = c4Re[0][2];
        c4Im[2][0] = -c4Im[0][2];

        c4Re[2][1] = c4Re[1][2];
        c4Im[2][1] = -c4Im[1][2];

        c4Re[2][2] = 0.5 * (t4Re[2][2] + t4Im[2][3] - t4Im[3][2] + t4Re[3][3]);
        c4Im[2][2] = 0.0;

        c4Re[2][3] = 0.5 * (t4Re[2][0] - t4Re[2][1] - t4Im[3][0] + t4Im[3][1]);
        c4Im[2][3] = 0.5 * (t4Im[2][0] - t4Im[2][1] + t4Re[3][0] - t4Re[3][1]);

        c4Re[3][0] = c4Re[0][3];
        c4Im[3][0] = -c4Im[0][3];

        c4Re[3][1] = c4Re[1][3];
        c4Im[3][1] = -c4Im[1][3];

        c4Re[3][2] = c4Re[2][3];
        c4Im[3][2] = -c4Im[2][3];

        c4Re[3][3] = 0.5 * (t4Re[0][0] - t4Re[0][1] - t4Re[1][0] + t4Re[1][1]);
        c4Im[3][3] = 0.0;
    }

    /**
     * Convert covariance matrix C3 to coherency matrix T3
     *
     * @param c3Re Real part of C3 matrix
     * @param c3Im Imaginary part of C3 matrix
     * @param t3Re Real part of T3 matrix
     * @param t3Im Imaginary part of T3 matrix
     */
    default void c3ToT3(final double[][] c3Re, final double[][] c3Im,
                        final double[][] t3Re, final double[][] t3Im) {

        t3Re[0][0] = (c3Re[0][0] + 2 * c3Re[0][2] + c3Re[2][2]) / 2;
        t3Im[0][0] = 0.0;
        t3Re[0][1] = (c3Re[0][0] - c3Re[2][2]) / 2;
        t3Im[0][1] = -c3Im[0][2];
        t3Re[0][2] = (c3Re[0][1] + c3Re[1][2]) / Constants.sqrt2;
        t3Im[0][2] = (c3Im[0][1] - c3Im[1][2]) / Constants.sqrt2;

        t3Re[1][0] = t3Re[0][1];
        t3Im[1][0] = -t3Im[0][1];
        t3Re[1][1] = (c3Re[0][0] - 2 * c3Re[0][2] + c3Re[2][2]) / 2;
        t3Im[1][1] = 0.0;
        t3Re[1][2] = (c3Re[0][1] - c3Re[1][2]) / Constants.sqrt2;
        t3Im[1][2] = (c3Im[0][1] + c3Im[1][2]) / Constants.sqrt2;

        t3Re[2][0] = t3Re[0][2];
        t3Im[2][0] = -t3Im[0][2];
        t3Re[2][1] = t3Re[1][2];
        t3Im[2][1] = -t3Im[1][2];
        t3Re[2][2] = c3Re[1][1];
        t3Im[2][2] = 0.0;
    }

    /**
     * Convert coherency matrix T3 to covariance matrix C3
     *
     * @param t3Re Real part of T3 matrix
     * @param t3Im Imaginary part of T3 matrix
     * @param c3Re Real part of C3 matrix
     * @param c3Im Imaginary part of C3 matrix
     */
    default void t3ToC3(final double[][] t3Re, final double[][] t3Im,
                        final double[][] c3Re, final double[][] c3Im) {

        c3Re[0][0] = 0.5 * (t3Re[0][0] + t3Re[0][1] + t3Re[1][0] + t3Re[1][1]);
        c3Im[0][0] = 0.0;

        c3Re[0][1] = (t3Re[0][2] + t3Re[1][2]) / Constants.sqrt2;
        c3Im[0][1] = (t3Im[0][2] + t3Im[1][2]) / Constants.sqrt2;

        c3Re[0][2] = 0.5 * (t3Re[0][0] - t3Re[0][1] + t3Re[1][0] - t3Re[1][1]);
        c3Im[0][2] = 0.5 * (t3Im[0][0] - t3Im[0][1] + t3Im[1][0] - t3Im[1][1]);

        c3Re[1][0] = c3Re[0][1];
        c3Im[1][0] = -c3Im[0][1];

        c3Re[1][1] = t3Re[2][2];
        c3Im[1][1] = 0.0;

        c3Re[1][2] = (t3Re[2][0] - t3Re[2][1]) / Constants.sqrt2;
        c3Im[1][2] = (t3Im[2][0] - t3Im[2][1]) / Constants.sqrt2;

        c3Re[2][0] = c3Re[0][2];
        c3Im[2][0] = -c3Im[0][2];

        c3Re[2][1] = c3Re[1][2];
        c3Im[2][1] = -c3Im[1][2];

        c3Re[2][2] = 0.5 * (t3Re[0][0] - t3Re[0][1] - t3Re[1][0] + t3Re[1][1]);
        c3Im[2][2] = 0.0;
    }

    /**
     * Convert coherency matrix T4 to coherency matrix T3
     *
     * @param t4Re Real part of T4 matrix
     * @param t4Im Imaginary part of T4 matrix
     * @param t3Re Real part of T3 matrix
     * @param t3Im Imaginary part of T3 matrix
     */
    default void t4ToT3(final double[][] t4Re, final double[][] t4Im,
                        final double[][] t3Re, final double[][] t3Im) {

        // loop unwrapping
        System.arraycopy(t4Re[0], 0, t3Re[0], 0, t3Re[0].length);
        System.arraycopy(t4Im[0], 0, t3Im[0], 0, t3Im[0].length);

        System.arraycopy(t4Re[1], 0, t3Re[1], 0, t3Re[1].length);
        System.arraycopy(t4Im[1], 0, t3Im[1], 0, t3Im[1].length);

        System.arraycopy(t4Re[2], 0, t3Re[2], 0, t3Re[2].length);
        System.arraycopy(t4Im[2], 0, t3Im[2], 0, t3Im[2].length);
    }

    /**
     * Convert covariance matrix C4 to covariance matrix C3
     *
     * @param c4Re Real part of C4 matrix
     * @param c4Im Imaginary part of C4 matrix
     * @param c3Re Real part of C3 matrix
     * @param c3Im Imaginary part of C3 matrix
     */
    default void c4ToC3(final double[][] c4Re, final double[][] c4Im,
                        final double[][] c3Re, final double[][] c3Im) {

        c3Re[0][0] = c4Re[0][0];
        c3Im[0][0] = c4Im[0][0];

        c3Re[0][1] = (c4Re[0][1] + c4Re[0][2]) / Constants.sqrt2;
        c3Im[0][1] = (c4Im[0][1] + c4Im[0][2]) / Constants.sqrt2;

        c3Re[0][2] = c4Re[0][3];
        c3Im[0][2] = c4Im[0][3];

        c3Re[1][0] = (c4Re[1][0] + c4Re[2][0]) / Constants.sqrt2;
        c3Im[1][0] = (c4Im[1][0] + c4Im[2][0]) / Constants.sqrt2;

        c3Re[1][1] = (c4Re[1][1] + c4Re[2][1] + c4Re[1][2] + c4Re[2][2]) / 2.0;
        c3Im[1][1] = (c4Im[1][1] + c4Im[2][1] + c4Im[1][2] + c4Im[2][2]) / 2.0;

        c3Re[1][2] = (c4Re[1][3] + c4Re[2][3]) / Constants.sqrt2;
        c3Im[1][2] = (c4Im[1][3] + c4Im[2][3]) / Constants.sqrt2;

        c3Re[2][0] = c4Re[3][0];
        c3Im[2][0] = c4Im[3][0];

        c3Re[2][1] = (c4Re[3][1] + c4Re[3][2]) / Constants.sqrt2;
        c3Im[2][1] = (c4Im[3][1] + c4Im[3][2]) / Constants.sqrt2;

        c3Re[2][2] = c4Re[3][3];
        c3Im[2][2] = c4Im[3][3];
    }

    /**
     * Get mean coherency matrix for given pixel.
     *
     * @param x                 X coordinate of the given pixel.
     * @param y                 Y coordinate of the given pixel.
     * @param halfWindowSizeX   The sliding window size / 2.
     * @param halfWindowSizeY   The sliding window size / 2.
     * @param sourceProductType The source product type.
     * @param sourceImageWidth  The source image width.
     * @param sourceImageHeight The source image height.
     * @param srcIndex          The TileIndex of the first source tile
     * @param dataBuffers       Source tile data buffers.
     * @param Tr                The real part of the mean coherency matrix.
     * @param Ti                The imaginary part of the mean coherency matrix.
     */
    default void getMeanCoherencyMatrix(
            final int x, final int y, final int halfWindowSizeX, final int halfWindowSizeY,
            final int sourceImageWidth, final int sourceImageHeight,
            final PolBandUtils.MATRIX sourceProductType, final TileIndex srcIndex, final ProductData[] dataBuffers,
            final double[][] Tr, final double[][] Ti) {

        final double[][] tempSr = new double[2][2];
        final double[][] tempSi = new double[2][2];
        final double[][] tempCr = new double[3][3];
        final double[][] tempCi = new double[3][3];
        final double[][] tempTr = new double[3][3];
        final double[][] tempTi = new double[3][3];

        final int xSt = FastMath.max(x - halfWindowSizeX, 0);
        final int xEd = FastMath.min(x + halfWindowSizeX, sourceImageWidth - 1);
        final int ySt = FastMath.max(y - halfWindowSizeY, 0);
        final int yEd = FastMath.min(y + halfWindowSizeY, sourceImageHeight - 1);
        final int num = (yEd - ySt + 1) * (xEd - xSt + 1);

        final Matrix TrMat = new Matrix(3, 3);
        final Matrix TiMat = new Matrix(3, 3);

        if (sourceProductType == PolBandUtils.MATRIX.T3) {

            for (int yy = ySt; yy <= yEd; ++yy) {
                srcIndex.calculateStride(yy);
                for (int xx = xSt; xx <= xEd; ++xx) {
                    getCoherencyMatrixT3(srcIndex.getIndex(xx), dataBuffers, tempTr, tempTi);
                    TrMat.plusEquals(new Matrix(tempTr));
                    TiMat.plusEquals(new Matrix(tempTi));
                }
            }

        } else if (sourceProductType == PolBandUtils.MATRIX.C3) {

            for (int yy = ySt; yy <= yEd; ++yy) {
                srcIndex.calculateStride(yy);
                for (int xx = xSt; xx <= xEd; ++xx) {
                    getCovarianceMatrixC3(srcIndex.getIndex(xx), dataBuffers, tempCr, tempCi);
                    c3ToT3(tempCr, tempCi, tempTr, tempTi);
                    TrMat.plusEquals(new Matrix(tempTr));
                    TiMat.plusEquals(new Matrix(tempTi));
                }
            }

        } else if (sourceProductType == PolBandUtils.MATRIX.FULL) {

            for (int yy = ySt; yy <= yEd; ++yy) {
                srcIndex.calculateStride(yy);
                for (int xx = xSt; xx <= xEd; ++xx) {
                    getComplexScatterMatrix(srcIndex.getIndex(xx), dataBuffers, tempSr, tempSi);
                    computeCoherencyMatrixT3(tempSr, tempSi, tempTr, tempTi);
                    TrMat.plusEquals(new Matrix(tempTr));
                    TiMat.plusEquals(new Matrix(tempTi));
                }
            }
        }

        TrMat.timesEquals(1.0 / num);
        TiMat.timesEquals(1.0 / num);
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                Tr[i][j] = TrMat.get(i, j);
                Ti[i][j] = TiMat.get(i, j);
            }
        }
    }

    /**
     * Get mean covariance matrix for given pixel.
     *
     * @param x                 X coordinate of the given pixel.
     * @param y                 Y coordinate of the given pixel.
     * @param halfWindowSizeX   The sliding window size / 2
     * @param halfWindowSizeY   The sliding window size / 2
     * @param sourceProductType The source product type.
     * @param sourceTiles       The source tiles for all bands.
     * @param dataBuffers       Source tile data buffers.
     * @param Cr                The real part of the mean covariance matrix.
     * @param Ci                The imaginary part of the mean covariance matrix.
     */
    default void getMeanCovarianceMatrix(
            final int x, final int y, final int halfWindowSizeX, final int halfWindowSizeY,
            final PolBandUtils.MATRIX sourceProductType, final Tile[] sourceTiles, final ProductData[] dataBuffers,
            final double[][] Cr, final double[][] Ci) {

        final double[][] tempCr = new double[3][3];
        final double[][] tempCi = new double[3][3];

        final int xSt = Math.max(x - halfWindowSizeX, sourceTiles[0].getMinX());
        final int xEd = Math.min(x + halfWindowSizeX, sourceTiles[0].getMaxX());
        final int ySt = Math.max(y - halfWindowSizeY, sourceTiles[0].getMinY());
        final int yEd = Math.min(y + halfWindowSizeY, sourceTiles[0].getMaxY());
        final int num = (yEd - ySt + 1) * (xEd - xSt + 1);

        final TileIndex srcIndex = new TileIndex(sourceTiles[0]);

        final Matrix CrMat = new Matrix(3, 3);
        final Matrix CiMat = new Matrix(3, 3);

        if (sourceProductType == PolBandUtils.MATRIX.C3) {

            for (int yy = ySt; yy <= yEd; ++yy) {
                srcIndex.calculateStride(yy);
                for (int xx = xSt; xx <= xEd; ++xx) {
                    getCovarianceMatrixC3(srcIndex.getIndex(xx), dataBuffers, tempCr, tempCi);
                    CrMat.plusEquals(new Matrix(tempCr));
                    CiMat.plusEquals(new Matrix(tempCi));
                }
            }

        } else if (sourceProductType == PolBandUtils.MATRIX.T3) {
            final double[][] tempTr = new double[3][3];
            final double[][] tempTi = new double[3][3];

            for (int yy = ySt; yy <= yEd; ++yy) {
                srcIndex.calculateStride(yy);
                for (int xx = xSt; xx <= xEd; ++xx) {
                    getCoherencyMatrixT3(srcIndex.getIndex(xx), dataBuffers, tempTr, tempTi);
                    t3ToC3(tempTr, tempTi, tempCr, tempCi);
                    CrMat.plusEquals(new Matrix(tempCr));
                    CiMat.plusEquals(new Matrix(tempCi));
                }
            }

        } else if (sourceProductType == PolBandUtils.MATRIX.FULL) {
            final double[][] tempSr = new double[2][2];
            final double[][] tempSi = new double[2][2];

            for (int yy = ySt; yy <= yEd; ++yy) {
                srcIndex.calculateStride(yy);
                for (int xx = xSt; xx <= xEd; ++xx) {
                    getComplexScatterMatrix(srcIndex.getIndex(xx), dataBuffers, tempSr, tempSi);
                    computeCovarianceMatrixC3(tempSr, tempSi, tempCr, tempCi);
                    CrMat.plusEquals(new Matrix(tempCr));
                    CiMat.plusEquals(new Matrix(tempCi));
                }
            }
        }

        CrMat.timesEquals(1.0 / num);
        CiMat.timesEquals(1.0 / num);
        for (int i = 0; i < 3; i++) {
            Cr[i][0] = CrMat.get(i, 0);
            Ci[i][0] = CiMat.get(i, 0);

            Cr[i][1] = CrMat.get(i, 1);
            Ci[i][1] = CiMat.get(i, 1);

            Cr[i][2] = CrMat.get(i, 2);
            Ci[i][2] = CiMat.get(i, 2);
        }
    }

    default void getMeanCovarianceMatrixC4(
            final int x, final int y, final int halfWindowSizeX, final int halfWindowSizeY,
            final PolBandUtils.MATRIX sourceProductType, final Tile[] sourceTiles, final ProductData[] dataBuffers,
            final double[][] Cr, final double[][] Ci) {

        final double[][] tempCr = new double[4][4];
        final double[][] tempCi = new double[4][4];

        final int xSt = Math.max(x - halfWindowSizeX, sourceTiles[0].getMinX());
        final int xEd = Math.min(x + halfWindowSizeX, sourceTiles[0].getMaxX());
        final int ySt = Math.max(y - halfWindowSizeY, sourceTiles[0].getMinY());
        final int yEd = Math.min(y + halfWindowSizeY, sourceTiles[0].getMaxY());
        final int num = (yEd - ySt + 1) * (xEd - xSt + 1);

        final TileIndex srcIndex = new TileIndex(sourceTiles[0]);

        final Matrix CrMat = new Matrix(4, 4);
        final Matrix CiMat = new Matrix(4, 4);

        for (int yy = ySt; yy <= yEd; ++yy) {
            srcIndex.calculateStride(yy);
            for (int xx = xSt; xx <= xEd; ++xx) {
                getCovarianceMatrixC4(srcIndex.getIndex(xx), sourceProductType, dataBuffers, tempCr, tempCi);
                CrMat.plusEquals(new Matrix(tempCr));
                CiMat.plusEquals(new Matrix(tempCi));
            }
        }

        CrMat.timesEquals(1.0 / num);
        CiMat.timesEquals(1.0 / num);
        for (int i = 0; i < 4; i++) {
            Cr[i][0] = CrMat.get(i, 0);
            Ci[i][0] = CiMat.get(i, 0);

            Cr[i][1] = CrMat.get(i, 1);
            Ci[i][1] = CiMat.get(i, 1);

            Cr[i][2] = CrMat.get(i, 2);
            Ci[i][2] = CiMat.get(i, 2);

            Cr[i][3] = CrMat.get(i, 3);
            Ci[i][3] = CiMat.get(i, 3);
        }
    }

    /**
     * Get covariance matrix C4 for given pixel.
     *
     * @param index             Pixel index in the given tile.
     * @param sourceProductType The source product type.
     * @param dataBuffers       Source tile data buffers.
     * @param Cr                The real part of the covariance matrix C4.
     * @param Ci                The imaginary part of the covariance matrix C4.
     */
    default void getCovarianceMatrixC4(
            final int index, final PolBandUtils.MATRIX sourceProductType, final ProductData[] dataBuffers,
            final double[][] Cr, final double[][] Ci) {

        if (sourceProductType == PolBandUtils.MATRIX.FULL) {

            final double[][] tempSr = new double[2][2];
            final double[][] tempSi = new double[2][2];

            getComplexScatterMatrix(index, dataBuffers, tempSr, tempSi);
            computeCovarianceMatrixC4(tempSr, tempSi, Cr, Ci);

        } else if (sourceProductType == PolBandUtils.MATRIX.T4) {

            final double[][] tempTr = new double[4][4];
            final double[][] tempTi = new double[4][4];

            getCoherencyMatrixT4(index, dataBuffers, tempTr, tempTi);
            t4ToC4(tempTr, tempTi, Cr, Ci);

        } else if (sourceProductType == PolBandUtils.MATRIX.C4) {

            getCovarianceMatrixC4(index, dataBuffers, Cr, Ci);

        }
    }

    /**
     * Get coherency matrix T4 for given pixel.
     *
     * @param index             Pixel index in the given tile.
     * @param sourceProductType The source product type.
     * @param dataBuffers       Source tile data buffers.
     * @param Tr                The real part of the coherency matrix T4.
     * @param Ti                The imaginary part of the coherency matrix T4.
     */
    default void getCoherencyMatrixT4(
            final int index, final PolBandUtils.MATRIX sourceProductType, final ProductData[] dataBuffers,
            final double[][] Tr, final double[][] Ti) {

        if (sourceProductType == PolBandUtils.MATRIX.FULL) {

            final double[][] tempSr = new double[2][2];
            final double[][] tempSi = new double[2][2];

            getComplexScatterMatrix(index, dataBuffers, tempSr, tempSi);
            computeCoherencyMatrixT4(tempSr, tempSi, Tr, Ti);

        } else if (sourceProductType == PolBandUtils.MATRIX.T4) {

            getCoherencyMatrixT4(index, dataBuffers, Tr, Ti);

        } else if (sourceProductType == PolBandUtils.MATRIX.C4) {

            final double[][] tempCr = new double[4][4];
            final double[][] tempCi = new double[4][4];

            getCovarianceMatrixC4(index, dataBuffers, tempCr, tempCi);
            c4ToT4(tempCr, tempCi, Tr, Ti);
        }
    }

    /**
     * Get covariance matrix C3 for given pixel.
     *
     * @param index             Pixel index in the given tile.
     * @param sourceProductType The source product type.
     * @param dataBuffers       Source tile data buffers.
     * @param Cr                The real part of the covariance matrix C3.
     * @param Ci                The imaginary part of the covariance matrix C3.
     */
    default void getCovarianceMatrixC3(
            final int index, final PolBandUtils.MATRIX sourceProductType, final ProductData[] dataBuffers,
            final double[][] Cr, final double[][] Ci) {

        if (sourceProductType == PolBandUtils.MATRIX.FULL) {

            final double[][] tempSr = new double[2][2];
            final double[][] tempSi = new double[2][2];

            getComplexScatterMatrix(index, dataBuffers, tempSr, tempSi);
            computeCovarianceMatrixC3(tempSr, tempSi, Cr, Ci);

        } else if (sourceProductType == PolBandUtils.MATRIX.T4) {

            final double[][] tempTr = new double[4][4];
            final double[][] tempTi = new double[4][4];
            final double[][] tempCr = new double[4][4];
            final double[][] tempCi = new double[4][4];

            getCoherencyMatrixT4(index, dataBuffers, tempTr, tempTi);
            t4ToC4(tempTr, tempTi, tempCr, tempCi);
            c4ToC3(tempCr, tempCi, Cr, Ci);

        } else if (sourceProductType == PolBandUtils.MATRIX.C4) {

            final double[][] tempCr = new double[4][4];
            final double[][] tempCi = new double[4][4];

            getCovarianceMatrixC4(index, dataBuffers, tempCr, tempCi);
            c4ToC3(tempCr, tempCi, Cr, Ci);

        } else if (sourceProductType == PolBandUtils.MATRIX.T3) {

            final double[][] tempTr = new double[3][3];
            final double[][] tempTi = new double[3][3];

            getCoherencyMatrixT3(index, dataBuffers, tempTr, tempTi);
            t3ToC3(tempTr, tempTi, Cr, Ci);

        } else if (sourceProductType == PolBandUtils.MATRIX.C3) {

            getCovarianceMatrixC3(index, dataBuffers, Cr, Ci);

        }
    }

    /**
     * Get coherency matrix T3 for given pixel.
     *
     * @param index             Pixel index in the given tile.
     * @param sourceProductType The source product type.
     * @param dataBuffers       Source tile data buffers.
     * @param Tr                The real part of the coherency matrix T3.
     * @param Ti                The imaginary part of the coherency matrix T3.
     */
    default void getCoherencyMatrixT3(
            final int index, final PolBandUtils.MATRIX sourceProductType, final ProductData[] dataBuffers,
            final double[][] Tr, final double[][] Ti) {

        if (sourceProductType == PolBandUtils.MATRIX.FULL) {

            final double[][] tempSr = new double[2][2];
            final double[][] tempSi = new double[2][2];

            getComplexScatterMatrix(index, dataBuffers, tempSr, tempSi);
            computeCoherencyMatrixT3(tempSr, tempSi, Tr, Ti);

        } else if (sourceProductType == PolBandUtils.MATRIX.T4) {

            final double[][] tempTr = new double[4][4];
            final double[][] tempTi = new double[4][4];

            getCoherencyMatrixT4(index, dataBuffers, tempTr, tempTi);
            t4ToT3(tempTr, tempTi, Tr, Ti);

        } else if (sourceProductType == PolBandUtils.MATRIX.C4) {

            final double[][] tempCr = new double[4][4];
            final double[][] tempCi = new double[4][4];
            final double[][] tempTr = new double[4][4];
            final double[][] tempTi = new double[4][4];

            getCovarianceMatrixC4(index, dataBuffers, tempCr, tempCi);
            c4ToT4(tempCr, tempCi, tempTr, tempTi);
            t4ToT3(tempTr, tempTi, Tr, Ti);

        } else if (sourceProductType == PolBandUtils.MATRIX.T3) {

            getCoherencyMatrixT3(index, dataBuffers, Tr, Ti);

        } else if (sourceProductType == PolBandUtils.MATRIX.C3) {

            final double[][] tempCr = new double[3][3];
            final double[][] tempCi = new double[3][3];

            getCovarianceMatrixC3(index, dataBuffers, tempCr, tempCi);
            c3ToT3(tempCr, tempCi, Tr, Ti);
        }
    }

    /**
     * Compute min/max values of the Span image.
     *
     * @param op       the decomposition operator
     * @param bandList the src band list
     * @return min max values
     * @throws OperatorException when thread fails
     */
    default DecompositionBase.MinMax computeSpanMinMax(final Operator op, final PolBandUtils.MATRIX sourceProductType,
                                                       final int halfWindowSizeX, final int halfWindowSizeY,
                                                       final PolBandUtils.PolSourceBand bandList)
            throws OperatorException {

        final DecompositionBase.MinMax minMaxValue = new DecompositionBase.MinMax();
        final Dimension tileSize = new Dimension(256, 256);
        final Rectangle[] tileRectangles = OperatorUtils.getAllTileRectangles(op.getSourceProduct(), tileSize, 25);
        final double[][] Cr = new double[3][3];
        final double[][] Ci = new double[3][3];

        final StatusProgressMonitor status = new StatusProgressMonitor(StatusProgressMonitor.TYPE.SUBTASK);
        status.beginTask("Computing min max span... ", tileRectangles.length);

        try {
            final ThreadManager threadManager = new ThreadManager();

            for (final Rectangle rectangle : tileRectangles) {

                final Thread worker = new Thread() {

                    double span = 0.0;
                    final int xMax = rectangle.x + rectangle.width;
                    final int yMax = rectangle.y + rectangle.height;
                    /*
                    System.out.println("setSpan x0 = " + rectangle.x + ", y0 = " + rectangle.y +
                                       ", w = " + rectangle.width + ", h = " + rectangle.height);
                    */

                    final Tile[] sourceTiles = new Tile[bandList.srcBands.length];
                    final ProductData[] dataBuffers = new ProductData[bandList.srcBands.length];

                    @Override
                    public void run() {
                        try {

                            getQuadPolDataBuffer(op, bandList.srcBands, rectangle, sourceProductType, sourceTiles, dataBuffers);

                            for (int y = rectangle.y; y < yMax; ++y) {

                                for (int x = rectangle.x; x < xMax; ++x) {

                                    getMeanCovarianceMatrix(x, y, halfWindowSizeX, halfWindowSizeX,
                                            sourceProductType, sourceTiles, dataBuffers, Cr, Ci);

                                    span = Cr[0][0] + Cr[1][1] + Cr[2][2];

                                    if (minMaxValue.min > span) {
                                        synchronized (minMaxValue) {
                                            minMaxValue.min = span;
                                        }
                                    }
                                    if (minMaxValue.max < span) {
                                        synchronized (minMaxValue) {
                                            minMaxValue.max = span;
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            System.out.println(e.getMessage());
                        }
                    }
                };

                threadManager.add(worker);

                status.worked(1);
            }

            threadManager.finish();

            if (minMaxValue.min < Constants.EPS) {
                minMaxValue.min = Constants.EPS;
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(op.getId() + " computeMinMaxSpan ", e);
        } finally {
            status.done();
        }
        return minMaxValue;
    }

    /*default void getT3(final int index, final PolBandUtils.MATRIX sourceProductType, final ProductData[] dataBuffers,
                             final double[][] Tr, final double[][] Ti) {

        if (sourceProductType == PolBandUtils.MATRIX.FULL) {

            final double[][] Sr = new double[2][2];
            final double[][] Si = new double[2][2];
            getComplexScatterMatrix(index, dataBuffers, Sr, Si);
            computeCoherencyMatrixT3(Sr, Si, Tr, Ti);

        } else if (sourceProductType == PolBandUtils.MATRIX.T3) {

            getCoherencyMatrixT3(index, dataBuffers, Tr, Ti);

        } else if (sourceProductType == PolBandUtils.MATRIX.C3) {

            final double[][] Cr = new double[3][3];
            final double[][] Ci = new double[3][3];
            getCovarianceMatrixC3(index, dataBuffers, Cr, Ci);
            c3ToT3(Cr, Ci, Tr, Ti);
        }
    }*/
}
