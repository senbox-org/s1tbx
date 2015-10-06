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
package org.esa.snap.core.datamodel;

/**
 * A listener adapter which listens to product internal changes.
 * This class is an emply implementation of the {@link ProductNodeListener} interface and is defined to overwrite
 * some method you need without implementing methods which are not needed.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public class ProductNodeListenerAdapter implements ProductNodeListener {

    /**
     * Overwrite this method if you want to be notified when a node changed.
     *
     * @param event the product node which the listener to be notified
     */
    @Override
    public void nodeChanged(ProductNodeEvent event) {
    }

    /**
     * Overwrite this method if you want to be notified when a node's data changed.
     *
     * @param event the product node which the listener to be notified
     */
    @Override
    public void nodeDataChanged(ProductNodeEvent event) {
    }

    /**
     * Overwrite this method if you want to be notified when a node was added.
     *
     * @param event the product node which the listener to be notified
     */
    @Override
    public void nodeAdded(ProductNodeEvent event) {
    }

    /**
     * Overwrite this method if you want to be notified when a node was removed.
     *
     * @param event the product node which the listener to be notified
     */
    @Override
    public void nodeRemoved(ProductNodeEvent event) {
    }
}


