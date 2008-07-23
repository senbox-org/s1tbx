package com.bc.ceres.glayer;

import com.bc.ceres.grendering.Rendering;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * A base class for {@code GraphicalLayer} implementations.
 *
 * @author Norman Fomferra
 */
public abstract class AbstractGraphicalLayer implements GraphicalLayer {
    private String name;
    private boolean visible;
    private float transparency;
    private AlphaCompositeMode alphaCompositeMode;

    private PropertyChangeSupport propertyChangeSupport;

    protected AbstractGraphicalLayer() {
        name = getClass().getName();
        visible = true;
        alphaCompositeMode = AlphaCompositeMode.SRC_OVER;
        propertyChangeSupport = new PropertyChangeSupport(this);
    }

    /**
     * Removes all registered property change listeners.
     */
    public void dispose() {
        final PropertyChangeListener[] changeListeners = propertyChangeSupport.getPropertyChangeListeners();
        for (PropertyChangeListener pcl : changeListeners) {
            propertyChangeSupport.removePropertyChangeListener(pcl);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        final String oldValue = this.name;
        if (!oldValue.equals(name)) {
            this.name = name;
            firePropertyChange("name", oldValue, this.name);
        }
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        final boolean oldValue = this.visible;
        if (oldValue != visible) {
            this.visible = visible;
            firePropertyChange("visible", oldValue, this.visible);
        }
    }

    public float getTransparency() {
        return transparency;
    }

    public void setTransparency(float transparency) {
        if (transparency < 0.0f || transparency > 1.0f) {
            throw new IllegalArgumentException("transparency");
        }
        final float oldValue = this.transparency;
        if (Math.abs(oldValue - transparency) > 1.0e-5f) {
            this.transparency = transparency;
            firePropertyChange("transparency", oldValue, this.transparency);
        }
    }

    public AlphaCompositeMode getAlphaCompositeMode() {
        return alphaCompositeMode;
    }

    public void setAlphaCompositeMode(AlphaCompositeMode alphaCompositeMode) {
        if (alphaCompositeMode == null) {
            throw new NullPointerException("alphaCompositeMode");
        }
        final AlphaCompositeMode oldValue = this.alphaCompositeMode;
        if (!oldValue.equals(alphaCompositeMode)) {
            this.alphaCompositeMode = alphaCompositeMode;
            firePropertyChange("alphaCompositeMode", oldValue, this.alphaCompositeMode);
        }
    }

    public void render(Rendering rendering) {
        if (!isVisible() || getTransparency() == 1.0f) {
            return;
        }
        final Graphics2D g = rendering.getGraphics();
        Composite composite = null;
        try {
            if (getTransparency() > 0.0f) {
                composite = g.getComposite();
                g.setComposite(AlphaComposite.getInstance(getAlphaCompositeMode().getValue(), 1.0f - getTransparency()));
            }
            renderLayer(rendering);
        } finally {
            if (composite != null) {
                g.setComposite(composite);
            }
        }
    }

    protected abstract void renderLayer(Rendering rendering);

    public Rectangle2D getBoundingBox() {
        return new Rectangle();
    }

    @Override
    public String toString() {
        return getName();
    }

    public void addPropertyChangeListener(PropertyChangeListener propertyChangeListener) {
        propertyChangeSupport.addPropertyChangeListener(propertyChangeListener);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener propertyChangeListener) {
        propertyChangeSupport.addPropertyChangeListener(propertyName, propertyChangeListener);
    }

    public void removePropertyChangeListener(PropertyChangeListener propertyChangeListener) {
        propertyChangeSupport.removePropertyChangeListener(propertyChangeListener);
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener propertyChangeListener) {
        propertyChangeSupport.removePropertyChangeListener(propertyName, propertyChangeListener);
    }

    protected void firePropertyChange(String propertyName,
                                      boolean oldValue, boolean newValue) {
        propertyChangeSupport.firePropertyChange(propertyName, oldValue, newValue);
    }

    protected void firePropertyChange(String propertyName,
                                      int oldValue, int newValue) {
        propertyChangeSupport.firePropertyChange(propertyName, oldValue, newValue);
    }

    protected void firePropertyChange(String propertyName,
                                      Object oldValue, Object newValue) {
        propertyChangeSupport.firePropertyChange(propertyName, oldValue, newValue);
    }

    protected void firePropertyChange(PropertyChangeEvent event) {
        propertyChangeSupport.firePropertyChange(event);
    }
}