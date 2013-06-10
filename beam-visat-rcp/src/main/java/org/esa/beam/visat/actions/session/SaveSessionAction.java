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
package org.esa.beam.visat.actions.session;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.ui.product.ProductNodeView;
import org.esa.beam.visat.VisatApp;

import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import java.awt.Container;
import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;


/**
 * Saves a VISAT session.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
public class SaveSessionAction extends ExecCommand {

    public static final String ID = "saveSession";
    private static final String TITLE = "Save Session As";

    @Override
    public final void actionPerformed(final CommandEvent event) {
        saveSession(false);
    }

    @Override
    public final void updateState(final CommandEvent event) {
        setEnabled(VisatApp.getApp().getProductManager().getProductCount() > 0);
    }

    public void saveSession(boolean saveAs) {
        final VisatApp app = VisatApp.getApp();

        File sessionFile = app.getSessionFile();
        if (sessionFile == null || saveAs) {
            sessionFile = app.showFileSaveDialog(TITLE, false,
                                                 OpenSessionAction.getSessionFileFilter(),
                                                 OpenSessionAction.getSessionFileFilter().getDefaultExtension(),
                                                 sessionFile != null ? sessionFile.getName() : System.getProperty(
                                                         "user.name", "noname"),
                                                 OpenSessionAction.LAST_SESSION_DIR_KEY);
            if (sessionFile == null) {
                return;
            }
        }

        if (!saveProductsOrLetItBe(app, sessionFile)) {
            return;
        }

        app.setSessionFile(sessionFile);
        try {
            final Session session = createSession(app);
            SessionIO.getInstance().writeSession(session, sessionFile);
            app.showInfoDialog(TITLE, "Session saved.", null);
        } catch (Exception e) {
            e.printStackTrace();
            app.showErrorDialog(TITLE, e.getMessage());
        } finally {
            app.updateState(); // to update menu entries e.g. 'Close Session'
        }
    }

    private boolean saveProductsOrLetItBe(VisatApp app, File sessionFile) {
        final Product[] products = app.getProductManager().getProducts();

        for (Product product : products) {
            if (product.getFileLocation() == null) {
                String message = MessageFormat.format(
                        "The following product has not been saved yet:\n" +
                        "{0}.\n" +
                        "Do you want to save it now?\n\n" +
                        "Note: If you select 'No', the session cannot be saved.",
                        product.getDisplayName());
                // Here: No == Cancel, its because we need a file location in the session XML
                int i = app.showQuestionDialog(TITLE, message, false, null);
                if (i == JOptionPane.YES_OPTION) {
                    File sessionDir = sessionFile.getAbsoluteFile().getParentFile();
                    product.setFileLocation(new File(sessionDir, product.getName() + ".dim"));
                    VisatApp.getApp().saveProduct(product);
                } else {
                    return false;
                }
            }
        }

        for (Product product : products) {
            if (product.isModified()) {
                String message = MessageFormat.format(
                        "The following product has been modified:\n" +
                        "{0}.\n" +
                        "Do you want to save it now?\n\n" +
                        "Note: It is recommended to save the product in order to \n" +
                        "fully restore the session later.",
                        product.getDisplayName());
                // Here: Yes, No + Cancel, its because we have file location for the session XML
                int i = app.showQuestionDialog(TITLE, message, true, null);
                if (i == JOptionPane.YES_OPTION) {
                    VisatApp.getApp().saveProduct(product);
                } else if (i == JOptionPane.CANCEL_OPTION) {
                    return false;
                }
            }
        }

        return true;
    }

    private Session createSession(VisatApp app) {
        ArrayList<ProductNodeView> nodeViews = new ArrayList<ProductNodeView>();
        final JInternalFrame[] internalFrames = app.getAllInternalFrames();
        for (JInternalFrame internalFrame : internalFrames) {
            final Container contentPane = internalFrame.getContentPane();
            if (contentPane instanceof ProductNodeView) {
                nodeViews.add((ProductNodeView) contentPane);
            }
        }
        return new Session(app.getSessionFile().getParentFile().toURI(),
                           app.getProductManager().getProducts(),
                           nodeViews.toArray(new ProductNodeView[nodeViews.size()]));
    }
}