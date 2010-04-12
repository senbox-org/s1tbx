package org.esa.beam.dataio.netcdf4;

import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class which provides a fast, hash map based access to NetCDF variables lists.
 *
 * @author Norman Fomferra
 */
public class Nc4VariableMap {

    private final Map<String, Variable> map;

    public Nc4VariableMap(int initialCapacity) {
        map = new HashMap<String, Variable>(initialCapacity);
    }

    public Nc4VariableMap(Variable[] variables) {
        this((3 * variables.length) / 2 + 1);
        for (Variable variable : variables) {
            put(variable);
        }
    }

    private Nc4VariableMap(List<Variable> variables) {
        this(variables.toArray(new Variable[variables.size()]));
    }

    public static Nc4VariableMap create(NetcdfFile file) {
        return new Nc4VariableMap(file.getVariables());
    }

    public static Nc4VariableMap create(Group group) {
        return new Nc4VariableMap(group.getVariables());
    }

    /**
     * Gets a variable from this map.
     *
     * @param name The absolute path name. Use the dot character '.' to separate groups.
     *
     * @return The variable or null.
     */
    public Variable get(String name) {
        return map.get(name);
    }

    /**
     * Puts a variable into this map.
     *
     * @param variable The variable.
     */
    public void put(Variable variable) {
        map.put(getAbsoluteName(variable), variable);
    }

    /**
     * Removes all variables from this map.
     */
    public void clear() {
        map.clear();
    }


    /**
     * Tests if this map is empty.
     *
     * @return <code>true</code>  if this map is empty.
     */
    public boolean isEmpty() {
        return map.isEmpty();
    }

    /**
     * Gets a variable's absolute path name. The dot character '.' is used to separate groups.
     *
     * @param variable the netcdf variable from which the absolut path should be created
     *
     * @return The absolute path name.
     */
    public static String getAbsoluteName(Variable variable) {
        return variable.getName();
    }
}
