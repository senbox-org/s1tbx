/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.core.util.math;

/**
 * The class <code>FXYSum</code> represents a sum of function terms <i>sum(c[i] * f[i](x,y), i=0, n-1)</i>
 * where the vector <i>c</i> is a <code>double</code> array of constant coefficients and the vector <i>f</i>
 * is an array of functions of type <code>{@link FXY}</code> in <i>x</i> and <i>y</i>.
 * <p>
 * The vector <i>c</i> of constants is set by the {@link FXYSum#approximate(double[][], int[])} method.
 * <p>
 * After vector <i>c</i> is set, the actual function values <i>z(x,y)</i> are retrieved using the
 * {@link FXYSum#computeZ(double, double)} method.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public class FXYSum {

    /**
     * Creates a {@link FXYSum} by the given order and coefficients.
     * <p><b>Note: </b>
     * This factory method supprots only the creation of instances of the following FXYSum classes:
     * <ul>
     * <li>{@link Linear} - order = 1 ; number of coefficients = 3</li>
     * <li>{@link BiLinear} - order = 2 ; number of coefficients = 4</li>
     * <li>{@link Quadric} - order = 2 ; number of coefficients = 6</li>
     * <li>{@link BiQuadric} - order = 4 ; number of coefficients = 9</li>
     * <li>{@link Cubic} - order = 3 ; number of coefficients = 10</li>
     * <li>{@link BiCubic} - order = 6 ; number of coefficients = 16</li>
     * </ul>
     *
     *
     * @param order        the order of the sum
     * @param coefficients the coefficients
     *
     * @return returns a FXYSum instance, or <code>null</code> if the resulting instance is one of the supported.
     */
    public static FXYSum createFXYSum(final int order, final double[] coefficients) {
        final FXYSum sum;
        switch (order) {
        case 1:
            if (coefficients.length == 3) {
                sum = new Linear(coefficients);
            } else {
                sum = null;
            }
            break;
        case 2:
            if (coefficients.length == 4) {
                sum = new BiLinear(coefficients);
            } else if (coefficients.length == 6) {
                sum = new Quadric(coefficients);
            } else {
                sum = null;
            }
            break;
        case 3:
            if (coefficients.length == 10) {
                sum = new Cubic(coefficients);
            } else {
                sum = null;
            }
            break;
        case 4:
            if (coefficients.length == 9) {
                sum = new BiQuadric(coefficients);
            } else {
                sum = null;
            }
            break;
        case 6:
            if (coefficients.length == 16) {
                sum = new BiCubic(coefficients);
            } else {
                sum = null;
            }
            break;
        default:
            sum = null;
            break;
        }
        return sum;
    }

    /**
     * Creates a copy of the given {@link FXYSum fxySum}.
     *
     * @param fxySum the {@link FXYSum} to copy
     *
     * @return a copy of the given {@link FXYSum}
     */
    public static FXYSum createCopy(final FXYSum fxySum) {
        final double[] coefficients = new double[fxySum.getCoefficients().length];
        System.arraycopy(fxySum.getCoefficients(), 0, coefficients, 0, coefficients.length);
        final FXYSum fxySumCopy = new FXYSum(fxySum.getFunctions(), fxySum.getOrder(), coefficients);
        return fxySumCopy;
    }

    public static final FXY[] FXY_LINEAR = new FXY[]{
        /*0*/ FXY.ONE,
        /*1*/ FXY.X, FXY.Y,
    };

    public static final FXY[] FXY_BI_LINEAR = new FXY[]{
        /*0*/ FXY.ONE,
        /*1*/ FXY.X, FXY.Y,
        /*2*/ FXY.XY,
    };

    public static final FXY[] FXY_QUADRATIC = new FXY[]{
        /*0*/ FXY.ONE,
        /*1*/ FXY.X, FXY.Y,
        /*2*/ FXY.X2, FXY.XY, FXY.Y2,
    };

    public static final FXY[] FXY_BI_QUADRATIC = new FXY[]{
        /*0*/ FXY.ONE,
        /*1*/ FXY.X, FXY.Y,
        /*2*/ FXY.X2, FXY.XY, FXY.Y2,
        /*3*/ FXY.X2Y, FXY.XY2,
        /*4*/ FXY.X2Y2,
    };

    public static final FXY[] FXY_CUBIC = new FXY[]{
        /*0*/ FXY.ONE,
        /*1*/ FXY.X, FXY.Y,
        /*2*/ FXY.X2, FXY.XY, FXY.Y2,
        /*3*/ FXY.X3, FXY.X2Y, FXY.XY2, FXY.Y3,
    };

    public static final FXY[] FXY_BI_CUBIC = new FXY[]{
        /*0*/ FXY.ONE,
        /*1*/ FXY.X, FXY.Y,
        /*2*/ FXY.X2, FXY.XY, FXY.Y2,
        /*3*/ FXY.X3, FXY.X2Y, FXY.XY2, FXY.Y3,
        /*4*/ FXY.X3Y, FXY.X2Y2, FXY.XY3,
        /*5*/ FXY.X3Y2, FXY.X2Y3,
        /*6*/ FXY.X3Y3,
    };

    public static final FXY[] FXY_4TH = new FXY[]{
        /*0*/ FXY.ONE,
        /*1*/ FXY.X, FXY.Y,
        /*2*/ FXY.X2, FXY.XY, FXY.Y2,
        /*3*/ FXY.X3, FXY.X2Y, FXY.XY2, FXY.Y3,
        /*4*/ FXY.X4, FXY.X3Y, FXY.X2Y2, FXY.XY3, FXY.Y4,
    };

    public static final FXY[] FXY_BI_4TH = new FXY[]{
        /*0*/ FXY.ONE,
        /*1*/ FXY.X, FXY.Y,
        /*2*/ FXY.X2, FXY.XY, FXY.Y2,
        /*3*/ FXY.X3, FXY.X2Y, FXY.XY2, FXY.Y3,
        /*4*/ FXY.X4, FXY.X3Y, FXY.X2Y2, FXY.XY3, FXY.Y4,
        /*5*/ FXY.X4Y, FXY.X3Y2, FXY.X2Y3, FXY.XY4,
        /*6*/ FXY.X4Y2, FXY.X3Y3, FXY.X2Y4,
        /*7*/ FXY.X4Y3, FXY.X3Y4,
        /*8*/ FXY.X4Y4,
    };

    private final FXY[] _f;
    private final double[] _c;
    private final int _order;
    private double[] _errorStatistics;

    /**
     * Constructs a new sum of terms <i>sum(c[i] * f[i](x,y), i=0, n-1)</i> for the given vector of functions.
     * The vector <i>c</i> is initally set to zero and will remeain zero until the method {@link #approximate(double[][], int[])}
     * is performed with given data on this function sum.
     *
     * @param functions the vector F of functions
     */
    public FXYSum(FXY[] functions) {
        this(functions, -1);
    }

    /**
     * Constructs a new sum of terms <i>sum(c[i] * f[i](x,y), i=0, n-1)</i> for the given vector of functions and a polynomial order.
     * The vector <i>c</i> is initally set to zero and will remeain zero until the method {@link #approximate(double[][], int[])}
     * is performed with given data on this function sum.
     *
     * @param functions the vector F of functions
     * @param order     the polynomial order (for descriptive purpose only), -1 if unknown
     */
    public FXYSum(FXY[] functions, int order) {
        this(functions, order, null);
    }

    /**
     * Constructs a new sum of terms <i>sum(c[i] * f[i](x,y), i=0, n-1)</i> for the given vector of functions and a polynomial order.
     * The vector <i>c</i> is set by the <code>coefficients</code> parameter. The coefficients will be recalculated if
     * the method {@link #approximate(double[][], int[])} is called.
     *
     * @param functions    the vector F of functions
     * @param order        the polynomial order (for descriptive purpose only), -1 if unknown
     * @param coefficients the vector <i>c</i>
     */
    public FXYSum(final FXY[] functions, final int order, final double[] coefficients) {
        if (functions == null || functions.length == 0) {
            throw new IllegalArgumentException("'functions' is null or empty");
        }
        _f = functions;
        if (coefficients == null) {
            _c = new double[functions.length];
        } else {
            if (functions.length != coefficients.length) {
                throw new IllegalArgumentException("'functions.length' != 'coefficients.length'");
            }
            _c = coefficients;
        }
        _order = order;
    }

    /**
     * Gets the number <i>n</i> of terms <i>c[i] * f[i](x,y)</i>.
     *
     * @return the number of function terms
     */
    public final int getNumTerms() {
        return _f.length;
    }

    /**
     * Gets the vector <i>f</i> of functions elements <i>f[i](x,y)</i>.
     *
     * @return the vector F of functions
     */
    public final FXY[] getFunctions() {
        return _f;
    }

    /**
     * Gets the vector <i>c</i> of coefficient elements <i>c[i]</i>.
     *
     * @return the vector F of functions
     */
    public final double[] getCoefficients() {
        return _c;
    }

    /**
     * Gets the polynomial order, if any.
     *
     * @return the polynomial order or -1 if unknown
     */
    public int getOrder() {
        return _order;
    }


    /**
     * Gets the root mean square error.
     *
     * @return the root mean square error
     */
    public double getRootMeanSquareError() {
        return _errorStatistics[0];
    }

    /**
     * Gets the maximum, absolute error of the approximation.
     *
     * @return the maximum, absolute error
     */
    public double getMaxError() {
        return _errorStatistics[1];
    }

    /**
     * Computes this sum of function terms <i>z(x,y) = sum(c[i] * f[i](x,y), i=0, n-1)</i>.
     * The method will return zero unless the {@link #approximate(double[][], int[])} is called in order to set
     * the coefficient vector <i>c</i>.
     *
     * @param x the x value
     * @param y the y value
     *
     * @return the z value
     *
     * @see #computeZ(FXY[], double[], double, double)
     */
    public double computeZ(final double x, final double y) {
        return computeZ(_f, _c, x, y);
    }

    /**
     * Computes <i>z(x,y) = sum(c[i] * f[i](x,y), i = 0, n - 1)</i>.
     *
     * @param f the function vector
     * @param c the coeffcient vector
     * @param x the x value
     * @param y the y value
     *
     * @return the z value
     */
    public static double computeZ(final FXY[] f, double[] c, double x, double y) {
        final int n = f.length;
        double z = 0.0;
        for (int i = 0; i < n; i++) {
            z += c[i] * f[i].f(x, y);
        }
        return z;
    }

    /**
     * Approximates the given data points <i>x,y,z</i> by this sum of function terms so that <i>z ~ sum(c[i] * f[i](x,y), i=0, n-1)</i>.
     * The method also sets the error statistics which can then be retrieved by the {@link #getRootMeanSquareError()} and {@link #getMaxError()} methods.
     *
     * @param data    an array of values of the form <i>{{x1,y1,z1}, {x2,y2,z2}, {x3,y3,z3}, ...} </i>
     * @param indices an array of coordinate indices, determining the indices of <i>x</i>, <i>y</i> and <i>z</i> within a <code>data</code> element. If
     *                <code>null</code> then <code>indices</code> defaults to the array <code>{0, 1, 2}</code>.
     *
     * @see Approximator#approximateFXY(double[][], int[], FXY[], double[])
     * @see Approximator#computeErrorStatistics(double[][], int[], FXY[], double[])
     * @see #computeZ(double, double)
     */
    public void approximate(double[][] data, int[] indices) {
        Approximator.approximateFXY(data, indices, _f, _c);
        _errorStatistics = Approximator.computeErrorStatistics(data, indices, _f, _c);
    }

    /**
     * Returns this sum of function terms as human-readable C-code.
     *
     * @param fname the name of the function z(x,y) = sum(C[i]*F[i](x,y), i, n)
     * @param x     the name of the x variable
     * @param y     the name of the y variable
     *
     * @return the C-code
     */
    public String createCFunctionCode(final String fname, final String x, final String y) {
        final StringBuffer sb = new StringBuffer(256 + getNumTerms() * 10);
        appendCFunctionCodeStart(fname, x, y, sb);
        appendCFunctionCodeBody(fname, x, y, sb);
        appendCFunctionCodeEnd(fname, x, y, sb);
        return sb.toString();
    }

    protected void appendCFunctionCodeStart(final String fname, final String x, final String y, StringBuffer sb) {
        final double[] c = getCoefficients();
        sb.append("double " + fname + "(double " + x + ", double " + y + ") {\n");
        sb.append("    static double c[" + c.length + "] = {\n");
        for (int i = 0; i < c.length; i++) {
            sb.append("        ");
            sb.append(c[i]);
            sb.append((i < c.length - 1) ? ",\n" : "};\n");
        }
    }

    protected void appendCFunctionCodeBody(final String fname, final String x, final String y, StringBuffer sb) {
        sb.append("    return\n");
        final FXY[] funcs = getFunctions();
        for (int i = 0; i < funcs.length; i++) {
            FXY func = funcs[i];
            sb.append("    c[" + i + "] * ");
            appendCFunctionCodePart(sb, func.getCCodeExpr(), x, y);
            sb.append(i < funcs.length - 1 ? " +\n" : ";\n");
        }
    }

    protected void appendCFunctionCodeEnd(final String fname, final String x, final String y, StringBuffer sb) {
        sb.append("}\n");
    }

    protected void appendCFunctionCodePart(StringBuffer sb, final String part, final String x, final String y) {
        sb.append(part.replaceAll("x", x).replaceAll("y", y));
    }

    /**
     * Provides an optimized <code>computeZ</code> method for linear polynomials (order = 1).
     *
     * @see #FXY_LINEAR
     */
    public final static class Linear extends FXYSum {

        public Linear() {
            super(FXY_LINEAR, 1);
        }

        public Linear(final double[] coefficients) {
            super(FXY_LINEAR, 1, coefficients);
        }

        @Override
        public double computeZ(final double x, final double y) {
            final double[] c = getCoefficients();
            return c[0] +
                   c[1] * x + c[2] * y;
        }
    }

    /**
     * Provides an optimized <code>computeZ</code> method for bi-linear polynomials (order = 1+1).
     *
     * @see #FXY_BI_LINEAR
     */
    public final static class BiLinear extends FXYSum {

        public BiLinear() {
            super(FXY_BI_LINEAR, 1 + 1);
        }

        public BiLinear(final double[] coefficients) {
            super(FXY_BI_LINEAR, 1 + 1, coefficients);
        }

        @Override
        public double computeZ(final double x, final double y) {
            final double[] c = getCoefficients();
            return c[0] +
                   (c[1] +
                    c[3] * y) * x +
                   c[2] * y;
        }
    }

    /**
     * Provides an optimized <code>computeZ</code> method for quadric polynomials (order = 2).
     *
     * @see #FXY_QUADRATIC
     */
    public final static class Quadric extends FXYSum {

        public Quadric() {
            super(FXY_QUADRATIC, 2);
        }

        public Quadric(final double[] coefficients) {
            super(FXY_QUADRATIC, 2, coefficients);
        }

        @Override
        public double computeZ(final double x, final double y) {
            final double[] c = getCoefficients();
            return c[0] +
                   (c[1] +
                    c[3] * x +
                    c[4] * y) * x +
                   (c[2] + c[5] * y) * y;
        }
    }


    /**
     * Provides an optimized <code>computeZ</code> method for bi-quadric polynomials (order = 2+2).
     *
     * @see #FXY_BI_QUADRATIC
     */
    public final static class BiQuadric extends FXYSum {

        public BiQuadric() {
            super(FXY_BI_QUADRATIC, 2 + 2);
        }

        public BiQuadric(final double[] coefficients) {
            super(FXY_BI_QUADRATIC, 2 + 2, coefficients);
        }

        @Override
        public double computeZ(final double x, final double y) {
            final double[] c = getCoefficients();
            return c[0] +
                   (c[1] +
                    (c[3] +
                     (c[6] + c[8] * y) * y) * x +
                    (c[4] + c[7] * y) * y) * x +
                   (c[2] + c[5] * y) * y;
        }
    }

    /**
     * Provides an optimized <code>computeZ</code> method for cubic polynomials (order = 3).
     *
     * @see #FXY_CUBIC
     */
    public final static class Cubic extends FXYSum {

        public Cubic() {
            super(FXY_CUBIC, 3);
        }

        public Cubic(final double[] coefficients) {
            super(FXY_CUBIC, 3, coefficients);
        }

        @Override
        public double computeZ(final double x, final double y) {
            final double[] c = getCoefficients();
            return c[0] +
                   (c[1] +
                    (c[3] +
                     c[6] * x +
                     c[7] * y) * x +
                    (c[4] + c[8] * y) * y) * x +
                   (c[2] + (c[5] + c[9] * y) * y) * y;
        }
    }

    /**
     * Provides an optimized <code>computeZ</code> method for bi-cubic polynomials (order = 3+3).
     *
     * @see #FXY_BI_CUBIC
     */
    public final static class BiCubic extends FXYSum {

        public BiCubic() {
            super(FXY_BI_CUBIC, 3 + 3);
        }

        public BiCubic(final double[] coefficients) {
            super(FXY_BI_CUBIC, 3 + 3, coefficients);
        }


        @Override
        public double computeZ(final double x, final double y) {
            final double[] c = getCoefficients();
            return c[0] +
                   (c[1] +
                    (c[3] +
                     (c[6] +
                      (c[10] + (c[13] + c[15] * y) * y) * y) * x +
                     (c[7] + (c[11] + c[14] * y) * y) * y) * x +
                    (c[4] + (c[8] + c[12] * y) * y) * y) * x +
                   (c[2] + (c[5] + c[9] * y) * y) * y;
        }

//        protected void appendCFunctionCodeBody(final String fname, final String x, final String y, StringBuffer sb) {
//            String s =
//                    "    return  x * (x * (x * (y * (y * (y * c[0] + c[1])  + c[3])  + c[6]) + \n" +
//                    "                          (y * (y * (y * c[2] + c[4])  + c[7])  + c[10])) + \n" +
//                    "                          (y * (y * (y * c[5] + c[8])  + c[11]) + c[13])) + \n" +
//                    "                           y * (y * (y * c[9] + c[12]) + c[14]) + c[15];\n";
//            appendCFunctionCodePart(sb, s, x, y);
//        }
    }
}
