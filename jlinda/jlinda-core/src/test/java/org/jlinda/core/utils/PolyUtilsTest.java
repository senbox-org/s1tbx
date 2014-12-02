package org.jlinda.core.utils;

import org.jblas.DoubleMatrix;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;

public class PolyUtilsTest {

    private static final double DELTA_06 = 1e-06;
    private static final double DELTA_03 = 1e-03;
    private static final double DELTA_02 = 1e-02;
    private static final double DELTA_01 = 1e-01;

    // fractional error in math formula less than 1.2 * 10 ^ -7.
    // although subject to catastrophic cancellation when z in very close to 0 ok for testing
    // from Chebyshev fitting formula for erf(z) from Numerical Recipes, 6.2
    private static double erf(double z) {
        double t = 1.0 / (1.0 + 0.5 * Math.abs(z));

        // use Horner's method
        double ans = 1 - t * Math.exp(-z * z - 1.26551223 +
                t * (1.00002368 +
                        t * (0.37409196 +
                                t * (0.09678418 +
                                        t * (-0.18628806 +
                                                t * (0.27886807 +
                                                        t * (-1.13520398 +
                                                                t * (1.48851587 +
                                                                        t * (-0.82215223 +
                                                                                t * (0.17087277))))))))));
        if (z >= 0) return ans;
        else return -ans;
    }


    @Test
    public void testNormalize2() throws Exception {
        double normValue1_EXPECTED = -2;
        double normValue2_EXPECTED = -1.636363636363;
        int min_int = 1;
        int max_int = 100;
        double min_double = 1.0;
        double max_double = 100.;
        Assert.assertEquals(normValue1_EXPECTED, PolyUtils.normalize2(1, min_int, max_int), DELTA_06);
        Assert.assertEquals(normValue1_EXPECTED, PolyUtils.normalize2(1, min_double, max_double), DELTA_06);
        Assert.assertEquals(normValue2_EXPECTED, PolyUtils.normalize2(10, min_int, max_int), DELTA_06);
        Assert.assertEquals(normValue2_EXPECTED, PolyUtils.normalize2(10, min_double, max_double), DELTA_06);
    }

    @Test
    public void testNormalize() throws Exception {
        double[] array_EXPECTED = {1, 2};
        double[] normArray_EXPECTED = {-0.1, 0};

        Assert.assertEquals(new DoubleMatrix(normArray_EXPECTED), PolyUtils.normalize(new DoubleMatrix(array_EXPECTED)));
    }

    @Test
    public void testDegreeFromCoefficients() throws Exception {
        final int degree_EXPECTED = 5;
        int degree_ACTUAL = PolyUtils.degreeFromCoefficients(21);
        Assert.assertEquals(degree_EXPECTED, degree_ACTUAL);
    }

    @Test
    public void testNumberOfCoefficients() throws Exception {
        final int coeffs_EXPECTED = 21;
        int coeffs_ACTUAL = PolyUtils.numberOfCoefficients(5);
        Assert.assertEquals(coeffs_EXPECTED, coeffs_ACTUAL);
    }

    @Test
    public void testPolyFit() throws Exception {

        // max degree
        final int maxDegree = 6;

        double[] x = MathUtils.increment(11, 0, 0.1);
        double[] y = new double[x.length];

        // define double as errf(x)
        for (int i = 0; i < x.length; i++) {
            y[i] = erf(x[i]);
//            System.out.println("y = " + y[i]);
        }

        // define EXPECTED values : precomputde in matlab with fliplr(polyfit)
        ArrayList<double[]> coeff_EXPECTED = new ArrayList<double[]>();
        coeff_EXPECTED.add(new double[]{0.0531277556004621, 0.853026625583286});
        coeff_EXPECTED.add(new double[]{-0.00634545069301346, 1.24951466753979, -0.396488041956504});
        coeff_EXPECTED.add(new double[]{-0.00117120913502653, 1.16730171834066, -0.180894643707045,
                -0.143728932166306});
        coeff_EXPECTED.add(new double[]{1.87578727256749e-05, 1.12598341946038, 0.0256968506943939,
                -0.474275323208611, 0.165273195521153});
        coeff_EXPECTED.add(new double[]{7.50968728582951e-06, 1.12686515221902, 0.0184323975978185,
                -0.453809874699964, 0.141839475854753, 0.00937348786656298});
        coeff_EXPECTED.add(new double[]{6.40296881978214e-08, 1.12837041599673, -3.2419597312071e-05,
                -0.374389526989057, -0.0116238001908376, 0.145877210494725, -0.0455012408760536});

        // loop over assert
        for (int degree = 0; degree < maxDegree; degree++) {
            double[] coeff_ACTUAL = PolyUtils.polyFit(new DoubleMatrix(x), new DoubleMatrix(y), degree + 1);
            Assert.assertArrayEquals(coeff_EXPECTED.get(degree), coeff_ACTUAL, DELTA_03);
        }

    }

    @Test
    public void testPolyFitNormalize() throws Exception {

        final double[] x = MathUtils.increment(11, 0, 0.1);
        final double[] y = new double[x.length];

        // define double as errf(x)
        for (int i = 0; i < x.length; i++) {
            y[i] = erf(x[i]);
        }

        final double[] coeff_EXPECTED = {0.52048, 8.786103, -43.780634, -143.72893, 1652.73195};
        final double[] coeff_ACTUAL = PolyUtils.polyFitNormalized(new DoubleMatrix(x), new DoubleMatrix(y), 4);
        Assert.assertArrayEquals(coeff_EXPECTED, coeff_ACTUAL, DELTA_01);
    }

    @Test
    public void testPolyVal1d() throws Exception {
        // NOTE: definition of coeffs is reversed from Matlab's
        double[] coeff = {1, 2, 3};
        double[] input = {5, 7, 9};
        double[] solutionArray_EXPECTED = {86, 162, 262};
        for (int i = 0; i < solutionArray_EXPECTED.length; i++) {
            double solution_EXPECTED = solutionArray_EXPECTED[i];
            Assert.assertEquals(solution_EXPECTED, PolyUtils.polyVal1D(input[i], coeff), DELTA_02);
        }
    }

    @Test
    public void testPolyval() throws Exception {

        final double[] x = {0, 1, 2, 3};
        final double[] y = {0, 1, 2, 3};
        final double[] coeffsTemplate = MathUtils.increment(60, 1, 1);

        final double[][] values_EXPECTED = {{1, 6, 11, 16},
                {1, 21, 71, 151},
                {1, 55, 343, 1069},
                {1, 120, 1383, 6334},
                {1, 231, 4935, 33307},
                {1, 406, 16135, 160882},
                {1, 666, 49415, 729502},
                {1, 1035, 143879, 3150511},
                {1, 1540, 402439, 13090426}};

        for (int degree = 1; degree < 10; degree++) {

            int numCoefs = PolyUtils.numberOfCoefficients(degree);
            double[] coeffs = new double[numCoefs];
            System.arraycopy(coeffsTemplate, 0, coeffs, 0, numCoefs);
/*
            System.out.println("\n-----------------------");
            System.out.println("degree = " + degree);
            System.out.println("number of coefs = " + numCoefs);
            System.out.println("coeffs = " + Arrays.toString(coeffs));
*/
            double value_ACTUAL;
            for (int i = 0; i < x.length; i++) {
                value_ACTUAL = PolyUtils.polyval(x[i], y[i], new DoubleMatrix(coeffs), degree);
                Assert.assertEquals(values_EXPECTED[degree - 1][i], value_ACTUAL, DELTA_01);
            }
        }
    }

    //    TODO: finish unit test, matlab prototype '/d2/scratch/cr_lab/polyvalDoris.m'
    @Test
    public void testPolyvalGrid() throws Exception {

        final DoubleMatrix xMatrix = new DoubleMatrix(new double[]{0, 1, 2, 3});
        final DoubleMatrix yMatrix = xMatrix.dup();
        final double[] coeffsArrayTemplate = MathUtils.increment(60, 1, 1);

        DoubleMatrix valueMatrix_EXPECTED = new DoubleMatrix(xMatrix.rows, xMatrix.rows);

        for (int degree = 1; degree < 10; degree++) {

            int numCoefs = PolyUtils.numberOfCoefficients(degree);
            double[] coeffs = new double[numCoefs];
            System.arraycopy(coeffsArrayTemplate, 0, coeffs, 0, numCoefs);

            DoubleMatrix coeff = new DoubleMatrix(coeffs);
            double[][] polyval = PolyUtils.polyval(xMatrix.toArray(), yMatrix.toArray(), coeff.toArray(), degree);
            DoubleMatrix valueMatrix_ACTUAL = new DoubleMatrix(polyval);

            //// construct _EXPECTED_ MATRIX ////
            for (int i = 0; i < xMatrix.rows; i++) {
                for (int j = 0; j < yMatrix.rows; j++) {
                    valueMatrix_EXPECTED.put(i, j, PolyUtils.polyval(xMatrix.get(i, 0), yMatrix.get(j, 0), coeff, degree));
                }
            }

            Assert.assertEquals(valueMatrix_EXPECTED, valueMatrix_ACTUAL);

        }
    }


}

