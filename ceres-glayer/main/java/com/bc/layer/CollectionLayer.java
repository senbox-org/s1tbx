package com.bc.layer;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;

/**
 * A collection layer is used to draw any number of layers.
 * Each layers is painted on top of its predecessor.
 *
 * @author Norman Fomferra
 */
public class CollectionLayer extends AbstractGraphicalLayer implements List<GraphicalLayer> {
    private final List<GraphicalLayer> layerList;
    private final PropertyChangeDelegate propertyChangeDelegate;
    private static final String SIZE_PROPERTY_NAME = "size";

    public CollectionLayer() {
        this(new ArrayList<GraphicalLayer>(16));
    }

    public CollectionLayer(Collection<GraphicalLayer> layerList) {
        this(new ArrayList<GraphicalLayer>(layerList));
    }

    private CollectionLayer(List<GraphicalLayer> layerList) {
        this.layerList = layerList;
        propertyChangeDelegate = new PropertyChangeDelegate();
    }

    /////////////////////////////////////////////////////////////////////////
    // GraphicalLayer implementation

    @Override
    public Rectangle2D getBoundingBox() {
        if (layerList.size() == 1) {
            return get(0).getBoundingBox();
        } else {
            final Rectangle2D.Double boundingBox = new Rectangle2D.Double();
            for (GraphicalLayer layer : layerList) {
                Rectangle2D childBox = layer.getBoundingBox();
                boundingBox.add(childBox);
            }
            return boundingBox;
        }
    }

    @Override
    protected void paintLayer(Graphics2D g, Viewport vp) {
        final GraphicalLayer[] graphicalLayers = layerList.toArray(new GraphicalLayer[layerList.size()]);
        for (int i = graphicalLayers.length - 1; i >= 0; i--) {
            GraphicalLayer layer = graphicalLayers[i];
            if (layer.isVisible()) {
                layer.paint(g, vp);
            }
        }
    }

    /**
     * Disposes all children.
     */
    @Override
    public void dispose() {
        final GraphicalLayer[] layers = layerList.toArray(new GraphicalLayer[layerList.size()]);
        clear();
        for (GraphicalLayer layer : layers) {
            layer.dispose();
        }
        super.dispose();
    }

    /////////////////////////////////////////////////////////////////////////
    // List<GraphicalLayer> implementation

    @Override
    public boolean add(GraphicalLayer layer) {
        final int oldSize = layerList.size();
        final boolean b = layerList.add(layer);
        if (b) {
            installPCD(layer);
            firePropertyChange(SIZE_PROPERTY_NAME, oldSize, layerList.size());
        }
        return b;
    }

    @Override
    public void add(int index, GraphicalLayer layer) {
        installPCD(layer);
        final int oldSize = layerList.size();
        layerList.add(index, layer);
        firePropertyChange(SIZE_PROPERTY_NAME, oldSize, layerList.size());
    }

    @Override
    public boolean addAll(Collection<? extends GraphicalLayer> c) {
        for (GraphicalLayer layer : c) {
            installPCD(layer);
        }
        final int oldSize = layerList.size();
        final boolean sizeChanged = layerList.addAll(c);
        if (sizeChanged) {
            firePropertyChange(SIZE_PROPERTY_NAME, oldSize, layerList.size());
        }
        return sizeChanged;
    }

    @Override
    public boolean addAll(int index, Collection<? extends GraphicalLayer> c) {
        for (GraphicalLayer layer : c) {
            installPCD(layer);
        }
        final int oldSize = layerList.size();
        final boolean sizeChanged = layerList.addAll(index, c);
        if (sizeChanged) {
            firePropertyChange(SIZE_PROPERTY_NAME, oldSize, layerList.size());
        }
        return sizeChanged;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        final int oldSize = layerList.size();
        final boolean b = layerList.removeAll(c);
        if (b) {
            firePropertyChange(SIZE_PROPERTY_NAME, oldSize, layerList.size());
            for (Object object : c) {
                uninstallPCD(((GraphicalLayer) object));
            }
        }
        return b;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        final int oldSize = layerList.size();
        final boolean sizeChanged = layerList.retainAll(c);
        if (sizeChanged) {
            firePropertyChange(SIZE_PROPERTY_NAME, oldSize, layerList.size());
        }
        return sizeChanged;
    }

    @Override
    public GraphicalLayer set(int index, GraphicalLayer layer) {
        final int oldSize = layerList.size();
        final GraphicalLayer oldValue = layerList.set(index, layer);
        if (oldValue != null) {
            uninstallPCD(oldValue);
            firePropertyChange(SIZE_PROPERTY_NAME, oldSize, layerList.size());
        }
        installPCD(layer);
        return oldValue;
    }

    @Override
    public boolean remove(Object object) {
        final GraphicalLayer layer = (GraphicalLayer) object;
        final int oldSize = layerList.size();
        final boolean b = layerList.remove(layer);
        if (b) {
            uninstallPCD(layer);
            firePropertyChange(SIZE_PROPERTY_NAME, oldSize, layerList.size());
        }
        return b;
    }


    @Override
    public GraphicalLayer remove(int index) {
        final int oldSize = layerList.size();
        final GraphicalLayer layer = layerList.remove(index);
        if (layer != null) {
            uninstallPCD(layer);
            firePropertyChange(SIZE_PROPERTY_NAME, oldSize, layerList.size());
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
        final int oldSize = layerList.size();
        for (GraphicalLayer layer : layerList) {
            uninstallPCD(layer);
        }
        layerList.clear();
        firePropertyChange(SIZE_PROPERTY_NAME, oldSize, layerList.size());

    }

    @Override
    public GraphicalLayer get(int index) {
        return layerList.get(index);
    }

    @Override
    public int size() {
        return layerList.size();
    }

    @Override
    public Iterator<GraphicalLayer> iterator() {
        return layerList.iterator();
    }

    @Override
    public ListIterator<GraphicalLayer> listIterator() {
        return layerList.listIterator();
    }

    @Override
    public ListIterator<GraphicalLayer> listIterator(int index) {
        return layerList.listIterator(index);
    }

    @Override
    public List<GraphicalLayer> subList(int fromIndex, int toIndex) {
        return layerList.subList(fromIndex, toIndex);
    }

    @Override
    public boolean equals(Object object) {
        return object == this
                || object.getClass() == CollectionLayer.class
                && ((com.bc.layer.CollectionLayer) object).layerList.equals(layerList);
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

    ///////////////////////////////////////////////////////////////////////////////////
    // PC support

    private class PropertyChangeDelegate implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent evt) {
            firePropertyChange(evt);
        }
    }

    private void installPCD(GraphicalLayer layer) {
        layer.addPropertyChangeListener(propertyChangeDelegate);
    }

    private void uninstallPCD(GraphicalLayer layer) {
        layer.removePropertyChangeListener(propertyChangeDelegate);
    }

}