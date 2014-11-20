package org.jlinda.core.utils;

import org.jblas.ComplexDouble;
import org.jblas.ComplexDoubleMatrix;
import org.jblas.DoubleMatrix;
import org.jlinda.core.Window;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class LinearAlgebraUtilsTest {

    private static DoubleMatrix A_15 = new DoubleMatrix(5, 1);
    private static DoubleMatrix A_15_SHIFT_EXPECTED = new DoubleMatrix(5, 1);

    private static DoubleMatrix A_33 = new DoubleMatrix(3, 3);
    private static DoubleMatrix A_33_FLIP_EXPECTED = new DoubleMatrix(3, 3);
    private static DoubleMatrix ATA_33_EXPECTED = new DoubleMatrix(3, 3);

    private static DoubleMatrix A_PASCAL_22 = new DoubleMatrix(2, 2);
    private static DoubleMatrix A_PASCAL_33 = new DoubleMatrix(3, 3);
    private static ComplexDoubleMatrix A_PASCAL_22_CPLX;
    private static double[][] A_PASCAL_33_SQUARED = new double[3][3];

    private static final double[] X_22 = new double[]{3, 1};
    private static final double[] X_33 = new double[]{3, 1, 4};

    private static final double[] SOL_22_EXPECTED = new double[]{5, -2};
    private static final double[] SOL_33_EXPECTED = new double[]{10, -12, 5};
    private static final double[] SOL_33_EXPECTED_ABS = new double[]{10, 12, 5};

    private static final double DELTA = 1e-06;
    private static DoubleMatrix A_PASCAL_33_CHOL_EXPECTED = new DoubleMatrix(3, 3);

    private static DoubleMatrix A_PASCAL_33_CHOL_INV_EXPECTED = new DoubleMatrix(3, 3);
    private static ComplexDoubleMatrix A_PASCAL_22_CPLX_times2_EXPECTED = new ComplexDoubleMatrix(2, 2);

    @BeforeClass
    public static void setUpTestMatrices() throws Exception {


        A_PASCAL_22 = DoubleMatrix.ones(2, 2);
        A_PASCAL_22.put(1, 1, 2);

        A_PASCAL_33 = DoubleMatrix.ones(3, 3);
        A_PASCAL_33.put(1, 1, 2);
        A_PASCAL_33.put(1, 2, 3);
        A_PASCAL_33.put(2, 1, 3);
        A_PASCAL_33.put(2, 2, 6);

        A_PASCAL_33_SQUARED = new double[][]{{1, 1, 1}, {1, 8, 27}, {1, 27, 216}};

        A_PASCAL_33_CHOL_EXPECTED = A_PASCAL_33.dup();
        // define lower triangular block
        A_PASCAL_33_CHOL_EXPECTED.put(1, 0, 1);
        A_PASCAL_33_CHOL_EXPECTED.put(1, 1, 1);
        A_PASCAL_33_CHOL_EXPECTED.put(2, 0, 1);
        A_PASCAL_33_CHOL_EXPECTED.put(2, 1, 2);
        A_PASCAL_33_CHOL_EXPECTED.put(2, 2, 1);

        // define inverted matrix
        A_PASCAL_33_CHOL_INV_EXPECTED.put(0, 0, 3);
        A_PASCAL_33_CHOL_INV_EXPECTED.put(0, 1, -3);
        A_PASCAL_33_CHOL_INV_EXPECTED.put(0, 2, 1);
        A_PASCAL_33_CHOL_INV_EXPECTED.put(1, 0, -3);
        A_PASCAL_33_CHOL_INV_EXPECTED.put(1, 1, 5);
        A_PASCAL_33_CHOL_INV_EXPECTED.put(1, 2, -2);
        A_PASCAL_33_CHOL_INV_EXPECTED.put(2, 0, 1);
        A_PASCAL_33_CHOL_INV_EXPECTED.put(2, 1, -2);
        A_PASCAL_33_CHOL_INV_EXPECTED.put(2, 2, 1);

        // define complex PASCAL_22 matrix
        A_PASCAL_22_CPLX = new ComplexDoubleMatrix(A_PASCAL_22, A_PASCAL_22);

//        A_PASCAL_22_CPLX_times2_EXPECTED.put(0, 0, new ComplexDouble(0, 4));
//        A_PASCAL_22_CPLX_times2_EXPECTED.put(0, 1, new ComplexDouble(0, 6));
//        A_PASCAL_22_CPLX_times2_EXPECTED.put(1, 0, new ComplexDouble(0, 6));
//        A_PASCAL_22_CPLX_times2_EXPECTED.put(1, 1, new ComplexDouble(0, 10));
        A_PASCAL_22_CPLX_times2_EXPECTED.put(0, 0, new ComplexDouble(0, 2));
        A_PASCAL_22_CPLX_times2_EXPECTED.put(0, 1, new ComplexDouble(0, 2));
        A_PASCAL_22_CPLX_times2_EXPECTED.put(1, 0, new ComplexDouble(0, 2));
        A_PASCAL_22_CPLX_times2_EXPECTED.put(1, 1, new ComplexDouble(0, 8));

        // A_33
        A_33.put(0, 0, 1);
        A_33.put(0, 1, 2);
        A_33.put(0, 2, 3);
        A_33.put(1, 0, 4);
        A_33.put(1, 1, 5);
        A_33.put(1, 2, 6);
        A_33.put(2, 0, 7);
        A_33.put(2, 1, 8);
        A_33.put(2, 2, 9);

        // A_33
        A_33.put(0, 0, 1);
        A_33.put(0, 1, 2);
        A_33.put(0, 2, 3);
        A_33.put(1, 0, 4);
        A_33.put(1, 1, 5);
        A_33.put(1, 2, 6);
        A_33.put(2, 0, 7);
        A_33.put(2, 1, 8);
        A_33.put(2, 2, 9);

        // ATA_33
        ATA_33_EXPECTED.put(0, 0, 66);
        ATA_33_EXPECTED.put(0, 1, 78);
        ATA_33_EXPECTED.put(0, 2, 90);
        ATA_33_EXPECTED.put(1, 0, 78);
        ATA_33_EXPECTED.put(1, 1, 93);
        ATA_33_EXPECTED.put(1, 2, 108);
        ATA_33_EXPECTED.put(2, 0, 90);
        ATA_33_EXPECTED.put(2, 1, 108);
        ATA_33_EXPECTED.put(2, 2, 126);

        // A_33_FLIPED_EXPECTED
        A_33_FLIP_EXPECTED.putColumn(0, A_33.getColumn(2));
        A_33_FLIP_EXPECTED.putColumn(1, A_33.getColumn(1));
        A_33_FLIP_EXPECTED.putColumn(2, A_33.getColumn(0));

        // A_15_EXPECTED
        A_15 = new DoubleMatrix(MathUtils.increment(5, 0, 1));

        // A_15_SHIFTED_EXPECTED
        A_15_SHIFT_EXPECTED = new DoubleMatrix(new double[]{3, 4, 0, 1, 2});

    }

    //// SOLVERS ////
    @Test
    public void testSolve22() throws Exception {
        double[] SOL_22_ACTUAL = LinearAlgebraUtils.solve22(A_PASCAL_22.toArray2(), X_22);
        Assert.assertArrayEquals(SOL_22_EXPECTED, SOL_22_ACTUAL, DELTA);
    }

    @Test
    public void testSolve33() throws Exception {
        double[] SOL_33_ACTUAL = LinearAlgebraUtils.solve33(A_PASCAL_33.toArray2(), X_33);
        Assert.assertArrayEquals(SOL_33_EXPECTED, SOL_33_ACTUAL, DELTA);

    }

    //// MATRIX ARITHMETIC ////
    @Test
    public void testAbsMatrix_JBLAS() throws Exception {
        DoubleMatrix SOL_33_ACTUAL_ABS = LinearAlgebraUtils.absMatrix(new DoubleMatrix(SOL_33_EXPECTED));
        Assert.assertEquals(new DoubleMatrix(SOL_33_EXPECTED_ABS), SOL_33_ACTUAL_ABS);
    }

    @Test
    public void testAbsMatrix_Arrays() throws Exception {
        double[][] SOL_33_EXPECTED_TEMP = new double[][]{SOL_33_EXPECTED, SOL_33_EXPECTED};
        double[][] SOL_33_EXPECTED_ABS_TEMP = new double[][]{SOL_33_EXPECTED_ABS, SOL_33_EXPECTED_ABS};

        double[][] SOL_33_ACTUAL_ABS = LinearAlgebraUtils.absMatrix(SOL_33_EXPECTED_TEMP);
        Assert.assertArrayEquals(SOL_33_EXPECTED_ABS_TEMP, SOL_33_ACTUAL_ABS);
    }

    @Test
    public void testMatrixPower_JBLAS() throws Exception {
        Assert.assertEquals(new DoubleMatrix(A_PASCAL_33_SQUARED),
                LinearAlgebraUtils.matrixPower(A_PASCAL_33, 3));
    }

    @Test
    public void testMatrixPower_Arrays() throws Exception {
        Assert.assertEquals(A_PASCAL_33_SQUARED,
                LinearAlgebraUtils.matrixPower(A_PASCAL_33.toArray2(), 3));
    }


    @Test
    public void testDotmult_JBLAS() throws Exception {
        ComplexDoubleMatrix A_PASCAL_22_CPLX_times2_ACTUAL = LinearAlgebraUtils.dotmult(A_PASCAL_22_CPLX, A_PASCAL_22_CPLX);
        Assert.assertEquals(A_PASCAL_22_CPLX_times2_EXPECTED, A_PASCAL_22_CPLX_times2_ACTUAL);
    }

    @Test
    public void testDotmult_inplace_JBLAS() throws Exception {
        ComplexDoubleMatrix A_PASCAL_22_CPLX_times2_ACTUAL = A_PASCAL_22_CPLX.dup();

        LinearAlgebraUtils.dotmult_inplace(A_PASCAL_22_CPLX_times2_ACTUAL, A_PASCAL_22_CPLX);
        Assert.assertEquals(A_PASCAL_22_CPLX_times2_EXPECTED, A_PASCAL_22_CPLX_times2_ACTUAL);
    }

    @Test
    public void testMatTxmatDouble_JBLAS() throws Exception {
        DoubleMatrix ATA_33_ACTUAL = LinearAlgebraUtils.matTxmat(A_33, A_33);
        Assert.assertEquals(ATA_33_EXPECTED, ATA_33_ACTUAL);
    }

    @Test
    public void testMatTxmatComplex_JBLASS() throws Exception {
        ComplexDoubleMatrix ATA_33_CPLX_ACTUAL =
                LinearAlgebraUtils.matTxmat(new ComplexDoubleMatrix(A_33, A_33), new ComplexDoubleMatrix(A_33, A_33));
        ComplexDoubleMatrix ATA_33_CPLX_EXPECTED =
                new ComplexDoubleMatrix(DoubleMatrix.zeros(A_33.rows, A_33.columns), ATA_33_EXPECTED.mmul(2));

        Assert.assertEquals(ATA_33_CPLX_EXPECTED, ATA_33_CPLX_ACTUAL);

    }

    @Test
    public void testFliplr() throws Exception {
        DoubleMatrix A_33_FLIP_ACTUAL = A_33.dup();
        LinearAlgebraUtils.fliplr_inplace(A_33_FLIP_ACTUAL);
        Assert.assertEquals(A_33_FLIP_EXPECTED, A_33_FLIP_ACTUAL);
    }

    @Test
    public void testWshift_inplace_JBLAS() throws Exception {
        DoubleMatrix A_15_SHIFT_ACTUAL = A_15.dup();
        LinearAlgebraUtils.wshift_inplace(A_15_SHIFT_ACTUAL, 3);
        Assert.assertEquals(A_15_SHIFT_ACTUAL, A_15_SHIFT_EXPECTED);
    }

    @Test
    public void testWshift_JBLAS() throws Exception {
        DoubleMatrix A_15_SHIFT_ACTUAL = LinearAlgebraUtils.wshift(A_15, 3);
        Assert.assertEquals(A_15_SHIFT_ACTUAL, A_15_SHIFT_EXPECTED);
    }

    //// DECOMPOSITION AND INVERSION : internal implementation ////

    // arrays[][]
    @Test
    public void testCholesky_inplace_Arrays() throws Exception {
        double[][] A_PASCAL_33_CHOL_ARRAY_ACTUAL = A_PASCAL_33.toArray2();
        LinearAlgebraUtils.chol_inplace(A_PASCAL_33_CHOL_ARRAY_ACTUAL);
        Assert.assertEquals(A_PASCAL_33_CHOL_EXPECTED.toArray2(), A_PASCAL_33_CHOL_ARRAY_ACTUAL);
    }

    @Test
    public void testCholesky_DecInvArrng_Methods_inplace_Arrays() throws Exception {

        double[][] A_PASCAL_33_CHOL_ARRAY_ACTUAL = A_PASCAL_33.toArray2();
        LinearAlgebraUtils.chol_inplace(A_PASCAL_33_CHOL_ARRAY_ACTUAL);
        LinearAlgebraUtils.invertChol_inplace(A_PASCAL_33_CHOL_ARRAY_ACTUAL);
        LinearAlgebraUtils.arrangeCholesky_inplace(A_PASCAL_33_CHOL_ARRAY_ACTUAL);

        Assert.assertEquals(A_PASCAL_33_CHOL_INV_EXPECTED.toArray2(), A_PASCAL_33_CHOL_ARRAY_ACTUAL);

    }

    @Test
    public void testCholeskyInvert_inplace_Arrays() throws Exception {

        double[][] A_PASCAL_33_CHOL_ARRAY_ACTUAL = A_PASCAL_33.toArray2();
        LinearAlgebraUtils.invert(A_PASCAL_33_CHOL_ARRAY_ACTUAL);

        Assert.assertEquals(A_PASCAL_33_CHOL_INV_EXPECTED.toArray2(), A_PASCAL_33_CHOL_ARRAY_ACTUAL);

    }

    @Test
    public void testCholeskyInvert_Arrays() throws Exception {
        double[][] A_PASCAL_33_CHOL_INV_ACTUAL = LinearAlgebraUtils.invert(A_PASCAL_33.toArray2());
        Assert.assertEquals(A_PASCAL_33_CHOL_INV_EXPECTED.toArray2(), A_PASCAL_33_CHOL_INV_ACTUAL);
    }

    // JBLAS DoubleMatrix()
    @Test
    public void testCholesky_inplace_JBLAS() throws Exception {

        DoubleMatrix A_PASCAL_33_CHOL_MATRIX_ACTUAL = A_PASCAL_33.dup();
        LinearAlgebraUtils.chol_inplace(A_PASCAL_33_CHOL_MATRIX_ACTUAL);
        Assert.assertEquals(A_PASCAL_33_CHOL_EXPECTED, A_PASCAL_33_CHOL_MATRIX_ACTUAL);

    }

    @Test
    public void testCholesky_DecInvArrng_Mathods_inplace_JBLAS() throws Exception {

        DoubleMatrix A_PASCAL_33_CHOL_MATRIX_ACTUAL = A_PASCAL_33.dup();
        LinearAlgebraUtils.chol_inplace(A_PASCAL_33_CHOL_MATRIX_ACTUAL);
        LinearAlgebraUtils.invertChol_inplace(A_PASCAL_33_CHOL_MATRIX_ACTUAL);
        LinearAlgebraUtils.arrangeCholesky_inplace(A_PASCAL_33_CHOL_MATRIX_ACTUAL);
        Assert.assertEquals(A_PASCAL_33_CHOL_INV_EXPECTED, A_PASCAL_33_CHOL_MATRIX_ACTUAL);

    }


    @Test
    public void testCholeskyInvert_inplace_JBLAS() throws Exception {

        DoubleMatrix A_PASCAL_33_CHOL_MATRIX_ACTUAL = A_PASCAL_33.dup();
        LinearAlgebraUtils.invert_inplace(A_PASCAL_33_CHOL_MATRIX_ACTUAL);
        Assert.assertEquals(A_PASCAL_33_CHOL_INV_EXPECTED, A_PASCAL_33_CHOL_MATRIX_ACTUAL);

    }

    @Test
    public void testCholeskyInvert_JBLAS() throws Exception {
        DoubleMatrix A_PASCAL_33_CHOL_INV_ACTUAL = LinearAlgebraUtils.invert(A_PASCAL_33);
        Assert.assertEquals(A_PASCAL_33_CHOL_INV_EXPECTED, A_PASCAL_33_CHOL_INV_ACTUAL);
    }


    //// DATA FILLING ////
    @Test
    public void testSetdataDouble_JBLAS() throws Exception {
        DoubleMatrix inMatrix = A_PASCAL_33;
        Window inWin = new Window(0, 1, 0, 1);

        DoubleMatrix outMatrix = new DoubleMatrix(inMatrix.rows - 1, inMatrix.columns - 1);
        Window outWin = new Window(0, 1, 0, 1);
        LinearAlgebraUtils.setdata(outMatrix, outWin, inMatrix, inWin);

        Assert.assertEquals(A_PASCAL_22, outMatrix);

    }

    @Test
    public void testSetdataCmplx_JBLAS() throws Exception {
        ComplexDoubleMatrix inMatrix = new ComplexDoubleMatrix(A_PASCAL_33, A_PASCAL_33);
        Window inWin = new Window(0, 1, 0, 1);

        ComplexDoubleMatrix outMatrix = new ComplexDoubleMatrix(inMatrix.rows - 1, inMatrix.columns - 1);
        Window outWin = new Window(0, 1, 0, 1);
        LinearAlgebraUtils.setdata(outMatrix, outWin, inMatrix, inWin);

        Assert.assertEquals(new ComplexDoubleMatrix(A_PASCAL_22, A_PASCAL_22), outMatrix);

    }

    @Test
    public void testSetdataDouble_fill_JBLAS() throws Exception {
        DoubleMatrix inMatrix = A_PASCAL_33;
        Window inWin = new Window(0, 1, 0, 1);

        DoubleMatrix outMatrix = new DoubleMatrix(inMatrix.rows - 1, inMatrix.columns - 1);
        LinearAlgebraUtils.setdata(outMatrix, inMatrix, inWin);

        Assert.assertEquals(A_PASCAL_22, outMatrix);

    }


    @Test
    public void testSetdataFor() throws Exception {

        double[][] doubles = {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}};

        DoubleMatrix inMatrix = new DoubleMatrix(doubles);
        DoubleMatrix outMatrix_ACTUAL = new DoubleMatrix(2*inMatrix.rows, inMatrix.columns);
        DoubleMatrix outMatrix_EXPECTED = new DoubleMatrix(2*inMatrix.rows, inMatrix.columns);
        outMatrix_EXPECTED.put(0, 1, 5);
        outMatrix_EXPECTED.put(1, 1, 8);

        LinearAlgebraUtils.setdata(outMatrix_ACTUAL, new Window(0, 1, 1, 1), inMatrix, new Window(1, 2, 1, 1));

        Assert.assertEquals(outMatrix_ACTUAL, outMatrix_EXPECTED);

//        System.out.println("inMatrix = " + inMatrix.toString());
//        System.out.println("outMatrix_ACTUAL = " + outMatrix_ACTUAL.toString());
//
//        System.out.println("inMatrix = " + Arrays.toString(inMatrix.data));
//        System.out.println("outMatrix_ACTUAL = " + Arrays.toString(outMatrix_ACTUAL.data));

    }

}
