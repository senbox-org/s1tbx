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

import ucar.ma2.DataType;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;

import java.io.IOException;

/**
 * A wrapper around the netCDF 3 {@link ucar.nc2.NetcdfFileWriteable}.
 *
 * @author MarcoZ
 */
public class N3FileWriteable implements NFileWriteable {

    private final NetcdfFileWriteable netcdfFileWriteable;

    public static NFileWriteable create(String filename) throws IOException {
        NetcdfFileWriteable writeable = NetcdfFileWriteable.createNew(filename);
        writeable.setLargeFile(true);
        return new N3FileWriteable(writeable);
    }

    public N3FileWriteable(NetcdfFileWriteable netcdfFileWriteable) {
        this.netcdfFileWriteable = netcdfFileWriteable;
    }

    @Override
    public void addDimension(String name, int length) {
        netcdfFileWriteable.addDimension(name, length);
    }

    @Override
    public String getDimensions() {
        return Dimension.makeDimensionList(netcdfFileWriteable.getRootGroup().getDimensions());
    }

    @Override
    public void addGlobalAttribute(String name, String value) {
        netcdfFileWriteable.addGlobalAttribute(name, value);
    }

    @Override
    public NVariable addScalarVariable(String name, DataType dataType) throws IOException {
        Variable variable = netcdfFileWriteable.addVariable(name, dataType, "");
        return new N3Variable(variable, netcdfFileWriteable);

    }

    @Override
    public NVariable addVariable(String name, DataType dataType, java.awt.Dimension tileSize, String dims) throws IOException {
        Variable variable = netcdfFileWriteable.addVariable(name, dataType, dims);
        return new N3Variable(variable, netcdfFileWriteable);
    }


    @Override
    public NVariable addVariable(String name, DataType dataType, boolean unsigned, java.awt.Dimension tileSize, String dims, int compressionLevel) throws IOException {
        return addVariable(name, dataType, tileSize, dims);
    }

    @Override
    public NVariable addVariable(String name, DataType dataType, boolean unsigned, java.awt.Dimension tileSize, String dims) throws IOException {
        return addVariable(name, dataType, tileSize, dims);
    }

    @Override
    public NVariable findVariable(String variableName) {
        Variable variable = netcdfFileWriteable.getRootGroup().findVariable(variableName);
        return variable != null ? new N3Variable(variable, netcdfFileWriteable) : null;
    }

    @Override
    public boolean isNameValid(String name) {
        return true;
    }

    @Override
    public String makeNameValid(String name) {
        return name;
    }


    @Override
    public void create() throws IOException {
        netcdfFileWriteable.create();
    }

    @Override
    public void close() throws IOException {
        netcdfFileWriteable.close();
    }
}
