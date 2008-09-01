package org.esa.beam.visat.actions;

import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
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

    @Override
    public void actionPerformed(CommandEvent event) {
        ProductSceneView psv = VisatApp.getApp().getSelectedProductSceneView();
        if (psv != null) {
            updateNoDataLayer(psv);
        }
    }

    @Override
    public void updateState(CommandEvent event) {
        VisatApp visatApp = VisatApp.getApp();
        if (!initialized) {
            init(visatApp);
            initialized = true;
        }
        ProductSceneView psv = visatApp.getSelectedProductSceneView();
        updateCommandState(psv);
    }

    private void updateCommandState(ProductSceneView psv) {
        if (psv != null) {
            setEnabled(isNoDataOverlayApplicable(psv));
            setSelected(psv.isNoDataOverlayEnabled());
        } else {
            setEnabled(false);
        }
    }

    private void init(VisatApp visatApp) {
        registerProductNodeListener(visatApp);
        visatApp.addInternalFrameListener(new InternalFrameAdapter() {
            /**
             * Invoked when an internal frame is activated.
             */
            @Override
            public void internalFrameActivated(InternalFrameEvent e) {
                final ProductSceneView psv = getProductSceneView(e.getInternalFrame());
                updateCommandState(psv);
            }
        });

    }

    private static ProductSceneView getProductSceneView(final JInternalFrame internalFrame) {
        final Container contentPane = internalFrame.getContentPane();
        if (contentPane instanceof ProductSceneView) {
            return (ProductSceneView) contentPane;
        }
        return null;
    }


    /*
     * Creates a listener for product node changes, which we will add to all products.
     *
     */
    private void registerProductNodeListener(final VisatApp visatApp) {
        final ProductNodeListenerAdapter productNodeListener = new ProductNodeListenerAdapter() {
            @Override
            public void nodeChanged(ProductNodeEvent event) {
                maybeUpdateAllNoDataOverlays(event);
            }

            @Override
            public void nodeDataChanged(ProductNodeEvent event) {
                maybeUpdateAllNoDataOverlays(event);
            }

            private void maybeUpdateAllNoDataOverlays(final ProductNodeEvent event) {
                ProductNode productNode = event.getSourceNode();
                if (productNode instanceof RasterDataNode) {
                    RasterDataNode rasterDataNode = (RasterDataNode) productNode;
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
                        updateCommandState(psv);
                    }
                }
            }

        };

        // Register the listener for product node changes in all products
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
    }

    private void updateNoDataLayer(ProductSceneView psv) {
        if (isNoDataOverlaySelected()) {
            updateNoDataImage(psv);
        }
        psv.setNoDataOverlayEnabled(isNoDataOverlaySelected());
    }

    private static void updateNoDataImage(final ProductSceneView view) {

        final ProgressMonitorSwingWorker<Boolean, Object> swingWorker = new ProgressMonitorSwingWorker<Boolean, Object>(view, "Create No-Data Overlay") {

            @Override
            protected Boolean doInBackground(com.bc.ceres.core.ProgressMonitor pm) throws Exception {
                view.updateNoDataImage(pm);
                return true;
            }

            @Override
            public void done() {
                try {
                    get();
                } catch (Exception e) {
                    VisatApp.getApp().showErrorDialog("Unable to create no-data overlay image due to an error:\n" +
                            e.getMessage());
                    e.printStackTrace();
                }
            }
        };
        swingWorker.execute();
    }


    private boolean isNoDataOverlaySelected() {
        return isEnabled() && isSelected();
    }

    private static boolean isNoDataOverlayApplicable(ProductSceneView psv) {
        return psv != null && psv.getRaster().isValidMaskUsed();
    }
}
