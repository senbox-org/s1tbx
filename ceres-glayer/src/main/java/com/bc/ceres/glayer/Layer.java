package com.bc.ceres.glayer;

import com.bc.ceres.core.Assert;
import com.bc.ceres.glayer.support.DefaultStyle;
import com.bc.ceres.grender.Rendering;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * A layer contributes graphical elements to a drawing represented by a {@link com.bc.ceres.grender.Rendering}.
 *
 * @author Norman Fomferra
 */
public abstract class Layer {
    private CollectionLayer parentLayer;
    private String name;
    private boolean visible;
    private Style style;
    private ArrayList<LayerListener> layerChangeListeners;
    private StylePCL stylePCL;

    /**
     * Constructor.
     */
    protected Layer() {
        name = getClass().getName();
        visible = true;
        layerChangeListeners = new ArrayList<LayerListener>(8);
        stylePCL = new StylePCL();
        setStyle(new DefaultStyle());
        getStyle().setComposite(Composite.SRC_OVER);
    }

    /**
     * Gets the bounding box of the layer in model coordinates.
     *
     * @return the bounding box of the layer in model coordinates
     */
    public abstract Rectangle2D getBounds();


    /**
     * @return The parent collection layer, or {@code null} if this layer has no parent.
     */
    public CollectionLayer getParentLayer() {
        return parentLayer;
    }

    /**
     * @param parentLayer The parent collection layer, or {@code null} if this layer has no parent.
     */
    void setParentLayer(CollectionLayer parentLayer) {
        this.parentLayer = parentLayer;
    }

    /**
     * @return The layer's style.
     */
    public Style getStyle() {
        return style;
    }

    /**
     * @param style The layer's style.
     */
    public void setStyle(Style style) {
        if (this.style != style) {
            if (this.style != null) {
                this.style.removePropertyChangeListener(stylePCL);
            }
            this.style = style;
            if (this.style != null) {
                this.style.addPropertyChangeListener(stylePCL);
            }
        }
    }

    /**
     * Dispoases all allocated resources. Called if the layer will no longer be in use.
     * The base class implememntation removes all registered change listeners.
     */
    public void dispose() {
        layerChangeListeners.clear();
    }

    /**
     * @return The name.
     */
    public String getName() {
        return name;
    }

    /**
     * @param name The name.
     */
    public void setName(String name) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        final String oldValue = this.name;
        if (!oldValue.equals(name)) {
            this.name = name;
            fireLayerPropertyChanged("name", oldValue, this.name);
        }
    }

    /**
     * @return {@code true}, if this layer is visible.
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * @param visible {@code true}, if this layer is visible.
     */
    public void setVisible(boolean visible) {
        final boolean oldValue = this.visible;
        if (oldValue != visible) {
            this.visible = visible;
            fireLayerPropertyChanged("visible", oldValue, this.visible);
        }
    }

    /**
     * Renders the layer. The base class implementation sets the layer style "opacity"
     * and then calls {@link #renderLayer(com.bc.ceres.grender.Rendering)}.
     *
     * @param rendering The rendering to which the layer will be rendered.
     */
    public void render(Rendering rendering) {
        final double opacity = getStyle().getOpacity();
        if (!isVisible() || opacity == 0.0) {
            return;
        }
        final Graphics2D g = rendering.getGraphics();
        java.awt.Composite oldComposite = null;
        try {
            if (opacity < 1.0) {
                oldComposite = g.getComposite();
                g.setComposite(getStyle().getComposite().getAlphaComposite((float) opacity));
            }
            renderLayer(rendering);
        } finally {
            if (oldComposite != null) {
                g.setComposite(oldComposite);
            }
        }
    }

    /**
     * Renders the layer. Called by {@link #render(com.bc.ceres.grender.Rendering)}.
     *
     * @param rendering The rendering to which the layer will be rendered.
     */
    protected abstract void renderLayer(Rendering rendering);


    @Override
    public String toString() {
        return getName();
    }

    /**
     * Adds a change listener to this layer.
     *
     * @param listener The listener.
     */
    public void addListener(LayerListener listener) {
        Assert.notNull(listener, "listener");
        if (!layerChangeListeners.contains(listener)) {
            layerChangeListeners.add(listener);
        }
    }

    /**
     * Removes a change listener from this layer.
     *
     * @param listener The listener.
     */
    public void removeListener(LayerListener listener) {
        layerChangeListeners.remove(listener);
    }

    /**
     * @return The listeners added to this layer..
     */
    public LayerListener[] getListeners() {
        return layerChangeListeners.toArray(new LayerListener[layerChangeListeners.size()]);
    }

    /**
     * @return All listeners added to this layer's parent.
     */
    protected LayerListener[] getParentListeners() {
        return getParentLayer() != null ? getParentLayer().getListeners() : new LayerListener[0];
    }

    /**
     * @return This layer's listeners plus the ones of all parent layers.
     */
    LayerListener[] getReachableListeners() {
        ArrayList<LayerListener> list = new ArrayList<LayerListener>(16);
        list.addAll(Arrays.asList(getListeners()));
        CollectionLayer parent = getParentLayer();
        while (parent != null) {
            list.addAll(Arrays.asList(parent.getListeners()));
            parent = parent.getParentLayer();
        }
        return list.toArray(new LayerListener[list.size()]);
    }

    protected void fireLayerPropertyChanged(String propertyName, Object oldValue, Object newValue) {
        fireLayerPropertyChanged(new PropertyChangeEvent(this, propertyName, oldValue, newValue));
    }

    protected void fireLayerPropertyChanged(PropertyChangeEvent event) {
        for (LayerListener listener : getReachableListeners()) {
            listener.handleLayerPropertyChanged(this, event);
        }
    }

    protected void fireLayerDataChanged(Rectangle2D modelRegion) {
        for (LayerListener listener : getReachableListeners()) {
            listener.handleLayerDataChanged(this, modelRegion);
        }
    }

    private class StylePCL implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent event) {
            fireLayerPropertyChanged(event);
        }
    }

}
