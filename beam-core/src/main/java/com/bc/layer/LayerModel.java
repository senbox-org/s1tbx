/*
 * $Id: LayerModel.java,v 1.1.1.1 2006/09/11 08:16:43 norman Exp $
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


/**
 * A data model comprising multiple {@link Layer}s.
 *
 * @author Norman Fomferra (norman.fomferra@brockmann-consult.de)
 * @version $Revision$ $Date$
 */
public interface LayerModel {

    /**
     * Gets the total number of layer in this layer model.
     *
     * @return the  number of layer
     */
    int getLayerCount();

    /**
     * Gets the layer at the specified index.
     *
     * @param index the zero-based layer index
     * @return the layer at the given index, never null
     */
    Layer getLayer(int index);

    /**
     * Gets the layer by its name.
     *
     * @param name the name of the layer to retrieve
     * @return the layer with the given name, or {@code null} if not found
     */
    Layer getLayer(String name);

    /**
     * Adds a new layer to this model.
     *
     * @param layer the new layer, must not be null
     */
    void addLayer(Layer layer);

    /**
     * Removes an existing layer from this model.
     *
     * @param layer the existing layer, must not be null
     */
    void removeLayer(Layer layer);

    /**
     * Computes the bounding box given as sum of the bounding boxes of all visible layer.
     *
     * @return the bounding box
     */
    Rectangle2D getVisibleBoundingBox(Rectangle2D r);

    /**
     * Draws all visible layer of this model.
     *
     * @param g2d       the 2D graphics context
     * @param viewModel the current view model
     */
    void draw(Graphics2D g2d, ViewModel viewModel);

    /**
     * Releases all resources held by this model.
     * Method calls to this model after  <code>dispose</code> has been called, are undefined.
     */
    void dispose();

    /**
     * Tests if fireing layer model change events is suspended.
     *
     * @return true, if so
     */
    boolean isLayerModelChangeFireingSuspended();

    /**
     * Suspends the fireing of layer model change events.
     *
     * @param layerModelChangeFireingSuspended
     *         true, if suspended
     */
    void setLayerModelChangeFireingSuspended(boolean layerModelChangeFireingSuspended);

    /**
     * Gets all layer manager listeners of this layer.
     */
    LayerModelChangeListener[] getLayerModelChangeListeners();

    /**
     * Adds a layer manager listener to this layer.
     */
    void addLayerModelChangeListener(LayerModelChangeListener listener);

    /**
     * Removes a layer manager listener from this layer.
     */
    void removeLayerModelChangeListener(LayerModelChangeListener listener);

    /**
     * Notifies all listeners about a layer model change.
     */
    void fireLayerModelChanged();

}
