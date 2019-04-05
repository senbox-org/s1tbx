package org.esa.snap.vfs.preferences.model;

/**
 * Property data structure for storing Remote File Repositories properties.
 *
 * @author Adrian Drăghici
 */
public final class Property {

    private final String name;
    private String value;

    /**
     * Creates a new Property data structure
     *
     * @param name  The name of Property
     * @param value The value of Property
     */
    public Property(String name, String value) {
        this.name = name;
        this.value = value;
    }

    /**
     * Gets the name of Property
     *
     * @return The name of Property
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the value of Property
     *
     * @return The value of Property
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value of Property
     *
     * @param value The value of Property
     */
    public void setValue(String value) {
        this.value = value;
    }
}
