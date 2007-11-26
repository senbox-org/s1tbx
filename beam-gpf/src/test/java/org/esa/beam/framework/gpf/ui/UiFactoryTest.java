package org.esa.beam.framework.gpf.ui;

import com.bc.ceres.core.ProgressMonitor;
import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.*;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.util.PropertyMap;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import java.awt.Window;
import java.util.ArrayList;


public class UiFactoryTest extends TestCase {
    private TestOp.Spi operatorSpi;

    @Override
    protected void setUp() throws Exception {
        operatorSpi = new TestOp.Spi();
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(operatorSpi);
    }

    @Override
    protected void tearDown() throws Exception {
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(operatorSpi);
    }

    public void testNothing() {
    }

    public static void main(String[] args) {
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(new TestOp.Spi());
        TestApp app = new TestApp();
        final SingleTargetProductDialog dialog = DefaultSingleTargetProductDialog.createDefaultDialog(TestOp.Spi.class.getName(), app);
        dialog.show();
    }

    public static class TestOp extends Operator {
        @SourceProduct
        Product source;
        @TargetProduct
        Product target;
        @Parameter(interval = "[-1,+1]", defaultValue = "-0.1")
        double threshold;
        @Parameter(valueSet = {"MA", "MB", "MC"}, defaultValue = "MB")
        String method;

        public void initialize() throws OperatorException {
            Product product = new Product("N", "T", 16, 16);
            product.addBand("B1", ProductData.TYPE_FLOAT32);
            product.addBand("B2", ProductData.TYPE_FLOAT32);
            product.setPreferredTileSize(4, 4);
            target = product;
        }

        public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        }

        public static class Spi extends OperatorSpi {

            public Spi() {
                super(TestOp.class);
            }
        }
    }

    private static class TestApp implements AppContext {

        private static final String GRUNTZ = "Gruntz";
        private JFrame frame;
        private ArrayList<Product> products = new ArrayList<Product>(10);
        private Product selectedProduct;
        private PropertyMap preferences;

        private TestApp() {
            frame = new JFrame(GRUNTZ);
            frame.setSize(200, 200);
        }

        public String getApplicationName() {
            return GRUNTZ;
        }

        public Window getApplicationWindow() {
            return frame;
        }

        public Product[] getProducts() {
            return products.toArray(new Product[0]);
        }

        public Product getSelectedProduct() {
            return null;
        }

        public void addProduct(Product product) {
            products.add(product);
            selectedProduct = product;
        }

        public void handleError(Throwable e) {
            JOptionPane.showMessageDialog(frame, e.getMessage());
            e.printStackTrace();
        }

        public PropertyMap getPreferences() {
            preferences = new PropertyMap();
            return preferences;
        }

    }
}
