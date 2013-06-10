package org.jlinda.core.utils;

import org.jblas.DoubleMatrix;
import org.jlinda.core.Constants;
import org.jlinda.core.Window;

import java.util.Random;

public class MathUtils {

    //    static Logger logger = Logger.getLogger(MathUtils.class.getName());
    public static boolean isEven(long value) {
        return value % 2 == 0;
    }

    public static boolean isOdd(long value) {
        return !isEven(value);
    }

    public static boolean isPower2(long value) {
        return value == 1 || value == 2 || value == 4 || value == 8 || value == 16 ||
                value == 32 || value == 64 || value == 128 || value == 256 ||
                value == 512 || value == 1024 || value == 2048 || value == 4096;
    }

    public static double rad2deg(double valueInRadians) {
//        return Math.toDegrees(rad);
        return valueInRadians * Constants.RTOD;
    }

    public static double deg2rad(double valueInDegrees) {
//        return Math.toDegrees(rad);
        return valueInDegrees * Constants.DTOR;
    }

    public static int[][] distributePoints(final int numOfPoints, final Window window) {

        final float lines = window.lines();
        final float pixels = window.pixels();

        int[][] result = new int[numOfPoints][2];

        // Distribution for dl=dp
        float winP = (float) Math.sqrt(numOfPoints / (lines / pixels));   // wl: #windows in line direction
        float winL = numOfPoints / winP;                     // wp: #windows in pixel direction
        if (winL < winP) {
            // switch wl,wp : later back
            winL = winP;
        }

        final int winL_int = (int) Math.floor(winL); // round
        final float deltaLin = (lines - 1) / (float) (winL_int - 1);
        final int totalPix = (int) Math.floor(pixels * winL_int);
        final float deltaPix = (float) (totalPix - 1) / (float) (numOfPoints - 1);

        float pix = -deltaPix;
        float lin;
        int lCounter = 0;
        for (int i = 0; i < numOfPoints; i++) {
            pix += deltaPix;
            while (Math.floor(pix) >= pixels) {
                pix -= pixels;
                lCounter++;
            }
            lin = lCounter * deltaLin;

            // also correct distribution to window
            result[i][0] = (int) (Math.floor(lin) + window.linelo);
            result[i][1] = (int) (Math.floor(pix) + window.pixlo);
        }
        return result;
    }

    // 1D increment
    public static double[] increment(int m, double begin, double pitch) {
        double[] array = new double[m];
        for (int i = 0; i < m; i++) {
            array[i] = begin + i * pitch;
        }
        return array;
    }

    // 2D increment
    public static double[][] increment(int m, int n, double begin, double pitch) {
        double[][] array = new double[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                array[i][j] = begin + i * pitch;
            }
        }
        return array;
    }

    // TODO: move to simulation package
    // generates unit ramp (left to right)
    public static DoubleMatrix ramp(final int nRows, final int nColumns) {
        // Ramp generation (to easy for function?)
        final double maxHeight = 1;
        return DoubleMatrix.ones(nRows, 1).mmul(lying(new DoubleMatrix(increment(nColumns, 0, maxHeight / (nColumns - 1)))));
    }

    // lying vector (vectorize)
    public static DoubleMatrix lying(DoubleMatrix inMatrix) {
        return new DoubleMatrix(inMatrix.toArray()).transpose();
    }

    public static int randomIntInRange(int min, int max) {
        Random rand = new Random();
        return rand.nextInt(max - min + 1) + min;
    }

    /// only for legacy support ///
    @Deprecated
    public static double sqrt(double value) {
        return Math.sqrt(value);
    }

    @Deprecated
    public static double sqr(double value) {
        return Math.pow(value, 2);
    }

}

