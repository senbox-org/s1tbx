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

package com.bc.ceres.glayer;

import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;

public class TracingLayerListener implements LayerListener {

    public String trace = "";

    public void handleLayerPropertyChanged(Layer layer, PropertyChangeEvent event) {
        trace += event.getPropertyName() + ";";
    }

    public void handleLayerDataChanged(Layer layer, Rectangle2D region) {
        trace += "data " + region + ";";
    }

    public void handleLayersAdded(Layer parentLayer, Layer[] childLayers) {
        trace += "added " + childLayers.length + ";";
    }

    public void handleLayersRemoved(Layer parentLayer, Layer[] childLayers) {
        trace += "removed " + childLayers.length + ";";
    }
}
