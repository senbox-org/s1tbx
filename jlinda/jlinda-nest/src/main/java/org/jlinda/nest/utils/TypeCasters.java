package org.jlinda.nest.utils;

import org.jblas.ComplexDoubleMatrix;
import org.jblas.ComplexFloatMatrix;
import org.jblas.DoubleMatrix;
import org.jblas.FloatMatrix;

/**
* User: pmar@ppolabs.com
* Date: 6/20/11
* Time: 11:20 PM
*/
public class TypeCasters {

    public static FloatMatrix toFloat(final DoubleMatrix data) {
        return data.toFloat();
    }

    public static ComplexFloatMatrix toFloat(final ComplexDoubleMatrix data) {
        return new ComplexFloatMatrix(data.real().toFloat(), data.imag().toFloat());
    }

    public static DoubleMatrix toDouble(final FloatMatrix data) {
        DoubleMatrix result = new DoubleMatrix(data.rows, data.columns);
        for (int i = 0; i < data.length; i++) {
            result.put(i, (double) data.get(i));
        }
        return result;
    }

    public static ComplexDoubleMatrix toDouble(final ComplexFloatMatrix data) {
        return new ComplexDoubleMatrix(toDouble(data.real()), toDouble(data.imag()));
    }

    public static float[] toFloat(final double[] data) {
        float[] result = new float[data.length];
        for (int i = 0; i < data.length; i++) {
            result[i] = (float) data[i];
        }
        return result;
    }

    public static float[][] toFloat(final double[][] data) {
        float[][] result = new float[data.length][data[0].length];
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[0].length; j++) {
                result[i][j] = (float) data[i][j];
            }
        }
        return result;
    }

    public static double[] toDouble(final float[] data) {
        double[] result = new double[data.length];
        for (int i = 0; i < data.length; i++) {
            result[i] = (double) data[i];
        }
        return result;
    }

    public static double[][] toDouble(final float[][] data) {
        double[][] result = new double[data.length][data[0].length];
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[0].length; j++) {
                result[i][j] = (double) data[i][j];
            }
        }
        return result;
    }

}
