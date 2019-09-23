package org.csa.rstb.polarimetric.gpf;

public interface MatrixMath {

    default void matrixPlusEquals(final double[][] array1, final double[][] array2) {
        for(int i = 0; i < array1.length; ++i) {
            for(int j = 0; j < array1[0].length; ++j) {
                array1[i][j] += array2[i][j];
            }
        }
    }

    default void matrixTimesEquals(final double[][] array1, double var1) {
        for(int i = 0; i < array1.length; ++i) {
            for(int j = 0; j < array1[0].length; ++j) {
                array1[i][j] *= var1;
            }
        }
    }
}
