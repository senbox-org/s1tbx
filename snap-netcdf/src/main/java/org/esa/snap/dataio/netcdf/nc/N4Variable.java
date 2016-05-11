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

import edu.ucar.ral.nujan.netcdf.NhDimension;
import edu.ucar.ral.nujan.netcdf.NhException;
import edu.ucar.ral.nujan.netcdf.NhVariable;
import org.esa.snap.core.datamodel.ProductData;
import ucar.ma2.Array;
import ucar.ma2.DataType;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A wrapper around the netCDF 4 {@link edu.ucar.ral.nujan.netcdf.NhVariable}.
 *
 * @author MarcoZ
 */
public class N4Variable implements NVariable {

    // MAX_ATTRIBUTE_LENGTH taken from
    // https://github.com/bcdev/nujan/blob/master/src/main/java/edu/ucar/ral/nujan/hdf/MsgAttribute.java#L185
    public static final int MAX_ATTRIBUTE_LENGTH = 65535 - 1000;

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
        addAttributeImpl(name, cropStringToMaxAttributeLength(name, value), NhVariable.TP_STRING_VAR);
    }

    @Override
    public void addAttribute(String name, Number value) throws IOException {
        addAttribute(name, value, false);
    }

    @Override
    public void addAttribute(String name, Number value, boolean isUnsigned) throws IOException {
        DataType dataType = DataType.getType(value.getClass());
        int nhType = N4DataType.convert(dataType, isUnsigned);
        addAttributeImpl(name, value, nhType);
    }

    @Override
    public void addAttribute(String name, Array value) throws IOException {
        DataType dataType = DataType.getType(value.getElementType());
        int nhType = N4DataType.convert(dataType, value.isUnsigned());

        addAttributeImpl(name, value.getStorage(), nhType);
    }

    private void addAttributeImpl(String name, Object value, int type) throws IOException {
        name = name.replace('.', '_');
        try {
            if (!variable.attributeExists(name)) {
                //attributes can only bet set once
                variable.addAttribute(name, type, value);
            }
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

    static String cropStringToMaxAttributeLength(String name,  String value) {
        if(value.length() > MAX_ATTRIBUTE_LENGTH) {
            value = value.substring(0, MAX_ATTRIBUTE_LENGTH);
            String msg = String.format("Metadata attribute '%s' has been cropped. Exceeded maximum length of %d", name, MAX_ATTRIBUTE_LENGTH);
            Logger.getLogger(N4Variable.class.getSimpleName()).log(Level.WARNING, msg);
        }
        return value;
    }

    private ChunkWriter createWriter(boolean isYFlipped) {
        NhDimension[] nhDimensions = variable.getDimensions();
        int sceneWidth = nhDimensions[1].getLength();
        int sceneHeight = nhDimensions[0].getLength();
        int chunkWidth = tileSize.width;
        int chunkHeight = tileSize.height;
        return new NetCDF4ChunkWriter(sceneWidth, sceneHeight, chunkWidth, chunkHeight, isYFlipped);
    }

    private class NetCDF4ChunkWriter extends ChunkWriter {

        private final Set<Rectangle> writtenChunkRects;

        public NetCDF4ChunkWriter(int sceneWidth, int sceneHeight, int chunkWidth, int chunkHeight, boolean YFlipped) {
            super(sceneWidth, sceneHeight, chunkWidth, chunkHeight, YFlipped);
            writtenChunkRects = new HashSet<Rectangle>((sceneWidth / chunkWidth) * (sceneHeight / chunkHeight));
        }

        @Override
        public void writeChunk(Rectangle rect, ProductData data) throws IOException {
            if (!writtenChunkRects.contains(rect)) {
                // netcdf4 chunks can only be written once
                final int[] origin = new int[]{rect.y, rect.x};
                final int[] shape = new int[]{rect.height, rect.width};
                DataType dataType = N4DataType.convert(variable.getType());
                final Array values = Array.factory(dataType, shape, data.getElems());
                try {
                    variable.writeData(origin, values);
                } catch (NhException e) {
                    throw new IOException(e);
                }
                writtenChunkRects.add(rect);
            }
        }
    }
}
