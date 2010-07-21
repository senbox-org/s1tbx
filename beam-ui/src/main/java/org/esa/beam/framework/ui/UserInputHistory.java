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
package org.esa.beam.framework.ui;

import org.esa.beam.util.Guardian;
import org.esa.beam.util.PropertyMap;

import java.util.LinkedList;
import java.util.List;

/**
 * <code>UserInputHistory</code> is a fixed-size array for {@code String} entries edited by a user. If a new entry is added
 * and the history is full, the list of registered entries is shifted so that the oldest entry is beeing
 * skipped.
 *
 * @author Norman Fomferra
 * @version $Revision$  $Date$
 */
public class UserInputHistory {

    private String _propertyKey;
    private int _maxNumEntries;
    private List<String> _entriesList;

    public UserInputHistory(int maxNumEntries, String propertyKey) {
        Guardian.assertNotNullOrEmpty("propertyKey", propertyKey);
        _propertyKey = propertyKey;
        setMaxNumEntries(maxNumEntries);
    }

    public int getNumEntries() {
        if (_entriesList != null) {
            return _entriesList.size();
        }
        return 0;
    }

    public int getMaxNumEntries() {
        return _maxNumEntries;
    }

    public String getPropertyKey() {
        return _propertyKey;
    }

    public String[] getEntries() {
        if (_entriesList != null) {
            return _entriesList.toArray(new String[_entriesList.size()]);
        }
        return null;
    }

    public void initBy(final PropertyMap propertyMap) {
        int maxNumEntries = propertyMap.getPropertyInt(getLengthKey(), getMaxNumEntries());
        setMaxNumEntries(maxNumEntries);

        for (int i = maxNumEntries - 1; i >= 0; i--) {
            String entry = propertyMap.getPropertyString(getNumKey(i), null);
            if (entry != null && isValidItem(entry)) {
                push(entry);
            }
        }
    }

    protected boolean isValidItem(String item) {
        return true;
    }

    public void push(String entry) {
        if (entry != null && isValidItem(entry)) {
            if (_entriesList == null) {
                _entriesList = new LinkedList<String>();
            }
            for (String anEntry : _entriesList) {
                if (anEntry.equals(entry)) {
                    _entriesList.remove(anEntry);
                    break;
                }
            }
            if (_entriesList.size() == _maxNumEntries) {
                _entriesList.remove(_entriesList.size() - 1);
            }
            _entriesList.add(0, entry);
        }
    }

    public void copyInto(PropertyMap propertyMap) {
        propertyMap.setPropertyInt(getLengthKey(), _maxNumEntries);
        for (int i = 0; i < 100; i++) {
            propertyMap.setPropertyString(getNumKey(i), null);
        }
        final String[] entries = getEntries();
        if (entries != null) {
            for (int i = 0; i < entries.length; i++) {
                propertyMap.setPropertyString(getNumKey(i), entries[i]);
            }
        }
    }

    private String getLengthKey() {
        return _propertyKey + ".length";
    }

    private String getNumKey(int index) {
        return _propertyKey + "." + index;
    }

    public void setMaxNumEntries(int maxNumEntries) {
        _maxNumEntries = maxNumEntries > 0 ? maxNumEntries : 16;
        if (_entriesList != null) {
            while (_maxNumEntries < _entriesList.size()) {
                _entriesList.remove(_entriesList.size() - 1);
            }
        }
    }
}
