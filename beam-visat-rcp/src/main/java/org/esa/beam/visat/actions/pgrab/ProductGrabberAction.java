package org.esa.beam.visat.actions.pgrab;

import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductManager;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.actions.pgrab.model.RepositoryManager;
import org.esa.beam.visat.actions.pgrab.ui.ProductGrabber;

import javax.swing.SwingUtilities;
import java.io.File;
import java.io.IOException;


/**
 * The ProductGrabberVPI opens a dialog to preview and open products.
 *
 * @author Marco Peters
 */
public class ProductGrabberAction extends ExecCommand {

    private static final String ID = "productGrabber";
    private static final String HELP_ID = ID;

    private static ProductGrabberAction instance;
    private RepositoryManager repositoryManager;
    private ProductGrabber productGrabber;

    public ProductGrabberAction() {
        super(ID);
        instance = this;
    }

    /**
     * Retrieves the instance of this class.
     *
     * @return the single instance.
     */
    public static ProductGrabberAction getInstance() {
        return instance;
    }

    /**
     * Gets the repository manager.
     *
     * @return the repository manager
     */
    public RepositoryManager getRepositoryManager() {
        return repositoryManager;
    }

    /**
     * Gets the product grabber.
     *
     * @return the product grabber
     */
    public ProductGrabber getProductGrabber() {
        return productGrabber;
    }

    @Override
    public void actionPerformed(final CommandEvent event) {
        if (productGrabber == null) {
            VisatApp visatApp = VisatApp.getApp();
            repositoryManager = new RepositoryManager();
            productGrabber = new ProductGrabber(visatApp, repositoryManager, HELP_ID);
            productGrabber.setProductOpenHandler(new MyProductOpenHandler(visatApp));
            productGrabber.getFrame().setIconImage(visatApp.getMainFrame().getIconImage());
        }
        productGrabber.getFrame().setVisible(true);
    }

    @Override
    public void updateState(final CommandEvent event) {
    }

    /**
     * Tells a plug-in to update its component tree (if any) since the Java look-and-feel has changed.
     * <p/>
     * <p>If a plug-in uses top-level containers such as dialogs or frames, implementors of this method should invoke
     * <code>SwingUtilities.updateComponentTreeUI()</code> on such containers.
     */
    @Override
    public void updateComponentTreeUI() {
        if (productGrabber != null) {
            SwingUtilities.updateComponentTreeUI(productGrabber.getFrame());
        }
    }

    private static class MyProductOpenHandler implements ProductGrabber.ProductOpenHandler {

        private final VisatApp visatApp;

        public MyProductOpenHandler(final VisatApp visatApp) {
            this.visatApp = visatApp;
        }

        public void openProducts(final File[] productFiles) {
            for (File productFile : productFiles) {
                if (isProductOpen(productFile)) {
                    continue;
                }
                try {
                    final Product product = ProductIO.readProduct(productFile);

                    final ProductManager productManager = visatApp.getProductManager();
                    productManager.addProduct(product);
                } catch (IOException e) {
                    visatApp.showErrorDialog("Not able to open product:\n" +
                            productFile.getPath());
                }
            }
        }

        private boolean isProductOpen(final File productFile) {
            final Product openedProduct = visatApp.getOpenProduct(productFile);
            if (openedProduct != null) {
                visatApp.showInfoDialog("Product '" + openedProduct.getName() +
                        "' is already opened.", null);
                return true;
            }
            return false;
        }
    }
}