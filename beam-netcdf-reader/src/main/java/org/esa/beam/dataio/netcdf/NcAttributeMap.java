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

package org.esa.beam.dataio.netcdf;

import ucar.nc2.Attribute;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class which provides a fast, hash map based access to NetCDF attribute lists.
 *
 * @author Norman Fomferra
 */
public class NcAttributeMap {

    private Map<String, Attribute> _map;

    public NcAttributeMap(int initialCapacity) {
        _map = new HashMap<String, Attribute>(initialCapacity);
    }

    public NcAttributeMap(Attribute[] attributes) {
        this((3 * attributes.length) / 2 + 1);
        for (int i = 0; i < attributes.length; i++) {
            put(attributes[i]);
        }
    }

    private NcAttributeMap(List<Attribute> attributes) {
        this(attributes.toArray(new Attribute[attributes.size()]));
    }

    public static NcAttributeMap create(NetcdfFile file) {
        return new NcAttributeMap(file.getGlobalAttributes());
    }

    public static NcAttributeMap create(Group group) {
        return new NcAttributeMap(group.getAttributes());
    }

    public static NcAttributeMap create(Variable variable) {
        return new NcAttributeMap(variable.getAttributes());
    }

    public Attribute get(String name) {
        return _map.get(name);
    }

    public void put(Attribute attribute) {
        _map.put(attribute.getName(), attribute);
    }

    /**
     * Removes all attributes from this map.
     */
    public void clear() {
        _map.clear();
    }

    public Number getNumericValue(String name) {
        final Attribute attribute = get(name);
        return attribute != null ? attribute.getNumericValue() : null;
    }

    public String getStringValue(String name) {
        final Attribute attribute = get(name);
        return attribute != null ? attribute.getStringValue() : null;
    }

    public int getValue(String name, int defaultValue) {
        final Number numericValue = getNumericValue(name);
        return numericValue != null ? numericValue.intValue() : defaultValue;
    }

    public double getValue(String name, double defaultValue) {
        final Number numericValue = getNumericValue(name);
        return numericValue != null ? numericValue.doubleValue() : defaultValue;
    }

    public String getValue(String name, String defaultValue) {
        final String stringValue = getStringValue(name);
        return stringValue != null ? stringValue : defaultValue;
    }
}
