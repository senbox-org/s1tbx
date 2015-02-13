/*
 * Copyright (C) 2015 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.binning.reader;

import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.Arrays;

class VariableReader {

    private final Variable binVariable;
    private final int[] origin;
    private final int[] shape;
    private final int binDimIndex;

    VariableReader(Variable binVariable) {
        this.binVariable = binVariable;
        this.origin = null;
        this.shape = null;
        this.binDimIndex = 0;
    }

    VariableReader(Variable binVariable, int[] origin, int[] shape, int binDimIndex) {
        this.binVariable = binVariable;
        this.origin = origin;
        this.shape = shape;
        this.binDimIndex = binDimIndex;
    }

    Variable getBinVariable() {
        return binVariable;
    }

    Array readFully() throws IOException {
        if (origin == null) {
            return binVariable.read();
        } else {
            try {
                return binVariable.read(origin, shape).reduce();
            } catch (InvalidRangeException e) {
                throw new IOException(e.getMessage());
            }
        }
    }

    Array read(int firstIndex, int length) throws IOException {
        try {
            if (origin == null) {
                return binVariable.read(new int[]{firstIndex}, new int[]{length});
            } else {
                int[] originFull = origin.clone();
                int[] shapeFull = shape.clone();
                originFull[binDimIndex] = firstIndex;
                shapeFull[binDimIndex] = length;
                return binVariable.read(originFull, shapeFull).reduce();
            }
        } catch (InvalidRangeException e) {
            throw new IOException(e.getMessage());
        }
    }
}
