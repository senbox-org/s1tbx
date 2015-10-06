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
 * Represents a function <i>f(x)</i>.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public interface FX {

    /**
     * The function <i>f(x) = x <sup>4</sup></i>
     */
    FX XXXX = new Functions.FX_X4();
    /**
     * The function <i>f(x) = x <sup>3</sup></i>
     */
    FX XXX = new Functions.FX_X3();
    /**
     * The function <i>f(x) = x <sup>2</sup></i>
     */
    FX XX = new Functions.FX_X2();
    /**
     * The function <i>f(x) = x</i>
     */
    FX X = new Functions.FX_X();
    /**
     * The function <i>f(x) = 1</i>
     */
    FX ONE = new Functions.FX_1();

    /**
     * The function <i>y = f(x)</i>
     *
     * @param x the x parameter
     *
     * @return y
     */
    double f(double x);

    /**
     * Returns the function as C code expression, e.g. <code>"pow(x, 3) * y"</code>
     * @return the C code
     */
    String getCCodeExpr();
}
