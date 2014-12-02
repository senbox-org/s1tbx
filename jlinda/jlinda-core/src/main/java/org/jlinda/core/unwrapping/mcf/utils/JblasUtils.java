package org.jlinda.core.unwrapping.mcf.utils;

import org.apache.commons.math3.util.FastMath;
import org.jblas.DoubleMatrix;

public class JblasUtils {
    public static DoubleMatrix getMatrixFromIdx(DoubleMatrix in, DoubleMatrix idx, int offset) {

        DoubleMatrix out = new DoubleMatrix(idx.rows, idx.columns);
        for (int i = 0; i < idx.length; i++) {
            out.put(i, in.get((int) (FastMath.round(idx.get(i))) - offset));
        }
        return out;
    }

    public static DoubleMatrix getMatrixFromIdx(DoubleMatrix in, DoubleMatrix idx) {
        DoubleMatrix out = new DoubleMatrix(idx.rows, idx.columns);
        for (int i = 0; i < idx.length; i++) {
            out.put(i, in.get((int) idx.get(i)));
        }
        return out;
    }

    public static DoubleMatrix getMatrixFromRange(int rowMin, int rowMax, int colMin, int colMax, DoubleMatrix in, int offset) {
        DoubleMatrix out = new DoubleMatrix(rowMax - rowMin + 1, colMax - colMin + 1);
        for (int i = rowMin - offset; i < rowMax; i++) {
            for (int j = colMin - offset; j < colMax; j++) {
                out.put(i, j, in.get(i, j));
            }
        }
        return out;
    }

    public static DoubleMatrix getMatrixFromRange(int rowMin, int rowMax, int colMin, int colMax, DoubleMatrix in) {
        DoubleMatrix out = new DoubleMatrix(rowMax - rowMin, colMax - colMin);
        for (int i = rowMin; i < rowMax; i++) {
            for (int j = colMin; j < colMax; j++) {
                out.put(i, j, in.get(i, j));
            }
        }
        return out;
    }

    public static DoubleMatrix setUpMatrixFromIdx(int nRows, int nCols, DoubleMatrix idxRow, DoubleMatrix idxCol) {
        return setUpMatrixFromIdx(nRows, nCols, idxRow, idxCol, 0, 1);
    }

    public static DoubleMatrix setUpMatrixFromIdx(int nRows, int nCols, DoubleMatrix idxRow, DoubleMatrix idxCol, int offset) {
        return setUpMatrixFromIdx(nRows, nCols, idxRow, idxCol, offset, 1);
    }

    public static DoubleMatrix setUpMatrixFromIdx(int nRows, int nCols, DoubleMatrix idxRow, DoubleMatrix idxCol, int offset, int value) {

        if (idxRow.length != idxCol.length) throw new IllegalArgumentException();

        DoubleMatrix out = new DoubleMatrix(nRows, nCols);
        for (int i = 0; i < idxRow.length; i++) {
            out.put((int) idxRow.get(i) - offset, (int) idxCol.get(i) - offset, value);
        }
        return out;
    }

    public static DoubleMatrix linspaceInt(int lower, int upper, int size) {
        DoubleMatrix result = new DoubleMatrix(size);
        for (int i = 0; i < size; i++) {
            double t = (double) i / (size - 1);
            result.put(i, FastMath.round(lower * (1 - t) + t * upper));
        }
        return result;
    }

    public static DoubleMatrix intRangeDoubleMatrix(int min, int max) {
        return linspaceInt(min, max, max - min + 1);
    }


    public static DoubleMatrix[] grid2D(DoubleMatrix x, DoubleMatrix y) {

        // assumes y and x are vectors
        if (!x.isVector() || !y.isVector()) {
            throw new IllegalArgumentException();
        }

        // make both input vectors 'laying' and 'standing' vectors
        if (!x.isColumnVector()) x = x.transpose();
        if (!y.isRowVector()) y = y.transpose();

        // allocate return array
        DoubleMatrix[] returnMatrixArray = new DoubleMatrix[2];

        // should work using repmat
        returnMatrixArray[0] = x.repmat(1, y.length);
        returnMatrixArray[1] = y.repmat(x.length, 1);

        return returnMatrixArray;
    }

    public static DoubleMatrix[] meshgrid(DoubleMatrix x, DoubleMatrix y) {
        DoubleMatrix[] tempArray = grid2D(y, x); // swapping x and y!
        DoubleMatrix[] output = new DoubleMatrix[2];
        output[0] = tempArray[1];
        output[1] = tempArray[0];
        return output;
    }

    public static int[] intRangeIntArray(int min, int max) {
        int numelems = max - min + 1;
        int[] out = new int[numelems];
        for (int i = 0; i < numelems; i++) {
            out[i] = min + i;
        }
        return out;
    }

    public static DoubleMatrix sub2ind(int nRows, DoubleMatrix rowMatrix, DoubleMatrix colMatrix, int offset) {
        return rowMatrix.add(colMatrix.sub(offset).mmul(nRows));
    }

    public static DoubleMatrix sub2ind(int nRows, DoubleMatrix rowMatrix, DoubleMatrix colMatrix) {
        return rowMatrix.add(colMatrix.mmul(nRows));
    }

    public static DoubleMatrix cumsum(DoubleMatrix input, int dimension) throws IllegalArgumentException {

        // override dimnsions if column or row vectors are parsed
        if (input.isVector()) {
            return input.cumulativeSum();
        }

        if (dimension == 1) {
            // column wise cumsum
            DoubleMatrix output = new DoubleMatrix(input.rows, input.columns);
            for (int i = 0; i < input.columns; i++) {
                output.putColumn(i, input.getColumn(i).cumulativeSum());
            }
            return output;
        } else if (dimension == 2) {
            // row-wise cumsum
            DoubleMatrix output = new DoubleMatrix(input.rows, input.columns);
            for (int i = 0; i < input.rows; i++) {
                output.putRow(i, input.getRow(i).cumulativeSum());
            }
            return output;
        } else {
            throw new IllegalArgumentException();
        }

    }

}
