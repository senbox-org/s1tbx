package org.esa.beam.dataio.netcdf.metadata;

import ucar.nc2.NetcdfFileWriteable;

import java.util.HashMap;
import java.util.Map;

/**
 * No API.
 */
public class ProfileWriteContextImpl implements ProfileWriteContext {

    private final Map<String, Object> propertyMap;
    private final NetcdfFileWriteable netcdfWritable;

    public ProfileWriteContextImpl(NetcdfFileWriteable netcdfWritable) {
        this.netcdfWritable = netcdfWritable;
        propertyMap = new HashMap<String, Object>();
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
    public NetcdfFileWriteable getNetcdfFileWriteable() {
        return netcdfWritable;
    }
}
