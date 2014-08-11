/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.gpf.ui;

import junit.framework.TestCase;
import org.esa.beam.GlobalTestConfig;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductManager;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.application.ApplicationPage;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.util.PropertyMap;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Tests the SourceUI
 * User: lveci
 * Date: Feb 15, 2008
 */
public class SourceUITest extends TestCase {

    SourceUI sourceUI;
    private Product[] defaultProducts;
    private AppContext appContext;
    private final Map<String, Object> parameterMap = new HashMap<String, Object>(5);
    private static final String FILE_PARAMETER = "file";

    @Override
    protected void setUp() throws Exception {
        sourceUI = new SourceUI();
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
        sourceUI = null;
        appContext = null;
    }

    public void testCreateOpTab() {

        JComponent component = sourceUI.CreateOpTab("testOp", parameterMap, appContext);
        assertNotNull(component);

        assertEquals(sourceUI.sourceProductSelector.getProductNameComboBox().getModel().getSize(), 2);
    }

    public void testValidateParameters() {

        sourceUI.CreateOpTab("testOp", parameterMap, appContext);
        UIValidation valid = sourceUI.validateParameters();
        assertTrue(valid.getState() == UIValidation.State.OK);
    }

    public void testUpdateParameters() {

        sourceUI.CreateOpTab("testOp", parameterMap, appContext);
        parameterMap.put(FILE_PARAMETER, defaultProducts[0]);

        sourceUI.updateParameters();

        File path = (File) parameterMap.get(FILE_PARAMETER);
        assertTrue(path.getAbsolutePath().equals(defaultProducts[0].getFileLocation().getAbsolutePath()));
    }

    private class MockAppContext implements AppContext {
        private PropertyMap preferences = new PropertyMap();
        private ProductManager prodMan = new ProductManager();

        public Window getApplicationWindow() {
            return null;
        }

        public String getApplicationName() {
            return "Killer App";
        }

        public ApplicationPage getApplicationPage() {
            return null;
        }

        public Product getSelectedProduct() {
            return defaultProducts[0];
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
