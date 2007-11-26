package org.esa.beam.framework.gpf.ui;

import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValueContainerFactory;
import com.bc.ceres.binding.swing.PropertyPane;
import com.bc.ceres.binding.swing.SwingBindingContext;
import com.bc.ceres.core.ProgressMonitor;
import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.ParameterDescriptorFactory;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.TableLayout;
import org.esa.beam.util.PropertyMap;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: Norman
 * Date: 17.11.2007
 * Time: 16:44:48
 * To change this template use File | Settings | File Templates.
 */
public class UiFactoryTest extends TestCase {
    public void testNothing() {

    }

    public static void main(String[] args) {
        TestApp app = new TestApp();
        ValueContainerFactory factory = new ValueContainerFactory(new ParameterDescriptorFactory());
        HashMap<String, Object> map = new HashMap<String, Object>();
        ValueContainer valueContainer = factory.createMapBackedValueContainer(TestOp.class, map);
        SwingBindingContext context = new SwingBindingContext(valueContainer);
        SourceProductSelector sselector = new SourceProductSelector(app);
        TargetProductSelector tselector = new TargetProductSelector();
        JPanel sourcePanel = sselector.createDefaultPanel();
        JPanel targetPanel = tselector.createDefaultPanel();
        PropertyPane propertyPane = new PropertyPane(context);
        JPanel parametersPanel = propertyPane.createPanel();
        parametersPanel.setBorder(BorderFactory.createTitledBorder("Parameters"));

        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTableAnchor(TableLayout.Anchor.WEST);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setTablePadding(3, 3);
        JPanel panel = new JPanel(tableLayout);
        panel.add(sourcePanel);
        panel.add(targetPanel);
        panel.add(parametersPanel);
        panel.add(tableLayout.createVerticalSpacer());

        app.frame.add(panel);
        app.frame.pack();
        app.frame.setVisible(true);

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
