package com.bc.ceres.glayer.support;

import com.bc.ceres.glayer.Composite;
import com.bc.ceres.glayer.Style;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * TODO - Apidoc
 *
 * @author Norman Fomferra
 * @version $revision$ $date$
 */
public abstract class AbstractStyle implements Style {
    private Style defaultStyle;
    private transient final DefaultStylePCL defaultStyleStylePCL;

    protected AbstractStyle() {
        this(null);
    }

    protected AbstractStyle(Style defaultStyle) {
        defaultStyleStylePCL = new DefaultStylePCL();
        setDefaultStyle(defaultStyle);
    }

    @Override
    public double getOpacity() {
        return (Double) getProperty(PROPERTY_NAME_OPACITY);
    }

    @Override
    public void setOpacity(double opacity) {
        setProperty(PROPERTY_NAME_OPACITY, opacity);
    }

    @Override
    public Composite getComposite() {
        return (Composite) getProperty(PROPERTY_NAME_COMPOSITE);
    }

    @Override
    public void setComposite(Composite composite) {
        setProperty(PROPERTY_NAME_COMPOSITE, composite);
    }


    @Override
    public Style getDefaultStyle() {
        return defaultStyle;
    }

    @Override
    public void setDefaultStyle(Style defaultStyle) {
        if (this.defaultStyle != defaultStyle) {
            if (this.defaultStyle != null) {
                this.defaultStyle.removePropertyChangeListener(defaultStyleStylePCL);
            }
            this.defaultStyle = defaultStyle;
            if (this.defaultStyle != null) {
                this.defaultStyle.addPropertyChangeListener(defaultStyleStylePCL);
            }
        }
    }


    @Override
    public boolean hasProperty(String propertyName) {
        if (getDefaultStyle() != null && !hasPropertyNoDefault(propertyName)) {
            return getDefaultStyle().hasProperty(propertyName);
        }
        return hasPropertyNoDefault(propertyName);
    }

    @Override
    public Object getProperty(String propertyName) {
        if (getDefaultStyle() != null && !hasPropertyNoDefault(propertyName)) {
            return getDefaultStyle().getProperty(propertyName);
        }
        return getPropertyNoDefault(propertyName);
    }

    protected abstract boolean hasPropertyNoDefault(String propertyName);

    protected abstract Object getPropertyNoDefault(String propertyName);

    protected void firePropertyChanged(PropertyChangeEvent event) {
        for (PropertyChangeListener changeListener : getPropertyChangeListeners()) {
            changeListener.propertyChange(event);
        }
    }

    private class DefaultStylePCL implements PropertyChangeListener {
        /**
         * Called if a property the default style changed.
         */
        @Override
        public void propertyChange(PropertyChangeEvent event) {
            // delegate only if this style does not overwrite the default property 
            if (!hasPropertyNoDefault(event.getPropertyName())) {
                firePropertyChanged(event);
            }
        }
    }
}
