package org.esa.beam.dataio.netcdf4;

import com.bc.ceres.core.Assert;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class Nc4ReaderParameters {

    private final Nc4VariableMap rasterVariableMap;
    private final List<Variable> globalVariables;
    private final Map<String, Variable> globalVariablesMap;
    private final Nc4AttributeMap globalAttributes;
    private final Nc4RasterDigest rasterDigest;
    private NetcdfFile netcdfFile;

    public Nc4ReaderParameters(NetcdfFile netcdfFile) {
        this(netcdfFile, Nc4RasterDigest.createRasterDigest(netcdfFile.getRootGroup()));
    }

    public Nc4ReaderParameters(NetcdfFile netcdfFile, Nc4RasterDigest rasterDigest) {
        this(netcdfFile, rasterDigest,
             (rasterDigest != null ? new Nc4VariableMap(rasterDigest.getRasterVariables()) : null));
    }

    public Nc4ReaderParameters(NetcdfFile netcdfFile, Nc4RasterDigest rasterDigest, Nc4VariableMap nc4VariableMap) {
        Assert.argument(netcdfFile != null, "netcdfFile != null");
        this.netcdfFile = netcdfFile;
        globalAttributes = Nc4AttributeMap.create(netcdfFile);
        globalVariables = netcdfFile.getVariables();
        globalVariablesMap = Nc4ReaderUtils.createVariablesMap(globalVariables);
        this.rasterDigest = rasterDigest;
        this.rasterVariableMap = nc4VariableMap;
    }

    public NetcdfFile getNetcdfFile() {
        return netcdfFile;
    }

    public Nc4VariableMap getRasterVariableMap() {
        return rasterVariableMap;
    }

    public List<Variable> getGlobalVariables() {
        return globalVariables;
    }

    public Nc4AttributeMap getGlobalAttributes() {
        return globalAttributes;
    }

    public Nc4RasterDigest getRasterDigest() {
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
