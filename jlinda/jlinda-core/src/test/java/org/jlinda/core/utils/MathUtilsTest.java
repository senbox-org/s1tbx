package org.jlinda.core.utils;

import org.jblas.DoubleMatrix;
import org.jlinda.core.Window;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class MathUtilsTest {

    static double powerOfTwo_EXPECTED;
    static double notPowerOfTwo_EXPECTED;
    static long oddVal_EXPECTED;
    static long evenVal_EXPECTED;
    static double valInDegrees_EXPECTED;
    static double valInRadians_EXPECTED;
    static double[] increment_1D_EXPECTED;
    static double[][] increment_2D_EXPECTED;

    final static double VALUE = 2;
    final static double EXPONENT = 2;
    final static double DELTA = Math.pow(10, -6);

    /// for RAMP ///
    static int nRows;
    static int nCols;
    static double[] ramp_1D_EXPECTED;
    static DoubleMatrix ramp_2D_EXPECTED;



    /// DISTRIBUTION ///
    final static int[][] distributedPoints_EXPECTED = {
            {0, 0},
            {0, 3921},
            {3707, 2941},
            {7413, 1961},
            {11120, 980},
            {14826, 0},
            {14826, 3921},
            {18533, 2940},
            {22239, 1960},
            {25945, 980},
            {25945, 4900}
    };

    final static int numOfPnts = 11;
    final static Window winForDistribution = new Window(0, 25945, 0, 4900);

    @BeforeClass
    public static void defineExpectedValues() {

        powerOfTwo_EXPECTED = Math.pow(VALUE, EXPONENT);
        notPowerOfTwo_EXPECTED = Math.pow(VALUE, EXPONENT) - 1;
        evenVal_EXPECTED = 8;
        oddVal_EXPECTED = 7;

        valInDegrees_EXPECTED = 45.4545;
        valInRadians_EXPECTED = Math.toRadians(valInDegrees_EXPECTED);

        increment_1D_EXPECTED = new double[]{0, 0.25, 0.5, 0.75, 1.00};
        increment_2D_EXPECTED = new double[][]{{0, 0}, {0.25, 0.25}, {0.5, 0.5}, {0.75, 0.75}, {1.00, 1.00}};
    }


    @Test
    public void testIsOdd() throws Exception {
        Assert.assertTrue(MathUtils.isOdd(oddVal_EXPECTED));
        Assert.assertFalse(MathUtils.isOdd(evenVal_EXPECTED));
    }

    @Test
    public void testIsEven() throws Exception {
        Assert.assertTrue(MathUtils.isEven(evenVal_EXPECTED));
        Assert.assertFalse(MathUtils.isEven(oddVal_EXPECTED));
    }

    @Test
    public void testIsPower2() throws Exception {
        Assert.assertTrue(MathUtils.isPower2((long) powerOfTwo_EXPECTED));
        Assert.assertFalse(MathUtils.isPower2((long) notPowerOfTwo_EXPECTED));
    }

    @Test
    public void testRad2deg() throws Exception {
        Assert.assertEquals(valInDegrees_EXPECTED, MathUtils.rad2deg(valInRadians_EXPECTED), DELTA);
    }

    @Test
    public void testDeg2rad() throws Exception {
        Assert.assertEquals(valInRadians_EXPECTED, MathUtils.deg2rad(valInDegrees_EXPECTED), DELTA);
    }

    @Test
    public void testDistributePoints() throws Exception {
        int[][] distribPnts_ACTUAL = MathUtils.distributePoints(numOfPnts, winForDistribution);
        for (int i = 0; i < distribPnts_ACTUAL.length; i++) {
//            for (int j = 0; j < distribPnts_ACTUAL[0].length; j++) {
//                System.out.println("point["+i+"]["+j+"] = " + distribPnts_ACTUAL[i][j]);
//
//            }
            Assert.assertArrayEquals(distributedPoints_EXPECTED[i], distribPnts_ACTUAL[i]);
        }
    }

    @Test
    public void testIncrement1D() throws Exception {
        double[] increment_1D_ACTUAL = MathUtils.increment(5, 0, 0.25);
        Assert.assertArrayEquals(increment_1D_EXPECTED, increment_1D_ACTUAL, DELTA);
    }

    @Test

    public void testIncrement2D() throws Exception {
        double[][] increment_2D_ACTUAL = MathUtils.increment(5, 2, 0, 0.25);
        for (int i = 0; i < increment_2D_ACTUAL.length; i++) {
            Assert.assertArrayEquals(increment_2D_EXPECTED[i], increment_2D_ACTUAL[i], DELTA);
        }

    }

    @Test
    public void testSqr() throws Exception {
        Assert.assertEquals(Math.pow(VALUE, 2), MathUtils.sqr(VALUE), DELTA);
    }

    @Test
    public void testSqrt() throws Exception {
        Assert.assertEquals(Math.sqrt(VALUE), MathUtils.sqrt(VALUE), DELTA);
    }


    @Test
    public void testLying() throws Exception{
        DoubleMatrix inMatrix = DoubleMatrix.ones(2, 2);
        DoubleMatrix lying_EXPECTED = DoubleMatrix.ones(1, 4);

        Assert.assertEquals(lying_EXPECTED, MathUtils.lying(inMatrix));

        // redefine inputMatrix to test vectors
        inMatrix = DoubleMatrix.ones(4, 1);
        Assert.assertEquals(lying_EXPECTED, MathUtils.lying(inMatrix));

    }



    @Before
    public void setUpTestDataForRamp() {

        nRows = 5;
        nCols = 5;

        ramp_1D_EXPECTED = increment_1D_EXPECTED.clone();
        ramp_2D_EXPECTED = new DoubleMatrix(nRows, nCols);
        for (int i = 0; i < nRows; i++) {
            ramp_2D_EXPECTED.putRow(i, new DoubleMatrix(ramp_1D_EXPECTED));
        }
    }

    @Test
    public void testRamp() throws Exception {

        DoubleMatrix ramp_2D_ACTUAL = MathUtils.ramp(nRows, nCols);
        Assert.assertEquals(ramp_2D_EXPECTED, ramp_2D_ACTUAL);
    }



}
