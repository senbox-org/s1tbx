package org.esa.beam.dataio.modis.netcdf;

import ucar.nc2.Variable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class NetCDFVariables {

    private final HashMap<String, Variable> variablesMap;

    public NetCDFVariables() {
        variablesMap = new HashMap<String, Variable>();
    }

    public void add(List<Variable> variables) {
        for (final Variable variable : variables) {
            final String name = extractBandName(variable.getName());
            variablesMap.put(name, variable);
        }
    }

    public Variable get(String name) {
        return variablesMap.get(name);
    }

    public Variable[] getAll() {
        return variablesMap.values().toArray(new Variable[variablesMap.size()]);
    }

    // package access for testing only tb 2012-05-22
    static String extractBandName(String variableName) {
        final int slashIndex = variableName.lastIndexOf('/');
        if (slashIndex > 0) {
            return variableName.substring(slashIndex + 1, variableName.length());
        }
        return variableName;
    }
}
