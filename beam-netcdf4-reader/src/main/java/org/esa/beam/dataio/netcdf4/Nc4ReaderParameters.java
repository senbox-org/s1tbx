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
    private boolean yFlipped;

    public Nc4ReaderParameters(NetcdfFile netcdfFile) {
        Assert.argument(netcdfFile != null, "netcdfFile != null");
        this.netcdfFile = netcdfFile;
        globalAttributes = Nc4AttributeMap.create(netcdfFile);
        globalVariables = netcdfFile.getVariables();
        globalVariablesMap = Nc4ReaderUtils.createVariablesMap(globalVariables);

        rasterDigest = Nc4RasterDigest.createRasterDigest(netcdfFile.getRootGroup());
        if (rasterDigest != null) {
            rasterVariableMap = new Nc4VariableMap(rasterDigest.getRasterVariables());
        } else {
            rasterVariableMap = null;
        }
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


    public boolean isYFlipped() {
        return yFlipped;
    }

    public void setYFlipped(boolean yFlipped) {
        this.yFlipped = yFlipped;
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
