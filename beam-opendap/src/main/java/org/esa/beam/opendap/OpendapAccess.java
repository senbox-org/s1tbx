package org.esa.beam.opendap;

import com.jidesoft.utils.Lm;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductManager;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.application.ApplicationPage;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.opendap.ui.OpendapAccessPanel;
import org.esa.beam.util.PropertyMap;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import java.awt.Window;

/**
 * @author Tonio Fincke
 */
public class OpendapAccess {

    public static void main(String[] args) {
        Lm.verifyLicense("Brockmann Consult", "BEAM", "lCzfhklpZ9ryjomwWxfdupxIcuIoCxg2");

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        final OpendapAccessPanel opendapAccessPanel = new OpendapAccessPanel(new DefaultAppContext(""), "");
        final JFrame mainFrame = new JFrame("OPeNDAP Access");
        mainFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        mainFrame.setContentPane(opendapAccessPanel);
        mainFrame.pack();
        mainFrame.setVisible(true);
    }


    /**
     * This trivial implementation of the {@link org.esa.beam.framework.ui.AppContext} class
     * is only for testing.
     */
    private static class DefaultAppContext implements AppContext {

        private Window applicationWindow;
        private String applicationName;
        private ProductManager productManager;
        private Product selectedProduct;
        private PropertyMap preferences;
        private ProductSceneView selectedSceneView;

        public DefaultAppContext(String applicationName) {
            this(applicationName,
                 new JFrame(applicationName),
                 new ProductManager(),
                 new PropertyMap());
        }


        public DefaultAppContext(String applicationName,
                                 Window applicationWindow,
                                 ProductManager productManager,
                                 PropertyMap preferences) {
            this.applicationWindow = applicationWindow;
            this.applicationName = applicationName;
            this.productManager = productManager;
            this.preferences = preferences;
        }

        @Override
        public String getApplicationName() {
            return applicationName;
        }

        public void setApplicationName(String applicationName) {
            this.applicationName = applicationName;
        }

        @Override
        public Window getApplicationWindow() {
            return applicationWindow;
        }

        @Override
        public ApplicationPage getApplicationPage() {
            return null;
        }

        public void setApplicationWindow(Window applicationWindow) {
            this.applicationWindow = applicationWindow;
        }

        @Override
        public PropertyMap getPreferences() {
            return preferences;
        }

        public void setPreferences(PropertyMap preferences) {
            this.preferences = preferences;
        }

        @Override
        public ProductManager getProductManager() {
            return productManager;
        }

        public void setProductManager(ProductManager productManager) {
            this.productManager = productManager;
        }

        @Override
        public Product getSelectedProduct() {
            return selectedProduct;
        }

        public void setSelectedProduct(Product selectedProduct) {
            this.selectedProduct = selectedProduct;
        }

        @Override
        public void handleError(String message, Throwable t) {
            if (t != null) {
                t.printStackTrace();
            }
            JOptionPane.showMessageDialog(getApplicationWindow(), message);
        }

        @Override
        public ProductSceneView getSelectedProductSceneView() {
            return selectedSceneView;
        }

        public void setSelectedSceneView(ProductSceneView selectedView) {
            this.selectedSceneView = selectedView;
        }
    }

}
