/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.dataio.netcdf.nc;

import org.esa.snap.core.datamodel.ProductData;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

import java.io.IOException;

/**
 * A wrapper around the netCDF 3 {@link ucar.nc2.Variable}.
 *
 * @author MarcoZ
 */
public class N3Variable implements NVariable {

    private final Variable variable;
    private final NetcdfFileWriteable netcdfFileWriteable;

    public N3Variable(Variable variable, NetcdfFileWriteable netcdfFileWriteable) {
        this.variable = variable;
        this.netcdfFileWriteable = netcdfFileWriteable;
    }

    @Override
    public String getName() {
        return variable.getFullName();
    }

    @Override
    public DataType getDataType() {
        return variable.getDataType();
    }

    @Override
    public void addAttribute(String name, String value) {
        variable.addAttribute(new Attribute(name, value));
    }

    @Override
    public void addAttribute(String name, Number value) {
        addAttribute(name, value, false);
    }

    @Override
    public void addAttribute(String name, Number value, boolean isUnsigned) {
        if(value instanceof Long) {
            variable.addAttribute(new Attribute(name, value.intValue()));
        }else {
            variable.addAttribute(new Attribute(name, value));
        }
    }

    @Override
    public void addAttribute(String name, Array value) {
        if (DataType.getType(value.getElementType()) == DataType.LONG) {
            long[] longElems = (long[]) value.get1DJavaArray(Long.class);
            int[] intElems = new int[longElems.length];
            for (int i = 0; i < longElems.length; i++) {
                intElems[i] = (int) longElems[i];
            }
            variable.addAttribute(new Attribute(name, Array.factory(intElems)));
        } else {
            variable.addAttribute(new Attribute(name, value));
        }
    }

    @Override
    public void writeFully(Array values) throws IOException {
        try {
            netcdfFileWriteable.write(variable.getFullName(), values);
        } catch (InvalidRangeException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void write(int x, int y, int width, int height, boolean isYFlipped, ProductData data) throws IOException {
        String variableName = variable.getFullName();
        final int yIndex = 0;
        final int xIndex = 1;
        final DataType dataType = variable.getDataType();
        final int sceneHeight = variable.getDimension(yIndex).getLength();
        final int[] writeOrigin = new int[2];
        writeOrigin[xIndex] = x;
        final int[] sourceShape = new int[]{height, width};
        final Array sourceArray = Array.factory(dataType, sourceShape, data.getElems());

        final int[] sourceOrigin = new int[2];
        sourceOrigin[xIndex] = 0;
        final int[] writeShape = new int[]{1, width};
        for (int line = y; line < y + height; line++) {
            writeOrigin[yIndex] = isYFlipped ? (sceneHeight - 1) - line : line;
            sourceOrigin[yIndex] = line - y;
            try {
                Array dataArrayLine = sourceArray.sectionNoReduce(sourceOrigin, writeShape, null);
                netcdfFileWriteable.write(variableName, writeOrigin, dataArrayLine);
            } catch (InvalidRangeException e) {
                e.printStackTrace();
                throw new IOException("Unable to encode netCDF data.", e);
            }
        }
    }
}
