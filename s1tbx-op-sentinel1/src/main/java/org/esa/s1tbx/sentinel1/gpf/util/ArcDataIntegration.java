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

import org.apache.commons.math3.analysis.MultivariateMatrixFunction;
import org.apache.commons.math3.analysis.MultivariateVectorFunction;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.optim.ConvergenceChecker;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.PointVectorValuePair;
import org.apache.commons.math3.optim.SimplePointChecker;
import org.apache.commons.math3.optim.linear.LinearConstraint;
import org.apache.commons.math3.optim.linear.LinearConstraintSet;
import org.apache.commons.math3.optim.linear.LinearObjectiveFunction;
import org.apache.commons.math3.optim.linear.Relationship;
import org.apache.commons.math3.optim.linear.SimplexSolver;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.vector.ModelFunction;
import org.apache.commons.math3.optim.nonlinear.vector.ModelFunctionJacobian;
import org.apache.commons.math3.optim.nonlinear.vector.Target;
import org.apache.commons.math3.optim.nonlinear.vector.Weight;
import org.apache.commons.math3.optim.nonlinear.vector.jacobian.LevenbergMarquardtOptimizer;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.util.MathArrays;
import org.esa.snap.core.util.SystemUtils;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Class for integrating node values from a weighted directed acyclic graph.
 *
 * @author David A. Monge
 */
public class ArcDataIntegration {

    public static boolean referenceNodeIsLast = false;

    /**
     * Integrates arc data to node data using weighted least squares (L2 norm).
     *     This function integrates arc `data` by finding `point_data` (the returned
     *     value), such that `point_data` minimizes:
     *         ||diag(weights) * (A * point_data - data)||_2
     *     where `A` is an incidence matrix of dimensions 'number of arcs' by
     *     'number of nodes'.
     *     Description of the algorithm:
     *         1. Apply weights
     *             A = diag(weights) * A
     *             b = diag(weights) * data
     *         2. Minimize
     *             ||A*x - b||_2
     *
     * @param arcs every row should contain two indices corresponding to the end nodes of a specific arc.
     * @param data data to be integrated.
     * @param weights quality of arc data.
     * @return
     */
    public static double[] integrateArcsL2(int[][] arcs, double[] data, double[] weights) {

        // graph size
        int noOfNodes = Arrays.stream(arcs)
                .flatMapToInt(Arrays::stream)
                .max()
                .getAsInt() + 1;
        int noOfArcs = arcs.length;
        SystemUtils.LOG.fine("Number of nodes: " + noOfNodes);
        SystemUtils.LOG.fine("Number of arcs: " + noOfArcs);

        // A: coefficients matrix
        double[][] incidenceMatrix = new double[noOfArcs][noOfNodes];
        int[] sourceNodes = getColumnVector(arcs, 0);  // get column and set to zero reference node occurrences
        int[] targetNodes = getColumnVector(arcs, 1);  // get column and set to zero reference node occurrences
        setValues(incidenceMatrix, repeat(-1.0, noOfArcs), null, sourceNodes);  // source nodes
        setValues(incidenceMatrix, repeat(1.0, noOfArcs), null, targetNodes);  // target nodes

        // remove reference node column
        if (!referenceNodeIsLast) {
            // remove first column
            shiftColumnsLeft(incidenceMatrix, 1);
        }
        dropLastColumn(incidenceMatrix);

        // problem
        ModelFunction problem = new ModelFunction(new MultivariateVectorFunction() {
            RealMatrix jacobian = new Array2DRowRealMatrix(incidenceMatrix);

            public double[] value(double[] params) {
                double[] values = jacobian.operate(params);

                return values;
            }
        });

        ModelFunctionJacobian problemJacobian =  new ModelFunctionJacobian(new MultivariateMatrixFunction() {
            double[][] jacobian = incidenceMatrix;

            public double[][] value(double[] params) {
                return jacobian;
            }
        });

        // optimization
        ConvergenceChecker<PointVectorValuePair> checker = new SimplePointChecker(0.00001, -1.0);
        LevenbergMarquardtOptimizer optimizer = new LevenbergMarquardtOptimizer(checker);
        int maxIterations = 10000;
        PointVectorValuePair solution = optimizer.optimize(new MaxEval(maxIterations),
                                                           problem,
                                                           problemJacobian,
                                                           new Target(data),
                                                           new Weight(weights),
                                                           new InitialGuess(new double[noOfNodes - 1]));

        // return solution
        double[] nodeData;
        if (referenceNodeIsLast) {
            nodeData = Arrays.copyOf(solution.getPoint(), noOfNodes);
            nodeData[noOfNodes - 1] = 0;  // add reference node back (last)
        } else {
            nodeData = new double[noOfNodes];
            System.arraycopy(solution.getPoint(), 0, nodeData, 1, noOfNodes - 1);  // reference node (first) is 0
        }

        return nodeData;
    }

    /**
     * Integrates arc data to node data using L1 norm.
     *     This function integrates arc `data` by finding `point_data` (the returned
     *     value), such that `point_data` minimizes:
     *         ||diag(weights) * (P * point_data - data)||_1
     *     where `P` is an incidence matrix of dimensions 'number of arcs' by
     *     'number of nodes'.
     *     Description of the algorithm:
     *         1. Apply weights
     *             P = diag(weights) * P
     *             q = diag(weights) * data
     *         2. Minimize
     *             ||P*u - q||_1
     *            by recasting it as
     *              minimize    c' * x
     *              subject to  G * x <= h
     *            where
     *                c = [0; 1]
     *                x = [u; v]
     *                G = [P, -I; -P, -I]
     *                h = [q; -q]
     *
     * Note: The static variable {@link ArcDataIntegration::referenceNodeIsLast} is used to know what node is the
     * reference one. If referenceNodeIsLast==false, then the reference node is the one with the lowest value in `arcs`.
     * Conversely, if referenceNodeIsLast==true, then the reference node is the one with the highest value in `arcs`.
     *
     * @param arcs every row should contain two indices corresponding to the end nodes of a specific arc.
     * @param data data to be integrated.
     * @param weights quality of arc data.
     * @return an array with the integrated node data.
     */
    public static double[] integrateArcsL1(int[][] arcs, double[] data, double[] weights) {
        // graph size
        int noOfNodes = Arrays.stream(arcs)
                .flatMapToInt(Arrays::stream)
                .max()
                .getAsInt() + 1;
        int noOfArcs = arcs.length;
        SystemUtils.LOG.fine("Number of nodes: " + noOfNodes);
        SystemUtils.LOG.fine("Number of arcs: " + noOfArcs);

        // prepare matrices for linear program
        // 1) c = [0; 1]
        double[] c = new double[(noOfNodes - 1) + noOfArcs];
        Arrays.fill(c, noOfNodes - 1, c.length, 1);
        SystemUtils.LOG.fine("\n>>> c:\n" + Arrays.toString(c));

        // 2) G = [[ P, -I],
        //         [-P, -I]]
        double[][] G = new double[2 * noOfArcs][(noOfNodes - 1) + noOfArcs];
        int rowOffsetG = noOfArcs;
        int colOffsetG = noOfNodes - 1;
        int[] sourceNodes = getColumnVector(arcs, 0);  // get column and set to zero reference node occurrences
        int[] targetNodes = getColumnVector(arcs, 1);  // get column and set to zero reference node occurrences

        // P
        setValues(G, MathArrays.scale(-1.0, weights), null, sourceNodes);  // source nodes
        setValues(G, weights, null, targetNodes);  // target nodes
        // -P
        setValues(G, weights, null, sourceNodes, rowOffsetG, 0);  // source nodes
        setValues(G, MathArrays.scale(-1.0, weights), null, targetNodes, rowOffsetG, 0);  // target nodes
        // remove reference node column
        if (referenceNodeIsLast) {
            // set to 0 the column of the reference node (first column of [-I; -I])
            int referenceNodeIndex = noOfNodes - 1;
            setValues(G, repeat(0.0, 2 * noOfArcs), null, repeat(referenceNodeIndex, 2 * noOfArcs));
        } else {
            // remove first column
            shiftColumnsLeft(G, 1);
        }

        // -I
        setValues(G, repeat(-1.0, noOfArcs), null, null, 0, colOffsetG);
        // -I
        setValues(G, repeat(-1.0, noOfArcs), null, null, rowOffsetG, colOffsetG);

        SystemUtils.LOG.fine("\n>>> G:\n" + Arrays.deepToString(G).replaceAll("], ", "]\n ").replaceAll("\\[", "[\t ").replaceAll(", ", ",\t "));

        // 3) h = [q, -q]
        //    where q = data * weights
        double[] q = MathArrays.ebeMultiply(data, weights);
        double[] h = new ArrayRealVector(q, MathArrays.scale(-1, q)).toArray();
        SystemUtils.LOG.fine("\n>>> h:\n" + Arrays.toString(h).replaceAll(", ", "\n "));

        // solve min c'x s.t. G x <= h
        PointValuePair solution = solveLinearProgram(c, G, h);
        if (solution == null) {
            throw new RuntimeException("No optimal solution could be found.");
        }

        // return solution
        double[] nodeData;
        if (referenceNodeIsLast) {
            nodeData = Arrays.copyOf(solution.getPoint(), noOfNodes);
            nodeData[noOfNodes - 1] = 0;  // add reference node back (last)
        } else {
            nodeData = new double[noOfNodes];
            System.arraycopy(solution.getPoint(), 0, nodeData, 1, noOfNodes - 1);  // reference node (first) is 0
        }

        SystemUtils.LOG.fine("\n>>> x:\n" + Arrays.toString(solution.getPoint()).replaceAll(", ", "\n "));
        SystemUtils.LOG.fine("\n>>> nodes:\n" + Arrays.toString(nodeData).replaceAll(", ", "\n "));

        return nodeData;
    }

    /**
     * Integrates arc data to node data using L1 and L2 norms. Steps are the following:
     * <p><ol>
     * <li>Integrate arc data using L1 to get a first approximation of node data.</li>
     * <li>Removal of outlier arcs, i.e. those with a large difference between the input arc data and the results from L1.</li>
     * <li>Integrate arc data using L2 to the (sub)set of arcs.</li>
     * </ol>
     * @param arcs every row should contain two indices corresponding to the end nodes of a specific arc.
     * @param data data to be integrated.
     * @param weights quality of arc data.
     * @return an array with the integrated node data.
     */
    public static double[] integrateArcsL1AndL2(int[][] arcs, double[] data, double[] weights) {
        double[] nodeData;

        // L1
        nodeData = integrateArcsL1(arcs, data, weights);

        // remove outliers
        List<Integer> nonOutlierIndices = flagNonOutlierArcs(arcs, data, nodeData);
        int[][] newArcs = new int[nonOutlierIndices.size()][];
        double[] newData = new double[nonOutlierIndices.size()];
        double[] newWeights = new double[nonOutlierIndices.size()];

        for (int i = 0; i < newArcs.length; i++) {
            newArcs[i] = arcs[nonOutlierIndices.get(i)];
            newData[i] = data[nonOutlierIndices.get(i)];
            newWeights[i] = weights[nonOutlierIndices.get(i)];
        }

        // validate that the new graph is connected
        int noOfNodes = Arrays.stream(arcs)
                .flatMapToInt(Arrays::stream)
                .max()
                .getAsInt() + 1;
        if (!GraphUtils.isConnectedGraph(newArcs, noOfNodes)) {
            throw new RuntimeException("Graph is not connected after removing outlier arcs.");
        }

        // L2
        nodeData = integrateArcsL2(newArcs, newData, newWeights);

        return nodeData;
    }

    private static List<Integer> flagNonOutlierArcs(int[][] arcs, double[] data, double[] nodeData) {
        // differences
        double[] arcDifferences = new double[arcs.length];
        for (int i = 0; i < arcs.length; i++) {
            double integratedArcData = nodeData[arcs[i][1]] - nodeData[arcs[i][0]];  // arc data computed from the nodes
            arcDifferences[i] = integratedArcData - data[i];  // difference with actual arc data
        }

        // get outlier flags for every element in input array
        return getNonOutlierIndicesIQR(arcDifferences);
    }

    /**
     * Get an array list indicating the element indices of those values which are not outliers.
     * Outliers are determined using the inter-quartile range (IQR) method.
     * @param values array of values.
     * @return
     */
    private static List<Integer> getNonOutlierIndicesIQR(double[] values) {
        DescriptiveStatistics descriptiveStatistics = new DescriptiveStatistics(values);
        double q1 = descriptiveStatistics.getPercentile(25);
        double q3 = descriptiveStatistics.getPercentile(75);
        double iqr = q3 - q1;
        double lowRange = q1 - 1.5 * iqr;
        double highRange = q3 + 1.5 * iqr;

        List<Integer> nonOutlierIndices = new ArrayList<>();
        for (int i = 0; i < values.length; i++) {
            if (lowRange <= values[i] && values[i] <= highRange) {
                nonOutlierIndices.add(i);
            }
        }

        return nonOutlierIndices;
    }

    /**
     * Shift every element in the matrix a number of positions to the left. Applies zero padding in the right.
     * @param matrix matrix to shift.
     * @param positions number of columns to shift.
     */
    static void shiftColumnsLeft(double[][] matrix, int positions) {
        int columns = matrix[0].length;
        for (int i = 0; i < matrix.length; i++) {
            System.arraycopy(matrix[i], positions, matrix[i], 0, columns - positions);  // copy elements with shift
            System.arraycopy(new double[columns - positions], 0, matrix[i], columns - positions, positions);  // zero padding on the right
        }
    }

    /**
     * Drops the las column in a matrix.
     * @param matrix matrix to modify.
     */
    static void dropLastColumn(double[][] matrix) {
        int columns = matrix[0].length;
        for (int i = 0; i < matrix.length; i++) {
            double[] newRow = new double[columns - 1];
            System.arraycopy(matrix[i], 0, newRow, 0, columns - 1);  // copy elements except last one
            matrix[i] = newRow;
        }
    }

    private static int[] getColumnVector(int[][] matrix, int column) {
        int[] vector = new int[matrix.length];

        for (int i = 0; i < matrix.length; i++) {
            vector[i] = matrix[i][column];
        }

        return vector;
    }

    /**
     * Set values in a matrix for the specified row and col indices. If row/column indices are not specified (null),
     * then sequential indices are assumed. Each value is set to position (rowOffset + rowIndex, colOffset + colIndex).
     *
     * @param matrix the matrix.
     * @param values values to insert.
     * @param rowIndices indices for the rows.
     * @param colIndices indices for the columns.
     * @param rowOffset offset for the columns.
     * @param colOffset offset for the rows.
     */
    public static void setValues(double[][] matrix, double[] values, int[] rowIndices, int[] colIndices, int rowOffset, int colOffset) {
        for (int i = 0; i < values.length; i++) {
            int rowIndex = (rowIndices == null) ? i : rowIndices[i];
            int colIndex = (colIndices == null) ? i : colIndices[i];
            int row = rowOffset + rowIndex;
            int col = colOffset + colIndex;

            matrix[row][col] = values[i];
        }
    }

    /**
     * Set values in a matrix for the specified row and col indices. If row/column indices are not specified (null),
     * then sequential indices are assumed. Each value is set to position (rowIndex, colIndex).
     * @param matrix the matrix.
     * @param values values to insert.
     * @param rowIndices indices for the rows.
     * @param colIndices indices for the columns.
     */
    public static void setValues(double[][] matrix, double[] values, int[] rowIndices, int[] colIndices) {
        setValues(matrix, values, rowIndices, colIndices, 0, 0);
    }

    /**
     * Creates a double array by repeating a value a number of times.
     * @param value
     * @param times
     * @return
     */
    public static double[] repeat(double value, int times) {
        double[] array = new double[times];
        Arrays.fill(array, value);
        return array;
    }

    /**
     * Creates an int array by repeating a value a number of times.
     * @param value
     * @param times
     * @return
     */
    public static int[] repeat(int value, int times) {
        int[] array = new int[times];
        Arrays.fill(array, value);
        return array;
    }

    /**
     * Element-wise vector multiplication.
     */
    public static double[] product(double[] a, double[] b) {
        double result[] = new double[a.length];
        Arrays.setAll(result, i -> a[i] * b[i]);

        return result;
    }

    /**
     * Solves the linear program:
     * ```
     *      min {c' x}
     *      s.t.
     *      G x <= h
     * ```
     *
     * @param c optimization objective coefficients.
     * @param G constraint coefficients.
     * @param h constraint value.
     */
    public static PointValuePair solveLinearProgram(double[] c, double[][] G, double[] h) {
        //describe the optimization problem
        LinearObjectiveFunction objective = new LinearObjectiveFunction(c, 0);

        int noOfConstraints = G.length;
        Collection<LinearConstraint> constraints = new ArrayList<>();
        for (int i = 0; i < noOfConstraints; i++) {
            constraints.add(new LinearConstraint(G[i], Relationship.LEQ, h[i]));
        }

        //create and run solver
        PointValuePair solution = new SimplexSolver(/*1e-10, 10, 1e-13*/)
                .optimize(objective, new LinearConstraintSet(constraints), GoalType.MINIMIZE);

        return solution;
    }
}
