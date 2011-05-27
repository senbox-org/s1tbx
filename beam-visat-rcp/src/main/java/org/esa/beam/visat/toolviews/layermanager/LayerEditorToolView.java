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
package org.esa.beam.visat.toolviews.layermanager;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.support.AbstractLayerListener;
import org.esa.beam.framework.ui.layer.LayerEditor;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;

/**
 * Layer manager tool view.
 * <p/>
 * <i>Note: This API is not public yet and may significantly change in the future. Use it at your own risk.</i>
 *
 * @author Norman Fomferra
 * @version $Revision: $ $Date: $
 * @since BEAM 4.6
 */
public class LayerEditorToolView extends AbstractLayerToolView {

    static final String ID = LayerEditorToolView.class.getName();
    private final LayerEditor emptyLayerEditor;
    private LayerEditor activeEditor;
    private final LayerHandler layerHandler;

    public LayerEditorToolView() {
        layerHandler = new LayerHandler();
        emptyLayerEditor = LayerEditor.EMPTY;
    }

    @Override
    protected void layerSelectionChanged(Layer oldLayer, Layer newLayer) {

        if (oldLayer != null) {
            oldLayer.removeListener(layerHandler);
        }

        final JPanel controlPanel = getControlPanel();

        if (controlPanel.getComponentCount() > 0) {
            controlPanel.remove(0);
        }

        LayerEditor oldEditor = activeEditor;

        if (newLayer != null) {
            activeEditor = getLayerEditor(newLayer);
            getDescriptor().setTitle("Layer Editor - " + newLayer.getName());
        } else {
            activeEditor = emptyLayerEditor;
            getDescriptor().setTitle("Layer Editor");
        }

        if (oldEditor != null) {
            oldEditor.handleEditorDetached();
        }
        controlPanel.add(activeEditor.createControl(getAppContext(), newLayer), BorderLayout.CENTER);
        activeEditor.handleEditorAttached();
        activeEditor.handleLayerContentChanged();

        controlPanel.validate();
        controlPanel.repaint();

        if (newLayer != null) {
            newLayer.addListener(layerHandler);
        }

    }

    private LayerEditor getLayerEditor(Layer layer) {
        LayerEditor layerEditor = layer.getExtension(LayerEditor.class);
        if (layerEditor != null) {
            return layerEditor;
        }

        layerEditor = layer.getLayerType().getExtension(LayerEditor.class);
        if (layerEditor != null) {
            return layerEditor;
        }

        return emptyLayerEditor;
    }

    private class LayerHandler extends AbstractLayerListener {

        @Override
        public void handleLayerPropertyChanged(Layer layer, PropertyChangeEvent event) {
            activeEditor.handleLayerContentChanged();
        }

        @Override
        public void handleLayerDataChanged(Layer layer, Rectangle2D modelRegion) {
            activeEditor.handleLayerContentChanged();
        }

        @Override
        public void handleLayersAdded(Layer parentLayer, Layer[] childLayers) {
            activeEditor.handleLayerContentChanged();
        }

        @Override
        public void handleLayersRemoved(Layer parentLayer, Layer[] childLayers) {
            activeEditor.handleLayerContentChanged();
        }
    }
}