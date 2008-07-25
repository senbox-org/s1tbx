package com.bc.ceres.glayer.support;

import com.bc.ceres.glayer.Composite;
import com.bc.ceres.glayer.Style;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.HashMap;


/**
 * A default {@link Style} implementation.
 *
 * @author Norman Fomferra
 * @version $revision$ $date$
 */
public class DefaultStyle extends AbstractStyle {
    private static final Style instance = new DefaultStyle(null);

    private final HashMap<String, Object> propertyMap;
    private final PropertyChangeSupport propertyChangeSupport;

    static {
        instance.setOpacity(1.0);
        instance.setComposite(Composite.SRC_OVER);
    }

    public DefaultStyle() {
        this(instance);
    }

    public DefaultStyle(Style defaultStyle) {
        super(defaultStyle);
        propertyMap = new HashMap<String, Object>();
        propertyChangeSupport = new PropertyChangeSupport(this);
    }

    public static Style getInstance() {
        return instance;
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