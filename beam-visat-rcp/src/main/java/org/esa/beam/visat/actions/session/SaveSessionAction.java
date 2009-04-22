/*
 * $Id: SaveAction.java,v 1.2 2006/11/21 09:05:56 olga Exp $
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
package org.esa.beam.visat.actions.session;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.ui.product.ProductNodeView;
import org.esa.beam.visat.VisatApp;

import javax.swing.JInternalFrame;
import java.awt.Container;
import java.io.File;
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
        final Product[] products = app.getProductManager().getProducts();
        final ArrayList<Product> noFileProducts = new ArrayList<Product>();
        for (Product product : products) {
            if (product.getFileLocation() == null) {
                noFileProducts.add(product);
            }
        }
        if (!noFileProducts.isEmpty()) {
            StringBuilder sb = new StringBuilder(
                    "The session connot be saved because the following" +
                            "products have not been saved yet:\n");
            for (Product product : noFileProducts) {
                sb.append("  ");
                sb.append(product.getDisplayName());
                sb.append("\n");
            }
            app.showErrorDialog(TITLE, sb.toString());
            return;
        }
        File sessionFile = app.getSessionFile();
        if (sessionFile == null || saveAs) {
             sessionFile = app.showFileSaveDialog(TITLE, false,
                                                        OpenSessionAction.SESSION_FILE_FILTER,
                                                        OpenSessionAction.SESSION_FILE_FILTER.getDefaultExtension(),
                                                        sessionFile != null ? sessionFile.getName() : System.getProperty("user.name", "noname"),
                                                        OpenSessionAction.LAST_SESSION_DIR_KEY);
            if (sessionFile == null) {
                return;
            }
        }
        app.setSessionFile(sessionFile);

        try {
            final Session session = createSession(app);
            SessionIO.getInstance().writeSession(session, sessionFile);
        } catch (Exception e) {
            e.printStackTrace();
            app.showErrorDialog(e.getMessage());
        }
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