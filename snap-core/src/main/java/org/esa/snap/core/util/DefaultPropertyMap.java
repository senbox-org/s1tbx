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
package org.esa.snap.core.util;

import com.bc.ceres.core.Assert;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * Default implementation of the {@link PropertyMap} interface backed by a {@link Properties} instance.
 *
 * @author Norman Fomferra
 * @since SNAP 2
 */
public class DefaultPropertyMap extends AbstractPropertyMap {

    private final Properties properties;
    private PropertyChangeSupport propertyChangeSupport;

    /**
     * Constructs a new and empty property map.
     */
    public DefaultPropertyMap() {
        this(null);
    }

    /**
     * Constructs a property map which uses the given <code>Properties</code> as a key/value container.
     */
    public DefaultPropertyMap(Properties properties) {
        this.properties = (properties != null) ? properties : new Properties();
    }

    /**
     * Loads key/value pairs from a text file into this property map.
     *
     * @param file the text file
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void load(Path file) throws IOException {
        Guardian.assertNotNull("file", file);
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            getProperties().load(reader);
        }
    }


    /**
     * Stores the key/value pairs of this property map into a text file.
     *
     * @param file   the text file
     * @param header an optional file header
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void store(Path file, String header) throws IOException {
        Guardian.assertNotNull("file", file);
        try (BufferedWriter writer = Files.newBufferedWriter(file)) {
            getProperties().store(writer, header);
        }
        List<String> lines = new ArrayList<>(Files.readAllLines(file));
        Collections.sort(lines);
        Files.write(file, lines);
    }

    /**
     * Returns the <code>Properties</code> instance in which this property map stores its key/value pairs.
     */
    @Override
    public Properties getProperties() {
        return properties;
    }

    /**
     * Returns an enumeration of the property keys in this map.
     */
    @Override
    public Set<String> getPropertyKeys() {
        return properties.stringPropertyNames();
    }


    @Override
    protected String get(String key) {
        Assert.notNull(key, "key");
        return properties.getProperty(key);
    }

    @Override
    protected String get(String key, String defaultValue) {
        Assert.notNull(key, "key");
        return properties.getProperty(key, defaultValue);
    }

    @Override
    protected String set(String key, String value) {
        Assert.notNull(key, "key");
        String oldValue = properties.getProperty(key);
        if (value != null) {
            properties.put(key, value);
        } else {
            properties.remove(key);
        }
        if (!ObjectUtils.equalObjects(value, oldValue)) {
            firePropertyChange(key, value, oldValue);
        }
        return oldValue;
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        addPropertyChangeListener(null, listener);
    }

    @Override
    public void addPropertyChangeListener(String key, PropertyChangeListener listener) {
        if (listener != null) {
            if (propertyChangeSupport == null) {
                propertyChangeSupport = new PropertyChangeSupport(this);
            }
            if (key == null) {
                propertyChangeSupport.addPropertyChangeListener(listener);
            } else {
                propertyChangeSupport.addPropertyChangeListener(key, listener);
            }
        }
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        removePropertyChangeListener(null, listener);
    }

    @Override
    public void removePropertyChangeListener(String key, PropertyChangeListener listener) {
        if (listener != null && propertyChangeSupport != null) {
            if (key == null) {
                propertyChangeSupport.removePropertyChangeListener(listener);
            } else {
                propertyChangeSupport.removePropertyChangeListener(key, listener);
            }
        }
    }

    @Override
    protected void firePropertyChange(String key, String oldValue, String newValue) {
        if (propertyChangeSupport != null) {
            propertyChangeSupport.firePropertyChange(key, oldValue, newValue);
        }
    }

}
