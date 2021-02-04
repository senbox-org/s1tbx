package org.csa.rstb.polarimetric.gpf.support;

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
	
	default void matrixtranspose(final double[][] array1, final double[][] array2) {
		for (int i = 0; i < array1.length; ++i) {
            for (int j = 0; j < array1.length; ++j) {
                array2[i][j] = array1[j][i]; 
			}
		}
    }

	default void matrixmultiply(final double[][] array1, final double[][] array2, final double[][] array3)
    {
        int i, j, k;
        for (i = 0; i < array1.length; i++) {
            for (j = 0; j < array1.length; j++) {
                array3[i][j] = 0;
                for (k = 0; k < array1.length; k++){
                    array3[i][j] += array1[i][k] * array2[k][j];
					}
            }
        }
    }



	
}
