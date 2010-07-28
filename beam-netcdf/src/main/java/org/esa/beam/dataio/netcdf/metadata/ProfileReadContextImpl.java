/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.dataio.netcdf.metadata;

import org.esa.beam.dataio.netcdf.util.RasterDigest;
import org.esa.beam.dataio.netcdf.util.ReaderUtils;
import org.esa.beam.dataio.netcdf.util.VariableMap;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * No API.
 */
public class ProfileReadContextImpl implements ProfileReadContext {

    private final Map<String, Object> propertyMap;
    private final NetcdfFile netcdfFile;

    private final VariableMap rasterVariableMap;
    private final RasterDigest rasterDigest;

    public ProfileReadContextImpl(NetcdfFile netcdfFile, RasterDigest rasterDigest, VariableMap variableMap) {
        this.netcdfFile = netcdfFile;
        this.propertyMap = new HashMap<String, Object>();
        this.rasterDigest = rasterDigest;
        this.rasterVariableMap = variableMap;
    }

    @Override
    public void setProperty(String name, Object property) {
        propertyMap.put(name, property);
    }

    @Override
    public Object getProperty(String name) {
        return propertyMap.get(name);
    }

    @Override
    public NetcdfFile getNetcdfFile() {
        return netcdfFile;
    }

    @Override
    public VariableMap getRasterVariableMap() {
        return rasterVariableMap;
    }

    @Override
    public RasterDigest getRasterDigest() {
        return rasterDigest;
    }
}
