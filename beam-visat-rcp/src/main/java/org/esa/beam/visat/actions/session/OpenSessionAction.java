/*
 * $Id: OpenAction.java,v 1.1 2006/11/15 16:21:48 marcop Exp $
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

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.grender.Viewport;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.ui.product.ProductNodeView;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.actions.ShowImageViewAction;

import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.JInternalFrame;
import java.awt.Rectangle;
import java.io.File;
import java.util.concurrent.ExecutionException;


public class OpenSessionAction extends ExecCommand {
    public static final BeamFileFilter SESSION_FILE_FILTER = new BeamFileFilter("BEAM-SESSION", ".beam", "BEAM session");
    public static final String LAST_SESSION_DIR_KEY = "beam.lastSessionDir";
    private static final String TITLE = "Open Session";

    // todo - move this file into the application instance
    static File sessionFile;

    @Override
    public void actionPerformed(final CommandEvent event) {

        final VisatApp app = VisatApp.getApp();

        if (app.getProductManager().getProductCount() > 0) {
            final int i = app.showQuestionDialog(TITLE,
                                                 "This will close the current session.\n" +
                                                         "Do you want to continue?", null);
            if (i != JOptionPane.YES_OPTION) {
                return;
            }
        }

        final File sessionFile = app.showFileOpenDialog(TITLE, false,
                                                        SESSION_FILE_FILTER,
                                                        LAST_SESSION_DIR_KEY);
        if (sessionFile == null) {
            return;
        }
        if (sessionFile.equals(app.getSessionFile())) {
            app.showErrorDialog(TITLE, "Session has already been opened.");
            return;
        }

        app.setSessionFile(sessionFile);

        VisatApp.getApp().closeAllProducts();
        SwingWorker<RestoredSession, Object> worker = new OpenSessionWorker(app, sessionFile);
        worker.execute();
    }

    private static class OpenSessionWorker extends ProgressMonitorSwingWorker<RestoredSession, Object> {
        private final VisatApp app;
        private final File sessionFile;

        public OpenSessionWorker(VisatApp app, File sessionFile) {
            super(app.getMainFrame(), TITLE);
            this.app = app;
            this.sessionFile = sessionFile;
        }

        @Override
        protected RestoredSession doInBackground(ProgressMonitor pm) throws Exception {
            final Session session = SessionIO.getInstance().readSession(sessionFile);
            return session.restore(pm);
        }

        @Override
        protected void done() {
            final RestoredSession restoredSession;
            try {
                restoredSession = get();
            } catch (InterruptedException e) {
                return;
            } catch (ExecutionException e) {
                app.showErrorDialog("An unexpected exception occured!\n" +
                        "Message: " + e.getCause().getMessage());
                return;
            }
            final Exception[] problems = restoredSession.getProblems();
            if (problems.length > 0) {
                StringBuilder sb = new StringBuilder();
                sb.append("The following problem(s) occured:\n");
                for (Exception problem : problems) {
                    problem.printStackTrace();
                    sb.append("  ");
                    sb.append(problem.getMessage());
                    sb.append("\n");
                }
                app.showWarningDialog(sb.toString());
            }

            final Product[] products = restoredSession.getProducts();
            for (Product product : products) {
                app.getProductManager().addProduct(product);
            }

            ShowImageViewAction showImageViewAction = getShowImageViewAction();

            final ProductNodeView[] nodeViews = restoredSession.getViews();
            for (ProductNodeView nodeView : nodeViews) {
                if (nodeView instanceof ProductSceneView) {
                    ProductSceneView sceneView = (ProductSceneView) nodeView;

                    Rectangle bounds = sceneView.getBounds();
                    Viewport viewport = sceneView.getLayerCanvas().getViewport().clone();

                    JInternalFrame internalFrame = showImageViewAction.openInternalFrame(sceneView);

                    internalFrame.setBounds(bounds);
                    sceneView.getLayerCanvas().getViewport().setTransform(viewport);
                }
            }
        }

        private ShowImageViewAction getShowImageViewAction() {
            String actionId = ShowImageViewAction.ID;
            ShowImageViewAction action = (ShowImageViewAction) app.getCommandManager().getCommand(actionId);
            if (action == null) {
                throw new IllegalStateException("ShowImageViewAction not found: actionId=" + actionId);
            }
            return action;
        }
    }
}