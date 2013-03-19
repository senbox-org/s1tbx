/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.timeseries.core.timeseries.datamodel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Class for mapping between raster names and insitu names.
 *
 * @author Thomas Storm
 * @author Sabine Embacher
 */
public class AxisMapping {

    private final Map<String, Set<String>> insituMap = new HashMap<String, Set<String>>();
    private final Map<String, Set<String>> rasterMap = new HashMap<String, Set<String>>();

    private final List<AxisMappingListener> listeners = new ArrayList<AxisMappingListener>();

    public List<String> getInsituNames(String alias) {
        return getNamesFor(alias, insituMap);
    }

    public List<String> getRasterNames(String alias) {
        return getNamesFor(alias, rasterMap);
    }

    public void addRasterName(String alias, String rasterName) {
        addNameToAliasMap(alias, rasterName, rasterMap);
    }

    public void addInsituName(String alias, String insituName) {
        addNameToAliasMap(alias, insituName, insituMap);
    }

    public void removeAlias(String alias) {
        insituMap.remove(alias);
        rasterMap.remove(alias);
        fireEvent();
    }

    public void addAlias(String alias) {
        insituMap.put(alias, new TreeSet<String>());
        fireEvent();
    }

    public void removeInsituName(String alias, String insituName) {
        removeName(alias, insituName, insituMap);
    }

    public void removeRasterName(String alias, String rasterName) {
        removeName(alias, rasterName, rasterMap);
    }

    public Set<String> getAliasNames() {
        final Set<String> names = new TreeSet<String>();
        names.addAll(insituMap.keySet());
        names.addAll(rasterMap.keySet());
        return names;
    }

    public void replaceAlias(String beforeName, String changedName) {
        replaceAliasInMap(beforeName, changedName, rasterMap);
        replaceAliasInMap(beforeName, changedName, insituMap);
        fireEvent();
    }

    public String getRasterAlias(String rasterName) {
        return getAlias(rasterName, rasterMap);
    }

    public String getInsituAlias(String insituName) {
        return getAlias(insituName, insituMap);
    }

    public void addAxisMappingListener(AxisMappingListener axisMappingListener) {
        listeners.add(axisMappingListener);
    }

    public int getRasterCount() {
        return getDataSourceCount(rasterMap);
    }

    public int getInsituCount() {
        return getDataSourceCount(insituMap);
    }

    private String getAlias(String rasterName, Map<String, Set<String>> map) {
        for (Map.Entry<String, Set<String>> entry : map.entrySet()) {
            for (String mappedRasterName : entry.getValue()) {
                if (mappedRasterName.equals(rasterName)) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    private List<String> getNamesFor(String alias, Map<String, Set<String>> map) {
        if(!map.containsKey(alias)) {
            return Collections.emptyList();
        }
        final Set<String> insituSet = map.get(alias);
        final ArrayList<String> names = new ArrayList<String>();
        names.addAll(insituSet);
        return Collections.unmodifiableList(names);
    }

    private void addNameToAliasMap(String alias, String name, Map<String, Set<String>> map) {
        ensureSetAvailable(alias, map);
        final Set<String> set = map.get(alias);
        set.add(name);
        fireEvent();
    }

    private void ensureSetAvailable(String alias, Map<String, Set<String>> map) {
        if (!map.containsKey(alias)) {
            map.put(alias, new TreeSet<String>());
        }
    }

    private void removeName(String alias, String name, Map<String, Set<String>> map) {
        ensureSetAvailable(alias, map);
        map.get(alias).remove(name);
        fireEvent();
    }

    private void replaceAliasInMap(String beforeName, String changedName, Map<String, Set<String>> map) {
        final Set<String> removedNames = map.remove(beforeName);
        if (removedNames != null) {
            map.put(changedName, removedNames);
        }
    }

    private void fireEvent() {
        for (AxisMappingListener listener : listeners) {
            listener.hasChanged();
        }
    }

    private int getDataSourceCount(Map<String, Set<String>> map) {
        int count = 0;
        for (Set<String> strings : map.values()) {
            count += strings.size();
        }
        return count;
    }

    interface AxisMappingListener {
        void hasChanged();
    }
}
