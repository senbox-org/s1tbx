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
        for (Iterator<Variable> iterator = variables.iterator(); iterator.hasNext(); ) {
            final Variable variable = iterator.next();
            variablesMap.put(variable.getName(), variable);
        }
    }

    public Variable get(String name) {
        return variablesMap.get(name);
    }

    public Variable[] getAll() {
        return variablesMap.values().toArray(new Variable[variablesMap.size()]);
    }
}
