/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package com.bc.ceres.glayer;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ExtensibleObject;
import com.bc.ceres.grender.Rendering;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.MessageFormat;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

// todo - make this class thread safe!!!

/**
 * A layer contributes graphical elements to a drawing represented by a {@link com.bc.ceres.grender.Rendering}.
 *
 * @author Marco Peters
 * @author Norman Fomferra
 * @version $revision$ $date$
 */
public abstract class Layer extends ExtensibleObject {

    private static volatile AtomicInteger instanceCount = new AtomicInteger(0);

    private final LayerType layerType;
    private Layer parent;
    private final LayerList children;
    private String id;
    private String name;

    private boolean visible;
    private double transparency;
    private double swipePercent;
    private Composite composite;

    private transient final ArrayList<LayerListener> layerListenerList;
    private transient final ConfigurationPCL configurationPCL;

    private final PropertySet configuration;

    protected Layer(LayerType layerType) {
        this(layerType, new PropertyContainer());
    }

    /**
     * Constructor. The following default properties are used:
     * <ul>
     * <li>{@code name = getClass().getName()}</li>
     * <li>{@code visible = true}</li>
     * <li>{@code transparency = 0.0}</li>
     * <li>{@code swipePercent = 1.0}</li>
     * </ul>
     *
     * @param layerType     the layer type.
     * @param configuration the configuration used by the layer type to create this layer.
     */
    protected Layer(LayerType layerType, PropertySet configuration) {
        this.configuration = configuration;
        Assert.notNull(layerType, "layerType");
        this.layerType = layerType;
        this.name = createDefaultName(layerType);
        this.parent = null;
        this.id = Long.toHexString(System.nanoTime() + (instanceCount.incrementAndGet()));
        this.children = new LayerList();

        this.visible = true;
        this.transparency = 0.0;
        this.swipePercent = 1.0;
        this.composite = Composite.SRC_OVER;

        this.layerListenerList = new ArrayList<LayerListener>(8);
        this.configurationPCL = new ConfigurationPCL();

        configuration.addPropertyChangeListener(configurationPCL);
    }

    /**
     * @return The layer type.
     */
    public LayerType getLayerType() {
        return layerType;
    }

    /**
     * Returns the configuration which can be used by the layer type to recreate this layer.
     *
     * @return the configuration.
     */
    public PropertySet getConfiguration() {
        return configuration;
    }

    /**
     * @return The parent layer, or {@code null} if this layer is not a child of any other layer.
     */
    public Layer getParent() {
        return parent;
    }

    /**
     * @return true, if this layer is a collection of other layers.
     */
    public boolean isCollectionLayer() {
        return false;
    }

    /**
     * Gets the child layers of this layer. The returned list is "life", modifying the list's content
     * will cause this layer to fire change events.
     *
     * @return The child layers of this layer. May be empty.
     */
    public List<Layer> getChildren() {
        return children;
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
        Assert.notNull(name, "name");
        final String oldValue = this.name;
        if (!oldValue.equals(name)) {
            this.name = name;
            fireLayerPropertyChanged("name", oldValue, this.name);
        }
    }

    /**
     * @return An identifier which can be used to search for special layers.
     *
     * @since Ceres 0.9
     */
    public String getId() {
        return id;
    }

    /**
     * @param id An identifier which can be used to search for special layers.
     *
     * @since Ceres 0.9
     */
    public void setId(String id) {
        Assert.notNull(id, "id");
        this.id = id;
    }

    /**
     * Gets the index of the first child layer having the given identifier.
     *
     * @param id The identifier.
     *
     * @return The child index, or {@code -1} if no such layer exists.
     *
     * @since Ceres 0.9
     */
    public int getChildIndex(String id) {
        Assert.notNull(id, "id");
        for (int i = 0; i < children.size(); i++) {
            Layer child = children.get(i);
            if (id.equals(child.getId())) {
                return i;
            }
        }
        return -1;
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
     * Returns the transparency of this layer.
     *
     * @return the transparency of this layer.
     */
    public double getTransparency() {
        return transparency;
    }

    /**
     * Sets the transparency of this layer to a new value.
     *
     * @param transparency the new transparency value of this layer.
     */
    public void setTransparency(double transparency) {
        final double oldValue = this.transparency;
        if (oldValue != transparency) {
            this.transparency = transparency;
            fireLayerPropertyChanged("transparency", oldValue, this.transparency);
        }
    }

    /**
     * Returns the swipe position of this layer.
     *
     * @return the swipe position of this layer.
     */
    public double getSwipePercent() {
        return swipePercent;
    }

    /**
     * Sets the swipe position of this layer to a new value.
     *
     * @param swipePercent the new swipe position value of this layer.
     */
    public void setSwipePercent(double swipePercent) {
        final double oldValue = this.swipePercent;
        if (oldValue != swipePercent) {
            this.swipePercent = swipePercent;
            fireLayerPropertyChanged("swipePercent", oldValue, this.swipePercent);
        }
    }

    /**
     * Returns the composite of this layer.
     *
     * @return the composite of this layer.
     */
    public Composite getComposite() {
        return composite;
    }

    /**
     * Sets the composite of this layer.
     *
     * @param composite the new composite of this layer.
     */
    public void setComposite(Composite composite) {
        final Composite oldValue = this.composite;
        if (oldValue != composite) {
            this.composite = composite;
            fireLayerPropertyChanged("composite", oldValue, this.composite);
        }
    }

    protected final <T> T getConfigurationProperty(String propertyName, T defaultValue) {
        T value = defaultValue;

        final PropertySet configuration = getConfiguration();
        final Property model = configuration.getProperty(propertyName);
        if (model != null) {
            final Class<?> expectedType = defaultValue.getClass();
            final Class<?> descriptorType = model.getDescriptor().getType();
            if (expectedType.isAssignableFrom(descriptorType)) {
                if (model.getValue() != null) {
                    //noinspection unchecked
                    value = (T) model.getValue();
                } else {
                    if (model.getDescriptor().getDefaultValue() != null) {
                        //noinspection unchecked
                        value = (T) model.getDescriptor().getDefaultValue();
                    }
                }
            } else {
                throw new IllegalArgumentException(MessageFormat.format(
                        "Class ''{0}'' is not assignable from class ''{1}''.", expectedType, descriptorType));
            }
        }

        return value;
    }

    /**
     * Gets the model bounds (bounding box) of the layer in model coordinates.
     * The default implementation returns the union of the model bounds (if any) returned by
     * {@link #getLayerModelBounds()} and {@link #getChildrenModelBounds()}.
     *
     * @return The bounds of the layer in model coordinates or {@code null} if this layer
     *         and all children have no specified boundary.
     */
    public final Rectangle2D getModelBounds() {
        final Rectangle2D layerBounds = getLayerModelBounds();
        final Rectangle2D childrenBounds = getChildrenModelBounds();
        if (layerBounds == null) {
            return childrenBounds;
        } else if (childrenBounds == null) {
            return layerBounds;
        } else {
            Rectangle2D bounds = (Rectangle2D) layerBounds.clone();
            bounds.add(childrenBounds);
            return bounds;
        }
    }

    /**
     * Gets the bounds (bounding box) of this layer in model coordinates.
     * Called by {@link #getModelBounds()}.
     * The default implementation returns {@code null}.
     *
     * @return The bounds of the layer in model coordinates or {@code null} if this layer
     *         has no specified boundary.
     */
    protected Rectangle2D getLayerModelBounds() {
        return null;
    }

    /**
     * Gets the bounds (bounding box) of the child layers in model coordinates.
     * Called by {@link #getModelBounds()}.
     * The default implementation returns the union bounds (if any) of all child layers.
     *
     * @return The bounds of the child layers in model coordinates or {@code null}
     *         none of the children have a specified boundary.
     */
    protected Rectangle2D getChildrenModelBounds() {
        Rectangle2D bounds = null;
        for (Layer layer : children) {
            Rectangle2D childBounds = layer.getModelBounds();
            if (childBounds != null) {
                if (bounds == null) {
                    bounds = (Rectangle2D) childBounds.clone();
                } else {
                    bounds.add(childBounds);
                }
            }
        }
        return bounds;
    }

    /**
     * Renders the layer. Calls {@code render(rendering,null)}.
     *
     * @param rendering The rendering to which the layer will be rendered.
     *
     * @see #render(com.bc.ceres.grender.Rendering, LayerFilter)
     */
    public final void render(Rendering rendering) {
        render(rendering, null);
    }


    /**
     * Renders the layer. The base class implementation configures the rendering with respect to the
     * "transparency" and "composite" style properties. Then
     * {@link #renderLayer(com.bc.ceres.grender.Rendering)} followed by
     * {@link #renderChildren(com.bc.ceres.grender.Rendering, LayerFilter)} are called.
     *
     * @param rendering The rendering to which the layer will be rendered.
     * @param filter    An optional layer filter. May be {@code null}.
     */
    public final void render(Rendering rendering, LayerFilter filter) {
        final double transparency = getTransparency();
        if (!isVisible() || transparency == 1.0) {
            return;
        }
        final Graphics2D g = rendering.getGraphics();
        java.awt.Composite oldComposite = null;
        try {
            if (transparency > 0.0) {
                oldComposite = g.getComposite();
                g.setComposite(getComposite().getAlphaComposite((float) (1.0 - transparency)));
            }
            final double swipe = getSwipePercent();
            if(swipe < 1.0) {
                final Rectangle clip = rendering.getGraphics().getClipBounds();
                rendering.getGraphics().clip(new Rectangle(clip.x,clip.y,(int)(clip.width*swipe),clip.height));
            }
            if (filter == null) {
                renderLayer(rendering);
                renderChildren(rendering, null);
            } else {
                if (filter.accept(this)) {
                    renderLayer(rendering);
                }
                renderChildren(rendering, filter);
            }
        } finally {
            if (oldComposite != null) {
                g.setComposite(oldComposite);
            }
        }
    }

    /**
     * Renders the layer. Called by {@link #render(com.bc.ceres.grender.Rendering)}.
     * The default implementation does nothing.
     *
     * @param rendering The rendering to which the layer will be rendered.
     */
    protected void renderLayer(Rendering rendering) {
    }

    /**
     * Renders the child layers of this layer. Called by {@link #render(com.bc.ceres.grender.Rendering)}.
     * The default implementation calls {@link #render(com.bc.ceres.grender.Rendering)} on all child layers.
     *
     * @param rendering The rendering to which the layer will be rendered.
     * @param filter    A layer filter. May be {@code null}.
     */
    protected void renderChildren(Rendering rendering, LayerFilter filter) {
        for (int i = children.size() - 1; i >= 0; --i) {
            children.get(i).render(rendering, filter);
        }
    }

    /**
     * Disposes all allocated resources. Called if the layer will no longer be in use.
     * The default implementation removes all registered listeners,
     * calls {@link #disposeChildren()} followed by {@link #disposeLayer()}.
     */
    public final void dispose() {
        configuration.removePropertyChangeListener(configurationPCL);
        layerListenerList.clear();
        disposeChildren();
        disposeLayer();
    }

    /**
     * Disposes the layer. Called by {@link #dispose()}.
     * The default implementation does nothing.
     */
    protected void disposeLayer() {
    }

    /**
     * Disposes the child layers of this layer. Called by {@link #dispose()}.
     * The default implementation calls {@link #dispose()} on all child layers
     * and removes them from this layer.
     */
    protected void disposeChildren() {
        children.dispose();
    }

    /**
     * Adds a change listener to this layer.
     *
     * @param listener The listener.
     */
    public void addListener(LayerListener listener) {
        Assert.notNull(listener, "listener");
        if (!layerListenerList.contains(listener)) {
            layerListenerList.add(listener);
        }
    }

    /**
     * Removes a change listener from this layer.
     *
     * @param listener The listener.
     */
    public void removeListener(LayerListener listener) {
        layerListenerList.remove(listener);
    }

    /**
     * @return The listeners added to this layer..
     */
    public LayerListener[] getListeners() {
        return layerListenerList.toArray(new LayerListener[layerListenerList.size()]);
    }

    /**
     * @return This layer's listeners plus the ones of all parent layers.
     */
    LayerListener[] getReachableListeners() {
        ArrayList<LayerListener> list = new ArrayList<LayerListener>(16);
        list.addAll(Arrays.asList(getListeners()));
        Layer currentParent = getParent();
        while (currentParent != null) {
            list.addAll(Arrays.asList(currentParent.getListeners()));
            currentParent = currentParent.getParent();
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

    protected void fireLayersAdded(Layer[] layers) {
        for (LayerListener listener : getReachableListeners()) {
            listener.handleLayersAdded(this, layers);
        }
    }

    protected void fireLayersRemoved(Layer[] layers) {
        for (LayerListener listener : getReachableListeners()) {
            listener.handleLayersRemoved(this, layers);
        }
    }

    @Override
    public String toString() {
        return getName();
    }

    /**
     * @param parent The parent layer, or {@code null} if this layer has no parent.
     */
    void setParent(Layer parent) {
        this.parent = parent;
    }

    /**
     * Regenerates the layer. May be called to update the layer data.
     * The default implementation does nothing.
     */
    public void regenerate() {
    }

    private class ConfigurationPCL implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent event) {
            fireLayerPropertyChanged(event);
        }
    }

    private static String createDefaultName(LayerType layerType) {
        String name = layerType.getClass().getSimpleName();
        final String suffix = "Type";
        if (name.endsWith(suffix)) {
            return name.substring(0, name.length() - suffix.length());
        }
        return name;
    }

    /**
     * A change-aware list of layers.
     */
    private class LayerList extends AbstractList<Layer> {

        private final List<Layer> layerList;

        LayerList() {
            this.layerList = new Vector<Layer>(8);
        }

        @Override
        public int size() {
            return layerList.size();
        }

        @Override
        public Layer get(int i) {
            return layerList.get(i);
        }

        @Override
        public Layer set(int i, Layer layer) {
            final Layer oldLayer = layerList.set(i, layer);
            if (layer != oldLayer) {
                oldLayer.setParent(null);
                layer.setParent(Layer.this);
                fireLayersRemoved(new Layer[]{oldLayer});
                fireLayersAdded(new Layer[]{layer});
            }
            return oldLayer;
        }

        @Override
        public void add(int i, Layer layer) {
            layerList.add(i, layer);
            layer.setParent(Layer.this);
            fireLayersAdded(new Layer[]{layer});
        }

        @Override
        public boolean remove(Object o) {
            synchronized (layerList) {
                final int i = indexOf(o);
                if (i != -1) {
                    return remove(i) == o;
                }
                return false;
            }
        }

        @Override
        public Layer remove(int i) {
            final Layer layer = layerList.remove(i);
            layer.setParent(null);
            fireLayersRemoved(new Layer[]{layer});
            return layer;
        }

        @Override
        public <T> T[] toArray(T[] array) {
            return layerList.toArray(array);
        }

        private void dispose() {
            synchronized (layerList) {
                final Layer[] layers = layerList.toArray(new Layer[layerList.size()]);
                layerList.clear();
                for (Layer layer : layers) {
                    layer.dispose();
                    layer.setParent(null);
                }
            }
        }
    }
}
