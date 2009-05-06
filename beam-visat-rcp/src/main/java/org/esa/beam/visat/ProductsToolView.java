package org.esa.beam.visat;

import com.jidesoft.swing.JideScrollPane;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.product.ProductTree;
import org.esa.beam.framework.ui.product.ProductTreeListener;
import org.esa.beam.util.Debug;
import org.esa.beam.visat.actions.ShowMetadataViewAction;

import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JScrollPane;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.Container;
import java.awt.Dimension;
import java.beans.PropertyVetoException;

/**
 * The tool window which displays the tree of open products.
 */
public class ProductsToolView extends AbstractToolView {

    public static final String ID = ProductsToolView.class.getName();

    /**
     * Product tree of the application
     */
    private ProductTree productTree;
    private VisatApp visatApp;

    public ProductsToolView() {
        this.visatApp = VisatApp.getApp();
        // We need product tree early, otherwise the application cannot add ProductTreeListeners
        initProductTree();
    }

    public ProductTree getProductTree() {
        return productTree;
    }

    @Override
    public JComponent createControl() {
        final JScrollPane productTreeScrollPane = new JideScrollPane(productTree); // <JIDE>
        productTreeScrollPane.setPreferredSize(new Dimension(320, 480));
        productTreeScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        productTreeScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        productTreeScrollPane.setBorder(null);
        productTreeScrollPane.setViewportBorder(null);

        return productTreeScrollPane;
    }

    private void initProductTree() {
        productTree = new ProductTree();
        productTree.setExceptionHandler(new org.esa.beam.framework.ui.ExceptionHandler() {

            @Override
            public boolean handleException(final Exception e) {
                visatApp.showErrorDialog(e.getMessage());
                return true;
            }
        });
        productTree.addProductTreeListener(new VisatPTL());
        productTree.setCommandManager(visatApp.getCommandManager());
        productTree.setCommandUIFactory(visatApp.getCommandUIFactory());
        visatApp.getProductManager().addListener(new ProductManager.Listener() {
            @Override
            public void productAdded(final ProductManager.Event event) {
                productTree.addProduct(event.getProduct());
                visatApp.getPage().showToolView(ID);
            }

            @Override
            public void productRemoved(final ProductManager.Event event) {
                final Product product = event.getProduct();
                productTree.removeProduct(product);
                if (visatApp.getSelectedProduct() == product) {
                    visatApp.setSelectedProductNode((ProductNode) null);
                }
            }
        });

        visatApp.addInternalFrameListener(new InternalFrameAdapter() {

            @Override
            public void internalFrameOpened(InternalFrameEvent e) {
                final Container contentPane = e.getInternalFrame().getContentPane();
                if (contentPane instanceof ProductSceneView) {
                    productTree.sceneViewOpened((ProductSceneView) contentPane);
                }
            }

            @Override
            public void internalFrameClosed(InternalFrameEvent e) {
                final Container contentPane = e.getInternalFrame().getContentPane();
                if (contentPane instanceof ProductSceneView) {
                    productTree.sceneViewClosed((ProductSceneView) contentPane);
                }
            }
        });

    }


    /**
     * This listener listens to product tree events in VISAT's product browser.
     */
    private class VisatPTL implements ProductTreeListener {

        public VisatPTL() {
        }

        @Override
        public void productAdded(final Product product) {
            Debug.trace("VisatApp: product added: " + product.getDisplayName());
            setSelectedProductNode(product);
        }

        @Override
        public void productRemoved(final Product product) {
            Debug.trace("VisatApp: product removed: " + product.getDisplayName());
            if (visatApp.getSelectedProduct() != null && visatApp.getSelectedProduct() == product) {
                visatApp.setSelectedProductNode((ProductNode) null);
            } else {
                visatApp.updateState();
            }
        }

        @Override
        public void productSelected(final Product product, final int clickCount) {
            setSelectedProductNode(product);
        }

        @Override
        public void tiePointGridSelected(final TiePointGrid tiePointGrid, final int clickCount) {
            rasterDataNodeSelected(tiePointGrid, clickCount);
        }

        @Override
        public void bandSelected(final Band band, final int clickCount) {
            rasterDataNodeSelected(band, clickCount);
        }

        @Override
        public void metadataElementSelected(final MetadataElement group, final int clickCount) {
            setSelectedProductNode(group);
            final JInternalFrame frame = visatApp.findInternalFrame(group);
            if (frame != null) {
                try {
                    frame.setSelected(true);
                } catch (PropertyVetoException e) {
                    // ok
                }
                return;
            }
            if (clickCount == 2) {
                final ExecCommand command = visatApp.getCommandManager().getExecCommand(ShowMetadataViewAction.ID);
                command.execute(group);
            }
        }

        private void rasterDataNodeSelected(final RasterDataNode raster, final int clickCount) {
            setSelectedProductNode(raster);
            final JInternalFrame[] internalFrames = visatApp.findInternalFrames(raster);
            JInternalFrame frame = null;
            for (final JInternalFrame internalFrame : internalFrames) {
                final int numRasters = ((ProductSceneView) internalFrame.getContentPane()).getNumRasters();
                if (numRasters == 1) {
                    frame = internalFrame;
                    break;
                }
            }
            if (frame != null) {
                try {
                    frame.setSelected(true);
                } catch (PropertyVetoException e) {
                    // ok
                }
            } else if (clickCount == 2) {
                final ExecCommand command = visatApp.getCommandManager().getExecCommand("showImageView");
                command.execute(clickCount);
            }
        }

        private void setSelectedProductNode(ProductNode product) {
            deselectInternalFrame();
            visatApp.setSelectedProductNode(product);
        }

        private void deselectInternalFrame() {
            try {
                final JInternalFrame frame = visatApp.getSelectedInternalFrame();
                if (frame != null) {
                    frame.setSelected(false);
                }
            } catch (PropertyVetoException ignore) {
            }
        }
    }
}
