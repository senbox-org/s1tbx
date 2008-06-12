/*
 * $Id: ProductNodeEvent.java,v 1.1.1.1 2006/09/11 08:16:45 norman Exp $
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
package org.esa.beam.framework.datamodel;

import java.util.EventObject;

import org.esa.beam.util.Guardian;

/**
 * A product node event informs a product change listener about the source of the notification.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @see ProductNodeListener
 */
public class ProductNodeEvent extends EventObject {

    private final int _id;
    private final String _propertyName;
    private Object _oldValue;
    public final static int NODE_CHANGED = 0;
    public final static int NODE_ADDED = 1;
    public final static int NODE_REMOVED = 2;
    public final static int NODE_DATA_CHANGED = 3;

    /**
     * Constructs a productEvent object.
     *
     * @param sourceNode the source class where the object originates
     * @param id         the event type
     */
    public ProductNodeEvent(ProductNode sourceNode, int id) {
        super(sourceNode);
        _propertyName = id == NODE_DATA_CHANGED ? "data" : null;
        _id = id;
    }

    /**
     * Constructs a productEvent object.
     *
     * @param sourceNode   the source class where the object originates
     * @param propertyName the name of the property that was changed
     */
    public ProductNodeEvent(final ProductNode sourceNode, final String propertyName) {
        this(sourceNode, propertyName, null);
    }

    /**
     * Constructs a productEvent object.
     *
     * @param sourceNode   the source class where the object originates.
     * @param propertyName the name of the property that was changed.
     * @param oldValue     the old value.
     */
    public ProductNodeEvent(final ProductNode sourceNode, final String propertyName, final Object oldValue) {
        super(sourceNode);
        Guardian.assertNotNull("propertyName", propertyName);
        _propertyName = propertyName;
        _id = NODE_CHANGED;
        _oldValue = oldValue;
    }

    /**
     * Retrieves a reference to the originating object, i.e. the one who fired the event.
     *
     * @return the originating object
     */
    public ProductNode getSourceNode() {
        return (ProductNode) getSource();
    }

    /**
     * Gets the name of the property that was changed.
     *
     * @return the name of the property that was changed.
     */
    public String getPropertyName() {
        return _propertyName;
    }

    /**
     * Gets the event type.
     *
     * @return the event type.
     */
    public int getId() {
        return _id;
    }

    /**
     * Gets the old value if there is any given in the constructor.
     *
     * @return the old value.
     */
    public Object getOldValue() {
        return _oldValue;
    }

    public String toString() {
        return getClass().getName() +
               " [sourceNode=" + getSourceNode() +
               ", propertyName=" + getPropertyName() +
               ", id=" + getId() + "]";
    }
}
