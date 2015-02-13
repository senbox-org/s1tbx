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

import java.awt.Dimension;
import java.io.IOException;

/**
 * An abstraction of the netcdf3/4 writing API.
 *
 * @author MarcoZ
 */
public interface NFileWriteable {

    void addDimension(String name, int length) throws IOException;

    String getDimensions();

    void addGlobalAttribute(String name, String value) throws IOException;

    NVariable addScalarVariable(String name, DataType dataType) throws IOException;

    NVariable addVariable(String name, DataType dataType, Dimension tileSize, String dims) throws IOException;

    NVariable addVariable(String name, DataType dataType, boolean unsigned, Dimension tileSize, String dims) throws IOException;

    NVariable addVariable(String name, DataType dataType, boolean unsigned, Dimension tileSize, String dims, int compressionLevel) throws IOException;

    NVariable findVariable(String variableName);

    boolean isNameValid(String name);

    String makeNameValid(String name);

    void create() throws IOException;

    void close() throws IOException;
}
