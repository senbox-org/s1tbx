package org.jlinda.core.utils;

import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;
import edu.emory.mathcs.jtransforms.fft.DoubleFFT_2D;
import org.apache.log4j.Logger;
import org.jblas.ComplexDoubleMatrix;
import org.jblas.DoubleMatrix;

public class SpectralUtils {

    static Logger logger = Logger.getLogger(SpectralUtils.class.getName());

    private static void fftTransform1D_inplace(ComplexDoubleMatrix vector, int fftLength, int direction) {
        switch (direction) {
            case 1:
                fft1D_inplace(vector, fftLength);
                break;
            case -1:
                invfft1D_inplace(vector, fftLength);
                break;
            default:
                throw new IllegalArgumentException("fourier1D: direction 1, or -1");
        }
    }

    public static void fft1D_inplace(ComplexDoubleMatrix vector, final int fftLength) {
        DoubleFFT_1D fft = new DoubleFFT_1D(fftLength);
        fft.complexForward(vector.data);
    }

    public static void invfft1D_inplace(ComplexDoubleMatrix vector, final int fftLength) {
        DoubleFFT_1D fft = new DoubleFFT_1D(fftLength);
        fft.complexInverse(vector.data, true);
    }

    public static ComplexDoubleMatrix fft1D(ComplexDoubleMatrix vector, final int fftLength) {
        DoubleFFT_1D fft = new DoubleFFT_1D(fftLength);
        fft.complexForward(vector.data);
        return vector;
    }

    public static ComplexDoubleMatrix invfft1D(ComplexDoubleMatrix vector, final int fftLength) {
        DoubleFFT_1D fft = new DoubleFFT_1D(fftLength);
        fft.complexInverse(vector.data, true);
        return vector;
    }

    private static ComplexDoubleMatrix fftTransform(ComplexDoubleMatrix A, final int dimension, final int flag) {
        ComplexDoubleMatrix result = A.dup(); // have to copy matrix!
        fftTransformInPlace(result, dimension, flag);
        return result;
    }

    private static void fftTransformInPlace(ComplexDoubleMatrix cplxData, int dimension, int flag) {
        int i;
        final int columns = cplxData.columns;
        final int rows = cplxData.rows;

        switch (dimension) {
            case 1: {
                logger.debug("1d ifft over columns");
                for (i = 0; i < columns; ++i) {
                    ComplexDoubleMatrix VECTOR = cplxData.getColumn(i);
                    fftTransform1D_inplace(VECTOR, rows, flag);
                    cplxData.putColumn(i, VECTOR);
                }
                break;
            }
            case 2: {
                logger.debug("1d ifft over rows");
                for (i = 0; i < rows; ++i) {
                    ComplexDoubleMatrix VECTOR = cplxData.getRow(i);
                    fftTransform1D_inplace(VECTOR, columns, flag);
                    cplxData.putRow(i, VECTOR);
                }
                break;
            }
            default:
                logger.error("ifft: dimension != {1,2}");
                throw new IllegalArgumentException("ifft: dimension != {1,2}");
        }
    }

    public static ComplexDoubleMatrix fft(ComplexDoubleMatrix inMatrix, final int dimension) {
        return fftTransform(inMatrix, dimension, 1);
    }

    public static ComplexDoubleMatrix invfft(ComplexDoubleMatrix inMatrix, final int dimension) {
        return fftTransform(inMatrix, dimension, -1);
    }

    public static void fft_inplace(ComplexDoubleMatrix inMatrix, int dimension) {
        fftTransformInPlace(inMatrix, dimension, 1);
    }

    public static void invfft_inplace(ComplexDoubleMatrix inMatrix, int dimension) {
        fftTransformInPlace(inMatrix, dimension, -1);
    }

    public static void fft2D_inplace(ComplexDoubleMatrix A) {
        ComplexDoubleMatrix aTemp = A.transpose();
        DoubleFFT_2D fft2d = new DoubleFFT_2D(aTemp.rows, aTemp.columns);
//        fft2d.complexForward(A.data);
        fft2d.complexForward(aTemp.data);
        A.data = aTemp.transpose().data;
    }

    public static ComplexDoubleMatrix fft2D(ComplexDoubleMatrix inMatrix) {
        ComplexDoubleMatrix outMatrix = inMatrix.dup();
        fft2D_inplace(outMatrix);
        return outMatrix;
    }

    public static void fft2D_inplace(DoubleMatrix A) {
        DoubleFFT_2D fft2d = new DoubleFFT_2D(A.rows, A.columns);
        fft2d.realForwardFull(A.data);
    }

    public static void invfft2D_inplace(ComplexDoubleMatrix A) {
        DoubleFFT_2D fft2d = new DoubleFFT_2D(A.rows, A.columns);
//        fft2d.complexInverse(A.data, true);
        ComplexDoubleMatrix aTemp = A.transpose();
        fft2d.complexInverse(aTemp.data, true);
        A.data = aTemp.transpose().data;
    }

    public static ComplexDoubleMatrix invfft2d(ComplexDoubleMatrix inMatrix) {
        ComplexDoubleMatrix outMatrix = inMatrix.dup();
        invfft2D_inplace(outMatrix);
        return outMatrix;
    }

    public static ComplexDoubleMatrix fftshift(ComplexDoubleMatrix inMatrix) {
        if (!inMatrix.isVector()) {
            logger.error("ifftshift: only vectors");
            throw new IllegalArgumentException("ifftshift: works only for vectors!");
        }

        final int cplxMatrixLength = 2*inMatrix.length;

        ComplexDoubleMatrix outMatrix = new ComplexDoubleMatrix(inMatrix.rows, inMatrix.columns);
        final int start = (int) (Math.floor((double) cplxMatrixLength / 2) + 1);

        System.arraycopy(inMatrix.data, start, outMatrix.data, 0, cplxMatrixLength - start);
        System.arraycopy(inMatrix.data, 0, outMatrix.data, cplxMatrixLength - start, start);

        return outMatrix;
    }

    public static DoubleMatrix fftshift(DoubleMatrix inMatrix) {
        if (!inMatrix.isVector()) {
            logger.error("ifftshift: only vectors");
            throw new IllegalArgumentException("ifftshift: works only for vectors!");
        }

        DoubleMatrix outMatrix = new DoubleMatrix(inMatrix.rows, inMatrix.columns);
        final int start = (int) (Math.ceil((double) inMatrix.length / 2));

        System.arraycopy(inMatrix.data, start, outMatrix.data, 0, inMatrix.length - start);
        System.arraycopy(inMatrix.data, 0, outMatrix.data, inMatrix.length - start, start);

        return outMatrix;
    }

    public static void fftshift_inplace(ComplexDoubleMatrix inMatrix) {
        // NOT very efficient! Allocating and copying! //
        inMatrix.copy(fftshift(inMatrix));
    }

    public static void fftshift_inplace(DoubleMatrix inMatrix) {
        // NOT very efficient! Allocating and copying! //
        inMatrix.copy(fftshift(inMatrix));
    }

    /**
     * ifftshift(inMatrix)                                                 *
     * ifftshift of vector inMatrix is returned in inMatrix by reference       *
     * undo effect of fftshift. ?p=floor(m/2); inMatrix=inMatrix[p:m-1 0:p-1]; *
     */
    public static ComplexDoubleMatrix ifftshift(ComplexDoubleMatrix inMatrix) throws IllegalArgumentException {

        if (!inMatrix.isVector()) {
            logger.error("ifftshift: only vectors");
            throw new IllegalArgumentException("ifftshift: works only for vectors!");
        }

        final int cplxMatrixLength = 2*inMatrix.length;

        ComplexDoubleMatrix outMatrix = new ComplexDoubleMatrix(inMatrix.rows, inMatrix.columns);
        final int start = (int) (Math.floor((double) cplxMatrixLength / 2) - 1);

        System.arraycopy(inMatrix.data, start, outMatrix.data, 0, cplxMatrixLength - start);
        System.arraycopy(inMatrix.data, 0, outMatrix.data, cplxMatrixLength - start, start);

        return outMatrix;
    }

    public static DoubleMatrix ifftshift(DoubleMatrix inMatrix) throws IllegalArgumentException {

        if (!inMatrix.isVector()) {
            logger.error("ifftshift: only vectors");
            throw new IllegalArgumentException("ifftshift: works only for vectors!");
        }

        DoubleMatrix outMatrix = new DoubleMatrix(inMatrix.rows, inMatrix.columns);
        final int start = (int) (Math.ceil((double) inMatrix.length) / 2);

        System.arraycopy(inMatrix.data, start, outMatrix.data, 0, inMatrix.length - start);
        System.arraycopy(inMatrix.data, 0, outMatrix.data, inMatrix.length - start, start);

        return outMatrix;
    }

    public static void ifftshift_inplace(ComplexDoubleMatrix inMatrix) throws IllegalArgumentException {
        // NOT very efficient! Allocating and copying! //
        inMatrix.copy(ifftshift(inMatrix));
    }

    public static void ifftshift_inplace(DoubleMatrix inMatrix) throws IllegalArgumentException {
        // NOT very efficient! Allocating and copying! //
        inMatrix.copy(ifftshift(inMatrix));
    }




}
