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

package org.esa.beam.visat.actions;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerFilter;
import com.bc.ceres.glayer.support.LayerUtils;
import com.bc.ceres.swing.figure.Interactor;
import com.bc.ceres.swing.figure.AbstractInteractorInterceptor;
import org.esa.beam.framework.datamodel.VectorDataNode;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.product.VectorDataLayer;
import org.esa.beam.framework.ui.product.VectorDataLayerFilterFactory;
import org.esa.beam.visat.VisatActivator;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.InputEvent;
import java.util.List;


public class InsertFigureInteractorInterceptor extends AbstractInteractorInterceptor {

    @Override
    public boolean interactionAboutToStart(Interactor interactor, InputEvent inputEvent) {
        final ProductSceneView productSceneView = getProductSceneView(inputEvent);
        if (productSceneView == null) {
            return false;
        }

        final LayerFilter geometryFilter = VectorDataLayerFilterFactory.createGeometryFilter();

        Layer layer = productSceneView.getSelectedLayer();
        if (geometryFilter.accept(layer)) {
            layer.setVisible(true);
            return true;
        }

        List<Layer> layers = LayerUtils.getChildLayers(productSceneView.getRootLayer(),
                                                       LayerUtils.SEARCH_DEEP, geometryFilter);

        VectorDataLayer vectorDataLayer;
        if (layers.isEmpty()) {
            VectorDataNode vectorDataNode = CreateVectorDataNodeAction.createDefaultVectorDataNode(productSceneView.getProduct());
            LayerFilter nodeFilter = VectorDataLayerFilterFactory.createNodeFilter(vectorDataNode);
            productSceneView.getVectorDataCollectionLayer(true);
            vectorDataLayer = (VectorDataLayer) LayerUtils.getChildLayer(productSceneView.getRootLayer(),
                                                                         LayerUtils.SEARCH_DEEP, nodeFilter);
        } else if (layers.size() == 1) {
            vectorDataLayer = (VectorDataLayer) layers.get(0);
        } else {
            vectorDataLayer = showSelectLayerDialog(productSceneView, layers);
        }
        if (vectorDataLayer == null) {
            // = Cancel
            return false;
        }
        productSceneView.setSelectedLayer(vectorDataLayer);
        if (productSceneView.getSelectedLayer() == vectorDataLayer) {
            vectorDataLayer.setVisible(true);
            return true;
        }
        return false;
    }

    private VectorDataLayer showSelectLayerDialog(ProductSceneView productSceneView, List<Layer> layers) {
        String[] layerNames = new String[layers.size()];
        for (int i = 0; i < layerNames.length; i++) {
            layerNames[i] = layers.get(i).getName();
        }
        JList listBox = new JList(layerNames);
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.add(new JLabel("Please select a geometry container:"), BorderLayout.NORTH);
        panel.add(new JScrollPane(listBox), BorderLayout.CENTER);
        ModalDialog dialog = new ModalDialog(SwingUtilities.getWindowAncestor(productSceneView),
                                             "Select Geometry Container",
                                             ModalDialog.ID_OK_CANCEL_HELP, "");
        dialog.setContent(panel);
        int i = dialog.show();
        if (i == ModalDialog.ID_OK) {
            final int index = listBox.getSelectedIndex();
            if (index >= 0) {
                return (VectorDataLayer) layers.get(index);
            }
        }
        return null;
    }

    private ProductSceneView getProductSceneView(InputEvent event) {
        ProductSceneView productSceneView = null;
        Component component = event.getComponent();
        while (component != null) {
            if (component instanceof ProductSceneView) {
                productSceneView = (ProductSceneView) component;
                break;
            }
            component = component.getParent();
        }
        return productSceneView;
    }

}
