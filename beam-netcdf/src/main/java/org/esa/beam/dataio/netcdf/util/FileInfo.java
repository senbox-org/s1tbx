package org.esa.beam.dataio.netcdf.util;

import com.bc.ceres.core.Assert;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class FileInfo {

    private final VariableMap rasterVariableMap;
    private final List<Variable> globalVariables;
    private final Map<String, Variable> globalVariablesMap;
    private final AttributeMap globalAttributes;
    private final RasterDigest rasterDigest;
    private NetcdfFile netcdfFile;

    public FileInfo(NetcdfFile netcdfFile) {
        this(netcdfFile, RasterDigest.createRasterDigest(netcdfFile.getRootGroup()));
    }

    public FileInfo(NetcdfFile netcdfFile, RasterDigest rasterDigest) {
        this(netcdfFile, rasterDigest,
             (rasterDigest != null ? new VariableMap(rasterDigest.getRasterVariables()) : null));
    }

    public FileInfo(NetcdfFile netcdfFile, RasterDigest rasterDigest, VariableMap variableMap) {
        Assert.argument(netcdfFile != null, "netcdfFile != null");
        this.netcdfFile = netcdfFile;
        globalAttributes = AttributeMap.create(netcdfFile);
        globalVariables = netcdfFile.getVariables();
        globalVariablesMap = ReaderUtils.createVariablesMap(globalVariables);
        this.rasterDigest = rasterDigest;
        this.rasterVariableMap = variableMap;
    }

    public NetcdfFile getNetcdfFile() {
        return netcdfFile;
    }

    public VariableMap getRasterVariableMap() {
        return rasterVariableMap;
    }

    public List<Variable> getGlobalVariables() {
        return globalVariables;
    }

    public AttributeMap getGlobalAttributes() {
        return globalAttributes;
    }

    public RasterDigest getRasterDigest() {
        return rasterDigest;
    }

    public void close() throws IOException {
        if (globalAttributes != null) {
            globalAttributes.clear();
        }
        if (globalVariables != null) {
            globalVariables.clear();
        }
        if (globalVariablesMap != null) {
            globalVariablesMap.clear();
        }
        if (rasterVariableMap != null) {
            rasterVariableMap.clear();
        }
        if (netcdfFile != null) {
            netcdfFile.close();
            netcdfFile = null;
        }
    }

    public Map<String, Variable> getGlobalVariablesMap() {
        return globalVariablesMap;
    }
}
