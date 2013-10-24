package org.jlinda.core.coregistration.estimation.utils;

import Jama.Matrix;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Various utility functions for the Jama matrix toolkit.
 * <p/>
 * Copyright (c) 2008 Eric Eaton
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * @author Eric Eaton (EricEaton@umbc.edu) <br>
 *         University of Maryland Baltimore County
 * @version 0.1
 */
public class JamaUtils {

    /**
     * Gets the specified column of a matrix.
     *
     * @param m   the matrix.
     * @param col the column to get.
     * @return the specified column of m.
     */
    public static Matrix getcol(Matrix m, int col) {
        return m.getMatrix(0, m.getRowDimension() - 1, col, col);
    }


    /**
     * Gets the specified columns of a matrix.
     *
     * @param m       the matrix
     * @param columns the columns to get
     * @return the matrix of the specified columns of m.
     */
    public static Matrix getcolumns(Matrix m, int[] columns) {
        Matrix colMatrix = new Matrix(m.getRowDimension(), columns.length);
        for (int i = 0; i < columns.length; i++) {
            setcol(colMatrix, i, getcol(m, columns[i]));
        }
        return colMatrix;
    }


    /**
     * Gets the specified row of a matrix.
     *
     * @param m   the matrix.
     * @param row the row to get.
     * @return the specified row of m.
     */
    public static Matrix getrow(Matrix m, int row) {
        return m.getMatrix(row, row, 0, m.getColumnDimension() - 1);
    }


    /**
     * Gets the specified rows of a matrix.
     *
     * @param m    the matrix
     * @param rows the rows to get
     * @return the matrix of the specified rows of m.
     */
    public static Matrix getrows(Matrix m, int[] rows) {
        Matrix rowMatrix = new Matrix(rows.length, m.getColumnDimension());
        for (int i = 0; i < rows.length; i++) {
            setrow(rowMatrix, i, getrow(m, rows[i]));
        }
        return rowMatrix;
    }

    /**
     * Sets the specified row of a matrix.  Modifies the passed matrix.
     *
     * @param m      the matrix.
     * @param row    the row to modify.
     * @param values the new values of the row.
     */
    public static void setrow(Matrix m, int row, Matrix values) {
        if (!isRowVector(values))
            throw new IllegalArgumentException("values must be a row vector.");
        m.setMatrix(row, row, 0, m.getColumnDimension() - 1, values);
    }


    /**
     * Sets the specified column of a matrix.  Modifies the passed matrix.
     *
     * @param m      the matrix.
     * @param col    the column to modify.
     * @param values the new values of the column.
     */
    public static void setcol(Matrix m, int col, Matrix values) {
        if (!isColumnVector(values))
            throw new IllegalArgumentException("values must be a column vector.");
        m.setMatrix(0, m.getRowDimension() - 1, col, col, values);
    }


    /**
     * Sets the specified column of a matrix.  Modifies the passed matrix.
     *
     * @param m      the matrix.
     * @param col    the column to modify.
     * @param values the new values of the column.
     */
    public static void setcol(Matrix m, int col, double[] values) {
        if (values.length != m.getRowDimension())
            throw new IllegalArgumentException("values must have the same number of rows as the matrix.");
        for (int i = 0; i < values.length; i++) {
            m.set(i, col, values[i]);
        }
    }


    /**
     * Appends additional rows to the first matrix.
     *
     * @param m the first matrix.
     * @param n the matrix to append containing additional rows.
     * @return a matrix with all the rows of m then all the rows of n.
     */
    public static Matrix rowAppend(Matrix m, Matrix n) {
        int mNumRows = m.getRowDimension();
        int mNumCols = m.getColumnDimension();
        int nNumRows = n.getRowDimension();
        int nNumCols = n.getColumnDimension();

        if (mNumCols != nNumCols)
            throw new IllegalArgumentException("Number of columns must be identical to row-append.");

        Matrix x = new Matrix(mNumRows + nNumRows, mNumCols);
        x.setMatrix(0, mNumRows - 1, 0, mNumCols - 1, m);
        x.setMatrix(mNumRows, mNumRows + nNumRows - 1, 0, mNumCols - 1, n);

        return x;
    }


    /**
     * Appends additional columns to the first matrix.
     *
     * @param m the first matrix.
     * @param n the matrix to append containing additional columns.
     * @return a matrix with all the columns of m then all the columns of n.
     */
    public static Matrix columnAppend(Matrix m, Matrix n) {
        int mNumRows = m.getRowDimension();
        int mNumCols = m.getColumnDimension();
        int nNumRows = n.getRowDimension();
        int nNumCols = n.getColumnDimension();

        if (mNumRows != nNumRows)
            throw new IllegalArgumentException("Number of rows must be identical to column-append.");

        Matrix x = new Matrix(mNumRows, mNumCols + nNumCols);
        x.setMatrix(0, mNumRows - 1, 0, mNumCols - 1, m);
        x.setMatrix(0, mNumRows - 1, mNumCols, mNumCols + nNumCols - 1, n);

        return x;
    }


    /**
     * Deletes a row from a matrix.  Does not change the passed matrix.
     *
     * @param m   the matrix.
     * @param row the row to delete.
     * @return m with the specified row deleted.
     */
    public static Matrix deleteRow(Matrix m, int row) {
        int numRows = m.getRowDimension();
        int numCols = m.getColumnDimension();
        Matrix m2 = new Matrix(numRows - 1, numCols);
        for (int mi = 0, m2i = 0; mi < numRows; mi++) {
            if (mi == row)
                continue;  // skips incrementing m2i
            for (int j = 0; j < numCols; j++) {
                m2.set(m2i, j, m.get(mi, j));
            }
            m2i++;
        }
        return m2;
    }


    /**
     * Deletes a column from a matrix.  Does not change the passed matrix.
     *
     * @param m   the matrix.
     * @param col the column to delete.
     * @return m with the specified column deleted.
     */
    public static Matrix deleteCol(Matrix m, int col) {
        int numRows = m.getRowDimension();
        int numCols = m.getColumnDimension();
        Matrix m2 = new Matrix(numRows, numCols - 1);
        for (int mj = 0, m2j = 0; mj < numCols; mj++) {
            if (mj == col)
                continue;  // skips incrementing m2j
            for (int i = 0; i < numRows; i++) {
                m2.set(i, m2j, m.get(i, mj));
            }
            m2j++;
        }
        return m2;
    }


    /**
     * Gets the sum of the specified row of the matrix.
     *
     * @param m   the matrix.
     * @param row the row.
     * @return the sum of m[row,*]
     */
    public static double rowsum(Matrix m, int row) {
        // error check the column index
        if (row < 0 || row >= m.getRowDimension()) {
            throw new IllegalArgumentException("row exceeds the row indices [0," + (m.getRowDimension() - 1) + "] for m.");
        }

        double rowsum = 0;

        // loop through the rows for this column and compute the sum
        int numCols = m.getColumnDimension();
        for (int j = 0; j < numCols; j++) {
            rowsum += m.get(row, j);
        }

        return rowsum;
    }


    /**
     * Gets the sum of the specified column of the matrix.
     *
     * @param m   the matrix.
     * @param col the column.
     * @return the sum of m[*,col]
     */
    public static double colsum(Matrix m, int col) {
        // error check the column index
        if (col < 0 || col >= m.getColumnDimension()) {
            throw new IllegalArgumentException("col exceeds the column indices [0," + (m.getColumnDimension() - 1) + "] for m.");
        }

        double colsum = 0;

        // loop through the rows for this column and compute the sum
        int numRows = m.getRowDimension();
        for (int i = 0; i < numRows; i++) {
            colsum += m.get(i, col);
        }

        return colsum;
    }


    /**
     * Computes the sum of each row of a matrix.
     *
     * @param m the matrix.
     * @return a column vector of the sum of each row of m.
     */
    public static Matrix rowsum(Matrix m) {
        int numRows = m.getRowDimension();
        int numCols = m.getColumnDimension();
        Matrix sum = new Matrix(numRows, 1);
        // loop through the rows and compute the sum
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numCols; j++) {
                sum.set(i, 0, sum.get(i, 0) + m.get(i, j));
            }
        }
        return sum;
    }


    /**
     * Computes the sum of each column of a matrix.
     *
     * @param m the matrix.
     * @return a row vector of the sum of each column of m.
     */
    public static Matrix colsum(Matrix m) {
        int numRows = m.getRowDimension();
        int numCols = m.getColumnDimension();
        Matrix sum = new Matrix(1, numCols);
        // loop through the rows and compute the sum
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numCols; j++) {
                sum.set(0, j, sum.get(0, j) + m.get(i, j));
            }
        }
        return sum;
    }

    /**
     * Computes the sum the elements of a matrix.
     *
     * @param m the matrix.
     * @return the sum of the elements of the matrix
     */
    public static double sum(Matrix m) {
        int numRows = m.getRowDimension();
        int numCols = m.getColumnDimension();
        double sum = 0;
        // loop through the rows and compute the sum
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numCols; j++) {
                sum += m.get(i, j);
            }
        }
        return sum;
    }


    /**
     * Determines if a given matrix is a row vector, that is, it has only one row.
     *
     * @param m the matrix.
     * @return whether the given matrix is a row vector (whether it has only one row).
     */
    public static boolean isRowVector(Matrix m) {
        return m.getRowDimension() == 1;
    }


    /**
     * Determines if a given matrix is a column vector, that is, it has only one column.
     *
     * @param m the matrix.
     * @return whether the given matrix is a column vector (whether it has only one column).
     */
    public static boolean isColumnVector(Matrix m) {
        return m.getColumnDimension() == 1;
    }


    /**
     * Transforms the given matrix into a column vector, that is, a matrix with one column.
     * The matrix must be a vector (row or column) to begin with.
     *
     * @param m
     * @return <code>m.transpose()</code> if m is a row vector,
     *         <code>m</code> if m is a column vector.
     * @throws IllegalArgumentException if m is not a row vector or a column vector.
     */
    public static Matrix makeColumnVector(Matrix m) {
        if (isColumnVector(m))
            return m;
        else if (isRowVector(m))
            return m.transpose();
        else
            throw new IllegalArgumentException("m is not a vector.");
    }


    /**
     * Transforms the given matrix into a row vector, that is, a matrix with one row.
     * The matrix must be a vector (row or column) to begin with.
     *
     * @param m
     * @return <code>m.transpose()</code> if m is a column vector,
     *         <code>m</code> if m is a row vector.
     * @throws IllegalArgumentException if m is not a row vector or a column vector.
     */
    public static Matrix makeRowVector(Matrix m) {
        if (isRowVector(m))
            return m;
        else if (isColumnVector(m))
            return m.transpose();
        else
            throw new IllegalArgumentException("m is not a vector.");
    }


    /**
     * Computes the dot product of two vectors.  Both must be either row or column vectors.
     *
     * @param m1
     * @param m2
     * @return the dot product of the two vectors.
     */
    public static double dotproduct(Matrix m1, Matrix m2) {

        Matrix m1colVector = makeColumnVector(m1);
        Matrix m2colVector = makeColumnVector(m2);

        int n = m1colVector.getRowDimension();
        if (n != m2colVector.getRowDimension()) {
            throw new IllegalArgumentException("m1 and m2 must have the same number of elements.");
        }

        double scalarProduct = 0;
        for (int row = 0; row < n; row++) {
            scalarProduct += m1colVector.get(row, 0) * m2colVector.get(row, 0);
        }

        return scalarProduct;

    }

    /**
     * Determines whether a matrix is symmetric.
     *
     * @param m the matrix.
     * @return <code>true</code> if a is symmetric, <code>false</code> otherwise
     */
    public static boolean isSymmetric(Matrix m) {
        int numRows = m.getRowDimension();
        int numCols = m.getColumnDimension();

        for (int i = 0; i < numRows; i++) {
            for (int j = i + 1; j < numCols; j++) {
                if (m.get(i, j) != m.get(j, i))
                    return false;
            }
        }
        return true;
    }


    /**
     * Specifies a simple mathematical function.
     */
    public enum Function {
        MAX, MIN, MEAN;

        /**
         * Apply the function to the two values.
         */
        public double applyFunction(int v1, int v2) {
            switch (this) {
                case MAX:
                    return Math.max(v1, v2);
                case MIN:
                    return Math.min(v1, v2);
                case MEAN:
                    return (v1 + v2) / 2.0;
            }
            throw new IllegalStateException("Unknown Function.");
        }

        /**
         * Apply the function to the two values.
         */
        public double applyFunction(double v1, double v2) {
            switch (this) {
                case MAX:
                    return Math.max(v1, v2);
                case MIN:
                    return Math.min(v1, v2);
                case MEAN:
                    return (v1 + v2) / 2.0;
            }
            throw new IllegalStateException("Unknown Function.");
        }
    }


    /**
     * Makes a matrix symmetric by applying a function to symmetric elements.
     *
     * @param m the matrix to make symmetric
     * @param f the function to apply
     */
    public static void makeMatrixSymmetric(Matrix m, Function f) {
        int numRows = m.getRowDimension();
        int numCols = m.getColumnDimension();
        for (int i = 0; i < numRows; i++) {
            for (int j = i; j < numCols; j++) {
                double value1 = m.get(i, j);
                double value2 = m.get(j, i);
                double similarity = f.applyFunction(value1, value2);
                m.set(i, j, similarity);
                m.set(j, i, similarity); // similarity is symmetric
            }
        }

    }


    /**
     * Normalizes a matrix to make the elements sum to 1.
     *
     * @param m the matrix
     * @return the normalized form of m with all elements summing to 1.
     */
    public static Matrix normalize(Matrix m) {
        int numRows = m.getRowDimension();
        int numCols = m.getColumnDimension();

        // compute the sum of the matrix
        double sum = 0;
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numCols; j++) {
                sum += m.get(i, j);
            }
        }

        // normalize the matrix
        Matrix normalizedM = new Matrix(numRows, numCols);
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numCols; j++) {
                normalizedM.set(i, j, m.get(i, j) / sum);
            }
        }

        return normalizedM;
    }

    /**
     * Gets the maximum value in a matrix.
     *
     * @param m the matrix
     * @return the maximum value in m.
     */
    public static double getMax(Matrix m) {
        int numRows = m.getRowDimension();
        int numCols = m.getColumnDimension();

        // compute the max of the matrix
        double maxValue = Double.MIN_VALUE;
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numCols; j++) {
                maxValue = Math.max(maxValue, m.get(i, j));
            }
        }
        return maxValue;
    }

    /**
     * Gets the maximum value in a matrix, in the absolute sense.
     *
     * @param m the matrix
     * @return the maximum value in m.
     */
    public static double getAbsMax(Matrix m) {
        int numRows = m.getRowDimension();
        int numCols = m.getColumnDimension();

        // compute the max of the matrix
        double maxValue = Double.MIN_VALUE;
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numCols; j++) {
                maxValue = Math.max(maxValue, Math.abs(m.get(i, j)));
            }
        }
        return maxValue;
    }

    /**
     * Gets the maximum value and [row, col] indices, in a matrix, in the absolute sense.
     *
     * @param m the matrix
     * @return the maximum value and [row, col] indices in m.
     */
    public static double[] getAbsArgMax(Matrix m) {
        int numRows = m.getRowDimension();
        int numCols = m.getColumnDimension();

        double abs;
        double[] maxArray = new double[3]; // value, row, col
        // compute the max of the matrix
        double maxValue = Double.MIN_VALUE;
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numCols; j++) {
                abs = Math.abs(m.get(i, j));
                if (maxValue <= abs) {
                    maxValue = abs;
                    maxArray[0] = maxValue;
                    maxArray[1] = i;
                    maxArray[2] = j;
                }
            }
        }
        return maxArray;
    }

    /**
     * Gets the minimum value in a matrix.
     *
     * @param m the matrix
     * @return the minimum value in m.
     */
    public static double getMin(Matrix m) {
        int numRows = m.getRowDimension();
        int numCols = m.getColumnDimension();

        // compute the min of the matrix
        double minValue = Double.MAX_VALUE;
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numCols; j++) {
                minValue = Math.min(minValue, m.get(i, j));
            }
        }
        return minValue;
    }

    /**
     * Gets the minimum value in a matrix, in the absolute sense.
     *
     * @param m the matrix
     * @return the minimum value in m.
     */
    public static double getAbsMin(Matrix m) {
        int numRows = m.getRowDimension();
        int numCols = m.getColumnDimension();

        // compute the min of the matrix
        double minValue = Double.MAX_VALUE;
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numCols; j++) {
                minValue = Math.min(minValue, Math.abs(m.get(i, j)));
            }
        }
        return minValue;
    }


    /**
     * Make a matrix of ones.
     *
     * @param numRows the number of rows.
     * @param numCols the number of columns.
     * @return the numRows x numCols matrix of ones.
     */
    public static Matrix ones(int numRows, int numCols) {
        return new Matrix(numRows, numCols, 1);
    }


    /**
     * Performs least squares regression using Tikhonov regularization.
     * Solves the problem Ax = b for x using regularization:
     * min || Ax - b ||^2 - lambda^2 || x ||^2 ,
     * which can be solved by
     * x = inv(A' * A + lambda^2 * I) * A' * b;
     * <p/>
     * Uses the identity matrix as the regularization operator.
     *
     * @param A      the data matrix (n x m).
     * @param b      the target function values (n x 1).
     * @param lambda the lambda values.  If less than zero, it is estimated
     *               using generalized cross-validation.
     */
    public static Matrix regLeastSquares(Matrix A, Matrix b, double lambda) {
        int m = A.getColumnDimension();
        Matrix regop = Matrix.identity(m, m).times(Math.pow(lambda, 2));
        return regLeastSquares(A, b, regop);
    }

    /**
     * Performs least squares regression using Tikhonov regularization.
     * Solves the problem Ax = b for x using regularization:
     * min || Ax - b ||^2 - || \sqrt(regop) x ||^2 ,
     * which can be solved by
     * x = inv(A' * A + regop) * A' * b;
     *
     * @param A     the data matrix (n x m).
     * @param b     the target function values (n x 1).
     * @param regop the regularization operator (m x m). The default is to use the identity matrix
     *              as the regularization operator, so you probably don't want to use this
     *              verion of regLeastSquares() without a really good reason.  Use
     *              regLeastSquares(Matrix A, Matrix b, double lambda) instead.
     */
    public static Matrix regLeastSquares(Matrix A, Matrix b, Matrix regop) {

        int m = A.getColumnDimension();
        int n = b.getRowDimension();

        // error check A and b
        if (A.getRowDimension() != n) {
            throw new IllegalArgumentException("A and b are incompatible sizes.");
        }

        // error check A and regop
        if (regop.getRowDimension() != m || regop.getColumnDimension() != m) {
            throw new IllegalArgumentException("A and regop are incompatible sizes.");
        }

        // solve the equation
        // x = inv(A' * A + regop) * A' * b;
        Matrix x = (A.transpose().times(A).plus(regop)).inverse().times(A.transpose()).times(b);
        return x;
    }

    /**
     * Computes the root mean squared error of two matrices
     *
     * @param a
     * @param b
     * @return the RMSE of a and b
     */
    public static double rmse(Matrix a, Matrix b) {
        Matrix difference = a.minus(b);
        double rmse = Math.sqrt(JamaUtils.sum(difference.transpose().times(difference)));
        return rmse;
    }


    public static Matrix loadSparseMatrix(File file) {
        FileReader fileReader = null;
        try {

            fileReader = new FileReader(file);
            BufferedReader reader = new BufferedReader(fileReader);
            int lineNumber = 0;
            String line = null;
            String[] split = null;
            int rows = -1, cols = -1;

            // read the matrix size
            while ((line = reader.readLine()) != null) {
                lineNumber++;

                // skip lines that don't start with a number
                if (!line.matches("^\\d+?.*"))
                    continue;

                split = line.split("[\\s,;]");

                if (split.length != 2) {
                    throw new IllegalArgumentException("Invalid matrix file format:  file must start with the size of the matrix.  Error on line number " + lineNumber + ".");
                }

                rows = Integer.parseInt(split[0]);
                cols = Integer.parseInt(split[1]);
                break;
            }

            Matrix matrix = new Matrix(rows, cols);
            int row = 0;

            // read each line of the matrix, skipping non-matrix rows
            while ((line = reader.readLine()) != null) {
                lineNumber++;

                // skip lines that don't start with a number
                if (!line.matches("^\\d+?.*"))
                    continue;

                split = line.split("[\\s,;]");
                // detect a full matrix specification
                if (split.length == cols) {
                    for (int col = 0; col < cols; col++) {
                        matrix.set(row, col, Double.parseDouble(split[col]));
                    }
                } else if (split.length == 3) {
                    matrix.set(Integer.parseInt(split[0]),
                            Integer.parseInt(split[1]),
                            Double.parseDouble(split[2]));
                } else {
                    throw new IllegalArgumentException("Invalid matrix file format:  must be either a full or sparse specification.  Error on line number " + lineNumber + ".");
                }
                row++;
            }

            return matrix;

        } catch (IOException e) {
            System.err.println("Invalid file:  " + file.getAbsolutePath());
        } finally {
            try {
                fileReader.close();
            } catch (Exception e) {
            }
        }

        return null;
    }

}
