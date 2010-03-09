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

import com.bc.ceres.glayer.Layer;
import com.jidesoft.docking.DockableFrame;
import com.jidesoft.docking.event.DockableFrameAdapter;
import com.jidesoft.docking.event.DockableFrameEvent;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.product.ProductTreeListenerAdapter;
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
    private String prefixTitle;

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

        prefixTitle = getDescriptor().getTitle();
        updateTitle();

        DockableFrame dockableFrame = (DockableFrame) getContext().getPane().getControl();
        dockableFrame.addDockableFrameListener(new DockableFrameAdapter() {
            @Override
            public void dockableFrameActivated(DockableFrameEvent dockableFrameEvent) {
                if (selectedView != null) {
                    final RasterDataNode raster = selectedView.getRaster();
                    VisatApp.getApp().getProductTree().select(raster);
                }
            }
        });

        final ProductSceneView sceneView = appContext.getSelectedProductSceneView();
        setSelectedView(sceneView);

        VisatApp.getApp().addProductTreeListener(new LayerPTL());
        VisatApp.getApp().addInternalFrameListener(new LayerManagerIFL());

        return controlPanel;
    }

    /**
     * A view opened.
     *
     * @param view The view.
     */
    protected void viewOpened(ProductSceneView view) {
    }

    /**
     * A view closed.
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

    private void updateTitle() {
        final String suffix;
        final ProductSceneView view = getSelectedView();
        if (view != null) {
            suffix = " - " + view.getRaster().getDisplayName();
        } else {
            suffix = "";
        }
        getDescriptor().setTitle(prefixTitle + suffix);
    }

    private class LayerManagerIFL extends InternalFrameAdapter {

        @Override
        public void internalFrameActivated(InternalFrameEvent e) {
            final Container contentPane = e.getInternalFrame().getContentPane();

            if (contentPane instanceof ProductSceneView) {
                final ProductSceneView view = (ProductSceneView) contentPane;
                setSelectedView(view);
                viewOpened(view);
                updateTitle();
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
                if (AbstractLayerToolView.this.selectedView == view) {
                    setSelectedView(null);
                }
                updateTitle();
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

    private class LayerPTL extends ProductTreeListenerAdapter {

        @Override
        public void productNodeSelected(ProductNode productNode, int clickCount) {
            if (productNode instanceof RasterDataNode) {
                updateTitle();
            }
        }
    }

}