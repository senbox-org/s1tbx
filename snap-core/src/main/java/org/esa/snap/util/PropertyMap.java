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
package org.esa.snap.util;

import com.bc.ceres.core.Assert;

import java.awt.Color;
import java.awt.Font;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * The <code>PropertyMap</code> class can be used instead of the standard JDK <code>java.util.Properties</code>
 * class.<code>PropertyMap</code> provides a generally more useful interface by adding a couple type conversion methods
 * for a set of most frequently used data types, such as <code>Boolean</code>, <code>Integer</code>,
 * <code>Double</code>, <code>Color</code> and <code>Font</code>.
 * <p>Additionally the class provides property change support.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public class PropertyMap {

    protected static final Font DEFAULT_FONT = new Font("SansSerif", Font.PLAIN, 12);
    protected static final Color DEFAULT_COLOR = Color.BLACK;

    private final Properties properties;
    private PropertyChangeSupport propertyChangeSupport;

    /**
     * Constructs a new and empty property map.
     */
    public PropertyMap() {
        this(null);
    }

    /**
     * Constructs a property map which uses the given <code>Properties</code> as a key/value container.
     */
    public PropertyMap(Properties properties) {
        this.properties = (properties != null) ? properties : new Properties();
    }

    /**
     * Loads key/value pairs from a text file into this property map.
     *
     * @param file the text file
     * @throws IOException if an I/O error occurs
     */
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
    public Properties getProperties() {
        return properties;
    }

    /**
     * Returns an enumeration of the property keys in this map.
     */
    public Set<String> getPropertyKeys() {
        return properties.stringPropertyNames();
    }

    /**
     * Gets a value of type <code>boolean</code>.
     *
     * @param key the key
     * @return the value for the given key, or <code>false</code> if the key is not contained in this property set.
     */
    public boolean getPropertyBool(String key) {
        return getPropertyBool(key, false);
    }

    /**
     * Gets a value of type <code>boolean</code>.
     *
     * @param key          the key
     * @param defaultValue the default value that is returned if the key was not found in this property set.
     * @return the value for the given key, or <code>defaultValue</code> if the key is not contained in this property
     * set.
     */
    public boolean getPropertyBool(String key, boolean defaultValue) {
        return getPropertyBool(key, defaultValue ? Boolean.TRUE : Boolean.FALSE);
    }

    /**
     * Gets a value of type <code>Boolean</code>.
     *
     * @param key          the key
     * @param defaultValue the default value that is returned if the key was not found in this property set.
     * @return the value for the given key, or <code>defaultValue</code> if the key is not contained in this property
     * set.
     */
    public Boolean getPropertyBool(String key, Boolean defaultValue) {
        String value = get(key);
        return value != null ? Boolean.valueOf(value) : defaultValue;
    }

    /**
     * Sets a value of type <code>boolean</code>.
     *
     * @param key   the key
     * @param value the value
     * @throws IllegalArgumentException
     */
    public void setPropertyBool(String key, boolean value) {
        setPropertyBool(key, value ? Boolean.TRUE : Boolean.FALSE);
    }

    /**
     * Sets a value of type <code>Boolean</code>.
     *
     * @param key      the key
     * @param newValue the new value
     * @throws IllegalArgumentException
     */
    public void setPropertyBool(String key, Boolean newValue) {
        set(key, newValue != null ? Boolean.toString(newValue) : null);
    }

    /**
     * Gets a value of type <code>int</code>.
     *
     * @param key the key
     * @return the value for the given key, or <code>0</code> (zero) if the key is not contained in this property set.
     */
    public int getPropertyInt(String key) {
        return getPropertyInt(key, 0);
    }

    /**
     * Gets a value of type <code>int</code>.
     *
     * @param key          the key
     * @param defaultValue the default value that is returned if the key was not found in this property set.
     * @return the value for the given key, or <code>defaultValue</code> if the key is not contained in this property
     * set.
     */
    public int getPropertyInt(String key, int defaultValue) {
        String value = get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }


    /**
     * Gets a value of type <code>Integer</code>.
     *
     * @param key          the key
     * @param defaultValue the default value that is returned if the key was not found in this property set.
     * @return the value for the given key, or <code>defaultValue</code> if the key is not contained in this property
     * set.
     */
    public Integer getPropertyInt(String key, Integer defaultValue) {
        String value = get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }


    /**
     * Sets a value of type <code>int</code>.
     *
     * @param key      the key
     * @param newValue the new value
     * @throws IllegalArgumentException
     */
    public void setPropertyInt(String key, int newValue) {
        setPropertyInt(key, new Integer(newValue));
    }

    /**
     * Sets a value of type <code>Integer</code>.
     *
     * @param key   the key
     * @param value the value
     * @throws IllegalArgumentException
     */
    public void setPropertyInt(String key, Integer value) {
        set(key, value != null ? Integer.toString(value) : null);
    }

    /**
     * Gets a value of type <code>double</code>.
     *
     * @param key the key
     * @return the value for the given key, or <code>0.0</code> (zero) if the key is not contained in this property
     * set.
     */
    public double getPropertyDouble(String key) {
        return getPropertyDouble(key, 0.0);
    }

    /**
     * Gets a value of type <code>double</code>.
     *
     * @param key          the key
     * @param defaultValue the default value that is returned if the key was not found in this property set.
     * @return the value for the given key, or <code>defaultValue</code> if the key is not contained in this property
     * set.
     */
    public double getPropertyDouble(String key, double defaultValue) {
        String value = get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Double.valueOf(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Gets a value of type <code>Double</code>.
     *
     * @param key          the key
     * @param defaultValue the default value that is returned if the key was not found in this property set.
     * @return the value for the given key, or <code>defaultValue</code> if the key is not contained in this property
     * set.
     */
    public Double getPropertyDouble(String key, Double defaultValue) {
        String value = get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Double.valueOf(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Sets a value of type <code>double</code>.
     *
     * @param key      the key
     * @param newValue the new value
     * @throws IllegalArgumentException
     */
    public void setPropertyDouble(String key, double newValue) {
        setPropertyDouble(key, new Double(newValue));
    }

    /**
     * Sets a value of type <code>Double</code>.
     *
     * @param key   the key
     * @param value the value
     * @throws IllegalArgumentException
     */
    public void setPropertyDouble(String key, Double value) {
        set(key, value != null ? Double.toString(value) : null);
    }

    /**
     * Gets a value of type <code>String</code>.
     *
     * @param key the key
     * @return the value for the given key, or <code>""</code> (empty string) if the key is not contained in this
     * property set, never <code>null</code>.
     */
    public String getPropertyString(String key) {
        return get(key, "");
    }

    /**
     * Gets a value of type <code>String</code>.
     *
     * @param key          the key
     * @param defaultValue the default value that is returned if the key was not found in this property set.
     * @return the value for the given key, or <code>defaultValue</code> if the key is not contained in this property
     * set.
     */
    public String getPropertyString(String key, String defaultValue) {
        return get(key, defaultValue);
    }

    /**
     * Sets a value of type <code>String</code>.
     *
     * @param key   the key
     * @param value the new value
     * @throws IllegalArgumentException
     */
    public void setPropertyString(String key, String value) {
        set(key, value);
    }

    /**
     * Gets a value of type <code>Color</code>.
     *
     * @param key the key
     * @return the value for the given key, or <code>Color.black</code> if the key is not contained in this property
     * set, never <code>null</code>.
     */
    public Color getPropertyColor(String key) {
        return getPropertyColor(key, DEFAULT_COLOR);
    }

    /**
     * Gets a value of type <code>Color</code>.
     *
     * @param key          the key
     * @param defaultValue the default value that is returned if the key was not found in this property set.
     * @return the value for the given key, or <code>defaultValue</code> if the key is not contained in this property
     * set.
     */
    public Color getPropertyColor(String key, Color defaultValue) {
        Guardian.assertNotNullOrEmpty("key", key);
        String value = get(key);
        if (value != null) {
            Color color = StringUtils.parseColor(value);
            if (color != null) {
                return color;
            }
        }
        return defaultValue;
    }

    /**
     * Sets a value of type <code>Color</code>.
     *
     * @param key      the key
     * @param newValue the value
     * @throws IllegalArgumentException
     */
    public void setPropertyColor(String key, Color newValue) {
        set(key, StringUtils.formatColor(newValue));
    }

    /**
     * Gets a value of type <code>Font</code>.
     *
     * @param key the key
     * @return the value for the given key, or a plain, 12-point "SandSerif" font if the key is not contained in this
     * property set, never <code>null</code>.
     */
    public Font getPropertyFont(String key) {
        return getPropertyFont(key, DEFAULT_FONT);
    }

    /**
     * Gets a value of type <code>Font</code>.
     *
     * @param key          the key
     * @param defaultValue the default value that is returned if the key was not found in this property set.
     * @return the value for the given key, or <code>defaultValue</code> if the key is not contained in this property
     * set.
     */
    public Font getPropertyFont(String key, Font defaultValue) {
        String value = get(key);
        if (value == null) {
            return defaultValue;
        }

        String[] parts = value.split(";");
        if (parts.length == 0 || parts.length > 3) {
            throw new IllegalArgumentException("illegal font value: " + value);
        }

        String fontName = parts[0];
        int fontStyle = DEFAULT_FONT.getStyle();
        int fontSize = DEFAULT_FONT.getSize();

        if (parts.length >= 2) {
            String styleValue = parts[1];
            if ("BOLD".equalsIgnoreCase(styleValue)) {
                fontStyle = Font.BOLD;
            } else if ("ITALIC".equalsIgnoreCase(styleValue)) {
                fontStyle = Font.ITALIC;
            }
        }
        if (parts.length >= 3) {
            fontSize = Integer.parseInt(parts[2], 10);
        }

        return new Font(fontName, fontStyle, fontSize);
    }

    /**
     * Sets a font of type <code>Font</code>. The method actually puts three keys in this property set in order to store
     * the font's properties:
     * <ul>
     * <li><code>&lt;key&gt;.name</code> for the font's name</li>
     * <li><code>&lt;key&gt;.style</code> for the font's style (an integer font)</li>
     * <li><code>&lt;key&gt;.name</code> for the font's size in points (an integer font)</li>
     * </ul>
     *
     * @param key  the key
     * @param font the font
     * @throws IllegalArgumentException
     */
    public void setPropertyFont(String key, Font font) {
        String value = null;
        if (font != null) {
            String styleValue = "PLAIN";
            int style = font.getStyle();
            if (style == Font.ITALIC) {
                styleValue = "ITALIC";
            } else if (style == Font.BOLD) {
                styleValue = "BOLD";
            }
            value = String.format("%s;%s;%s", font.getName(), styleValue, font.getSize());
        }
        set(key, value);
    }

    protected String get(String key) {
        Assert.notNull(key, "key");
        return properties.getProperty(key);
    }

    protected String get(String key, String defaultValue) {
        Assert.notNull(key, "key");
        return properties.getProperty(key, defaultValue);
    }

    protected String set(String key, String value) {
        Assert.notNull(key, "key");
        String oldValue = properties.getProperty(key, value);
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

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        addPropertyChangeListener(null, listener);
    }

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

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        removePropertyChangeListener(null, listener);
    }

    public void removePropertyChangeListener(String key, PropertyChangeListener listener) {
        if (listener != null && propertyChangeSupport != null) {
            if (key == null) {
                propertyChangeSupport.removePropertyChangeListener(listener);
            } else {
                propertyChangeSupport.removePropertyChangeListener(key, listener);
            }
        }
    }

    protected void firePropertyChange(String key, String oldValue, String newValue) {
        if (propertyChangeSupport != null) {
            propertyChangeSupport.firePropertyChange(key, oldValue, newValue);
        }
    }

}
