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

import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.visat.VisatApp;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.BorderLayout;
import java.awt.Container;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import com.bc.ceres.glayer.Layer;

/**
 * Layer manager tool view.
 */
public class LayerManagerToolView extends AbstractToolView {

    private JPanel panel;
    private WeakHashMap<ProductSceneView, LayerManagerForm> layerManagerMap;
    private ProductSceneView activeView;
    private LayerManagerForm activeForm;
    private final SelectedLayerPCL selectedLayerPCL;
    private final LayerSelectionListener activeFormLSL;

    public LayerManagerToolView() {
        selectedLayerPCL = new SelectedLayerPCL();
        activeFormLSL = new LayerSelectionListener() {
            public void layerSelectionChanged(Layer selectedLayer) {
                if (activeView != null) {
                    activeView.setSelectedLayer(selectedLayer);
                }
            }
        };
    }


    @Override
    protected JComponent createControl() {
        panel = new JPanel(new BorderLayout());
        layerManagerMap = new WeakHashMap<ProductSceneView, LayerManagerForm>();

        final ProductSceneView sceneView = VisatApp.getApp().getSelectedProductSceneView();
        setProductSceneView(sceneView);

        VisatApp.getApp().addInternalFrameListener(new LayerManagerIFL());

        return panel;
    }


    private void setProductSceneView(final ProductSceneView view) {
        if (panel.getComponentCount() > 0) {
            panel.remove(0);
        }

        if (view != null) {
            if (layerManagerMap.containsKey(view)) {
                activeForm = layerManagerMap.get(view);
            } else {
                activeForm = new LayerManagerForm(view.getRootLayer());
                activeForm.addLayerSelectionListener(activeFormLSL);
                layerManagerMap.put(view, activeForm);
            }
            panel.add(activeForm.getControl(), BorderLayout.CENTER);
        } else {
            activeForm = null;
        }

        panel.validate();
        panel.repaint();

        if (activeView != null) {
            activeView.removePropertyChangeListener("selectedLayer", selectedLayerPCL);
        }
        activeView = view;
        if (activeView != null) {
            activeView.addPropertyChangeListener("selectedLayer", selectedLayerPCL);
        }
    }

    private class LayerManagerIFL extends InternalFrameAdapter {
        @Override
        public void internalFrameActivated(InternalFrameEvent e) {
            final Container container = e.getInternalFrame().getContentPane();

            if (container instanceof ProductSceneView) {
                setProductSceneView((ProductSceneView) container);
            } else {
                setProductSceneView(null);
            }
        }

        @Override
        public void internalFrameDeactivated(InternalFrameEvent e) {
            final Container container = e.getInternalFrame().getContentPane();

            if (container instanceof ProductSceneView) {
                setProductSceneView(null);
            }
        }

        @Override
        public void internalFrameClosed(InternalFrameEvent e) {
            final Container contentPane = e.getInternalFrame().getContentPane();

            if (contentPane instanceof ProductSceneView) {

                final ProductSceneView view = VisatApp.getApp().getSelectedProductSceneView();
                if (activeView == view) {
                    setProductSceneView(null);
                }
                final LayerManagerForm form = layerManagerMap.get(view);
                if (form != null) {
                    form.removeLayerSelectionListener(activeFormLSL);
                }
                layerManagerMap.remove(view);
            }
        }
    }

    private class SelectedLayerPCL implements PropertyChangeListener {

        public void propertyChange(PropertyChangeEvent evt) {
            if (activeForm != null && activeView != null) {
                activeForm.setSelectedLayer(activeView.getSelectedLayer());
            }
        }
    }
}
