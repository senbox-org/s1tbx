package org.jlinda.core.coregistration.utils;

import org.ejml.data.DMatrixRMaj;
import org.ejml.data.DMatrix1Row;

public class CPMUtils {

    /**
     * C=diagxmat(vec,B) C=diag(vec) * B
     */
    public static DMatrixRMaj diagxmat(final DMatrix1Row diag, final DMatrix1Row B) {

        //if (!MatrixFeatures.isVector(diag))
            //logger.info("diagXMat: sizes A,B: diag is NOT vector.");

        DMatrixRMaj result = B.copy();
        for (int i = 0; i < result.numRows; i++) {
            for (int j = 0; j < result.numCols; j++) {
                result.unsafe_set(i, j, result.unsafe_get(i, j) * diag.get(i));
            }
        }
        return result;
    } // END diagxmat

    public static void scaleInputDiag(final DMatrix1Row matrix, final DMatrix1Row diag) {
        for (int i = 0; i < matrix.numRows; i++) {
            matrix.unsafe_set(i, i, matrix.unsafe_get(i, i) + 1 / diag.get(i));
        }
    }

    public static DMatrix1Row onesEJML(int rows, int columns) {
        DMatrixRMaj m = new DMatrixRMaj(rows, columns);
        for (int i = 0; i < rows * columns; i++) {
            m.set(i, 1);
        }
        return m;
    }

    public static DMatrix1Row onesEJML(int length) {
        return onesEJML(length, 1);
    }

    /**
     * Returns the linear index of the maximal element of the abs()
     * matrix. If there are more than one elements with this value,
     * the first one is returned.
     */
    public static int absArgmax(DMatrix1Row matrix) {
        int numElements = matrix.getNumElements();
        if (numElements == 0) {
            return -1;
        }
        double v = Double.NEGATIVE_INFINITY;
        int a = -1;
        for (int i = 0; i < numElements; i++) {
            double abs = Math.abs(matrix.get(i));
            if (!Double.isNaN(abs) && abs > v) {
                v = abs;
                a = i;
            }
        }

        return a;
    }
}
