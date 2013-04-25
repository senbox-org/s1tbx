package org.esa.beam.dataio.modis.netcdf;

import org.esa.beam.dataio.modis.ModisUtils;
import ucar.nc2.Variable;

import java.util.HashMap;
import java.util.List;

public class NetCDFVariables {

    private final HashMap<String, Variable> variablesMap;

    public NetCDFVariables() {
        variablesMap = new HashMap<String, Variable>();
    }

    public void add(List<Variable> variables) {
        for (final Variable variable : variables) {
            final String name = ModisUtils.extractBandName(variable.getFullName());
            variablesMap.put(name, variable);
        }
    }

    public Variable get(String name) {
        return variablesMap.get(name);
    }

    public Variable[] getAll() {
        return variablesMap.values().toArray(new Variable[variablesMap.size()]);
    }
}
