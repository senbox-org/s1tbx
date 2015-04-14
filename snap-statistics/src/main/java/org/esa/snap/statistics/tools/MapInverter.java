package org.esa.snap.statistics.tools;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class MapInverter {

    public static Map<String, String> createInvertedTreeMap(Map map) {
        final TreeMap<String, String> swappedMap = new TreeMap<String, String>();
        //noinspection unchecked
        final Set<Map.Entry> set = map.entrySet();
        for (Map.Entry entry : set) {
            final String newKey = (String) entry.getValue();
            if (swappedMap.containsKey(newKey)) {
                throw new IllegalArgumentException("The map must contain only unique values");
            }
            swappedMap.put(newKey, (String) entry.getKey());
        }
        return swappedMap;
    }
}
