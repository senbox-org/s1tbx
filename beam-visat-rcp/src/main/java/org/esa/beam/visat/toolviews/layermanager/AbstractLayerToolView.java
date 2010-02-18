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
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.visat.VisatApp;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.BorderLayout;
import java.awt.Container;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import com.bc.ceres.glayer.Layer;

/**
 * Layer manager tool view.
 * <p/>
 * <i>Note: This API is not public yet and may significantly change in the future. Use it at your own risk.</i>
 */
public abstract class AbstractLayerToolView extends AbstractToolView {

    private AppContext appContext;
    private JPanel controlPanel;
    private ProductSceneView selectedView;
    private Layer selectedLayer;
    private final SelectedLayerPCL selectedLayerPCL;

    protected AbstractLayerToolView() {
        appContext = VisatApp.getApp();
        selectedLayerPCL = new SelectedLayerPCL();
    }

    protected AppContext getAppContext() {
        return appContext;
    }

    protected ProductSceneView getSelectedView() {
        return selectedView;
    }

    protected Layer getSelectedLayer() {
        return selectedLayer;
    }

    protected JPanel getControlPanel() {
        return controlPanel;
    }

    @Override
    protected JComponent createControl() {
        controlPanel = new JPanel(new BorderLayout());
        controlPanel.setBorder(new EmptyBorder(4, 4, 4, 4));

        final ProductSceneView sceneView = appContext.getSelectedProductSceneView();
        setSelectedView(sceneView);

        VisatApp.getApp().addInternalFrameListener(new LayerManagerIFL());

        return controlPanel;
    }

    /**
     * A view closed.
     *
     * @param view The view.
     */
    protected void viewOpened(ProductSceneView view) {
    }

    /**
     * A view opened.
     *
     * @param view The view.
     */
    protected void viewClosed(ProductSceneView view) {
    }

    /**
     * The selected view changed.
     *
     * @param oldView The old selected view. May be null.
     * @param newView The new selected view. May be null.
     */
    protected void viewSelectionChanged(ProductSceneView oldView, ProductSceneView newView) {
    }

    /**
     * The selected layer changed.
     *
     * @param oldLayer The old selected layer. May be null.
     * @param newLayer The new selected layer. May be null.
     */
    protected void layerSelectionChanged(Layer oldLayer, Layer newLayer) {
    }

    private void setSelectedView(final ProductSceneView newView) {
        ProductSceneView oldView = selectedView;
        if (newView != oldView) {
            if (oldView != null) {
                oldView.removePropertyChangeListener("selectedLayer", selectedLayerPCL);
            }
            if (newView != null) {
                newView.addPropertyChangeListener("selectedLayer", selectedLayerPCL);
            }
            selectedView = newView;
            viewSelectionChanged(oldView, newView);
            setSelectedLayer(newView != null ? newView.getSelectedLayer() : null);
        }
    }

    private void setSelectedLayer(final Layer newLayer) {
        Layer oldLayer = selectedLayer;
        if (newLayer != oldLayer) {
            selectedLayer = newLayer;
            layerSelectionChanged(oldLayer, newLayer);
        }
    }

    private class LayerManagerIFL extends InternalFrameAdapter {
        @Override
        public void internalFrameActivated(InternalFrameEvent e) {
            final Container contentPane = e.getInternalFrame().getContentPane();

            if (contentPane instanceof ProductSceneView) {
                final ProductSceneView view = (ProductSceneView) contentPane;
                setSelectedView(view);
                viewOpened(view);
            } else {
                setSelectedView(null);
            }
        }

        @Override
        public void internalFrameDeactivated(InternalFrameEvent e) {
            final Container container = e.getInternalFrame().getContentPane();

            if (container instanceof ProductSceneView) {
                setSelectedView(null);
            }
        }

        @Override
        public void internalFrameClosed(InternalFrameEvent e) {
            final Container contentPane = e.getInternalFrame().getContentPane();

            if (contentPane instanceof ProductSceneView) {
                final ProductSceneView view = (ProductSceneView) contentPane;
                viewClosed(view);
                final ProductSceneView selectedView = VisatApp.getApp().getSelectedProductSceneView();
                if (AbstractLayerToolView.this.selectedView == selectedView) {
                    setSelectedView(null);
                }
            }
        }
    }

    private class SelectedLayerPCL implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (getSelectedView() != null) {
                setSelectedLayer(getSelectedView().getSelectedLayer());
            }
        }
    }
}