package org.esa.beam.framework.gpf.ui;

import com.bc.ceres.core.ProgressMonitor;
import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.util.PropertyMap;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import java.awt.Window;
import java.util.ArrayList;


public class DefaultSingleTargetProductDialogTest extends TestCase {
    private static final TestOp.Spi SPI = new TestOp.Spi();

    @Override
    protected void setUp() throws Exception {
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(SPI);
    }

    @Override
    protected void tearDown() throws Exception {
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(SPI);
    }

    public void testNothing() {
    }

    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(SPI);
        TestApp app = new TestApp();
        final SingleTargetProductDialog dialog = DefaultSingleTargetProductDialog.createDefaultDialog(TestOp.Spi.class.getName(), app);
        dialog.getJDialog().setTitle("TestOp GUI");
        dialog.show();
    }

    public static class TestOp extends Operator {
        @SourceProduct
        Product masterProduct;
        @SourceProduct
        Product slaveProduct;
        @TargetProduct
        Product target;
        @Parameter(defaultValue = "true")
        boolean copyTiePointGrids;
        @Parameter(defaultValue = "false")
        Boolean copyMetadata;
        @Parameter(interval = "[-1,+1]", defaultValue = "-0.1")
        double threshold;
        @Parameter(valueSet = {"ME-203", "ME-208", "ME-002"}, defaultValue = "ME-208")
        String method;

        @Override
        public void initialize() throws OperatorException {
            Product product = new Product("N", "T", 16, 16);
            product.addBand("B1", ProductData.TYPE_FLOAT32);
            product.addBand("B2", ProductData.TYPE_FLOAT32);
            product.setPreferredTileSize(4, 4);
            System.out.println("product = " + product);
            target = product;
        }

        @Override
        public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        }

        public static class Spi extends OperatorSpi {

            public Spi() {
                super(TestOp.class);
            }
        }
    }

    private static class TestApp implements AppContext {

        private static final String GRUNTZ = "TestApp";
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














































