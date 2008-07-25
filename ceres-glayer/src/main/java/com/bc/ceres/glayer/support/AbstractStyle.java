package com.bc.ceres.glayer.support;

import com.bc.ceres.glayer.Style;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.awt.*;

/**
 * TODO - Apidoc
 *
 * @author Norman Fomferra
 * @version $revision$ $date$
 */
public abstract class AbstractStyle implements Style {
    private Style defaultStyle;
    private final DefaultPCL defaultStylePCL;

    protected AbstractStyle() {
        defaultStylePCL = new DefaultPCL();
    }

    public double getOpacity() {
        return (Double) getProperty(PROPERTY_NAME_OPACITY);
    }

    public void setOpacity(double opacity) {
        setProperty(PROPERTY_NAME_OPACITY, opacity);
    }

    public AlphaComposite getAlphaComposite(){
        return (AlphaComposite) getProperty(PROPERTY_NAME_ALPHA_COMPOSITE);
    }

    public void setAlphaComposite(AlphaComposite alphaComposite){
        setProperty(PROPERTY_NAME_ALPHA_COMPOSITE, alphaComposite);
    }


    @Override
    public Style getDefaultStyle() {
        return defaultStyle;
    }

    @Override
    public void setDefaultStyle(Style defaultStyle) {
        if (this.defaultStyle != defaultStyle) {
            if (this.defaultStyle != null) {
                this.defaultStyle.removePropertyChangeListener(defaultStylePCL);
            }
            this.defaultStyle = defaultStyle;
            if (this.defaultStyle != null) {
                this.defaultStyle.addPropertyChangeListener(defaultStylePCL);
            }
        }
    }


    @Override
    public boolean hasProperty(String propertyName) {
        if (getDefaultStyle() != null && !hasPropertyNoDefault(propertyName)) {
            return getDefaultStyle().hasProperty(propertyName);
        }
        return hasProperty(propertyName);
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

    private class DefaultPCL implements PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent event) {
            firePropertyChanged(event);
        }
    }
}
