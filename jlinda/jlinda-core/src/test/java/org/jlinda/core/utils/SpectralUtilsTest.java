package org.jlinda.core.utils;

import org.jblas.ComplexDoubleMatrix;
import org.jblas.DoubleMatrix;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class SpectralUtilsTest {

    private static ComplexDoubleMatrix complexMatrix_EXPECTED;
    private static ComplexDoubleMatrix complexMatrix_EXPECTED_2;

//    private static ComplexDoubleMatrix fftVector_EXPECTED;

    private static ComplexDoubleMatrix fftMatrix_dim1_EXPECTED;
    private static ComplexDoubleMatrix fftMatrix_dim2_EXPECTED;
    private static ComplexDoubleMatrix fftMatrix_2D_EXPECTED;
    private static ComplexDoubleMatrix fftMatrix_2D_EXPECTED_2;

    private static double[] vector_EXPECTED;
    private static double[] shiftVector_EXPECTED;


    @BeforeClass
    public static void setUpTestData() {

        DoubleMatrix realMatrix_EXPECTED = new DoubleMatrix(new double[][]{{2, 4, 8, 16}, {4, 4, 8, 16}, {8, 8, 8, 16}, {16, 16, 16, 16}});
        complexMatrix_EXPECTED = new ComplexDoubleMatrix(realMatrix_EXPECTED, realMatrix_EXPECTED);

        DoubleMatrix realMatrix_EXPECTED_2 = new DoubleMatrix(new double[][]{{2, 4}, {4, 4}, {8, 8}, {16, 16}});
        complexMatrix_EXPECTED_2 = new ComplexDoubleMatrix(realMatrix_EXPECTED_2, realMatrix_EXPECTED_2);

        fftMatrix_dim1_EXPECTED = new ComplexDoubleMatrix(
                new DoubleMatrix(new double[][]{{30, 32, 40, 64}, {-18, -16, -8, 0}, {-10, -8, -8, 0}, {6, 8, 8, 0}}),
                new DoubleMatrix(new double[][]{{30, 32, 40, 64}, {6, 8, 8, 0}, {-10, -8, -8, 0}, {-18, -16, -8, 0}}));

        fftMatrix_dim2_EXPECTED = new ComplexDoubleMatrix(
                new DoubleMatrix(new double[][]{{30, -18, -10, 6}, {32, -16, -8, 8}, {40, -8, -8, 8}, {64, 0, 0, 0}}),
                new DoubleMatrix(new double[][]{{30, 6, -10, -18}, {32, 8, -8, -16}, {40, 8, -8, -8}, {64, 0, 0, 0}}));

        fftMatrix_2D_EXPECTED = new ComplexDoubleMatrix(
                new DoubleMatrix(new double[][]{{166, -42, -26, 22}, {-42, -2, -10, -18}, {-26, -10, -10, 6}, {22, -18, 6, 14}}),
                new DoubleMatrix(new double[][]{{166, 22, -26, -42}, {22, 14, 6, -18}, {-26, 6, -10, -10}, {-42, -18, -10, -2}}));

        fftMatrix_2D_EXPECTED_2 = new ComplexDoubleMatrix(
                new DoubleMatrix(new double[][]{{62, -2}, {-34, -2}, {-18, -2}, {14, -2}}),
                new DoubleMatrix(new double[][]{{62, -2}, {14, -2}, {-18, -2}, {-34, -2}}));

        vector_EXPECTED = new double[]{0, 1, 2, 3, 4};
        shiftVector_EXPECTED = new double[]{3, 4, 0, 1, 2};

//        DoubleMatrix real = new DoubleMatrix(new double[]{30, -18, -10, 6});
//        DoubleMatrix complex = new DoubleMatrix(new double[]{30, 6, -10, -18});
//        fftVector_EXPECTED = new ComplexDoubleMatrix(real, complex);

    }

    @Test
    public void testFourier1D_inplace() throws Exception {

        ComplexDoubleMatrix tempVectorMatrix = complexMatrix_EXPECTED.getColumn(0);
        SpectralUtils.fft1D_inplace(tempVectorMatrix, tempVectorMatrix.length);

        Assert.assertEquals(fftMatrix_dim1_EXPECTED.getColumn(0), tempVectorMatrix);

       SpectralUtils.invfft1D_inplace(tempVectorMatrix, tempVectorMatrix.length);
        Assert.assertEquals(complexMatrix_EXPECTED.getColumn(0), tempVectorMatrix);

    }

    @Test
    public void testFourier1D() throws Exception {

        ComplexDoubleMatrix tempVectorMatrix = complexMatrix_EXPECTED.getColumn(0);
        ComplexDoubleMatrix fftVector_EXPECTED = SpectralUtils.fft1D(tempVectorMatrix, tempVectorMatrix.length);

        Assert.assertEquals(fftMatrix_dim1_EXPECTED.getColumn(0), fftVector_EXPECTED);

        ComplexDoubleMatrix complexVector_EXPECTED = SpectralUtils.invfft1D(fftVector_EXPECTED, tempVectorMatrix.length);
        Assert.assertEquals(complexMatrix_EXPECTED.getColumn(0), complexVector_EXPECTED);

    }

    @Test
    public void testFft_dim1_inplace() throws Exception {

        ComplexDoubleMatrix tempMatrix = complexMatrix_EXPECTED;
        SpectralUtils.fft_inplace(tempMatrix, 1);

        Assert.assertEquals(fftMatrix_dim1_EXPECTED, tempMatrix);

        SpectralUtils.invfft_inplace(tempMatrix, 1);
        Assert.assertEquals(complexMatrix_EXPECTED, tempMatrix);

    }

    @Test
    public void testFft_dim1() throws Exception {

        ComplexDoubleMatrix fftMatrix_dim1_ACTUAL = SpectralUtils.fft(complexMatrix_EXPECTED, 1);
        Assert.assertEquals(fftMatrix_dim1_EXPECTED, fftMatrix_dim1_ACTUAL);

        ComplexDoubleMatrix ifftMatrix_dim_ACTUAL = SpectralUtils.invfft(fftMatrix_dim1_ACTUAL, 1);
        Assert.assertEquals(complexMatrix_EXPECTED, ifftMatrix_dim_ACTUAL);

    }

    @Test
    public void testFft_dim2_inplace() throws Exception {

        ComplexDoubleMatrix tempMatrix = complexMatrix_EXPECTED;
        SpectralUtils.fft_inplace(tempMatrix, 2);

        Assert.assertEquals(fftMatrix_dim2_EXPECTED, tempMatrix);

        SpectralUtils.invfft_inplace(tempMatrix, 2);
        Assert.assertEquals(complexMatrix_EXPECTED, tempMatrix);

    }

    @Test
    public void testFft_dim2() throws Exception {

        ComplexDoubleMatrix fftMatrix_dim2_ACTUAL = SpectralUtils.fft(complexMatrix_EXPECTED.getRow(0), 2);
        Assert.assertEquals(fftMatrix_dim2_EXPECTED.getRow(0), fftMatrix_dim2_ACTUAL);

        ComplexDoubleMatrix ifftMatrix_dim_ACTUAL = SpectralUtils.invfft(fftMatrix_dim2_ACTUAL, 2);
        Assert.assertEquals(complexMatrix_EXPECTED.getRow(0), ifftMatrix_dim_ACTUAL);

    }

    @Test
    public void testFftshiftDouble() throws Exception {

        final DoubleMatrix matrix_EXPECTED = new DoubleMatrix(vector_EXPECTED);
        final DoubleMatrix matrixShift_EXPECTED = new DoubleMatrix(shiftVector_EXPECTED);

        DoubleMatrix matrixShift_ACTUAL = SpectralUtils.fftshift(matrix_EXPECTED);
        Assert.assertEquals(matrixShift_EXPECTED,matrixShift_ACTUAL);

    }

    @Test
    public void testFftshiftComplex() throws Exception {

        ComplexDoubleMatrix matrixCplx_EXPECTED = new ComplexDoubleMatrix(new DoubleMatrix(vector_EXPECTED),
                new DoubleMatrix(vector_EXPECTED));

        ComplexDoubleMatrix shiftMatrixCplx_EXPECTED = new ComplexDoubleMatrix(new DoubleMatrix(shiftVector_EXPECTED),
                new DoubleMatrix(shiftVector_EXPECTED));

        ComplexDoubleMatrix matrixShiftCplx_ACTUAL = SpectralUtils.fftshift(matrixCplx_EXPECTED);

        Assert.assertEquals(shiftMatrixCplx_EXPECTED, matrixShiftCplx_ACTUAL);

    }

    @Test
    public void testFftshiftDouble_inplace() throws Exception {

        double[] vectorTemp_EXPECTED = vector_EXPECTED.clone();

        DoubleMatrix shiftMatrixDouble_EXPECTED = new DoubleMatrix(shiftVector_EXPECTED);
        DoubleMatrix shiftMatrixDouble_ACTUAL = new DoubleMatrix(vectorTemp_EXPECTED);

        SpectralUtils.fftshift_inplace(shiftMatrixDouble_ACTUAL);

        Assert.assertEquals(shiftMatrixDouble_EXPECTED, shiftMatrixDouble_ACTUAL);

    }

    @Test
    public void testFftshiftComplex_inplace() throws Exception {

        double[] vectorTemp_EXPECTED = vector_EXPECTED.clone();

        ComplexDoubleMatrix shiftMatrixCplx_EXPECTED = new ComplexDoubleMatrix(new DoubleMatrix(shiftVector_EXPECTED),
                new DoubleMatrix(shiftVector_EXPECTED));

        ComplexDoubleMatrix shiftMatrixCplx_ACTUAL = new ComplexDoubleMatrix(new DoubleMatrix(vectorTemp_EXPECTED),
                new DoubleMatrix(vectorTemp_EXPECTED));

        SpectralUtils.fftshift_inplace(shiftMatrixCplx_ACTUAL);
        Assert.assertEquals(shiftMatrixCplx_EXPECTED, shiftMatrixCplx_ACTUAL);

    }

    @Test
    public void testIfftshiftDouble() throws Exception {

        final DoubleMatrix matrix_EXPECTED = new DoubleMatrix(vector_EXPECTED);
        final DoubleMatrix matrixShift_EXPECTED = new DoubleMatrix(shiftVector_EXPECTED);

        DoubleMatrix matrix_ACTUAL = SpectralUtils.ifftshift(matrixShift_EXPECTED);
        Assert.assertEquals(matrix_EXPECTED,matrix_ACTUAL);

    }

    @Test
    public void testIfftshiftComplex() throws Exception {

        ComplexDoubleMatrix matrixCplx_EXPECTED = new ComplexDoubleMatrix(new DoubleMatrix(vector_EXPECTED),
                new DoubleMatrix(vector_EXPECTED));

        ComplexDoubleMatrix shiftMatrixCplx_EXPECTED = new ComplexDoubleMatrix(new DoubleMatrix(shiftVector_EXPECTED),
                new DoubleMatrix(shiftVector_EXPECTED));

        ComplexDoubleMatrix matrixCplx_ACTUAL = SpectralUtils.ifftshift(shiftMatrixCplx_EXPECTED);

        Assert.assertEquals(matrixCplx_EXPECTED, matrixCplx_ACTUAL);

    }

    @Test
    public void testIfftshiftDouble_inplace() throws Exception {

        double[] shiftVectorTemp_EXPECTED = shiftVector_EXPECTED.clone();
        DoubleMatrix matrixDouble_EXPECTED = new DoubleMatrix(vector_EXPECTED);

        DoubleMatrix matrixDouble_ACTUAL = new DoubleMatrix(shiftVectorTemp_EXPECTED);

        SpectralUtils.ifftshift_inplace(matrixDouble_ACTUAL);

        Assert.assertEquals(matrixDouble_EXPECTED, matrixDouble_ACTUAL);

    }

    @Test
    public void testIfftshiftComplex_inplace() throws Exception {

        double[] shiftVectorTemp_EXPECTED = shiftVector_EXPECTED.clone();

        ComplexDoubleMatrix matrixCplx_ACTUAL = new ComplexDoubleMatrix(new DoubleMatrix(shiftVectorTemp_EXPECTED),
                new DoubleMatrix(shiftVectorTemp_EXPECTED));

        ComplexDoubleMatrix matrixCplx_EXPECTED = new ComplexDoubleMatrix(new DoubleMatrix(vector_EXPECTED),
                new DoubleMatrix(vector_EXPECTED));

        SpectralUtils.ifftshift_inplace(matrixCplx_ACTUAL);

        Assert.assertEquals(matrixCplx_EXPECTED, matrixCplx_ACTUAL);

    }


    @Test
    public void testFft2D() throws Exception {
        ComplexDoubleMatrix fftMatrix_2D_ACTUAL = SpectralUtils.fft2D(complexMatrix_EXPECTED);
        Assert.assertEquals(fftMatrix_2D_EXPECTED, fftMatrix_2D_ACTUAL);

        // Not Square matrix! row vs column order!
        ComplexDoubleMatrix fftMatrix_2D_ACTUAL_2 = SpectralUtils.fft2D(complexMatrix_EXPECTED_2);
        Assert.assertEquals(fftMatrix_2D_EXPECTED_2, fftMatrix_2D_ACTUAL_2);
    }

    @Test
    public void testFft2D_inplace() throws Exception {
        ComplexDoubleMatrix fftMatrix_2D_ACTUAL = complexMatrix_EXPECTED.dup();
        SpectralUtils.fft2D_inplace(fftMatrix_2D_ACTUAL);
        Assert.assertEquals(fftMatrix_2D_EXPECTED, fftMatrix_2D_ACTUAL);

        // Not Square matrix!
        ComplexDoubleMatrix fftMatrix_2D_ACTUAL_2 = complexMatrix_EXPECTED_2.dup();
        SpectralUtils.fft2D_inplace(fftMatrix_2D_ACTUAL_2);
        Assert.assertEquals(fftMatrix_2D_EXPECTED_2, fftMatrix_2D_ACTUAL_2);

    }

    @Test
    public void testInvfft2D() throws Exception {
        ComplexDoubleMatrix complexMatrix_ACTUAL = SpectralUtils.invfft2d(fftMatrix_2D_EXPECTED);
        Assert.assertEquals(complexMatrix_EXPECTED, complexMatrix_ACTUAL);

        // Not Square matrix!
        ComplexDoubleMatrix complexMatrix_ACTUAL_2 = SpectralUtils.invfft2d(fftMatrix_2D_EXPECTED_2);
        Assert.assertEquals(complexMatrix_EXPECTED_2, complexMatrix_ACTUAL_2);

    }

    @Test
    public void testInvfft2D_inplace() throws Exception {

        ComplexDoubleMatrix complexMatrix_ACTUAL = fftMatrix_2D_EXPECTED.dup();
        SpectralUtils.invfft2D_inplace(complexMatrix_ACTUAL);
        Assert.assertEquals(complexMatrix_EXPECTED, complexMatrix_ACTUAL);

        // Not Square matrix!
        ComplexDoubleMatrix complexMatrix_ACTUAL_2 = fftMatrix_2D_EXPECTED_2.dup();
        SpectralUtils.invfft2D_inplace(complexMatrix_ACTUAL_2);
        Assert.assertEquals(complexMatrix_EXPECTED_2, complexMatrix_ACTUAL_2);

    }

}