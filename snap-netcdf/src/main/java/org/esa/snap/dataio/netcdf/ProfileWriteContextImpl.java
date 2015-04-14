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

package org.esa.snap.dataio.netcdf;

import org.esa.snap.dataio.netcdf.nc.NFileWriteable;

import java.util.HashMap;
import java.util.Map;

public class ProfileWriteContextImpl implements ProfileWriteContext {

    private final Map<String, Object> propertyMap;
    private final NFileWriteable netcdfWriteable;

    public ProfileWriteContextImpl(NFileWriteable netcdfWriteable) {
        this.netcdfWriteable = netcdfWriteable;
        propertyMap = new HashMap<String, Object>();
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
    public NFileWriteable getNetcdfFileWriteable() {
        return netcdfWriteable;
    }
}
