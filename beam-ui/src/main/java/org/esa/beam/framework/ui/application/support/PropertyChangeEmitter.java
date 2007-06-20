package org.esa.beam.framework.ui.application.support;

import java.beans.PropertyChangeListener;

// todo move somewhere else
public interface PropertyChangeEmitter {
    void addPropertyChangeListener(PropertyChangeListener listener);

    void addPropertyChangeListener(String propertyName, PropertyChangeListener listener);

    void removePropertyChangeListener(PropertyChangeListener listener);

    void removePropertyChangeListener(String propertyName, PropertyChangeListener listener);
}
