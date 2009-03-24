/* 
 * Copyright (C) 2002-2008 by Brockmann Consult
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
package org.esa.beam.visat.toolviews.layermanager;

import javax.swing.JPanel;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.support.AbstractLayerListener;

import java.awt.BorderLayout;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;

/**
 * Layer manager tool view.
 */
public class LayerEditorToolView extends AbstractLayerToolView {

    private final LayerEditorForm.Empty emptyForm;
    private LayerEditorForm activeForm;
    private final MyAbstractLayerListener layerListener;

    public LayerEditorToolView() {
        emptyForm = new LayerEditorForm.Empty(getAppContext());
        layerListener = new MyAbstractLayerListener();
    }

    @Override
    protected void layerSelectionChanged(Layer oldLayer, Layer newLayer) {

        if (oldLayer != null) {
            oldLayer.removeListener(layerListener);
        }
        if (newLayer != null) {
            newLayer.addListener(layerListener);
        }

        final JPanel controlPanel = getControlPanel();

        if (controlPanel.getComponentCount() > 0) {
            controlPanel.remove(0);
        }

        if (getSelectedView() != null) {
            activeForm = getOrCreateActiveForm(newLayer);
            controlPanel.add(activeForm.getFormControl(), BorderLayout.CENTER);
        } else {
            activeForm = null;
        }

        controlPanel.validate();
        controlPanel.repaint();
    }

    private LayerEditorForm getOrCreateActiveForm(Layer layer) {
        // todo - select LayerUI from Layer.getType()
        if (layer != null) {
            return new LayerEditorForm.NonEmpty(getAppContext(), layer);
        } else {
            return emptyForm;
        }
    }

    private void updateFormControl() {
        if (activeForm != null) {
            activeForm.updateFormControl();
        }
    }

    private class MyAbstractLayerListener extends AbstractLayerListener {
        @Override
        public void handleLayerPropertyChanged(Layer layer, PropertyChangeEvent event) {
            updateFormControl();
        }

        @Override
        public void handleLayerDataChanged(Layer layer, Rectangle2D modelRegion) {
            updateFormControl();
        }

        @Override
        public void handleLayersAdded(Layer parentLayer, Layer[] childLayers) {
            updateFormControl();
        }

        @Override
        public void handleLayersRemoved(Layer parentLayer, Layer[] childLayers) {
            updateFormControl();
        }
    }
}