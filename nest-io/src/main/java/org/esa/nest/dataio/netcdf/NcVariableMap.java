/*
 * Copyright (C) 2012 by Array Systems Computing Inc. http://www.array.ca
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.nest.dataio.netcdf;

import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utility class which provides a fast, hash map based access to NetCDF variables lists.
 *
 * @author Norman Fomferra
 */
public class NcVariableMap {

    private final Map<String, Variable> map;

    private NcVariableMap(final int initialCapacity) {
        map = new HashMap<String, Variable>(initialCapacity);
    }

    public NcVariableMap(final Variable[] variables) {
        this((3 * variables.length) / 2 + 1);
        for (Variable variable : variables) {
            put(variable);
        }
    }

    private NcVariableMap(final List<Variable> variables) {
        this(variables.toArray(new Variable[variables.size()]));
    }

    public static NcVariableMap create(final NetcdfFile file) {
        return new NcVariableMap(file.getVariables());
    }

    public static NcVariableMap create(final Group group) {
        return new NcVariableMap(group.getVariables());
    }

    /**
     * Gets a variable from this map.
     *
     * @param name The absolute path name. Use the dot character '.' to separate groups.
     *
     * @return The variable or null.
     */
    public Variable get(final String name) {
        return map.get(name);
    }

    /**
     * Puts a variable into this map.
     *
     * @param variable The variable.
     */
    void put(final Variable variable) {
        map.put(variable.getName(), variable);
    }

    /**
     * Removes all variables from this map.
     */
    public void clear() {
        map.clear();
    }

    /**
     * Tests if this map is empty.
     * @return true if empty
     */
    public boolean isEmpty() {
        return map.isEmpty();
    }

    public Variable[] getAll() {
        final Set<String> keySet = map.keySet();
        final Variable[] variables = new Variable[keySet.size()];
        int i = 0;
        for(String key : keySet) {
            variables[i++] = map.get(key);
        }
        return variables;
    }

    /**
     * Gets a variable's absolute path name. The dot character '.' is used to separate groups.
     * @param variable the entry to get the name from
     * @return The absolute path name.
     */
    public static String getAbsoluteName(final Variable variable) {
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