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


import org.esa.snap.dataio.netcdf.util.VariableNameHelper;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

///**
/// * A wrapper around the netCDF 4 {@link edu.ucar.ral.nujan.netcdf.NhFileWriter}.
// *
// * @author MarcoZ
// */
public abstract class NFileWriteable {

    private static final int DEFAULT_COMPRESSION = 6;
    private String dimensions = "";
    protected Map<String, Dimension> dimensionsMap = new HashMap<>();

    public NetcdfFileWriter getWriter() {
        return netcdfFileWriter;
    }

    protected NetcdfFileWriter netcdfFileWriter;
    protected Map<String, NVariable> variables = new HashMap<>();

    public void addDimension(String name, int length) throws IOException {
        try {
            dimensionsMap.put(name, netcdfFileWriter.addDimension(null, name, length));
        } catch (Exception e) {
            throw new IOException(e);
        }
        boolean firstDimension = dimensions.length() == 0;
        if (firstDimension) {
            dimensions = name;
        } else {
            dimensions = dimensions + " " + name;
        }
    }

    public String getDimensions() {
        return dimensions;
    }

    public void addGlobalAttribute(String name, String value) throws IOException {
        try {
            Attribute attribute = new Attribute(name, value);
            netcdfFileWriter.addGroupAttribute(null, attribute);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public void addGlobalAttribute(String name, Number value) throws IOException {
        try {
            Attribute attribute = new Attribute(name, value);
            netcdfFileWriter.addGroupAttribute(null, attribute);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public NVariable addVariable(String name, DataType dataType, java.awt.Dimension tileSize, String dims) throws IOException {
        return addVariable(name, dataType, false, tileSize, dims);
    }

    abstract public NVariable addScalarVariable(String name, DataType dataType);

    public NVariable addVariable(String name, DataType dataType, boolean unsigned, java.awt.Dimension tileSize, String dims) {
        return addVariable(name, dataType, unsigned, tileSize, dims, DEFAULT_COMPRESSION);
    }

    abstract public NVariable addVariable(String name, DataType dataType, boolean unsigned, java.awt.Dimension tileSize, String dimensions, int compressionLevel);

    public NVariable findVariable(String variableName) {
        return variables.get(variableName);
    }

    public boolean isNameValid(String name) {
        return VariableNameHelper.isVariableNameValid(name);
    }

    public String makeNameValid(String name) {
        return VariableNameHelper.convertToValidName(name);
    }

    abstract public DataType getNetcdfDataType(int dataType);

    public void create() throws IOException {
        netcdfFileWriter.create();
    }

    public void close() throws IOException {
        try {
            netcdfFileWriter.close();
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

}
