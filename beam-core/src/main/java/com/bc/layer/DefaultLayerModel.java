/*
 * $Id: DefaultLayerModel.java,v 1.1.1.1 2006/09/11 08:16:43 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package com.bc.layer;

import com.bc.view.ViewModel;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * A default layer manager implementation.
 *
 * @author Norman Fomferra (norman.fomferra@brockmann-consult.de)
 * @version $Revision$ $Date$
 */
public class DefaultLayerModel implements LayerModel {

    private List<Layer> layerList;
    private List<LayerModelChangeListener> listenerList;
    private LayerChangeHandler layerChangeHandler;
    private boolean layerModelChangeFireingSuspended;

    public DefaultLayerModel() {
        layerList = new ArrayList<Layer>();
        listenerList = new ArrayList<LayerModelChangeListener>();
        layerChangeHandler = new LayerChangeHandler();
    }

    public boolean isLayerModelChangeFireingSuspended() {
        return layerModelChangeFireingSuspended;
    }

    public void setLayerModelChangeFireingSuspended(boolean layerModelChangeFireingSuspended) {
        this.layerModelChangeFireingSuspended = layerModelChangeFireingSuspended;
    }

    public Rectangle2D getVisibleBoundingBox(Rectangle2D r) {
        if (r == null) {
            r = new Rectangle2D.Double();
        }
        r.setRect(0, 0, 0, 0);
        final int n = getLayerCount();
        for (int i = 0; i < n; i++) {
            if (getLayer(i).isVisible()) {
                final Rectangle2D r2 = getLayer(i).getBoundingBox();
                r.add(r2);
            }
        }
        return r;
    }

    public void dispose() {
        final Layer[] layers = getLayers();
        layerList.clear();
        listenerList.clear();
        final int n = layers.length;
        for (int i = n - 1; i >= 0; i--) {
            layers[i].removeLayerChangeListener(layerChangeHandler);
        }
        for (int i = n - 1; i >= 0; i--) {
            layers[i].dispose();
        }
    }

    public void draw(Graphics2D g2d, ViewModel viewModel) {
        final int n = getLayerCount();
        for (int i = 0; i < n; i++) {
            if (getLayer(i).isVisible()) {
                getLayer(i).draw(g2d, viewModel);
            }
        }
    }

    public Layer[] getLayers() {
        return layerList.toArray(new Layer[layerList.size()]);
    }

    public int getLayerCount() {
        return layerList.size();
    }

    public Layer getLayer(int index) {
        return layerList.get(index);
    }

    public Layer getLayer(String name) {
        Layer foundLayer = null;
        for (Layer layer : layerList) {
            if(name.equals(layer.getName())) {
                foundLayer = layer;
                break;
            }
        }
        return foundLayer;
    }

    public void addLayer(Layer layer) {
        if (layer != null && !layerList.contains(layer)) {
            layerList.add(layer);
            layer.addLayerChangeListener(layerChangeHandler);
            fireLayerAdded(layer);
        }
    }

    public void removeLayer(Layer layer) {
        if (layer != null && layerList.contains(layer)) {
            layerList.remove(layer);
            layer.removeLayerChangeListener(layerChangeHandler);
            fireLayerRemoved(layer);
        }
    }

    /**
     * Gets all layer manager listeners of this layer.
     */
    public LayerModelChangeListener[] getLayerModelChangeListeners() {
        return listenerList.toArray(new LayerModelChangeListener[listenerList.size()]);
    }

    /**
     * Adds a layer manager listener to this layer.
     */
    public void addLayerModelChangeListener(LayerModelChangeListener listener) {
        if (listener != null && !listenerList.contains(listener)) {
            listenerList.add(listener);
        }
    }

    /**
     * Removes a layer manager listener from this layer.
     */
    public void removeLayerModelChangeListener(LayerModelChangeListener listener) {
        if (listener != null) {
            listenerList.remove(listener);
        }
    }

    public void fireLayerModelChanged() {
        if (!isLayerModelChangeFireingSuspended()) {
            for (LayerModelChangeListener l : listenerList) {
                l.handleLayerModelChanged(this);
            }
        }
    }

    protected void fireLayerAdded(Layer layer) {
        if (!isLayerModelChangeFireingSuspended()) {
            for (LayerModelChangeListener l : listenerList) {
                l.handleLayerAdded(this, layer);
            }
            fireLayerModelChanged();
        }
    }

    protected void fireLayerRemoved(Layer layer) {
        if (!isLayerModelChangeFireingSuspended()) {
            for (LayerModelChangeListener l : listenerList) {
                l.handleLayerRemoved(this, layer);
            }
            fireLayerModelChanged();
        }
    }

    protected void fireLayerChanged(Layer layer) {
        if (!isLayerModelChangeFireingSuspended()) {
            for (LayerModelChangeListener l : listenerList) {
                l.handleLayerChanged(this, layer);
            }
            fireLayerModelChanged();
        }
    }

    private class LayerChangeHandler implements LayerChangeListener {

        public void handleLayerChanged(Layer layer) {
            fireLayerChanged(layer);
        }
    }
}
