package org.jlinda.core.utils;

import org.apache.log4j.Logger;
import org.jblas.ComplexDoubleMatrix;
import org.jblas.DoubleMatrix;
import org.jlinda.core.Window;

import static org.jblas.MatrixFunctions.abs;
import static org.jblas.MatrixFunctions.pow;

public class LinearAlgebraUtils {

    static Logger logger = Logger.getLogger(LinearAlgebraUtils.class.getName());

    /**
     * solve22
     * Solves setof 2 equations by straightforward substitution
     * y=Ax (unknown x)
     * input:
     * - matrix<real8> righthandside 2x1 (y)
     * - matrix<real8> partials 2x2 (A)
     * output:
     * - matrix<real8> result 2x1 unknown dx,dy,dz
     */
    public static double[] solve22(double[][] A, double[] y) throws IllegalArgumentException {

        double[] result = new double[2];

        if (A[0].length != 2 || A.length != 2) {
            throw new IllegalArgumentException("solve22: input: size of A not 22.");
        }
        if (y.length != 2) {
            throw new IllegalArgumentException("solve22: input: size y not 2x1.");
        }

        // Direct Solution
        result[1] = (y[0] - ((A[0][0] / A[1][0]) * y[1])) / (A[0][1] - ((A[0][0] * A[1][1]) / A[1][0]));
        result[0] = (y[0] - A[0][1] * result[1]) / A[0][0];

        return result;

    }

    /* Solve33
       Solves setof 3 equations by straightforward (no pivotting) LU
       y=Ax (unknown x)

        input:
          - matrix righthandside 3x1 (y)
          - matrix partials 3x3 (A)

        output/return:
         - matrix result 3x1 unknown
    */
    public static double[] solve33(double[][] A, double[] rhs) throws IllegalArgumentException {

        double[] result = new double[3];

        if (A[0].length != 3 || A.length != 3) {
            throw new IllegalArgumentException("solve33: input: size of A not 33.");
        }
        if (rhs.length != 3) {
            throw new IllegalArgumentException("solve33: input: size rhs not 3x1.");
        }

        // real8 L10, L20, L21: used lower matrix elements
        // real8 U11, U12, U22: used upper matrix elements
        // real8 b0,  b1,  b2:  used Ux=b
        final double L10 = A[1][0] / A[0][0];
        final double L20 = A[2][0] / A[0][0];
        final double U11 = A[1][1] - L10 * A[0][1];
        final double L21 = (A[2][1] - (A[0][1] * L20)) / U11;
        final double U12 = A[1][2] - L10 * A[0][2];
        final double U22 = A[2][2] - L20 * A[0][2] - L21 * U12;

        // ______ Solution: forward substitution ______
        final double b0 = rhs[0];
        final double b1 = rhs[1] - b0 * L10;
        final double b2 = rhs[2] - b0 * L20 - b1 * L21;

        // ______ Solution: backwards substitution ______
        result[2] = b2 / U22;
        result[1] = (b1 - U12 * result[2]) / U11;
        result[0] = (b0 - A[0][1] * result[1] - A[0][2] * result[2]) / A[0][0];

        return result;

    }

    public static DoubleMatrix absMatrix(DoubleMatrix inMatrix) {
        return abs(inMatrix);
    }

    public static double[][] absMatrix(double[][] inMatrix) {
        for (int i = 0; i < inMatrix.length; i++) {
            for (int j = 0; j < inMatrix[0].length; j++) {
                inMatrix[i][j] = Math.abs(inMatrix[i][j]);
            }
        }
        return inMatrix;
    }

    public static DoubleMatrix matrixPower(DoubleMatrix data, double scalar) {
        return pow(data, scalar);
    }

    public static double[][] matrixPower(double[][] inMatrix, double scalar) {
        for (int i = 0; i < inMatrix.length; ++i) {
            for (int j = 0; j < inMatrix[0].length; ++j) {
                inMatrix[i][j] = Math.pow(inMatrix[i][j], scalar);
            }
        }
        return inMatrix;
    }

    public static void invertChol_inplace(double[][] inMatrix) {
        final int numOfRows = inMatrix.length;
        double sum;
        int i, j, k;
        // Compute inv(L) store in lower of inMatrix
        for (i = 0; i < numOfRows; ++i) {
            inMatrix[i][i] = 1. / inMatrix[i][i];
            for (j = i + 1; j < numOfRows; ++j) {
                sum = 0.;
                for (k = i; k < j; ++k) {
                    sum -= inMatrix[j][k] * inMatrix[k][i];
                }
                inMatrix[j][i] = sum / inMatrix[j][j];
            }
        }
        // Compute inv(inMatrix)=inv(LtL) store in lower of inMatrix
        for (i = 0; i < numOfRows; ++i) {
            for (j = i; j < numOfRows; ++j) {
                sum = 0.;
                for (k = j; k < numOfRows; ++k) {
                    sum += inMatrix[k][i] * inMatrix[k][j];
                }
                inMatrix[j][i] = sum;
            }
        }
    }

    public static double[][] invertChol(double[][] inMatrix) {
        double[][] outMatrix = inMatrix.clone();
        invertChol_inplace(outMatrix);
        return outMatrix;
    }

    public static void invertChol_inplace(DoubleMatrix inMatrix) {
        final int numOfRows = inMatrix.rows;
        double sum;
        int i, j, k;
        // Compute inv(L) store in lower of inMatrix
        for (i = 0; i < numOfRows; ++i) {
            inMatrix.put(i, i, 1. / inMatrix.get(i, i));
            for (j = i + 1; j < numOfRows; ++j) {
                sum = 0.;
                for (k = i; k < j; ++k) {
                    sum -= inMatrix.get(j, k) * inMatrix.get(k, i);
                }
                inMatrix.put(j, i, sum / inMatrix.get(j, j));
            }
        }
        // Compute inv(inMatrix)=inv(LtL) store in lower of inMatrix
        for (i = 0; i < numOfRows; ++i) {
            for (j = i; j < numOfRows; ++j) {
                sum = 0.;
                for (k = j; k < numOfRows; ++k) {
                    sum += inMatrix.get(k, i) * inMatrix.get(k, j);
                }
                inMatrix.put(j, i, sum);
            }
        }
    }

    public static DoubleMatrix invertChol(DoubleMatrix inMatrix) {
        DoubleMatrix outMatrix = inMatrix.dup();
        invertChol_inplace(outMatrix);
        return outMatrix;
    }

    public static ComplexDoubleMatrix dotmult(ComplexDoubleMatrix A, ComplexDoubleMatrix B) {
        return A.mul(B);
    }

    public static void dotmult_inplace(ComplexDoubleMatrix A, ComplexDoubleMatrix B) {
        A.muli(B);
    }


    /**
     * B.fliplr()
     * Mirror in center vertical (flip left right).
     */
    public static void fliplr_inplace(DoubleMatrix A) {

        final int nRows = A.rows;
        final int nCols = A.columns;

        if (nRows == 1 || nCols == 1) {
            double tmp;
//            for (int i = 0; i < (nCols / 2); ++i) {
//                tmp = A.get(0, i);
//                A.put(0, i, A.get(0, nRows - i));
//                A.put(0, nRows - 1, tmp);
//            }
            final int length = A.length;
            for (int i = 0; i < (length / 2); i++) {
                tmp = A.data[i];
                A.data[i] = A.data[length - i - 1];
                A.data[length - i - 1] = tmp;
            }
        } else {
            for (int i = 0; i < (nCols / 2); ++i)     // floor
            {
                DoubleMatrix tmp1 = A.getColumn(i);
                DoubleMatrix tmp2 = A.getColumn(nCols - i - 1);
                A.putColumn(i, tmp2);
                A.putColumn(nCols - i - 1, tmp1);
            }
        }
    }

    public static DoubleMatrix matTxmat(DoubleMatrix matrix1, DoubleMatrix matrix2) {
        return matrix1.transpose().mmul(matrix2);
    }

    public static ComplexDoubleMatrix matTxmat(ComplexDoubleMatrix matrix1, ComplexDoubleMatrix matrix2) {
        return matrix1.transpose().mmul(matrix2);
    }

    /**
     * wshift(inVector,n)
     * circular shift of vector inVector by n slots. positive n for
     * right to left shift.
     * implementation: WSHIFT(inVector,n) == WSHIFT(inVector,n-sizeA);
     * inVector is changed itself!
     */
    public static void wshift_inplace(DoubleMatrix inVector, int n) throws IllegalArgumentException {

        if (n >= inVector.length) {
            System.err.println("wshift: shift larger than matrix not implemented.");
            throw new IllegalArgumentException("wshift: shift larger than matrix not implemented.");
        }

        if (!inVector.isVector()) {
            System.err.println("wshift: only vectors supported!");
            throw new IllegalArgumentException("wshift: only vectors supported!");
        }

        // positive only, use rem!  n = n%inVector.nsize;
        if (n == 0) return;
        if (n < 0) n += inVector.length;

        DoubleMatrix Res = new DoubleMatrix(inVector.rows, inVector.columns);

        //  n always >0 here
        System.arraycopy(inVector.data, n, Res.data, 0, inVector.length - n);
        System.arraycopy(inVector.data, 0, Res.data, inVector.length - n, n);

        inVector.copy(Res);

    }

    public static DoubleMatrix wshift(DoubleMatrix inMatrix, int n) {
        DoubleMatrix outMatrix = inMatrix.dup();
        wshift_inplace(outMatrix, n);
        return outMatrix;
    }

    /**
     * setdata(outMatrix, outWin, inMatrix, inWin):
     * set outWin of outMatrix to inWin of inMatrix
     * if outWin==0 defaults to totalB, inWin==0 defaults to totalA
     * first line matrix =0 (?)
     */
    public static void setdata(DoubleMatrix outMatrix, Window outWin, DoubleMatrix inMatrix, Window inWin) {

        if (outWin.linehi == 0 && outWin.pixhi == 0) {
            outWin.linehi = outMatrix.rows - 1;
            outWin.pixhi = outMatrix.columns - 1;
        }
        if (inWin.linehi == 0 && inWin.pixhi == 0) {
            inWin.linehi = inMatrix.rows - 1;
            inWin.pixhi = inMatrix.columns - 1;
        }

        if (((outWin.linehi - outWin.linelo) != (inWin.linehi - inWin.linelo)) ||
                ((outWin.pixhi - outWin.pixlo) != (inWin.pixhi - inWin.pixlo))) {
            logger.error("setdata: wrong input.");
            throw new IllegalArgumentException("setdata: wrong input.");

        }
        if (outWin.linehi < outWin.linelo || outWin.pixhi < outWin.pixlo) {
            logger.error("setdata: wrong input.1");
            throw new IllegalArgumentException("setdata: wrong input.1");
        }

        if ((outWin.linehi > outMatrix.rows - 1) ||
                (outWin.pixhi > outMatrix.columns - 1)) {
            logger.error("setdata: wrong input.2");
            throw new IllegalArgumentException("setdata: wrong input.2");
        }

        if ((inWin.linehi > inMatrix.rows - 1) ||
                (inWin.pixhi > inMatrix.columns - 1)) {
            logger.error("setdata: wrong input.3");
            throw new IllegalArgumentException("setdata: wrong input.3");
        }

        //// Fill data ////
        int sizeLin = (int) inWin.lines();
        for (int i = (int) outWin.pixlo, j = (int) inWin.pixlo; i <= outWin.pixhi; i++, j++) {

            int startOut = (int) (i * outMatrix.rows + outWin.linelo);
            int startIn = (int) (j * inMatrix.rows + inWin.linelo);

            System.arraycopy(inMatrix.data, startIn, outMatrix.data, startOut, sizeLin);

        }
    }

/*
    public static void setdata(double[][] outMatrix, Window outWin, double[][] inMatrix, Window inWin) {

        if (outWin.linehi == 0 && outWin.pixhi == 0) {
            outWin.linehi = outMatrix.length - 1;
            outWin.pixhi = outMatrix[0].length - 1;
        }
        if (inWin.linehi == 0 && inWin.pixhi == 0) {
            inWin.linehi = inMatrix.length - 1;
            inWin.pixhi = inMatrix[0].length - 1;
        }

        if (((outWin.linehi - outWin.linelo) != (inWin.linehi - inWin.linelo)) ||
                ((outWin.pixhi - outWin.pixlo) != (inWin.pixhi - inWin.pixlo))) {
            logger.error("setdata: wrong input.");
            throw new IllegalArgumentException("setdata: wrong input.");

        }
        if (outWin.linehi < outWin.linelo || outWin.pixhi < outWin.pixlo) {
            logger.error("setdata: wrong input.1");
            throw new IllegalArgumentException("setdata: wrong input.1");
        }

        if ((outWin.linehi > outMatrix.length - 1) ||
                (outWin.pixhi > outMatrix[0].length - 1)) {
            logger.error("setdata: wrong input.2");
            throw new IllegalArgumentException("setdata: wrong input.2");
        }

        if ((inWin.linehi > inMatrix.length - 1) ||
                (inWin.pixhi > inMatrix[0].length - 1)) {
            logger.error("setdata: wrong input.3");
            throw new IllegalArgumentException("setdata: wrong input.3");
        }

        //// Fill data ////
        int sizeLin = (int) inWin.pixels();
//        for (int i = (int) outWin.linelo; i <= outWin.linehi; i++) {
        for (int i = 0; i <= outWin.lines(); i++) {


            int startIn = (int) (i * inMatrix.length + inWin.pixlo);
            int startOut = (int) (i * outMatrix.length + outWin.pixlo);

            System.arraycopy(inMatrix[i], startIn, outMatrix[i], startOut, sizeLin);

        }
    }
*/

    public static void setdata(ComplexDoubleMatrix outMatrix, Window outWin, ComplexDoubleMatrix inMatrix, Window inWin) {

        // Check default request
        if (outWin.linehi == 0 && outWin.pixhi == 0) {
            outWin.linehi = outMatrix.rows - 1;
            outWin.pixhi = outMatrix.columns - 1;
        }
        if (inWin.linehi == 0 && inWin.pixhi == 0) {
            inWin.linehi = inMatrix.rows - 1;
            inWin.pixhi = inMatrix.columns - 1;
        }

        if (((outWin.linehi - outWin.linelo) != (inWin.linehi - inWin.linelo)) ||
                ((outWin.pixhi - outWin.pixlo) != (inWin.pixhi - inWin.pixlo))) {
            logger.error("setdata: wrong input.");
            throw new IllegalArgumentException("setdata: wrong input.");

        }
        if (outWin.linehi < outWin.linelo || outWin.pixhi < outWin.pixlo) {
            logger.error("setdata: wrong input.1");
            throw new IllegalArgumentException("setdata: wrong input.1");
        }

        if ((outWin.linehi > outMatrix.rows - 1) ||
                (outWin.pixhi > outMatrix.columns - 1)) {
            logger.error("setdata: wrong input.2");
            throw new IllegalArgumentException("setdata: wrong input.2");
        }

        if ((inWin.linehi > inMatrix.rows - 1) ||
                (inWin.pixhi > inMatrix.columns - 1)) {
            logger.error("setdata: wrong input.3");
            throw new IllegalArgumentException("setdata: wrong input.3");
        }

        //// Fill data ////
        int sizeLin = (int) inWin.lines() * 2;
//        int sizeLin = (int) inWin.lines();
        for (int i = (int) outWin.pixlo, j = (int) inWin.pixlo; i <= outWin.pixhi; i++, j++) {

            int startIn = (int) (j * (2 * inMatrix.rows) + (2 * inWin.linelo));
            int startOut = (int) (i * (2 * outMatrix.rows) + (2 * outWin.linelo));

            System.arraycopy(inMatrix.data, startIn, outMatrix.data, startOut, sizeLin);
        }
    }

    public static void setdata(ComplexDoubleMatrix outMatrix, ComplexDoubleMatrix inMatrix, Window inWin) {
        setdata(outMatrix, new Window(0, outMatrix.rows - 1, 0, outMatrix.columns - 1), inMatrix, inWin);
    }

    public static void setdata(DoubleMatrix outMatrix, DoubleMatrix inMatrix, Window inWin) {
        //setdata(outMatrix, new Window(0, outMatrix.rows, 0, outMatrix.columns), inMatrix, inWin);
        setdata(outMatrix, new Window(0, outMatrix.rows - 1, 0, outMatrix.columns - 1), inMatrix, inWin);
    }


    /**
     * choles(inMatrix);   cholesky factorisation internal implementation
     * lower triangle of inMatrix is changed on output
     * upper reamins un referenced
     * this one is a lot slower then veclib and there may be more
     * efficient implementations.
     */
    public static void chol_inplace(double[][] inMatrix) {
        final int N = inMatrix.length;
        double sum;
        for (int i = 0; i < N; ++i) {
            for (int j = i; j < N; ++j) {
                sum = inMatrix[i][j];
                for (int k = i - 1; k >= 0; --k) {
                    sum -= inMatrix[i][k] * inMatrix[j][k];
                }
                if (i == j) {
                    if (sum <= 0.) {
                        logger.error("choles: internal: inMatrix not pos. def.");
                    }
                    inMatrix[i][i] = Math.sqrt(sum);
                } else {
                    inMatrix[j][i] = sum / inMatrix[i][i];
                }
            }
        }
    }

    public static void chol_inplace(DoubleMatrix inMatrix) {
        final int N = inMatrix.rows;
        double sum;
        for (int i = 0; i < N; ++i) {
            for (int j = i; j < N; ++j) {
                sum = inMatrix.get(i, j);
                for (int k = i - 1; k >= 0; --k) {
                    sum -= inMatrix.get(i, k) * inMatrix.get(j, k);
                }
                if (i == j) {
                    if (sum <= 0.) {
                        logger.error("choles: internal: inMatrix not pos. def.");
                    }
                    inMatrix.put(i, i, Math.sqrt(sum));
                } else {
                    inMatrix.put(j, i, sum / inMatrix.get(i, i));
                }
            }
        }
    }

    // TODO: new invert() _wrapper_ methods, to be documented and unit tested
    public static void invert_inplace(double[][] inMatrix) {
        chol_inplace(inMatrix);
        invertChol_inplace(inMatrix);
        arrangeCholesky_inplace(inMatrix);
    }

    public static double[][] invert(double[][] inMatrix) {
        double[][] outMatrix = inMatrix.clone();
        invert_inplace(outMatrix);
        return outMatrix;
    }

    public static void invert_inplace(DoubleMatrix inMatrix) {
        chol_inplace(inMatrix);
        invertChol_inplace(inMatrix);
        arrangeCholesky_inplace(inMatrix);
    }

    public static DoubleMatrix invert(DoubleMatrix inMatrix) {
        DoubleMatrix outMatrix = inMatrix.dup();
        invert_inplace(outMatrix);
        return outMatrix;
    }


    public static void arrangeCholesky_inplace(DoubleMatrix inMatrix) {
        // assume squared
        for (int i = 0; i < inMatrix.rows; i++) {
            for (int j = 0; j < i; j++) {
                inMatrix.put(j, i, inMatrix.get(i, j));
            }
        }
    }

    // assume squared
    public static void arrangeCholesky_inplace(double[][] inArray) {
        for (int i = 0; i < inArray.length; i++) {
            for (int j = 0; j < i; j++) {
                inArray[j][i] = inArray[i][j];
            }
        }
    }
}
