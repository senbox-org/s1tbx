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
    private final List<Variable> globalVariables;
    private final Map<String, Variable> globalVariablesMap;
    private final RasterDigest rasterDigest;

    public ProfileReadContextImpl(NetcdfFile netcdfFile, RasterDigest rasterDigest, VariableMap variableMap) {
        this.netcdfFile = netcdfFile;
        this.propertyMap = new HashMap<String, Object>();

        globalVariables = Collections.unmodifiableList(netcdfFile.getVariables());
        globalVariablesMap = Collections.unmodifiableMap(ReaderUtils.createVariablesMap(globalVariables));
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
    public List<Variable> getGlobalVariables() {
        return globalVariables;
    }

    @Override
    public RasterDigest getRasterDigest() {
        return rasterDigest;
    }

    @Override
    public Map<String, Variable> getGlobalVariablesMap() {
        return globalVariablesMap;
    }
}
