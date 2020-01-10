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

import org.esa.snap.dataio.netcdf.util.DataTypeUtils;
import ucar.ma2.DataType;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.ArrayList;

/**
 * A wrapper around the netCDF 3 {@link ucar.nc2.NetcdfFileWriter}.
 *
 * @author MarcoZ
 */
public class N3FileWriteable extends NFileWriteable {

     N3FileWriteable(String filename) throws IOException {
        netcdfFileWriter = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3,filename) ;
    }

    @Override
    public NVariable addScalarVariable(String name, DataType dataType)  {
        Variable variable = netcdfFileWriter.addVariable(null, name, dataType, new ArrayList<Dimension>());
        NVariable nVariable = new N3Variable(variable, netcdfFileWriter);
        variables.put(name, nVariable);
        return nVariable;
    }

    @Override
    public NVariable addVariable(String name, DataType dataType, boolean unsigned, java.awt.Dimension tileSize, String dimensions, int compressionLevel) {
        Variable variable = netcdfFileWriter.addVariable(null, name, dataType, dimensions);
        NVariable nVariable = new N3Variable(variable, netcdfFileWriter);
        variables.put(name, nVariable);
        return nVariable;
    }

    @Override
    public DataType getNetcdfDataType(int dataType){
        return DataTypeUtils.getNetcdfDataType(dataType);
    };




}
