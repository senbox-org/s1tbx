package com.bc.ceres.glayer.support;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.HashMap;
import java.awt.*;

/**
 * TODO - Apidoc
 *
 * @author Norman Fomferra
 * @version $revision$ $date$
 */
public class DefaultStyle extends AbstractStyle {

    private final HashMap<String, Object> propertyMap;
    private final PropertyChangeSupport propertyChangeSupport;

    public DefaultStyle() {
        propertyMap = new HashMap<String, Object>();
        propertyChangeSupport = new PropertyChangeSupport(this);
        setOpacity(1.0);
        setAlphaComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
    }


    @Override
    protected boolean hasPropertyNoDefault(String propertyName) {
        return propertyMap.containsKey(propertyName);
    }

    @Override
    protected Object getPropertyNoDefault(String propertyName) {
        return propertyMap.get(propertyName);
    }

    @Override
    public void setProperty(String propertyName, Object value) {
        final Object oldValue = propertyMap.get(propertyName);
        if (value == oldValue || (value != null && value.equals(oldValue))) {
            return;
        }
        propertyMap.put(propertyName, value);
        propertyChangeSupport.firePropertyChange(propertyName, oldValue, value);
    }

    @Override
    public PropertyChangeListener[] getPropertyChangeListeners() {
        return propertyChangeSupport.getPropertyChangeListeners();
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener propertyChangeListener) {
        propertyChangeSupport.addPropertyChangeListener(propertyChangeListener);
    }

    @Override
    public void addPropertyChangeListener(String propertyName, PropertyChangeListener propertyChangeListener) {
        propertyChangeSupport.addPropertyChangeListener(propertyName, propertyChangeListener);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener propertyChangeListener) {
        propertyChangeSupport.removePropertyChangeListener(propertyChangeListener);
    }

    @Override
    public void removePropertyChangeListener(String propertyName, PropertyChangeListener propertyChangeListener) {
        propertyChangeSupport.removePropertyChangeListener(propertyName, propertyChangeListener);
    }
}