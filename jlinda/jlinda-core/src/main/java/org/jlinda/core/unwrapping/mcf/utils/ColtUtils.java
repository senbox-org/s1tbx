package org.jlinda.core.unwrapping.mcf.utils;

import cern.colt.function.DoubleDoubleFunction;
import cern.colt.function.DoubleFunction;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;
import org.jblas.DoubleMatrix;

public class ColtUtils {
    
    public static SparseDoubleMatrix2D setUpSparseMatrixFromIdx(int nRows, int nCols, DoubleMatrix idxRow, DoubleMatrix idxCol) {
        return setUpSparseMatrixFromIdx(nRows, nCols, idxRow, idxCol, 0, 1);
    }
    
    
    public static SparseDoubleMatrix2D setUpSparseMatrixFromIdx(int nRows, int nCols, DoubleMatrix idxRow, DoubleMatrix idxCol, int offset, int value) {

        if (idxRow.length != idxCol.length) throw new IllegalArgumentException();

        SparseDoubleMatrix2D out = new SparseDoubleMatrix2D(nRows, nCols);
        for (int i = 0; i < idxRow.length; i++) {
            out.setQuick((int) idxRow.get(i) - offset, (int) idxCol.get(i) - offset, value);
        }
        return out;
    }
    
    public static DoubleDoubleFunction add = new DoubleDoubleFunction() {
        public double apply(double a, double b) {
            return a + b;
        }
    };

    public static DoubleDoubleFunction subtract = new DoubleDoubleFunction() {
        public double apply(double a, double b) {
            return a - b;
        }
    };

    public static DoubleFunction muli = new DoubleFunction() {
        public double apply(double a) {
            return (-1) * a;
        }
    };
    
    
}
