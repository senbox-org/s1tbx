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

package org.esa.beam.dataio.netcdf.nc;

import edu.ucar.ral.nujan.netcdf.NhDimension;
import edu.ucar.ral.nujan.netcdf.NhException;
import edu.ucar.ral.nujan.netcdf.NhVariable;
import org.esa.beam.framework.datamodel.ProductData;
import ucar.ma2.Array;
import ucar.ma2.DataType;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.IOException;

/**
 * A wrapper around the netCDF 4 {@link edu.ucar.ral.nujan.netcdf.NhVariable}.
 *
 * @author MarcoZ
 */
public class N4Variable implements NVariable {

    private final NhVariable variable;
    private final Dimension tileSize;
    private ChunkWriter writer;

    public N4Variable(NhVariable variable, Dimension tileSize) {
        this.variable = variable;
        this.tileSize = tileSize;
    }

    @Override
    public String getName() {
        return variable.getName();
    }

    @Override
    public DataType getDataType() {
        int type = variable.getType();
        return N4DataType.convert(type);
    }

    @Override
    public void addAttribute(String name, String value) throws IOException {
        addAttributeImpl(name, value, NhVariable.TP_STRING_VAR);
    }

    @Override
    public void addAttribute(String name, Number value) throws IOException {
        if (value instanceof Double) {
            addAttributeImpl(name, value, NhVariable.TP_DOUBLE);
        } else if (value instanceof Float) {
            addAttributeImpl(name, value, NhVariable.TP_FLOAT);
        } else {
            addAttributeImpl(name, value.intValue(), NhVariable.TP_INT);
        }
    }

    @Override
    public void addAttribute(String name, Array value) throws IOException {
        Class elementType = value.getElementType();
        int type;
        if (elementType == long.class) {
            type = NhVariable.TP_LONG;
        } else if (elementType == int.class) {
            type = NhVariable.TP_INT;
        } else if (elementType == short.class) {
            type = NhVariable.TP_SHORT;
        } else if (elementType == byte.class) {
            type = NhVariable.TP_SBYTE;
        } else if (elementType == double.class) {
            type = NhVariable.TP_DOUBLE;
        } else if (elementType == float.class) {
            type = NhVariable.TP_FLOAT;
        } else {
            throw new IllegalArgumentException("Unsupported attribute date type: " + elementType);
        }

        addAttributeImpl(name, value.getStorage(), type);
    }

    private void addAttributeImpl(String name, Object value, int type) throws IOException {
        name = name.replace('.', '_');
        try {
            variable.addAttribute(name, type, value);
        } catch (NhException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void writeFully(Array values) throws IOException {
        int[] idxes = new int[values.getShape().length];
        try {
            variable.writeData(idxes, values);
        } catch (NhException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void write(int x, int y, int width, int height, boolean isYFlipped, ProductData data) throws IOException {
         if (writer == null) {
             writer = createWriter(isYFlipped);
         }
        writer.write(x, y, width, height, data);
    }

    ChunkWriter createWriter(boolean isYFlipped) {
        NhDimension[] nhDimensions = variable.getDimensions();
        int sceneWidth = nhDimensions[1].getLength();
        int sceneHeight = nhDimensions[0].getLength();
        int chunkWidth = tileSize.width;
        int chunkHeight = tileSize.height;
        return new ChunkWriter(sceneWidth, sceneHeight, chunkWidth, chunkHeight, isYFlipped) {

            @Override
            public void writeChunk(Rectangle rect, ProductData data) throws IOException {
                final int[] origin = new int[]{rect.y, rect.x};
                final int[] shape = new int[]{rect.height, rect.width};
                DataType dataType = N4DataType.convert(variable.getType());
                final Array values = Array.factory(dataType, shape, data.getElems());
                try {
                    variable.writeData(origin, values);
                } catch (NhException e) {
                    throw new IOException(e);
                }
            }
        };
    }


}
