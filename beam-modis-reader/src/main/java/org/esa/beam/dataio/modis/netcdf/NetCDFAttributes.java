package org.esa.beam.dataio.modis.netcdf;

import ucar.nc2.Attribute;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class NetCDFAttributes {

    private HashMap<String, Attribute> attributeHashMap;

    public NetCDFAttributes() {
        attributeHashMap = new HashMap<String, Attribute>();
    }

    public void add(List<Attribute> globalAttributes) {
        if (globalAttributes == null) {
            return;
        }

        for (Iterator<Attribute> iterator = globalAttributes.iterator(); iterator.hasNext(); ) {
            final Attribute attribute = iterator.next();
            attributeHashMap.put(attribute.getName(), attribute);
        }
    }

    public Attribute get(String name) {
        return attributeHashMap.get(name);
    }

    public Attribute[] getAll() {
        return attributeHashMap.values().toArray(new Attribute[attributeHashMap.size()]);
    }
}
