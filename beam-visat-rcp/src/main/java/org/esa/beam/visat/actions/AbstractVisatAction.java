/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.beam.visat.actions;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductManager;
import org.esa.beam.framework.ui.AppCommand;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.application.ApplicationPage;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.util.PropertyMap;
import org.esa.beam.visat.VisatApp;

import java.awt.Window;


public abstract class AbstractVisatAction extends AppCommand {

    private VisatContext context;

    public AbstractVisatAction() {
    }

    @Override
    public AppContext getAppContext() {
        if (context == null) {
            context = new VisatContext(getText());
            setAppContext(context);
        }
        return context;
    }

    private static class VisatContext implements AppContext {

        private String toolTitle;
        private final VisatApp app = VisatApp.getApp();

        private VisatContext(String toolTitle) {
            this.toolTitle = toolTitle;
        }

        @Override
        public ApplicationPage getApplicationPage() {
            return app.getApplicationPage();
        }

        @Override
        public ProductManager getProductManager() {
            return app.getProductManager();
        }

        @Override
        public Product getSelectedProduct() {
            return app.getSelectedProduct();
        }

        @Override
        public Window getApplicationWindow() {
            return app.getMainFrame();
        }

        @Override
        public String getApplicationName() {
            return app.getAppName();
        }

        @Override
        public void handleError(String message, Throwable t) {
            if (t != null) {
                t.printStackTrace();
            }
            app.showErrorDialog(toolTitle, message);
        }

        @Override
        public PropertyMap getPreferences() {
            return app.getPreferences();
        }

        @Override
        public ProductSceneView getSelectedProductSceneView() {
            return app.getSelectedProductSceneView();
        }
    }
}