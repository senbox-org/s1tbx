package org.esa.beam.dataio.netcdf4;

import com.bc.ceres.core.Assert;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class Nc4ReaderParameters {

    private NetcdfFile netcdfFile;
    private Nc4VariableMap rasterVariableMap;
    private boolean yFlipped;
    private Nc4RasterDigest rasterDigest;
    private List<Variable> globalVariables;
    private Map<String, Variable> globalVariablesMap;
    private Nc4AttributeMap globalAttributes;

    public NetcdfFile getNetcdfFile() {
        return netcdfFile;
    }

    public Nc4ReaderParameters(NetcdfFile netcdfFile) {
        Assert.argument(netcdfFile != null, "netcdfFile != null");
        this.netcdfFile = netcdfFile;
        init();
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
            globalAttributes = null;
        }
        if (globalVariables != null) {
            globalVariables.clear();
            globalVariables = null;
        }
        if (globalVariablesMap != null) {
            globalVariablesMap.clear();
            globalVariablesMap = null;
        }
        if (rasterDigest != null) {
            rasterDigest = null;
        }
        if (rasterVariableMap != null) {
            rasterVariableMap.clear();
            rasterVariableMap = null;
        }
        if (netcdfFile != null) {
            netcdfFile.close();
            netcdfFile = null;
        }
    }

    private void init() {
        globalAttributes = Nc4AttributeMap.create(netcdfFile);
        globalVariables = netcdfFile.getVariables();
        globalVariablesMap = Nc4ReaderUtils.createVariablesMap(globalVariables);

        rasterDigest = Nc4RasterDigest.createRasterDigest(netcdfFile.getRootGroup(), this);
        if (rasterDigest != null) {
            rasterVariableMap = new Nc4VariableMap(rasterDigest.getRasterVariables());
        }
    }

    public Map<String, Variable> getGlobalVariablesMap() {
        return globalVariablesMap;
    }
}
