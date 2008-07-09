/*
 * $Id: ProcessorAction.java,v 1.6 2007/04/18 13:01:13 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.visat.actions;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductManager;
import org.esa.beam.framework.ui.AppCommand;
import org.esa.beam.framework.ui.AppContext;
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

        public VisatContext(String toolTitle) {
            this.toolTitle = toolTitle;
        }

        public ProductManager getProductManager() {
            return VisatApp.getApp().getProductManager();
        }

        public Product getSelectedProduct() {
            return VisatApp.getApp().getSelectedProduct();
        }

        public Window getApplicationWindow() {
            return VisatApp.getApp().getMainFrame();
        }

        public String getApplicationName() {
            return VisatApp.getApp().getAppName();
        }

        public void handleError(Throwable e) {
            VisatApp.getApp().showErrorDialog(toolTitle, e.getMessage());
        }

        public PropertyMap getPreferences() {
            return VisatApp.getApp().getPreferences();
        }
    }
}