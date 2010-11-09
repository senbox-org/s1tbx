package org.esa.beam.dataio.netcdf;

/**
 * Provides storage for properties.
 */
interface PropertyStore {
    /**
     * Sets a property value with the given name.
     *
     * @param name  the name of the property
     * @param value the value of the property
     */
    void setProperty(String name, Object value);

    /**
     * Retrieves the value of a property.
     *
     * @param name  the name of the property
     * @return The value of the property or {@code null} if the property name is unknown.
     */
    Object getProperty(String name);
}
