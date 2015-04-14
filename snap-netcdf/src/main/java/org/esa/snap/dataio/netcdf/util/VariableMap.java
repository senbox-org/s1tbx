/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.dataio.netcdf.util;

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
public class VariableMap {

    private final Map<String, Variable> map;

    public VariableMap(int initialCapacity) {
        map = new HashMap<String, Variable>(initialCapacity);
    }

    public VariableMap(Variable[] variables) {
        this((3 * variables.length) / 2 + 1);
        for (Variable variable : variables) {
            put(variable);
        }
    }

    private VariableMap(List<Variable> variables) {
        this(variables.toArray(new Variable[variables.size()]));
    }

    public static VariableMap create(NetcdfFile file) {
        return new VariableMap(file.getVariables());
    }

    public static VariableMap create(Group group) {
        return new VariableMap(group.getVariables());
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
     * Puts a variable with the given name into this map.
     *
     * @param name The name.
     * @param variable The variable.
     */
    public void put(String name, Variable variable) {
        map.put(name, variable);
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
        return variable.getFullName();
    }
}
