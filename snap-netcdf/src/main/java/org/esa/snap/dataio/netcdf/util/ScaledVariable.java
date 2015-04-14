/*
 * Copyright (C) 2013 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.dataio.netcdf.util;

import ucar.nc2.Variable;

/**
 * Wraps a netcdf variable and a scaling factor.
 */
public class ScaledVariable {
    private final float scaleFactor;
    private final Variable variable;

    public ScaledVariable(float scaleFactor, Variable variable) {
        this.scaleFactor = scaleFactor;
        this.variable = variable;
    }

    public float getScaleFactor() {
        return scaleFactor;
    }

    public Variable getVariable() {
        return variable;
    }

    @Override
    public String toString() {
        return "ScaledVariable{" +
                "scaleFactor=" + scaleFactor +
                ", variable=" + variable.getFullName() +
                '}';
    }
}
