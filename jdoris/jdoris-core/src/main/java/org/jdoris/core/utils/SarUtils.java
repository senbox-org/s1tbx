package org.jdoris.core.utils;

import org.apache.log4j.Logger;
import org.jblas.ComplexDouble;
import org.jblas.ComplexDoubleMatrix;
import org.jblas.DoubleMatrix;
import org.jdoris.core.Window;

import static org.jblas.MatrixFunctions.pow;
import static org.jblas.MatrixFunctions.sqrt;

public class SarUtils {

    static Logger logger = Logger.getLogger(SarUtils.class.getName());

    /**
     * HARMONIC INTERPOLATION
     * B=oversample(A, factorrow, factorcol);
     * 2 factors possible, extrapolation at end.
     * no vectors possible.
     */
    public static ComplexDoubleMatrix oversample(ComplexDoubleMatrix inputMatrix, final int factorRow, final int factorCol) throws IllegalArgumentException {

        final int l = inputMatrix.rows;
        final int p = inputMatrix.columns;
        final int halfL = l / 2;
        final int halfP = p / 2;
        final int L2 = factorRow * l;  // numRows of output matrix
        final int P2 = factorCol * p;  // columns of output matrix

        if (inputMatrix.isVector()) {
            logger.error("oversample: only 2d matrices.");
            throw new IllegalArgumentException("oversample: only 2d matrices");
        }
        if (!MathUtils.isPower2(l) && factorRow != 1) {
            logger.error("oversample: numlines != 2^n");
            throw new IllegalArgumentException("oversample: numlines != 2^n");
        }
        if (!MathUtils.isPower2(p) && factorCol != 1) {
            logger.error("oversample: numcols != 2^n");
            throw new IllegalArgumentException("oversample: numcols != 2^n");
        }

        if (factorRow == 1 && factorCol == 1) {
            logger.info("oversample: both azimuth and range oversampling factors equal to 1!");
            logger.info("oversample: returning inputMatrix!");
            return inputMatrix;
        }

        final ComplexDouble half = new ComplexDouble(0.5);
        ComplexDoubleMatrix returnMatrix = new ComplexDoubleMatrix(L2, P2);

        final Window winA1;
        final Window winA2;
        final Window winR2;

        ComplexDoubleMatrix tempMatrix;
        if (factorRow == 1) {

            // 1d fourier transform per row
            tempMatrix = SpectralUtils.fft(inputMatrix, 2);

            // TODO: check this
            // divide by 2 because even fftlength
            tempMatrix.putColumn(halfP, tempMatrix.getColumn(halfP).mmuli(half));

            // zero padding windows
            winA1 = new Window(0, l - 1, 0, halfP);
            winA2 = new Window(0, l - 1, halfP, p - 1);
            winR2 = new Window(0, l - 1, P2 - halfP, P2 - 1);

            // prepare data
            LinearAlgebraUtils.setdata(returnMatrix, winA1, tempMatrix, winA1);
            LinearAlgebraUtils.setdata(returnMatrix, winR2, tempMatrix, winA2);

            // inverse fft per row
            SpectralUtils.invfft_inplace(returnMatrix, 2);

        } else if (factorCol == 1) {

            // 1d fourier transform per column
            tempMatrix = SpectralUtils.fft(inputMatrix, 1);

            // divide by 2 'cause even fftlength
            tempMatrix.putRow(halfL, tempMatrix.getRow(halfL).mmul(half));
//            for (i=0; i<p; ++i){
//                A(halfl,i) *= half;
//            }

            // zero padding windows
            winA1 = new Window(0, halfL, 0, p - 1);
            winR2 = new Window(L2 - halfL, L2 - 1, 0, p - 1);
            winA2 = new Window(halfL, l - 1, 0, p - 1);

            // prepare data
            LinearAlgebraUtils.setdata(returnMatrix, winA1, tempMatrix, winA1);
            LinearAlgebraUtils.setdata(returnMatrix, winR2, tempMatrix, winA2);

            // inverse fft per row
            SpectralUtils.invfft_inplace(returnMatrix, 1);

        } else {

            // define extra windows for 2d oversampling
            Window winA3;
            Window winA4;
            Window winR3;
            Window winR4;

            // A=fft2d(A)
            tempMatrix = SpectralUtils.fft2D(inputMatrix);

            // divide by 2 'cause even fftlength
            tempMatrix.putColumn(halfP, tempMatrix.getColumn(halfP).mmuli(half));
            tempMatrix.putRow(halfL, tempMatrix.getRow(halfL).mmuli(half));
//            for (i=0; i<l; ++i) {
//                A(i,halfp) *= half;
//            }
//            for (i=0; i<p; ++i) {
//                A(halfl,i) *= half;
//            }

            // zero padding windows
            winA1 = new Window(0, halfL, 0, halfP);   // zero padding windows
            winA2 = new Window(0, halfL, halfP, p - 1);
            winA3 = new Window(halfL, l - 1, 0, halfP);
            winA4 = new Window(halfL, l - 1, halfP, p - 1);
            winR2 = new Window(0, halfL, P2 - halfP, P2 - 1);
            winR3 = new Window(L2 - halfL, L2 - 1, 0, halfP);
            winR4 = new Window(L2 - halfL, L2 - 1, P2 - halfP, P2 - 1);

            // prepare data
            LinearAlgebraUtils.setdata(returnMatrix, winA1, tempMatrix, winA1);
            LinearAlgebraUtils.setdata(returnMatrix, winR2, tempMatrix, winA2);
            LinearAlgebraUtils.setdata(returnMatrix, winR3, tempMatrix, winA3);
            LinearAlgebraUtils.setdata(returnMatrix, winR4, tempMatrix, winA4);

            // inverse back in 2d
            SpectralUtils.invfft2D_inplace(returnMatrix);
        }

        // scale
        returnMatrix.mmuli((double) (factorRow * factorCol));
        return returnMatrix;

    }

    public static DoubleMatrix intensity(final ComplexDoubleMatrix inputMatrix) {
        return pow(inputMatrix.real(), 2).add(pow(inputMatrix.imag(), 2));
    }

    public static DoubleMatrix magnitude(final ComplexDoubleMatrix inputMatrix) {
        return sqrt(intensity(inputMatrix));
    }

    @Deprecated
    public static DoubleMatrix coherence(final ComplexDoubleMatrix inputMatrix, final ComplexDoubleMatrix normsMatrix, final int winL, final int winP) {

        logger.trace("coherence ver #2");
        if (!(winL >= winP)) {
            logger.warn("coherence: estimator window size L<P not very efficiently programmed.");
        }

        if (inputMatrix.rows != normsMatrix.rows || inputMatrix.rows != inputMatrix.rows) {
            logger.error("coherence: not same dimensions.");
            throw new IllegalArgumentException("coherence: not the same dimensions.");
        }

        // allocate output :: account for window overlap
        DoubleMatrix outputMatrix = new DoubleMatrix(inputMatrix.rows - winL + 1, inputMatrix.columns);

        // temp variables
        int i, j, k, l;
        ComplexDouble sum;
        ComplexDouble power;
        int leadingZeros = (winP - 1) / 2;  // number of pixels=0 floor...
        int trailingZeros = (winP) / 2;     // floor...

        for (j = leadingZeros; j < outputMatrix.columns - trailingZeros; j++) {

            sum = new ComplexDouble(0);
            power = new ComplexDouble(0);

            //// Compute sum over first data block ////
            for (k = 0; k < winL; k++) {
                for (l = j - leadingZeros; l < j - leadingZeros + winP; l++) {
                    sum.addi(inputMatrix.get(k, l));
                    power.addi(normsMatrix.get(k, l));
                }
            }
            outputMatrix.put(0, j, coherenceProduct(sum, power));

            //// Compute (relatively) sum over rest of data blocks ////
            for (i = 0; i < outputMatrix.rows - 1; i++) {
                for (l = j - leadingZeros; l < j - leadingZeros + winP; l++) {
                    sum.addi(inputMatrix.get(i + winL, l).sub(inputMatrix.get(i, l)));
                    power.addi(normsMatrix.get(i + winL, l).sub(normsMatrix.get(i, l)));
                }
                outputMatrix.put(i + 1, j, coherenceProduct(sum, power));
            }
        }
        return outputMatrix;
    }

    public static DoubleMatrix coherence2(final ComplexDoubleMatrix input, final ComplexDoubleMatrix norms, final int winL, final int winP) {

        logger.trace("coherence ver #2");
        if (!(winL >= winP)) {
            logger.warn("coherence: estimator window size L<P not very efficiently programmed.");
        }

        if (input.rows != norms.rows || input.rows != input.rows) {
            logger.error("coherence: not same dimensions.");
            throw new IllegalArgumentException("coherence: not the same dimensions.");
        }

        // allocate output :: account for window overlap
        final int extent_RG = input.columns;
        final int extent_AZ = input.rows - winL + 1;
        DoubleMatrix result = new DoubleMatrix(input.rows - winL + 1, input.columns - winP + 1);

        // temp variables
        int i, j, k, l;
        ComplexDouble sum;
        ComplexDouble power;
        int leadingZeros = (winP - 1) / 2;  // number of pixels=0 floor...
        int trailingZeros = (winP) / 2;     // floor...

        for (j = leadingZeros; j < extent_RG - trailingZeros; j++) {

            sum = new ComplexDouble(0);
            power = new ComplexDouble(0);

            //// Compute sum over first data block ////
            for (k = 0; k < winL; k++) {
                for (l = j - leadingZeros; l < j - leadingZeros + winP; l++) {
                    sum.addi(input.get(k, l));
                    power.addi(norms.get(k, l));
                }
            }
            result.put(0, j - leadingZeros, coherenceProduct(sum, power));

            //// Compute (relatively) sum over rest of data blocks ////
            for (i = 0; i < extent_AZ - 1; i++) {
                for (l = j - leadingZeros; l < j - leadingZeros + winP; l++) {
                    sum.addi(input.get(i + winL, l).sub(input.get(i, l)));
                    power.addi(norms.get(i + winL, l).sub(norms.get(i, l)));
                }
                result.put(i + 1, j - leadingZeros, coherenceProduct(sum, power));
            }
        }
        return result;
    }

    static double coherenceProduct(final ComplexDouble sum, final ComplexDouble power) {
        final double product = power.real() * power.imag();
//        return (product > 0.0) ? Math.sqrt(Math.pow(sum.abs(),2) / product) : 0.0;
        return (product > 0.0) ? sum.abs() / Math.sqrt(product) : 0.0;
    }

    public static ComplexDoubleMatrix multilook(final ComplexDoubleMatrix inputMatrix, final int factorRow, final int factorColumn) {

        if (factorRow == 1 && factorColumn == 1) {
            return inputMatrix;
        }

        logger.debug("multilook input [inputMatrix] size: " +
                inputMatrix.length + " lines: " + inputMatrix.rows + " pixels: " + inputMatrix.columns);

        if (inputMatrix.rows / factorRow == 0 || inputMatrix.columns / factorColumn == 0) {
            logger.debug("Multilooking was not necessary for this inputMatrix: inputMatrix.rows < mlR or buffer.columns < mlC");
            return inputMatrix;
        }

        ComplexDouble sum;
        final ComplexDouble factorLP = new ComplexDouble(factorRow * factorColumn);
        ComplexDoubleMatrix outputMatrix = new ComplexDoubleMatrix(inputMatrix.rows / factorRow, inputMatrix.columns / factorColumn);
        for (int i = 0; i < outputMatrix.rows; i++) {
            for (int j = 0; j < outputMatrix.columns; j++) {
                sum = new ComplexDouble(0);
                for (int k = i * factorRow; k < (i + 1) * factorRow; k++) {
                    for (int l = j * factorColumn; l < (j + 1) * factorColumn; l++) {
                        sum.addi(inputMatrix.get(k, l));
                    }
                }
                outputMatrix.put(i, j, sum.div(factorLP));
            }
        }
        return outputMatrix;
    }

    public static ComplexDoubleMatrix computeIfg(final ComplexDoubleMatrix masterData, final ComplexDoubleMatrix slaveData) throws Exception {
        return LinearAlgebraUtils.dotmult(masterData, slaveData.conj());
    }

    public static void computeIfg_inplace(final ComplexDoubleMatrix masterData, final ComplexDoubleMatrix slaveData) throws Exception {
        LinearAlgebraUtils.dotmult_inplace(masterData, slaveData);
    }

    public static ComplexDoubleMatrix computeIfg(final ComplexDoubleMatrix masterData, final ComplexDoubleMatrix slaveData,
                                                 final int ovsFactorAz, final int ovsFactorRg) throws Exception {
        if (ovsFactorAz == 1 && ovsFactorRg == 1) {
            return computeIfg(masterData, slaveData);
        }   else {
            return computeIfg(oversample(masterData, ovsFactorAz, ovsFactorRg), oversample(slaveData, ovsFactorAz, ovsFactorRg));
        }

    }
}
