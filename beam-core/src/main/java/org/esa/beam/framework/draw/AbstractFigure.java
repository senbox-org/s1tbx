/*
 * $Id: AbstractFigure.java,v 1.1 2006/10/10 14:47:22 norman Exp $
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
package org.esa.beam.framework.draw;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.event.EventListenerList;

/**
 * An abstract implementation the <code>Figure</code> interface.
 *
 * @author Norman Fomferra
 * @version $Revision$  $Date$
 */
public abstract class AbstractFigure implements Figure {

    private int _zValue;
    private Map<String, Object> _attributes;
    private EventListenerList _listenerList;
    private PropertyChangeSupport _propertyChangeSupport;

    /**
     * Constructs an abstract figure.
     *
     * @param attributes the default attributes for the new figure, can be <code>null</code>
     */
    protected AbstractFigure(Map<String, Object> attributes) {
        if (attributes != null) {
            _attributes = new HashMap<String, Object>();
            _attributes.putAll(attributes);
        }
    }

    /**
     * Returns the handles used to manipulate the figure. <code>createHandles</code> is a Factory Method for creating
     * handle objects.
     *
     * @return a Vector of <code>FigureHandle</code>
     *
     * @see FigureHandle
     */
    @Override
    public FigureHandle[] createHandles() {
        return null;
    }

    /**
     * Returns an Enumeration of the figures contained in this figure
     */
    @Override
    public Figure[] getFigures() {
        return null;
    }

    /**
     * Returns the figure that contains the given point.
     */
    @Override
    public Figure findFigureInside(double x, double y) {
        return null;
    }

    /**
     * Checks whether the given figure is contained in this figure.
     */
    @Override
    public boolean includes(Figure figure) {
        return false;
    }

    /**
     * Decomposes a figure into its parts. A figure is considered as a part of itself.
     */
    @Override
    public Figure[] decompose() {
        return null;
    }

    /**
     * Releases a figure's resources. Release is called when a figure is removed from a drawing. Informs the listeners
     * that the figure is removed by calling figureRemoved.
     */
    @Override
    public void dispose() {
        if (_attributes != null) {
            _attributes.clear();
            _attributes = null;
        }
        if (_listenerList != null) {
            _listenerList = null;
        }
    }


    /**
     * Gets the z value (back-to-front ordering) of this figure. Z values are not guaranteed to not skip numbers.
     */
    @Override
    public int getZValue() {
        return _zValue;
    }

    /**
     * Sets the z value (back-to-front ordering) of this figure. Z values are not guaranteed to not skip numbers.
     */
    @Override
    public void setZValue(int zValue) {
        _zValue = zValue;
    }

    /**
     * Returns the attributes of this figure as a <code>Map</code>.
     */
    @Override
    public Map<String, Object> getAttributes() {
        return _attributes;
    }

    /**
     * Returns the named attribute or null if a a figure doesn't have an attribute. All figures support the attribute
     * names FillColor and FrameColor
     */
    @Override
    public Object getAttribute(String name) {
        if (_attributes == null) {
            return null;
        }
        return _attributes.get(name);
    }

    /**
     * Sets the named attribute to the new value
     */
    @Override
    public void setAttribute(String name, Object value) {
        if (_attributes == null) {
            _attributes = new HashMap<String, Object>();
        }
        Object oldValue = _attributes.get(name);
        if (oldValue == null && value != null
            || oldValue != null && !oldValue.equals(value)) {
            _attributes.put(name, value);
            if (_propertyChangeSupport != null) {
                _propertyChangeSupport.firePropertyChange(name, oldValue, value);
            }
        }
    }

    /**
     * Sets multiple attributes
     */
    @Override
    public void setAttributes(Map<String, Object> attributes) {
        for (final Map.Entry<String, Object> entry : attributes.entrySet()) {
            setAttribute(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Adds a listener for this figure.
     *
     * @param listener the listener to be added
     */
    @Override
    public void addFigureChangeListener(FigureChangeListener listener) {
        if (listener != null) {
            if (_listenerList == null) {
                _listenerList = new EventListenerList();
            }
            _listenerList.add(FigureChangeListener.class, listener);
        }
    }

    /**
     * Removes a listener for this figure.
     *
     * @param listener the listener to be removed
     */
    @Override
    public void removeFigureChangeListener(FigureChangeListener listener) {
        if (listener != null && _listenerList != null) {
            _listenerList.remove(FigureChangeListener.class, listener);
        }
    }


    /**
     * Returns a clone of this figure
     */
    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        if (listener == null) {
            return;
        }
        if (_propertyChangeSupport == null) {
            _propertyChangeSupport = new PropertyChangeSupport(this);
        }
        _propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        if (_propertyChangeSupport != null) {
            _propertyChangeSupport.removePropertyChangeListener(listener);
        }
    }
}
