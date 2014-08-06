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

import edu.ucar.ral.nujan.netcdf.*;
import org.esa.beam.dataio.netcdf.util.VariableNameHelper;
import ucar.ma2.DataType;

import java.awt.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * A wrapper around the netCDF 4 {@link edu.ucar.ral.nujan.netcdf.NhFileWriter}.
 *
 * @author MarcoZ
 */
public class N4FileWriteable implements NFileWriteable {

    private static final int DEFAULT_COMPRESSION = 6;
    private final NhFileWriter nhFileWriter;
    private Map<String, NVariable> variables;

    public static NFileWriteable create(String filename) throws IOException {
        try {
            return new N4FileWriteable(new NhFileWriter(filename, NhFileWriter.OPT_OVERWRITE));
        } catch (NhException e) {
            throw new IOException(e);
        }
    }

    private N4FileWriteable(NhFileWriter nhFileWriter) {
        this.nhFileWriter = nhFileWriter;
        this.variables = new HashMap<>();
    }

    @Override
    public void addDimension(String name, int length) throws IOException {
        try {
            nhFileWriter.getRootGroup().addDimension(name, length);
        } catch (NhException e) {
            throw new IOException(e);
        }
    }

    @Override
    public String getDimensions() {
        NhGroup rootGroup = nhFileWriter.getRootGroup();
        NhDimension[] dimensions = rootGroup.getDimensions();
        StringBuilder out = new StringBuilder();
        for (NhDimension dim : dimensions) {
            out.append(dim.getName()).append(" ");
        }
        return out.toString();
    }

    @Override
    public void addGlobalAttribute(String name, String value) throws IOException {
        try {
            nhFileWriter.getRootGroup().addAttribute(name, NhVariable.TP_STRING_VAR, value);
        } catch (NhException e) {
            throw new IOException(e);
        }
    }

    @Override
    public NVariable addScalarVariable(String name, DataType dataType) throws IOException {
        NhGroup rootGroup = nhFileWriter.getRootGroup();
        boolean unsigned = false; // TODO
        int nhType = N4DataType.convert(dataType, unsigned);
        try {
            NhVariable variable = rootGroup.addVariable(name, nhType, new NhDimension[0], null, null, 0);
            NVariable nVariable = new N4Variable(variable, null);
            variables.put(name, nVariable);
            return nVariable;
        } catch (NhException e) {
            throw new IOException(e);
        }
    }

    @Override
    public NVariable addVariable(String name, DataType dataType, Dimension tileSize, String dims) throws IOException {
        return addVariable(name, dataType, false, tileSize, dims);
    }


    @Override
    public NVariable addVariable(String name, DataType dataType, boolean unsigned, Dimension tileSize, String dims) throws IOException {
        return addVariable(name, dataType, unsigned, tileSize, dims, DEFAULT_COMPRESSION);
    }

    @Override
    public NVariable addVariable(String name, DataType dataType, boolean unsigned, Dimension tileSize, String dimensions, int compressionLevel) throws
            IOException {
        NhGroup rootGroup = nhFileWriter.getRootGroup();
        int nhType = N4DataType.convert(dataType, unsigned);
        String[] dims = dimensions.split(" ");
        NhDimension[] nhDims = new NhDimension[dims.length];
        for (int i = 0; i < dims.length; i++) {
            nhDims[i] = rootGroup.findLocalDimension(dims[i]);
        }
        int[] chunkLens = new int[dims.length];
        if (tileSize != null) {
            chunkLens[0] = tileSize.height;
            chunkLens[1] = tileSize.width;
            // compute tile size so that number of tiles is considerably smaller than Short.MAX_VALUE
            int imageWidth = nhDims[1].getLength();
            int imageHeight = nhDims[0].getLength();
            long imageSize = (long) imageHeight * imageWidth;
            for (int scalingFactor = 2; imageSize / (chunkLens[0] * chunkLens[1]) > Short.MAX_VALUE / 2; scalingFactor *= 2) {
                chunkLens[0] = tileSize.height * scalingFactor;
                chunkLens[1] = tileSize.width * scalingFactor;
            }

            // ensure that chunklens <= scene width/height
            chunkLens[1] = Math.min(chunkLens[1], imageWidth);
            chunkLens[0] = Math.min(chunkLens[0], imageHeight);
            tileSize = new Dimension(chunkLens[1], chunkLens[0]);
        } else {
            for (int i = 0; i < dims.length; i++) {
                chunkLens[i] = nhDims[i].getLength();
            }
        }

        Object fillValue = null; // TODO
        try {
            NhVariable variable = rootGroup.addVariable(name, nhType, nhDims, chunkLens, fillValue,
                    compressionLevel);
            NVariable nVariable = new N4Variable(variable, tileSize);
            variables.put(name, nVariable);
            return nVariable;
        } catch (NhException e) {
            throw new IOException(e);
        }
    }

    @Override
    public NVariable findVariable(String variableName) {
        return variables.get(variableName);
    }

    @Override
    public boolean isNameValid(String name) {
        return VariableNameHelper.isVariableNameValid(name);
    }

    @Override
    public String makeNameValid(String name) {
        return VariableNameHelper.convertToValidName(name);
    }

    @Override
    public void create() throws IOException {
        try {
            nhFileWriter.endDefine();
        } catch (NhException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            nhFileWriter.close();
        } catch (NhException e) {
            throw new IOException(e);
        }
    }

}
