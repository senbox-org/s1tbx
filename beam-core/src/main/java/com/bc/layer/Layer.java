/*
 * $Id: Layer.java,v 1.1.1.1 2006/09/11 08:16:43 norman Exp $
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
 * A graphics layer.
 * @author Norman Fomferra (norman.fomferra@brockmann-consult.de)
 * @version $Revision$ $Date$
 */
public interface Layer {

    /**
     * Gets the name for this layer.
     * @return the name, never null
     */
    String getName();

    /**
     * Sets the name for this layer.
     * @param name the name, must not be null
     */
    void setName(String name);

    /**
     * Gets the visible state of this layer.
     * @return if true, the layer is visible, otherwise invisible
     */
    boolean isVisible();

    /**
     * Sets the visible state of this layer.
     * @param visible if true, the layer becomes visible, otherwise invisible
     */
    void setVisible(boolean visible);

    /**
     * Gets the selected state of this layer.
     * @return if true, the layer is selected, otherwise deselected
     */
    boolean isSelected();

    /**
     * Sets the selected state of this layer.
     * @param selected if true, the layer becomes selected, otherwise deselected
     */
    void setSelected(boolean selected);

    /**
     * Gets the bounding box rectangle of this layer in world coordinates.
     * @return the bounding box rectangle, never null, but can be empty
     */
    Rectangle2D getBoundingBox();

    /**
     * Sets the bounding box rectangle of this layer in world coordinates.
     * @param boundingBox the bounding box rectangle, never null, but can be empty
     */
    void setBoundingBox(Rectangle2D boundingBox);

    /**
     * Draws the layer using the given 2D graphics context.
     * The graphics context expects world coordinates.
     * @param g2d the 2D graphics context, never null
     * @param viewModel the view model
     */
    void draw(Graphics2D g2d, ViewModel viewModel);

    /**
     * Releases all resources of this layer and it's children.
     * Any method invocations after the <code>dispose</code>
     * method has been called, are undefined.
     */
    void dispose();

    /**
     * Tests if fireing layer change events is suspended.
     * @return true, if so
     */
    boolean isLayerChangeFireingSuspended();

    /**
     * Suspends the fireing of layer change events.
     * @param layerChangeFireingSuspended true, if suspended
     */
    void setLayerChangeFireingSuspended(boolean layerChangeFireingSuspended);

    /**
     * Gets all layer change listeners of this layer.
     */
    LayerChangeListener[] getLayerChangeListeners();

    /**
     * Adds a new layer change listener to this layer.
     * @param listener a new change listener
     */
    void addLayerChangeListener(LayerChangeListener listener);

    /**
     * Removes an existing layer change listener from this layer.
     * @param listener an existing change listener
     */
    void removeLayerChangeListener(LayerChangeListener listener);

    /**
     * Notifies all listeners about a layer change.
     */
    void fireLayerChanged();
}
