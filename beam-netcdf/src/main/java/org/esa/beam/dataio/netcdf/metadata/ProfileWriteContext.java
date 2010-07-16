package org.esa.beam.dataio.netcdf.metadata;

import ucar.nc2.NetcdfFileWriteable;

/**
 * A context for writing metadata from the BEAM product model into netCDF.
 */
public interface ProfileWriteContext {

    public void setProperty(String name, Object property);

    public Object getProperty(String name);

    public NetcdfFileWriteable getNetcdfFileWriteable();

}