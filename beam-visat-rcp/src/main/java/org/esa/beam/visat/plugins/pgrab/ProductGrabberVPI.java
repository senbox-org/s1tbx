package org.esa.beam.visat.plugins.pgrab;

import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductManager;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.command.CommandAdapter;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.CommandManager;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.visat.AbstractVisatPlugIn;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.plugins.pgrab.model.RepositoryManager;
import org.esa.beam.visat.plugins.pgrab.ui.ProductGrabber;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.io.File;
import java.io.IOException;


/**
 * The ProductGrabberVPI opens a dialog to preview and open products.
 *
 * @author Marco Peters
 */
public class ProductGrabberVPI extends AbstractVisatPlugIn {

    private static final String HELP_ID = "productGrabber";
    private static ProductGrabberVPI _instance;
    private RepositoryManager _repositoryManager;
    private ProductGrabber _productGrabber;
    private JFrame _pgFrame;


    /**
     * Retrieves the instance of this class.
     *
     * @return the single instance.
     */
    public static ProductGrabberVPI getInstance() {
        return _instance;
    }

    /**
     * Gets the repository manager.
     *
     * @return the repository manager
     */
    public RepositoryManager getRepositoryManager() {
        return _repositoryManager;
    }

    /**
     * Gets the product grabber.
     *
     * @return the product grabber
     */
    public ProductGrabber getProductGrabber() {
        return _productGrabber;
    }


    /**
     * Called by VISAT after the plug-in _instance has been registered in VISAT's plug-in manager.
     *
     * @param visatApp a reference to the VISAT application _instance.
     */
    public void start(final VisatApp visatApp) {
        _instance = this;
        _repositoryManager = new RepositoryManager();
        _productGrabber = new ProductGrabber(visatApp, _repositoryManager, HELP_ID);
        _productGrabber.setProductOpenHandler(new MyProductOpenHandler(visatApp));

        final CommandManager commandManager = visatApp.getCommandManager();
        final ExecCommand command = commandManager.createExecCommand("openProductGrabber", new CommandAdapter() {
            @Override
            public void actionPerformed(final CommandEvent event) {
                if (_pgFrame == null) {
                    _pgFrame = _productGrabber.getFrame();
                    _pgFrame.setIconImage(visatApp.getMainFrame().getIconImage());
                }
                _pgFrame.setVisible(true);
            }
        });
        command.setParent("file");
        command.setPlaceAfter("reopen");
        command.setPlaceBefore("close");
        command.setText("Product Grabber");
        command.setShortDescription("Opens the product grabber dialog to preview and open products.");
        command.setSmallIcon(UIUtils.loadImageIcon("icons/RsProduct16.gif"));

    }

    /**
     * Tells a plug-in to update its component tree (if any) since the Java look-and-feel has changed.
     * <p/>
     * <p>If a plug-in uses top-level containers such as dialogs or frames, implementors of this method should invoke
     * <code>SwingUtilities.updateComponentTreeUI()</code> on such containers.
     */
    @Override
    public void updateComponentTreeUI() {
        if (_pgFrame != null) {
            SwingUtilities.updateComponentTreeUI(_pgFrame);
        }
    }

    private static class MyProductOpenHandler implements ProductGrabber.ProductOpenHandler {

        private final VisatApp _visatApp;

        public MyProductOpenHandler(final VisatApp visatApp) {
            _visatApp = visatApp;
        }

        public void openProducts(final File[] productFiles) {
            for (File productFile : productFiles) {
                if (isProductOpen(productFile)) {
                    continue;
                }
                try {
                    final Product product = ProductIO.readProduct(productFile, null);

                    final ProductManager productManager = _visatApp.getProductManager();
                    productManager.addProduct(product);
                } catch (IOException e) {
                    _visatApp.showErrorDialog("Not able to open product:\n" +
                                              productFile.getPath());
                }
            }
        }

        private boolean isProductOpen(final File productFile) {
            final Product openedProduct = _visatApp.getOpenProduct(productFile);
            if (openedProduct != null) {
                _visatApp.showInfoDialog("Product '" + openedProduct.getName() +
                                         "' is already opened.", null);
                return true;
            }
            return false;
        }
    }
}
