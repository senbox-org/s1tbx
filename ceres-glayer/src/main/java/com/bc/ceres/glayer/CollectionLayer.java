package com.bc.ceres.glayer;

import com.bc.ceres.grender.Rendering;

import java.awt.geom.Rectangle2D;
import java.util.*;

/**
 * A layer which comprises a collection of layers.
 * Each layers is painted on top of its predecessor.
 *
 * @author Norman Fomferra
 */
public class CollectionLayer extends Layer implements List<Layer> {
    private final ArrayList<Layer> layerList;

    public CollectionLayer() {
        this(new ArrayList<Layer>(16));
    }

    public CollectionLayer(Layer[] layers) {
        this(new ArrayList<Layer>(Arrays.asList(layers)));
    }

    public CollectionLayer(Collection<Layer> layerList) {
        this(new ArrayList<Layer>(layerList));
    }

    private CollectionLayer(ArrayList<Layer> layerList) {
        this.layerList = layerList;
    }

    /////////////////////////////////////////////////////////////////////////
    // Layer implementation

    @Override
    public Rectangle2D getBounds() {
        if (layerList.size() == 1) {
            return get(0).getBounds();
        } else {
            final Rectangle2D.Double bounds = new Rectangle2D.Double();
            for (Layer layer : layerList) {
                Rectangle2D childBox = layer.getBounds();
                bounds.add(childBox);
            }
            return bounds;
        }
    }

    @Override
    protected void renderLayer(Rendering rendering) {
        final Layer[] layers = layerList.toArray(new Layer[layerList.size()]);
        for (int i = layers.length - 1; i >= 0; i--) {
            Layer layer = layers[i];
            if (layer.isVisible()) {
                layer.render(rendering);
            }
        }
    }

    /**
     * Disposes all children.
     */
    @Override
    public void dispose() {
        final Layer[] layers = layerList.toArray(new Layer[layerList.size()]);
        clear();
        for (Layer layer : layers) {
            layer.dispose();
        }
        super.dispose();
    }

    /////////////////////////////////////////////////////////////////////////
    // List<Layer> implementation

    @Override
    public boolean add(Layer layer) {
        final boolean sizeChanged = layerList.add(layer);
        if (sizeChanged) {
            layer.setParentLayer(this);
            fireLayersAdded(new Layer[]{layer});
        }
        return sizeChanged;
    }

    @Override
    public void add(int index, Layer layer) {
        layerList.add(index, layer);
        layer.setParentLayer(this);
        fireLayersAdded(new Layer[]{layer});
    }

    @Override
    public boolean addAll(Collection<? extends Layer> c) {
        final boolean sizeChanged = layerList.addAll(c);
        if (sizeChanged) {
            setParentLayerOf(layerList);
            fireLayersAdded(c.toArray(new Layer[c.size()]));
        }
        return sizeChanged;
    }

    @Override
    public boolean addAll(int index, Collection<? extends Layer> c) {
        final boolean sizeChanged = layerList.addAll(index, c);
        if (sizeChanged) {
            setParentLayerOf(layerList);
            fireLayersAdded(c.toArray(new Layer[c.size()]));
        }
        return sizeChanged;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        final boolean sizeChanged = layerList.removeAll(c);
        if (sizeChanged) {
            final Layer[] layers = c.toArray(new Layer[c.size()]);
            for (Layer object : layers) {
                object.setParentLayer(null);
            }
            fireLayersRemoved(layers);
        }
        return sizeChanged;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        // todo
        throw new IllegalStateException();
    }

    @Override
    public Layer set(int index, Layer layer) {
        final Layer oldLayer = layerList.set(index, layer);
        if (oldLayer != layer) {
            if (oldLayer != null) {
                oldLayer.setParentLayer(null);
                fireLayersRemoved(new Layer[]{oldLayer});
            }
            layer.setParentLayer(this);
            fireLayersAdded(new Layer[]{layer});
        }
        return oldLayer;
    }

    @Override
    public boolean remove(Object object) {
        final Layer layer = (Layer) object;
        final boolean b = layerList.remove(layer);
        if (b) {
            layer.setParentLayer(null);
            fireLayersRemoved(new Layer[]{layer});
        }
        return b;
    }


    @Override
    public Layer remove(int index) {
        final Layer layer = layerList.remove(index);
        if (layer != null) {
            layer.setParentLayer(null);
            fireLayersRemoved(new Layer[]{layer});
        }
        return layer;
    }

    @Override
    public int indexOf(Object o) {
        return layerList.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return layerList.lastIndexOf(o);
    }

    @Override
    public void clear() {
        final Layer[] layers = layerList.toArray(new Layer[layerList.size()]);
        layerList.clear();
        fireLayersRemoved(layers);
    }

    @Override
    public Layer get(int index) {
        return layerList.get(index);
    }

    @Override
    public int size() {
        return layerList.size();
    }

    @Override
    public Iterator<Layer> iterator() {
        return layerList.iterator();
    }

    @Override
    public ListIterator<Layer> listIterator() {
        return layerList.listIterator();
    }

    @Override
    public ListIterator<Layer> listIterator(int index) {
        return layerList.listIterator(index);
    }

    @Override
    public List<Layer> subList(int fromIndex, int toIndex) {
        return layerList.subList(fromIndex, toIndex);
    }

    @Override
    public boolean equals(Object object) {
        return object == this
                || object.getClass() == CollectionLayer.class
                && ((CollectionLayer) object).layerList.equals(layerList);
    }

    @Override
    public int hashCode() {
        return layerList.hashCode();
    }

    @Override
    public boolean isEmpty() {
        return layerList.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return layerList.contains(o);
    }

    @Override
    public Object[] toArray() {
        return layerList.toArray();
    }

    @Override
    public <T> T[] toArray(T[] ts) {
        return layerList.toArray(ts);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return layerList.containsAll(c);
    }

    //
    /////////////////////////////////////////////////////////////////////////

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

    private void setParentLayerOf(Collection<? extends Layer> layers) {
        for (Layer layer : layers) {
            layer.setParentLayer(this);
        }
    }
}