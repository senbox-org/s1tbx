package com.bc.ceres.glayer;

import java.beans.PropertyChangeListener;

/**
 * TODO - Apidoc
 *
 * @author Norman Fomferra
 * @version $revision$ $date$
 */
public interface Style {
    final String PROPERTY_NAME_OPACITY = "opacity";
    final String PROPERTY_NAME_COMPOSITE = "composite";

    Style getDefaultStyle();

    void setDefaultStyle(Style style);

    boolean hasProperty(String propertyName);

    Object getProperty(String propertyName);

    void setProperty(String propertyName, Object value);

    PropertyChangeListener[] getPropertyChangeListeners();

    void addPropertyChangeListener(PropertyChangeListener propertyChangeListener);

    void addPropertyChangeListener(String propertyName, PropertyChangeListener propertyChangeListener);

    void removePropertyChangeListener(PropertyChangeListener propertyChangeListener);

    void removePropertyChangeListener(String propertyName, PropertyChangeListener propertyChangeListener);

    ////////////////////////////////////////
    // Common style property accessors

    double getOpacity();

    void setOpacity(double opacity);

    Composite getComposite();

    void setComposite(Composite composite);
}
