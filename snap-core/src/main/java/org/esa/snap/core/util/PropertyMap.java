package org.esa.snap.core.util;

import java.awt.Color;
import java.awt.Font;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.nio.file.Path;
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

    Boolean getPropertyBool(String key, Boolean defaultValue);

    void setPropertyBool(String key, Boolean newValue);

    int getPropertyInt(String key);

    Integer getPropertyInt(String key, Integer defaultValue);

    void setPropertyInt(String key, Integer value);

    double getPropertyDouble(String key);

    Double getPropertyDouble(String key, Double defaultValue);

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
