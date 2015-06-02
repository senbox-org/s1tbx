package org.esa.snap.util;

import java.awt.Color;
import java.awt.Font;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Set;

/**
 * Abstract implementation of the {@link PropertyMap} interface.
 *
 * @author Norman Fomferra
 * @since SNAP 2 (revision)
 */
public interface PropertyMap {
    /**
     * Loads key/value pairs from a text file into this property map.
     *
     * @param file the text file
     * @throws IOException if an I/O error occurs
     */
    void load(Path file) throws IOException;

    /**
     * Stores the key/value pairs of this property map into a text file.
     *
     * @param file   the text file
     * @param header an optional file header
     * @throws IOException if an I/O error occurs
     */
    void store(Path file, String header) throws IOException;

    /**
     * Returns the <code>Properties</code> instance in which this property map stores its key/value pairs.
     */
    Properties getProperties();

    /**
     * Returns an enumeration of the property keys in this map.
     */
    Set<String> getPropertyKeys();

    boolean getPropertyBool(String key);

    boolean getPropertyBool(String key, boolean defaultValue);

    Boolean getPropertyBool(String key, Boolean defaultValue);

    void setPropertyBool(String key, boolean value);

    void setPropertyBool(String key, Boolean newValue);

    int getPropertyInt(String key);

    int getPropertyInt(String key, int defaultValue);

    Integer getPropertyInt(String key, Integer defaultValue);

    void setPropertyInt(String key, int newValue);

    void setPropertyInt(String key, Integer value);

    double getPropertyDouble(String key);

    double getPropertyDouble(String key, double defaultValue);

    Double getPropertyDouble(String key, Double defaultValue);

    void setPropertyDouble(String key, double newValue);

    void setPropertyDouble(String key, Double value);

    String getPropertyString(String key);

    String getPropertyString(String key, String defaultValue);

    void setPropertyString(String key, String value);

    Color getPropertyColor(String key);

    Color getPropertyColor(String key, Color defaultValue);

    void setPropertyColor(String key, Color newValue);

    Font getPropertyFont(String key);

    Font getPropertyFont(String key, Font defaultValue);

    void setPropertyFont(String key, Font font);

    void addPropertyChangeListener(PropertyChangeListener listener);

    void addPropertyChangeListener(String key, PropertyChangeListener listener);

    void removePropertyChangeListener(PropertyChangeListener listener);

    void removePropertyChangeListener(String key, PropertyChangeListener listener);
}
