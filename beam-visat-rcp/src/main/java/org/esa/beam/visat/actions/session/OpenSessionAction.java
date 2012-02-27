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

import com.bc.ceres.core.CanceledException;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.grender.Viewport;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.ui.product.ProductMetadataView;
import org.esa.beam.framework.ui.product.ProductNodeView;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.actions.ShowImageViewAction;
import org.esa.beam.visat.actions.ShowImageViewRGBAction;
import org.esa.beam.visat.actions.ShowMetadataViewAction;

import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.Rectangle;
import java.beans.PropertyVetoException;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.text.MessageFormat;
import java.util.concurrent.ExecutionException;


/**
 * Opens a VISAT session.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
public class OpenSessionAction extends ExecCommand {

    public static final String ID = "openSession";
    public static final BeamFileFilter SESSION_FILE_FILTER = new BeamFileFilter("BEAM-SESSION", ".beam",
                                                                                "BEAM session");
    public static final String LAST_SESSION_DIR_KEY = "beam.lastSessionDir";
    private static final String TITLE = "Open Session";

    @Override
    public void actionPerformed(final CommandEvent event) {

        final VisatApp app = VisatApp.getApp();

        if (app.getSessionFile() != null) {
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

        openSession(app, sessionFile);
    }

    public void openSession(VisatApp app, File sessionFile) {
        app.setSessionFile(sessionFile);
        app.closeAllProducts();
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
            final File parentFile = sessionFile.getParentFile();
            final URI rootURI;
            if (parentFile != null) {
                rootURI = parentFile.toURI();
            } else {
                rootURI = new File(".").toURI();
            }
            return session.restore(app, rootURI, pm, new SessionProblemSolver());
        }

        @Override
        protected void done() {
            final RestoredSession restoredSession;
            try {
                restoredSession = get();
            } catch (InterruptedException e) {
                return;
            } catch (ExecutionException e) {
                if (e.getCause() instanceof CanceledException) {
                    return;
                }
                app.showErrorDialog(MessageFormat.format("An unexpected exception occured!\nMessage: {0}",
                                                         e.getCause().getMessage()));
                e.printStackTrace();
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


            // todo - Handle view persistence in a generic way. (nf - 08.05.2009)
            //        These are the only 3 views currently known in BEAM.
            //        NEST already uses another view type which cannot be stored/restored.
            //
            ShowImageViewAction showImageViewAction = getAction(ShowImageViewAction.ID);
            ShowImageViewRGBAction showImageViewRGBAction = getAction(ShowImageViewRGBAction.ID);
            ShowMetadataViewAction showMetadataViewAction = getAction(ShowMetadataViewAction.ID);

            final ProductNodeView[] nodeViews = restoredSession.getViews();
            for (ProductNodeView nodeView : nodeViews) {
                Rectangle bounds = nodeView.getBounds();
                JInternalFrame internalFrame = null;
                if (nodeView instanceof ProductSceneView) {
                    ProductSceneView sceneView = (ProductSceneView) nodeView;

                    sceneView.getLayerCanvas().setInitiallyZoomingAll(false);
                    Viewport viewport = sceneView.getLayerCanvas().getViewport().clone();
                    if (sceneView.isRGB()) {
                        internalFrame = showImageViewRGBAction.openInternalFrame(sceneView, false);
                    } else {
                        internalFrame = showImageViewAction.openInternalFrame(sceneView, false);
                    }
                    sceneView.getLayerCanvas().getViewport().setTransform(viewport);
                } else if (nodeView instanceof ProductMetadataView) {
                    ProductMetadataView metadataView = (ProductMetadataView) nodeView;

                    internalFrame = showMetadataViewAction.openInternalFrame(metadataView);
                }
                if (internalFrame != null) {
                    try {
                        internalFrame.setMaximum(false);
                    } catch (PropertyVetoException e) {
                        // ok to ignore
                    }
                    internalFrame.setBounds(bounds);
                }
            }
        }

        private <T> T getAction(String actionId) {
            T action = (T) app.getCommandManager().getCommand(actionId);
            if (action == null) {
                throw new IllegalStateException("Action not found: actionId=" + actionId);
            }
            return action;
        }

        private class SessionProblemSolver implements Session.ProblemSolver {
            @Override
            public Product solveProductNotFound(int id, File file) throws CanceledException {
                final File[] newFile = new File[1];
                final int[] answer = new int[1];
                final String title = MessageFormat.format(TITLE + " - Resolving [{0}]", file);
                final String msg = MessageFormat.format("Product [{0}] has been renamed or (re-)moved.\n" +
                                                                "Its location was [{1}].\n" +
                                                                "Do you wish to provide its new location?\n" +
                                                                "(Select ''No'' if the product shall no longer be part of the session.)",
                                                        id, file);
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {

                        @Override
                        public void run() {
                            answer[0] = app.showQuestionDialog(title, msg, true, null);
                            if (answer[0] == JOptionPane.YES_OPTION) {
                                newFile[0] = app.showFileOpenDialog(title, false, null);
                            }
                        }
                    });
                } catch (Exception e) {
                    throw new CanceledException();
                }

                if (answer[0] == JOptionPane.CANCEL_OPTION) {
                    throw new CanceledException();
                }

                if (newFile[0] != null) {
                    try {
                        return ProductIO.readProduct(newFile[0]);
                    } catch (IOException e) {
                    }
                }
                return null;
            }
        }
    }

}