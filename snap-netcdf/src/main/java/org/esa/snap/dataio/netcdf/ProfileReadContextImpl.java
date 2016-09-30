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

package org.esa.snap.dataio.netcdf;

import org.esa.snap.dataio.netcdf.util.RasterDigest;
import ucar.nc2.NetcdfFile;

import java.util.HashMap;
import java.util.Map;

class ProfileReadContextImpl implements ProfileReadContext {

    private final Map<String, Object> propertyMap;
    private final NetcdfFile netcdfFile;

    private RasterDigest rasterDigest;

    public ProfileReadContextImpl(NetcdfFile netcdfFile) {
        this.netcdfFile = netcdfFile;
        this.propertyMap = new HashMap<>();
    }


    @Override
    public void setRasterDigest(RasterDigest rasterDigest) {
        this.rasterDigest = rasterDigest;
    }

    @Override
    public RasterDigest getRasterDigest() {
        return rasterDigest;
    }

    @Override
    public void setProperty(String name, Object value) {
        propertyMap.put(name, value);
    }

    @Override
    public Object getProperty(String name) {
        return propertyMap.get(name);
    }

    @Override
    public NetcdfFile getNetcdfFile() {
        return netcdfFile;
    }

}
