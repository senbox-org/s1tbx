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
 * Represents a function <i>f(x,y)</i>.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public interface FXY {

    /**
     * The function <i>f(x,y) = x <sup>4</sup> y <sup>4</sup></i>
     */
    FXY X4Y4 = new Functions.FXY_X4Y4();
    /**
     * The function <i>f(x,y) = x <sup>4</sup> y <sup>3</sup></i>
     */
    FXY X4Y3 = new Functions.FXY_X4Y3();
    /**
     * The function <i>f(x,y) = x <sup>3</sup> y <sup>4</sup></i>
     */
    FXY X3Y4 = new Functions.FXY_X3Y4();
    /**
     * The function <i>f(x,y) = x <sup>4</sup> y <sup>2</sup></i>
     */
    FXY X4Y2 = new Functions.FXY_X4Y2();
    /**
     * The function <i>f(x,y) = x <sup>2</sup> y <sup>4</sup></i>
     */
    FXY X2Y4 = new Functions.FXY_X2Y4();
    /**
     * The function <i>f(x,y) = x <sup>4</sup> y </i>
     */
    FXY X4Y = new Functions.FXY_X4Y();
    /**
     * The function <i>f(x,y) = x y <sup>4</sup></i>
     */
    FXY XY4 = new Functions.FXY_XY4();
    /**
     * The function <i>f(x,y) = x <sup>4</sup> </i>
     */
    FXY X4 = new Functions.FXY_X4();
    /**
     * The function <i>f(x,y) = y <sup>4</sup></i>
     */
    FXY Y4 = new Functions.FXY_Y4();
    /**
     * The function <i>f(x,y) = x <sup>3</sup> y <sup>3</sup></i>
     */
    FXY X3Y3 = new Functions.FXY_X3Y3();
    /**
     * The function <i>f(x,y) = x <sup>3</sup> y <sup>2</sup></i>
     */
    FXY X3Y2 = new Functions.FXY_X3Y2();
    /**
     * The function <i>f(x,y) = x <sup>2</sup> y <sup>3</sup></i>
     */
    FXY X2Y3 = new Functions.FXY_X2Y3();
    /**
     * The function <i>f(x,y) = x <sup>3</sup> y</i>
     */
    FXY X3Y = new Functions.FXY_X3Y();
    /**
     * The function <i>f(x,y) = x <sup>2</sup> y <sup>2</sup></i>
     */
    FXY X2Y2 = new Functions.FXY_X2Y2();
    /**
     * The function <i>f(x,y) = x y <sup>3</sup></i>
     */
    FXY XY3 = new Functions.FXY_XY3();
    /**
     * The function <i>f(x,y) = x <sup>3</sup></i>
     */
    FXY X3 = new Functions.FXY_X3();
    /**
     * The function <i>f(x,y) = x <sup>2</sup> y</i>
     */
    FXY X2Y = new Functions.FXY_X2Y();
    /**
     * The function <i>f(x,y) = x y <sup>2</sup></i>
     */
    FXY XY2 = new Functions.FXY_XY2();
    /**
     * The function <i>f(x,y) = y <sup>3</sup></i>
     */
    FXY Y3 = new Functions.FXY_Y3();
    /**
     * The function <i>f(x,y) = x <sup>2</sup></i>
     */
    FXY X2 = new Functions.FXY_X2();
    /**
     * The function <i>f(x,y) = x y</i>
     */
    FXY XY = new Functions.FXY_XY();
    /**
     * The function <i>f(x,y) = y <sup>2</sup></i>
     */
    FXY Y2 = new Functions.FXY_Y2();
    /**
     * The function <i>f(x,y) = x</i>
     */
    FXY X = new Functions.FXY_X();
    /**
     * The function <i>f(x,y) = y</i>
     */
    FXY Y = new Functions.FXY_Y();
    /**
     * The function <i>f(x,y) = 1</i>
     */
    FXY ONE = new Functions.FXY_1();

    /**
     * The function <i>z = f(x,y)</i>
     *
     * @param x the x parameter
     * @param y the y parameter
     *
     * @return z
     */
    double f(double x, double y);

    /**
     * Returns the function as C code expression, e.g. <code>"pow(x, 3) * y"</code>
     * @return the C code
     */
    String getCCodeExpr();
}
