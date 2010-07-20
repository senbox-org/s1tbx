package org.esa.beam.dataio.netcdf.metadata;

import org.esa.beam.dataio.netcdf.util.RasterDigest;
import org.esa.beam.dataio.netcdf.util.VariableMap;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.util.List;
import java.util.Map;

/**
 * A context for reading metadata from netCDF into the BEAM product model.
 */
public interface ProfileReadContext {

    public void setProperty(String name, Object property);

    public Object getProperty(String name);

    public NetcdfFile getNetcdfFile();

    public VariableMap getRasterVariableMap();

    public List<Variable> getGlobalVariables();

    public RasterDigest getRasterDigest();

    public Map<String, Variable> getGlobalVariablesMap();
}
