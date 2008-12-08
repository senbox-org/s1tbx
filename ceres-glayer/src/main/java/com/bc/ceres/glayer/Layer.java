package com.bc.ceres.glayer;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ExtensibleObject;
import com.bc.ceres.glayer.support.DefaultStyle;
import com.bc.ceres.grender.Rendering;

import java.awt.Graphics2D;
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
    private Layer parent;
    private LayerList children;
    private String id;
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
        parent = null;
        children = new LayerList();
        name = getClass().getName();
        visible = true;
        layerListenerList = new ArrayList<LayerListener>(8);
        stylePCL = new StylePCL();
        setStyle(new DefaultStyle());
    }

    /**
     * @return The parent layer, or {@code null} if this layer has no parent.
     */
    public Layer getParent() {
        return parent;
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
     * @return An identifier which can be used to search for special layers. Can be {@code null}.
     * @since Ceres 0.9
     */
    public String getId() {
        return id;
    }

    /**
     * @param id An identifier which can be used to search for special layers. Can be {@code null}.
     * @since Ceres 0.9
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets the index of the first child layer having the given identifier.
     * @param id The identifier.
     * @return  The child index, or {@code -1} if no such layer exists.
     * @since Ceres 0.9
     */
    public int getChildIndex(String id) {
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
        return bounds ;
    }

    /**
     * Renders the layer. Calls {@code render(rendering, null)}.
     * @param rendering The rendering to which the layer will be rendered.
     * @see #render(com.bc.ceres.grender.Rendering, com.bc.ceres.glayer.Layer.RenderCustomizer)
     * 
     */
    public final void render(Rendering rendering) {
        render(rendering, null);
    }

    /**
     * Renders the layer. The base class implementation configures the rendering with respect to the
     * "opacity" and "composite" style properties. Then
     * {@link #renderLayer(com.bc.ceres.grender.Rendering)} followed by
     * {@link #renderChildren(com.bc.ceres.grender.Rendering, com.bc.ceres.glayer.Layer.RenderCustomizer)} are called if
     * {@code customizer} is {@code null}. Otherwise {@link com.bc.ceres.glayer.Layer.RenderCustomizer#render(com.bc.ceres.grender.Rendering, Layer)} is called.
     *
     * @param rendering  The rendering to which the layer will be rendered.
     * @param customizer An optional customizer for rendering the layer. May be {@code null}.
     */
    public final void render(Rendering rendering, RenderCustomizer customizer) {
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
            if (customizer == null) {
                renderLayer(rendering);
                renderChildren(rendering, null);
            } else {
                customizer.render(rendering, this);
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
     * @param customizer An optional customizer for rendering the layer. May be {@code null}.
     */
    protected void renderChildren(Rendering rendering, RenderCustomizer customizer) {
        for (int i = children.size() - 1; i >= 0; --i) {
            children.get(i).render(rendering, customizer);
        }
    }

    /**
     * Disposes all allocated resources. Called if the layer will no longer be in use.
     * The default implementation calls {@link #disposeChildren()} followed by
     * {@link #disposeLayer()}.
     */
    public final void dispose() {
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
        Layer parent = getParent();
        while (parent != null) {
            list.addAll(Arrays.asList(parent.getListeners()));
            parent = parent.getParent();
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
        public Layer remove(int i) {
            final Layer layer = layerList.remove(i);
            layer.setParent(null);
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
                layer.setParent(null);
            }
        }
    }

    // todo - apidoc (nf - 22.10.2008)
    public static interface RenderCustomizer {
          void render(Rendering rendering, Layer layer);
    }

    // todo - apidoc (nf - 22.10.2008)
    public static abstract class DefaultRenderCustomizer implements RenderCustomizer {

        public final void render(Rendering rendering, Layer layer) {
            renderBackground(rendering, layer);
            renderForeground(rendering, layer);
            renderOverlay(rendering, layer);
        }

        protected void renderBackground(Rendering rendering, Layer layer) {
        }

        protected void renderForeground(Rendering rendering, Layer layer) {
            renderLayer(rendering, layer);
            renderChildLayers(rendering, layer);
        }

        protected void renderLayer(Rendering rendering, Layer layer) {
            layer.renderLayer(rendering);
        }

        protected void renderChildLayers(Rendering rendering, Layer layer) {
            layer.renderChildren(rendering, this);
        }

        protected void renderOverlay(Rendering rendering, Layer layer) {
        }
    }

    // todo - apidoc (nf - 22.10.2008)
    public static abstract class RenderFilter implements RenderCustomizer {

        public final void render(Rendering rendering, Layer layer) {
            if (canRender(layer)) {
                layer.renderLayer(rendering);
            }
            layer.renderChildren(rendering, this);
        }

        public abstract boolean canRender(Layer layer);
    }

}
