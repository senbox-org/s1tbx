package org.esa.beam.dataio.netcdf.util;

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
public class AttributeMap {

    private final Map<String, Attribute> map;

    public AttributeMap(int initialCapacity) {
        map = new HashMap<String, Attribute>(initialCapacity);
    }

    public AttributeMap(Attribute[] attributes) {
        this((3 * attributes.length) / 2 + 1);
        for (Attribute attribute : attributes) {
            put(attribute);
        }
    }

    private AttributeMap(List<Attribute> attributes) {
        this(attributes.toArray(new Attribute[attributes.size()]));
    }

    public static AttributeMap create(NetcdfFile file) {
        return new AttributeMap(file.getGlobalAttributes());
    }

    public static AttributeMap create(Group group) {
        return new AttributeMap(group.getAttributes());
    }

    public static AttributeMap create(Variable variable) {
        return new AttributeMap(variable.getAttributes());
    }

    public Attribute get(String name) {
        return map.get(name);
    }

    public void put(Attribute attribute) {
        map.put(attribute.getName(), attribute);
    }

    /**
     * Removes all attributes from this map.
     */
    public void clear() {
        map.clear();
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
