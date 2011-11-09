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
import com.bc.ceres.swing.selection.AbstractSelectionContext;
import com.bc.ceres.swing.selection.Selection;
import com.bc.ceres.swing.selection.SelectionContext;
import com.bc.ceres.swing.selection.support.DefaultSelection;
import org.esa.beam.framework.ui.product.ProductSceneView;

import javax.swing.*;
import java.awt.*;
import java.util.WeakHashMap;

/**
 * Layer manager tool view.
 * <p/>
 * <i>Note: This API is not public yet and may significantly change in the future. Use it at your own risk.</i>
 *
 * @author Norman Fomferra
 * @version $Revision: $ $Date: $
 * @since BEAM 4.6
 */
public class LayerManagerToolView extends AbstractLayerToolView {

    private WeakHashMap<ProductSceneView, LayerManagerForm> layerManagerMap;
    private LayerManagerForm activeForm;

    private LayerSelectionContext selectionContext;


    public LayerManagerToolView() {
    }

    @Override
    protected JComponent createControl() {
        layerManagerMap = new WeakHashMap<ProductSceneView, LayerManagerForm>();
        selectionContext = new LayerSelectionContext();
        return super.createControl();
    }

    @Override
    protected void viewClosed(ProductSceneView view) {
        layerManagerMap.remove(view);
    }

    @Override
    protected void viewSelectionChanged(ProductSceneView oldView, ProductSceneView newView) {
        realizeActiveForm();
    }

    @Override
    protected void layerSelectionChanged(Layer oldLayer, Layer selectedLayer) {
        if (activeForm != null) {
            activeForm.updateFormControl();
            selectionContext.fireSelectionChange(new DefaultSelection<Layer>(selectedLayer));
        }
    }

    @Override
    public SelectionContext getSelectionContext() {
        return selectionContext;
    }

    private void realizeActiveForm() {
        final JPanel controlPanel = getControlPanel();

        if (controlPanel.getComponentCount() > 0) {
            controlPanel.remove(0);
        }

        if (getSelectedView() != null) {
            activeForm = getOrCreateActiveForm(getSelectedView());
            controlPanel.add(activeForm.getFormControl(), BorderLayout.CENTER);
        } else {
            activeForm = null;
        }

        controlPanel.validate();
        controlPanel.repaint();
    }

    protected LayerManagerForm getOrCreateActiveForm(ProductSceneView view) {
        if (layerManagerMap.containsKey(view)) {
            activeForm = layerManagerMap.get(view);
        } else {
            activeForm = new LayerManagerForm(getAppContext(), getDescriptor().getHelpId());
            layerManagerMap.put(view, activeForm);
        }
        return activeForm;
    }

    private class LayerSelectionContext extends AbstractSelectionContext {

        @Override
        public void setSelection(Selection selection) {
            Object selectedValue = selection.getSelectedValue();
            if (selectedValue instanceof Layer) {
                setSelectedLayer((Layer) selectedValue);
            }
        }

        @Override
        public Selection getSelection() {
            Layer selectedLayer = getSelectedLayer();
            if (selectedLayer != null) {
                return new DefaultSelection<Layer>(selectedLayer);
            } else {
                return DefaultSelection.EMPTY;
            }
        }

        @Override
        protected void fireSelectionChange(Selection selection) {
            super.fireSelectionChange(selection);
        }
    }
}
