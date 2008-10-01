package com.bc.ceres.glayer;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ExtensibleObject;
import com.bc.ceres.glayer.support.DefaultStyle;
import com.bc.ceres.grender.Rendering;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// todo - make this class thread safe!!!

/**
 * A layer contributes graphical elements to a drawing represented by a {@link com.bc.ceres.grender.Rendering}.
 *
 * @author Marco Peters
 * @author Norman Fomferra
 * @version $revision$ $date$
 */
public class Layer extends ExtensibleObject {
    private Layer parentLayer;
    private LayerList childLayerList;
    private String name;
    private boolean visible;
    private Style style;
    private ArrayList<LayerListener> layerListenerList;
    private StylePCL stylePCL;

    /**
     * Constructor. The following default properties are used:
     * <ul>
     * <li>{@code name = getClass().getName()}</li>
     * <li>{@code visible = true}</li>
     * <li>{@code style.opcacity = 1.0}</li>
     * <li>{@code style.composite = }{@link Composite#SRC_OVER}</li>
     * </ul>
     */
    public Layer() {
        parentLayer = null;
        childLayerList = new LayerList();
        name = getClass().getName();
        visible = true;
        layerListenerList = new ArrayList<LayerListener>(8);
        stylePCL = new StylePCL();
        setStyle(new DefaultStyle());
    }

    /**
     * @return The parent layer, or {@code null} if this layer has no parent.
     */
    public Layer getParentLayer() {
        return parentLayer;
    }

    /**
     * Gets the child layers of this layer. The returned list is "life", modifying the list's content
     * will cause this layer to fire change events.
     *
     * @return The child layers of this layer. May be empty.
     */
    public List<Layer> getChildLayerList() {
        return childLayerList;
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
     * @return The layer's style.
     */
    public Style getStyle() {
        return style;
    }

    /**
     * @param style The layer's style.
     */
    public void setStyle(Style style) {
        final Style oldStyle = this.style;
        if (oldStyle != style) {
            if (this.style != null) {
                this.style.removePropertyChangeListener(stylePCL);
            }
            this.style = style;
            if (this.style != null) {
                this.style.addPropertyChangeListener(stylePCL);
            }
            fireLayerPropertyChanged("style", oldStyle, style);
        }
    }


    /**
     * Gets the bounds (bounding box) of the layer in model coordinates.
     * The default implementation returns the union of the bounds (if any) returned by
     * {@link #getLayerBounds()} and {@link #getChildLayersBounds()}.
     *
     * @return The bounds of the layer in model coordinates or {@code null} if this layer
     *         has no specified boundary.
     */
    public Rectangle2D getBounds() {
        final Rectangle2D layerBounds = getLayerBounds();
        final Rectangle2D childLayersBounds = getChildLayersBounds();
        if (childLayersBounds != null && layerBounds != null) {
            Rectangle2D bounds = new Rectangle2D.Double();
            bounds.add(layerBounds);
            bounds.add(childLayersBounds);
            return bounds;
        } else if (childLayersBounds != null) {
            return childLayersBounds;
        } else if (layerBounds != null) {
            return layerBounds;
        }
        return null;
    }

    /**
     * Gets the bounds (bounding box) of this layer in model coordinates.
     * Called by {@link #getBounds()}.
     * The default implementation returns {@code null}.
     *
     * @return The bounds of the layer in model coordinates or {@code null} if this layer
     *         has no specified boundary.
     */
    protected Rectangle2D getLayerBounds() {
        return null;
    }

    /**
     * Gets the bounds (bounding box) of the child layers in model coordinates.
     * Called by {@link #getBounds()}.
     * The default implementation returns the union bounds (if any) of all child layers.
     *
     * @return The bounds of the child layers in model coordinates or {@code null} if none of the child layers
     *         has a specified boundary.
     */
    protected Rectangle2D getChildLayersBounds() {
        Rectangle2D.Double bounds = null;
        for (Layer layer : childLayerList) {
            Rectangle2D childBounds = layer.getBounds();
            if (childBounds != null && !childBounds.isEmpty()) {
                if (bounds == null) {
                    bounds = new Rectangle2D.Double(childBounds.getX(), childBounds.getY(), childBounds.getWidth(),
                            childBounds.getHeight());
                } else {
                    bounds.add(childBounds);
                }
            }
        }
        return bounds;
    }

    /**
     * Renders the layer. The base class implementation configures the rendering with respect to the
     * "opacity" and "composite" style properties
     * and then calls {@link #renderLayer(com.bc.ceres.grender.Rendering)} followed by
     * {@link #renderChildLayers(com.bc.ceres.grender.Rendering)}.
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
            renderChildLayers(rendering);
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
     */
    protected void renderChildLayers(Rendering rendering) {
        for (int i = childLayerList.size() - 1; i >= 0; --i) {
            childLayerList.get(i).render(rendering);
        }
    }

    /**
     * Disposes all allocated resources. Called if the layer will no longer be in use.
     * The default implementation calls {@link #disposeChildLayers()} followed by
     * {@link #disposeLayer()}.
     */
    public void dispose() {
        disposeChildLayers();
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
    protected void disposeChildLayers() {
        childLayerList.dispose();
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
        Layer parent = getParentLayer();
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
     * @param parentLayer The parent layer, or {@code null} if this layer has no parent.
     */
    void setParentLayer(Layer parentLayer) {
        this.parentLayer = parentLayer;
    }

    /**
     * Regenerates the layer. May be called to update the layer data.
     * The default implementation does nothing.
     */
    public void regenerate() {
    }

    private class StylePCL implements PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent event) {
            fireLayerPropertyChanged(event);
        }
    }

    /**
     * A change-aware list of layers.
     */
    private class LayerList extends AbstractList<Layer> {
        private final ArrayList<Layer> layerList;

        LayerList() {
            this.layerList = new ArrayList<Layer>(8);
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
                oldLayer.setParentLayer(null);
                layer.setParentLayer(Layer.this);
                fireLayersRemoved(new Layer[]{oldLayer});
                fireLayersAdded(new Layer[]{layer});
            }
            return oldLayer;
        }

        @Override
        public void add(int i, Layer layer) {
            layerList.add(i, layer);
            layer.setParentLayer(Layer.this);
            fireLayersAdded(new Layer[]{layer});
        }

        @Override
        public Layer remove(int i) {
            final Layer layer = layerList.remove(i);
            layer.setParentLayer(null);
            fireLayersRemoved(new Layer[]{layer});
            return layer;
        }

        public Layer[] toArray(Layer[] array) {
            return layerList.toArray(array);
        }

        private void dispose() {
            final Layer[] layers = layerList.toArray(new Layer[layerList.size()]);
            layerList.clear();
            for (Layer layer : layers) {
                layer.dispose();
                layer.setParentLayer(null);
            }
        }
    }

    /**
     * Accepts a given {@link Visitor}.
     * <p/>
     * This method belongs to preliminary API and may be removed or changed in the future.
     *
     * @param visitor the {@link Visitor}.
     */
    public void accept(Visitor visitor) {
        visitor.visit(this);

        for (int i = childLayerList.size() - 1; i >= 0; --i) {
            childLayerList.get(i).accept(visitor);
        }
    }

    /**
     * Visitor interface.
     * <p/>
     * This interface belongs to preliminary API and may be removed or changed in the future.
     */
    public interface Visitor {
        /**
         * Visits a given {@link Layer} for e.g. extracting information or performing
         * an action.
         *
         * @param layer the {@link Layer}.
         */
        void visit(Layer layer);
    }
}
