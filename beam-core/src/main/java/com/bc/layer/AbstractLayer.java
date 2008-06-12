/*
 * $Id: AbstractLayer.java,v 1.1.1.1 2006/09/11 08:16:43 norman Exp $
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

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Norman Fomferra (norman.fomferra@brockmann-consult.de)
 * @version $Revision$ $Date$
 */
public abstract class AbstractLayer implements Layer {

    private List<LayerChangeListener> listenerList;
    private Map<String, Object> propertyMap;
    private boolean layerChangeFireingSuspended;
    private boolean dirty;

    protected AbstractLayer() {
        this.listenerList = new ArrayList<LayerChangeListener>();
        this.propertyMap = new HashMap<String, Object>();
        setName(getClass().getName());
        setVisible(true);
        setSelected(false);
        setBoundingBox(new Rectangle());
    }

    /**
     * @return {@code true} if layer properties have changed since the last  {@link #fireLayerChanged()} call.
     * @see #fireLayerChanged()
     */
    public boolean isDirty() {
        return dirty;
    }

    /**
     * Gets the name for this layer.
     *
     * @return the name, never null
     */
    public String getName() {
        return (String) getPropertyValue("name");
    }

    /**
     * Sets the name for this layer.
     *
     * @param name the name, must not be null
     */
    public void setName(String name) {
        setPropertyValue("name", name);
    }

    /**
     * Gets the visible state of this layer.
     *
     * @return if true, the layer is visible, otherwise invisible
     */
    public boolean isVisible() {
        return (Boolean) getPropertyValue("visible");
    }

    /**
     * Sets the visible state of this layer.
     *
     * @param visible if true, the layer becomes visible, otherwise invisible
     */
    public void setVisible(boolean visible) {
        setPropertyValue("visible", visible ? Boolean.TRUE : Boolean.FALSE);
    }

    /**
     * Gets the selected state of this layer.
     *
     * @return if true, the layer is selected, otherwise deselected
     */
    public boolean isSelected() {
        return (Boolean) getPropertyValue("selected");
    }

    /**
     * Sets the selected state of this layer.
     *
     * @param selected if true, the layer becomes selected, otherwise deselected
     */
    public void setSelected(boolean selected) {
        setPropertyValue("selected", selected ? Boolean.TRUE : Boolean.FALSE);
    }

    /**
     * Gets the size (e.g. the bounding rectangle) of this layer in world coordinates.
     *
     * @return the size, never null
     */
    public Rectangle2D getBoundingBox() {
        return (Rectangle2D) getPropertyValue("boundingBox");
    }

    /**
     * Sets the size (e.g. the bounding rectangle) of this layer in world coordinates.
     *
     * @param boundingBox the size, never null
     */
    public void setBoundingBox(Rectangle2D boundingBox) {
        setPropertyValue("boundingBox", boundingBox);
    }

    /**
     * Gets a property value.
     *
     * @param name the property name
     * @return the property value
     */
    public Object getPropertyValue(String name) {
        return propertyMap.get(name);
    }

    /**
     * Sets a property value.
     *
     * @param name  the property name
     * @param value the new property value
     */
    public void setPropertyValue(String name, Object value) {
        Object oldValue = getPropertyValue(name);
        if (oldValue == value) {
            return;
        }
        if (oldValue != null && value != null && oldValue.equals(value)) {
            return;
        }
        propertyMap.put(name, value);
        fireLayerChanged();
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////
    // LayerChangeListener support

    public boolean isLayerChangeFireingSuspended() {
        return layerChangeFireingSuspended;
    }

    public void setLayerChangeFireingSuspended(boolean layerChangeFireingSuspended) {
        this.layerChangeFireingSuspended = layerChangeFireingSuspended;
        if (!layerChangeFireingSuspended && isDirty()) {
            fireLayerChanged();
        }
    }

    /**
     * Gets all layer manager listeners of this layer.
     */
    public LayerChangeListener[] getLayerChangeListeners() {
        return listenerList.toArray(new LayerChangeListener[listenerList.size()]);
    }

    /**
     * Adds a layer manager listener to this layer.
     */
    public void addLayerChangeListener(LayerChangeListener listener) {
        if (listener != null && !listenerList.contains(listener)) {
            listenerList.add(listener);
        }
    }

    /**
     * Removes a layer manager listener from this layer.
     */
    public void removeLayerChangeListener(LayerChangeListener listener) {
        if (listener != null) {
            listenerList.remove(listener);
        }
    }

    public void fireLayerChanged() {
        dirty = true;
        if (!isLayerChangeFireingSuspended()) {
            LayerChangeListener[] layerChangeListeners = getLayerChangeListeners();
            for (LayerChangeListener listener : layerChangeListeners) {
                listener.handleLayerChanged(this);
            }
            dirty = false;
        }
    }

    /**
     * Clears the listener lists and property map.
     *
     * @see Layer#dispose()
     */
    public void dispose() {
        if (listenerList != null) {
            listenerList.clear();
            listenerList = null;
        }
        if (propertyMap != null) {
            propertyMap.clear();
            propertyMap = null;
        }
    }
}
