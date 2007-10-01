package org.esa.beam.layer;

import com.bc.layer.AbstractLayer;
import org.esa.beam.util.PropertyMap;

public abstract class StyledLayer extends AbstractLayer {
    /**
     * The default prefix for style properties.
     */
    public static final String DEFAULT_PROPERTY_NAME_PREFIX = "style";

    protected StyledLayer() {
    }

    /**
     * Gets the property name prefix.
     * The default implementation returns {@link #DEFAULT_PROPERTY_NAME_PREFIX}.
     *
     * @return the property name prefix.
     */
    public String getPropertyNamePrefix() {
        return DEFAULT_PROPERTY_NAME_PREFIX;
    }

    /**
     * Gets the fully qualified property name defined by {@link #getPropertyNamePrefix()}{@code  +"."+shortName}.
     *
     * @param shortName the property short name.
     * @return The fully qualified property name.
     */
    protected String getPropertyName(String shortName) {
        String prefix = getPropertyNamePrefix();
        StringBuilder sb = new StringBuilder(prefix.length() + shortName.length() + 1);
        return sb.append(prefix).append('.').append(shortName).toString();
    }


    /**
     * Sets layer style properties.
     *
     * @param propertyMap the display style properties
     */
    public final void setStyleProperties(PropertyMap propertyMap) {
        boolean suspended = isLayerChangeFireingSuspended();
        setLayerChangeFireingSuspended(true);
        try {
            setStylePropertiesImpl(propertyMap);
        } finally {
            setLayerChangeFireingSuspended(suspended);
        }
    }

    /**
     * Sets layer style properties. Called by {@link #setStyleProperties(org.esa.beam.util.PropertyMap)}.
     * Overridden by subclasses in order to set desired style properties.
     * Override pattern:
     * <pre>
     *    public boolean setStylePropertiesImpl(final PropertyMap propertyMap) {
     *        boolean changed = super.setProperties(propertyMap);
     *        // ... set properties, detect changes, overwrite "changed" flag
     *        if (changed && notifyListeners) {
     *            fireLayerChanged();
     *        }
     *        return changed;
     *    }
     * </pre>
     *
     * @param propertyMap the display style properties
     */
    protected void setStylePropertiesImpl(PropertyMap propertyMap) {
    }


}
