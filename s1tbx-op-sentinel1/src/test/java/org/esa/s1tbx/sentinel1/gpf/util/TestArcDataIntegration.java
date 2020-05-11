/*
 * Copyright (C) 2020 by SENSAR B.V. http://www.sensar.nl
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
package org.esa.s1tbx.sentinel1.gpf.util;

import org.apache.commons.math3.optim.PointValuePair;
import org.junit.Assert;
import org.junit.Test;


/**
 * Unit test for ArcDataIntegration.
 *
 * @author David A. Monge
 */
public class TestArcDataIntegration {

    private static final double TOLERANCE = 0.00001;

    // graphs
    private static final int[][] ARCS = {
            {0, 1},
            {0, 2},
            {0, 3},
            {1, 2},
            {1, 3},
            {2, 3}};

    private static final double[] ARC_DATA_L1 = {
            0.83518829,
            0.5270889,
            -1.1267309,
            1.68593942,
            -2.1092428,
            0.00499272};

    private static final double[] ARC_DATA_L2 = {
            0.005029396569301776,
            -0.0004506372322133907,
            -0.0005283029010458212,
            -0.005564045343286754,
            -0.0056546851100774094,
            -8.322474047251389e-05};

    // arc weights
    private static final double[] WEIGHTS1 = {1, 1, 1, 1, 1, 1};
    private static final double[] WEIGHTS2 = {-2.4, -1, 2.3, 0.7, -0.2, 9};

    // expected solutions
    private static final double[] SOLUTION_L1_WEIGHTS1 = {1.12673084, 1.9619191, 1.65381976, 0.0};
    private static final double[] SOLUTION_L1_WEIGHTS2 = {1.12673088, 1.96191917, -0.00499272, 0.0};
    private static final double[] SOLUTION_L2_WEIGHTS1 =
            {0.0, 0.005113408111073361, -0.00045063723221339076, -0.0005338619726859046};


    /**
     * Test arc integration using L1 norm.
     */
    @Test
    public void testIntegrateArcsL1() {
        double[] nodes;

        ArcDataIntegration.referenceNodeIsLast = true;

        // test without weights
        nodes = ArcDataIntegration.integrateArcsL1(ARCS, ARC_DATA_L1, WEIGHTS1);
        assertDoubleArrays(SOLUTION_L1_WEIGHTS1, nodes, TOLERANCE);

        // test with weights
        nodes = ArcDataIntegration.integrateArcsL1(ARCS, ARC_DATA_L1, WEIGHTS2);
        assertDoubleArrays(SOLUTION_L1_WEIGHTS2, nodes, TOLERANCE);
    }

    /**
     * Test arc integration using L2 norm. Should throw exception because of sensitivity to outliers.
     *
     * This method succeeds if the tolerance is NOT MET. L2 must not reach the expected values as the least squares
     * method is sensitive to outliers.
     * This can be overcome by using either L1 or L1+L2.
     * @see #testIntegrateArcsL1()
     * @see #testIntegrateArcsL1AndL2()
     */
    @Test(expected = java.lang.AssertionError.class)
    public void testIntegrateArcsL2_fails() {
        double[] nodes;

        ArcDataIntegration.referenceNodeIsLast = false;

        // test without weights
        nodes = ArcDataIntegration.integrateArcsL2(ARCS, ARC_DATA_L2, WEIGHTS1);
        for (int i = 0; i < nodes.length; i++) {
            System.out.println(SOLUTION_L2_WEIGHTS1[i] + "     " + nodes[i]);
        }
        assertDoubleArrays(SOLUTION_L2_WEIGHTS1, nodes, TOLERANCE);
    }

    /**
     * Test arc integration using the combination of L1 and L2 norms.
     */
    @Test
    public void testIntegrateArcsL1AndL2() {
        double[] nodes;

        ArcDataIntegration.referenceNodeIsLast = false;

        // test without weights
        nodes = ArcDataIntegration.integrateArcsL1AndL2(ARCS, ARC_DATA_L2, WEIGHTS1);
        for (int i = 0; i < nodes.length; i++) {
            System.out.println(SOLUTION_L2_WEIGHTS1[i] + "     " + nodes[i]);
        }
        assertDoubleArrays(SOLUTION_L2_WEIGHTS1, nodes, TOLERANCE);
    }

    /**
     * Test some array operations.
     */
    @Test
    public void testArrayOperations() {
        double[][] matrix = new double[][]{{1., 2., 3., 4.}};
        double[] expected = new double[]{2., 3., 4., 0.};

        // shift matrix columns
        ArcDataIntegration.shiftColumnsLeft(matrix, 1);
        Assert.assertArrayEquals("Arrays differ.", expected, matrix[0], TOLERANCE);
    }

    /**
     * Test the correctness of solving linear program.
     */
    @Test
    public void testSolveSimpleLinearProgram() {
        // optimization function coefficients
        double[] c = {2, 1};

        // constraint coefficients
        double[][] G = {
                {-1.0,  1.0},
                {-1.0, -1.0},
                { 0.0, -1.0},
                { 1.0, -2.0}};

        // constraint values
        double[] h = {1.0, -2.0, 0.0, 4.0};

        // expected solution
        double[] expectedSolution = {0.5, 1.5};
        double expectedMinimum = 2.5;

        // solve problem
        PointValuePair solution = ArcDataIntegration.solveLinearProgram(c, G, h);
        validateSolution(solution, expectedSolution, expectedMinimum);
    }

    /**
     * Test the correctness of solving an unweighted linear program.
     */
    @Test
    public void testSolveLinearProgram_unweighted() {
        // optimization function coefficients
        double[] c = {0, 0, 0, 1, 1, 1, 1, 1, 1};

        // constraint coefficients
        double[][] G = {
                {-1,  1,  0, -1,  0,  0,  0,  0,  0},
                {-1,  0,  1,  0, -1,  0,  0,  0,  0},
                {-1,  0,  0,  0,  0, -1,  0,  0,  0},
                { 0, -1,  1,  0,  0,  0, -1,  0,  0},
                { 0, -1,  0,  0,  0,  0,  0, -1,  0},
                { 0,  0, -1,  0,  0,  0,  0,  0, -1},

                { 1, -1,  0, -1,  0,  0,  0,  0,  0},
                { 1,  0, -1,  0, -1,  0,  0,  0,  0},
                { 1,  0,  0,  0,  0, -1,  0,  0,  0},
                { 0,  1, -1,  0,  0,  0, -1,  0,  0},
                { 0,  1,  0,  0,  0,  0,  0, -1,  0},
                { 0,  0,  1,  0,  0,  0,  0,  0, -1},
        };

        // constraint values
        double[] h = {
                0.83518829,
                0.5270889,
                -1.1267309,
                1.68593942,
                -2.1092428,
                0.00499272,
                -0.83518829,
                -0.5270889,
                1.1267309,
                -1.68593942,
                2.1092428,
                -0.00499272};

        // expected solution
        double[] expectedSolution = {
                1.12673084e+00,
                1.96191910e+00,
                1.65381976e+00,
                -1.03275754e-08,
                1.99571269e-09,
                3.68713046e-08,
                1.99403872e+00,
                1.47323630e-01,
                1.65881242e+00};

        double expectedMinimum = 3.800174795756848;

        double sum  = 0;
        for (int i = 3; i < expectedSolution.length; i++) {
            sum += expectedSolution[i];
        }
        System.out.println(sum);

        // solve problem
        ArcDataIntegration.referenceNodeIsLast = false;
        PointValuePair solution = ArcDataIntegration.solveLinearProgram(c, G, h);
        System.out.println(solution.getValue());
        validateSolution(solution, expectedSolution, expectedMinimum);
    }

    private void assertDoubleArrays(double[] expected, double[] actual, double tolerance) {
        for (int i = 0; i < expected.length; i++) {
            Assert.assertEquals(i + "th element from arrays differ.", expected[i], actual[i], tolerance);
        }
    }

    private static void validateSolution(PointValuePair solution, double[] expectedSolution, double expectedMinimum) {
        double minimum = solution.getValue();
        double[] solutionValues = solution.getPoint();

        for (int i = 0; i < solutionValues.length; i++) {
            if (Math.abs(solutionValues[i] - expectedSolution[i]) > TOLERANCE) {
                printSolutions(solutionValues, expectedSolution);
                System.err.println("Minimum. Expected: " + expectedMinimum + "\tFound: " + minimum);
            }
            Assert.assertEquals(i + "th elements from solutions differ.", expectedSolution[i], solutionValues[i], TOLERANCE);
        }

        Assert.assertEquals("Objective function values differ.", expectedMinimum, minimum, TOLERANCE);
    }

    private static void printSolutions(double[] solutionValues, double[] expectedSolution) {
        for (int i = 0; i < solutionValues.length; i++) {
            System.err.println(i + " >> " + +expectedSolution[i] + "\t\t" + solutionValues[i]);
        }
    }
}
