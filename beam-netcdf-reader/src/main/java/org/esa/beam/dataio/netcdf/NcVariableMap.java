package org.esa.beam.dataio.netcdf;

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
public class NcVariableMap {

    private Map<String, Variable> _map;

    public NcVariableMap(int initialCapacity) {
        _map = new HashMap<String, Variable>(initialCapacity);
    }

    public NcVariableMap(Variable[] variables) {
        this((3 * variables.length) / 2 + 1);
        for (int i = 0; i < variables.length; i++) {
            put(variables[i]);
        }
    }

    private NcVariableMap(List<Variable> variables) {
        this(variables.toArray(new Variable[variables.size()]));
    }

    public static NcVariableMap create(NetcdfFile file) {
        return new NcVariableMap(file.getVariables());
    }

    public static NcVariableMap create(Group group) {
        return new NcVariableMap(group.getVariables());
    }

    /**
     * Gets a variable from this map.
     *
     * @param name The absolute path name. Use the dot character '.' to separate groups.
     *
     * @return The variable or null.
     */
    public Variable get(String name) {
        return _map.get(name);
    }

    /**
     * Puts a variable into this map.
     *
     * @param variable The variable.
     */
    public void put(Variable variable) {
        _map.put(getAbsoluteName(variable), variable);
    }

    /**
     * Removes all variables from this map.
     */
    public void clear() {
        _map.clear();
    }


    /**
     * Tests if this map is empty.
     */
    public boolean isEmpty() {
        return _map.isEmpty();
    }

    /**
     * Gets a variable's absolute path name. The dot character '.' is used to separate groups.
     *
     * @return The absolute path name.
     */
    public static String getAbsoluteName(Variable variable) {
        final StringBuilder sb = new StringBuilder(variable.getName());
        Group group = variable.getParentGroup();
        while (group != null) {
            final String groupName = group.getName();
            if (groupName.length() > 0) {
                sb.insert(0, '.');
                sb.insert(0, groupName);
            }
            group = group.getParentGroup();
        }
        return sb.toString();
    }
}
