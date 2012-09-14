package org.esa.beam.framework.gpf.ui;

import junit.framework.TestCase;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.application.ApplicationPage;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductManager;
import org.esa.beam.util.PropertyMap;
import org.esa.beam.GlobalTestConfig;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.HashMap;
import java.io.File;

/**
 * Tests the SourceUI
 * User: lveci
 * Date: Feb 15, 2008
 */
public class TargetUITest extends TestCase {

    TargetUI targetUI;
    private Product[] defaultProducts;
    private AppContext appContext;
    private final Map<String, Object> parameterMap = new HashMap<String, Object>();
    private final String FILE_PARAMETER = "file";

    @Override
    protected void setUp() throws Exception {
        targetUI = new TargetUI();
        appContext = new MockAppContext();

        final File path = GlobalTestConfig.getBeamTestDataOutputDirectory();
        defaultProducts = new Product[2];
        for (int i = 0; i < defaultProducts.length; i++) {

            Product prod = new Product("P" + i, "T" + i, 10, 10);
            prod.setFileLocation(path);
            appContext.getProductManager().addProduct(prod);
            defaultProducts[i] = prod;
        }
    }

    @Override
    protected void tearDown() throws Exception {
        targetUI = null;
        appContext = null;
    }

    public void testCreateOpTab() {

        JComponent component = targetUI.CreateOpTab("testOp", parameterMap, appContext);
        assertNotNull(component);
    }

    public void testValidateParameters() {

        targetUI.CreateOpTab("testOp", parameterMap, appContext);
        UIValidation valid = targetUI.validateParameters();
        assertFalse(valid.getState() == UIValidation.State.OK);

        targetUI.targetProductSelector.getModel().setProductName("abc");
        //todo need an existing file?
    }

    public void testUpdateParameters() {

        targetUI.CreateOpTab("testOp", parameterMap, appContext);
        parameterMap.put(FILE_PARAMETER, defaultProducts[0]);

        //todo need an existing file?
    }

    private class MockAppContext implements AppContext {
        private PropertyMap preferences = new PropertyMap();
        private ProductManager prodMan = new ProductManager();

        public Product getSelectedProduct() {
            return defaultProducts[0];
        }

        public Window getApplicationWindow() {
            return null;
        }

        public String getApplicationName() {
            return "Killer App";
        }

        public ApplicationPage getApplicationPage() {
            return null;
        }

        public void handleError(Throwable e) {
            JOptionPane.showMessageDialog(getApplicationWindow(), e.getMessage());
        }

        public void handleError(String message, Throwable e) {
            JOptionPane.showMessageDialog(getApplicationWindow(), message);
        }

        public PropertyMap getPreferences() {
            return preferences;
        }

        public ProductManager getProductManager() {
            return prodMan;
        }

        public ProductSceneView getSelectedProductSceneView() {
            return null;
        }
    }
}