package org.esa.beam.visat.actions;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.DialogProgressMonitor;
import com.bc.layer.Layer;
import com.bc.layer.LayerModel;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.layer.NoDataLayer;
import org.esa.beam.util.Debug;
import org.esa.beam.visat.VisatApp;

import javax.swing.JInternalFrame;
import javax.swing.SwingWorker;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.image.RenderedImage;

/**
 * Created by IntelliJ IDEA.
 * User: Norman
 * Date: 14.11.2006
 * Time: 15:59:53
 * To change this template use File | Settings | File Templates.
 */
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


    /**
     * Creates a listener for product node changes, which we will add to all products.
     *
     * @param visatApp
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
                for (int i = 0; i < internalFrames.length; i++) {
                    final ProductSceneView psv = getProductSceneView(internalFrames[i]);
                    if (psv != null) {
                        updateCommandState(psv);
                    }
                }
            }

            private boolean isNoDataOverlayRelevantPropertyName(final String propertyName) {
                return RasterDataNode.PROPERTY_NAME_NO_DATA_VALUE.equals(propertyName)
                        || RasterDataNode.PROPERTY_NAME_NO_DATA_VALUE_USED.equals(propertyName)
                        || RasterDataNode.PROPERTY_NAME_VALID_PIXEL_EXPRESSION.equals(propertyName)
                        || RasterDataNode.PROPERTY_NAME_DATA.equals(propertyName);
            }
        };

        // Register the listener for product node changes in all products
        visatApp.getProductManager().addListener(new ProductManager.Listener() {
            public void productAdded(ProductManager.Event event) {
                event.getProduct().addProductNodeListener(productNodeListener);
            }

            public void productRemoved(ProductManager.Event event) {
                event.getProduct().removeProductNodeListener(productNodeListener);
            }
        });
    }

    private void updateNoDataLayer(ProductSceneView psv) {
        final NoDataLayer noDataLayer = getNoDataLayer(psv);
        if (isNoDataOverlaySelected() && isNoDataOverlayApplicable(psv)) {
            Debug.trace("updateNoDataLayer: updating image for raster '" + noDataLayer.getRaster().getName() + "'");
            setNoDataImage(noDataLayer);
        } else {
            Debug.trace("updateNoDataLayer: clearing image for raster '" + noDataLayer.getRaster().getName() + "'");
            noDataLayer.setImage(null);
        }
        noDataLayer.setVisible(isNoDataOverlaySelected());
    }

    private boolean isNoDataOverlaySelected() {
        return isEnabled() && isSelected();
    }

    private static boolean isNoDataOverlayApplicable(ProductSceneView psv) {
        return psv != null && (psv.getRaster().isNoDataValueUsed() || psv.getRaster().getValidPixelExpression() != null);
    }

    private static NoDataLayer getNoDataLayer(ProductSceneView psv) {
        final LayerModel layerModel = psv.getImageDisplay().getLayerModel();
        for (int i = 0; i < layerModel.getLayerCount(); i++) {
            final Layer layer = layerModel.getLayer(i);
            if (layer instanceof NoDataLayer) {
                return (NoDataLayer) layer;
            }
        }
        return null;
    }

    private static void setNoDataImage(final NoDataLayer noDataLayer) {
        final SwingWorker swingWorker = new SwingWorker() {
            final ProgressMonitor pm = new DialogProgressMonitor(VisatApp.getApp().getMainFrame(),
                                                                 "Create No-Data Overlay",
                                                                 Dialog.ModalityType.APPLICATION_MODAL);

            @Override
            protected Object doInBackground() throws Exception {
                return noDataLayer.updateImage(true, pm);
            }

            @Override
            public void done() {
                Object value;
                try {
                    value = get();
                } catch (Exception e) {
                    value = e;
                }
                if (value instanceof RenderedImage) {
                    noDataLayer.setImage((RenderedImage) value);
                } else if (value instanceof Exception) {
                    VisatApp.getApp().showErrorDialog("Unable to create no-data overlay image due to an error:\n" +
                            ((Exception) value).getMessage());
                }
            }
        };
        swingWorker.execute();
    }


}
