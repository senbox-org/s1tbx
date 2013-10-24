package org.jlinda.core.unwrapping.mcf.utils;

import org.jblas.DoubleMatrix;

import static org.jlinda.core.Constants._PI;
import static org.jlinda.core.Constants._TWO_PI;

/**
 * Description: Utility code used in unwrapping. Code is mainly moved from around into this class.
 */
public class UnwrapUtils {

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

    public static void iWrapDoubleMatrix(DoubleMatrix phase) {
        for (int i = 0; i < phase.length; i++) {
            phase.put(i, wrapPosNegPI(phase.get(i)));
        }
    }

    public static DoubleMatrix wrapDoubleMatrix(DoubleMatrix input) {
        DoubleMatrix output = input.dup();
        for (int i = 0; i < input.length; i++) {
            output.put(i, wrapPosNegPI(input.get(i)));
        }
        return output;
    }

    // Floating-point modulo
    // The result (the remainder) has same sign as the divisor.
    // Similar to matlab's mod();
    // NOT(!!!) similar to math.h in c: fmod() => Mod(-3,4)=1  | fmod(-3,4)=-3
    // Based on: http://stackoverflow.com/a/4635752/320279
    public static double mod(double x, double y) {

        if (0. == y)
            return x;

        double m = x - y * Math.floor(x / y);

        // handle boundary cases resulted from floating-point cut off:
        if (y > 0) {              // modulo range: [0..y)
            if (m >= y)           // Mod(-1e-16             , 360.    ): m= 360.
                return 0;
            if (m < 0) {
                if (y + m == y)
                    return 0;     // just in case...
                else
                    return y + m; // Mod(106.81415022205296 , _TWO_PI ): m= -1.421e-14
            }
        } else {                  // modulo range: (y..0]
            if (m <= y)           // Mod(1e-16              , -360.   ): m= -360.
                return 0;
            if (m > 0) {
                if (y + m == y)
                    return 0;     // just in case...
                else
                    return y + m; // Mod(-106.81415022205296, -_TWO_PI): m= 1.421e-14
            }
        }

        return m;
    }

    // wrap [rad] angle to [-PI..PI)
    private static double wrapPosNegPI(double fAng) {
        return mod(fAng + _PI, _TWO_PI) - _PI;
    }

    // wrap [rad] angle to [0..TWO_PI)
    private static double wrapTwoPI(double fAng) {
        return mod(fAng, _TWO_PI);
    }

    // wrap [deg] angle to [-180..180)
    private static double wrapPosNeg180(double fAng) {
        return mod(fAng + 180., 360.) - 180.;
    }

    // wrap [deg] angle to [0..360)
    private static double wrap360(double fAng) {
        return mod(fAng, 360.);
    }


    private static int[] linspaceInt(int min, int max, int numelems, int offset) {
        int[] out = new int[numelems];
        for (int i = min; i < max; i++) {
            out[i] = (min - offset) + i;
        }
        return out;
    }

    public static int[] linspaceInt(int min, int max, int numelems) {
        return linspaceInt(min, max, numelems, 1); // starts at zero!
    }

    public static DoubleMatrix sub2ind(int nRows, int nCols, DoubleMatrix rowMatrix, DoubleMatrix colMatrix) {
        return rowMatrix.add(colMatrix.sub(1).mmul(nRows));
    }

}
