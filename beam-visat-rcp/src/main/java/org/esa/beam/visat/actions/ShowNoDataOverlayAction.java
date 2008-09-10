package org.esa.beam.visat.actions;

import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.visat.VisatApp;

import javax.swing.*;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.*;

public class ShowNoDataOverlayAction extends ExecCommand {

    private boolean initialized;
    private ProductSceneView.LayerContentListener layerContentListener;

    @Override
    public void actionPerformed(CommandEvent event) {
        final ProductSceneView psv = VisatApp.getApp().getSelectedProductSceneView();
        if (psv != null) {
            psv.setNoDataOverlayEnabled(isEnabled() && isSelected());
        }
    }

    @Override
    public void updateState(CommandEvent event) {
        VisatApp visatApp = VisatApp.getApp();

        if (!initialized) {
            init(visatApp);
            initialized = true;
        }

        updateState(visatApp.getSelectedProductSceneView());
    }

    private void updateState(ProductSceneView psv) {
        setEnabled(psv != null && psv.getRaster().isValidMaskUsed());
        setSelected(psv != null && psv.isNoDataOverlayEnabled());
    }

    private void init(final VisatApp visatApp) {
        final ProductNodeListenerAdapter productNodeListener = new ProductNodeListener(visatApp);

        visatApp.getProductManager().addListener(new ProductManager.Listener() {
            @Override
            public void productAdded(ProductManager.Event event) {
                event.getProduct().addProductNodeListener(productNodeListener);
            }

            @Override
            public void productRemoved(ProductManager.Event event) {
                event.getProduct().removeProductNodeListener(productNodeListener);
            }
        });

        visatApp.addInternalFrameListener(new InternalFrameListener());
    }

    private static ProductSceneView getProductSceneView(final JInternalFrame internalFrame) {
        final Container contentPane = internalFrame.getContentPane();

        if (contentPane instanceof ProductSceneView) {
            return (ProductSceneView) contentPane;
        }

        return null;
    }

    private class ProductNodeListener extends ProductNodeListenerAdapter {
        private final VisatApp visatApp;

        public ProductNodeListener(VisatApp visatApp) {
            this.visatApp = visatApp;
        }

        @Override
        public void nodeChanged(ProductNodeEvent event) {
            updateAllRelatedNoDataOverlays(event);
        }

        @Override
        public void nodeDataChanged(ProductNodeEvent event) {
            updateAllRelatedNoDataOverlays(event);
        }

        private void updateAllRelatedNoDataOverlays(final ProductNodeEvent event) {
            final ProductNode productNode = event.getSourceNode();
            if (productNode instanceof RasterDataNode) {
                final RasterDataNode rasterDataNode = (RasterDataNode) productNode;
                if (RasterDataNode.isValidMaskProperty(event.getPropertyName())) {
                    updateAllNoDataOverlays(visatApp, rasterDataNode);
                }
            }
        }

        private void updateAllNoDataOverlays(final VisatApp visatApp, final RasterDataNode rasterDataNode) {
            final JInternalFrame[] internalFrames = visatApp.findInternalFrames(rasterDataNode);
            for (JInternalFrame internalFrame : internalFrames) {
                final ProductSceneView psv = getProductSceneView(internalFrame);
                if (psv != null) {
                    updateState(psv);
                }
            }
        }
    }

    private class InternalFrameListener extends InternalFrameAdapter {
        private LayerContentListener layerContentListener;

        @Override
        public void internalFrameOpened(InternalFrameEvent e) {
            final ProductSceneView view = getProductSceneView(e.getInternalFrame());

            layerContentListener = new LayerContentListener(view);
            view.addLayerContentListener(layerContentListener);
        }

        @Override
        public void internalFrameActivated(InternalFrameEvent e) {
            final ProductSceneView view = getProductSceneView(e.getInternalFrame());
            updateState(view);
        }

        @Override
        public void internalFrameClosed(InternalFrameEvent e) {
            final ProductSceneView view = getProductSceneView(e.getInternalFrame());
            view.removeLayerContentListener(layerContentListener);
        }
    }

    private class LayerContentListener implements ProductSceneView.LayerContentListener {
        private final ProductSceneView view;

        public LayerContentListener(ProductSceneView view) {
            this.view = view;
        }

        @Override
        public void layerContentChanged(RasterDataNode raster) {
            updateState(view);
        }
    }
}
