/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.pfa.ui.toolviews.cbir;

import com.bc.ceres.core.*;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.figure.AbstractInteractorListener;
import com.bc.ceres.swing.figure.Interactor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.actions.InsertFigureInteractorInterceptor;
import org.esa.pfa.fe.op.FeatureWriter;
import org.esa.pfa.fe.op.Patch;
import org.esa.pfa.search.CBIRSession;

import javax.media.jai.RenderedOp;
import javax.media.jai.operator.CropDescriptor;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Query Toolview
 */
public class CBIRQueryToolView extends AbstractToolView implements ActionListener, CBIRSession.CBIRSessionListener {

    public final static String ID = "org.esa.pfa.ui.toolviews.cbir.CBIRQueryToolView";

    private CBIRSession session;
    private PatchDrawer drawer;
    private PatchSelectionInteractor interactor;
    private JButton addPatchBtn, editBtn, startTrainingBtn;

    public CBIRQueryToolView() {
        CBIRSession.Instance().addListener(this);
    }

    public JComponent createControl() {

        final JPanel mainPane = new JPanel(new BorderLayout(5, 5));

        final JPanel imageScrollPanel = new JPanel();
        imageScrollPanel.setLayout(new BoxLayout(imageScrollPanel, BoxLayout.X_AXIS));
        imageScrollPanel.setBorder(BorderFactory.createTitledBorder("Query Images"));

        drawer = new PatchDrawer(new Patch[]{});
        drawer.setMinimumSize(new Dimension(500, 310));
        final JScrollPane scrollPane = new JScrollPane(drawer, JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                                                       JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        final DragScrollListener dl = new DragScrollListener(drawer);
        dl.setDraggableElements(DragScrollListener.DRAGABLE_HORIZONTAL_SCROLL_BAR);
        drawer.addMouseListener(dl);
        drawer.addMouseMotionListener(dl);

        imageScrollPanel.add(scrollPane);

        final JPanel listsPanel = new JPanel();
        final BoxLayout layout = new BoxLayout(listsPanel, BoxLayout.Y_AXIS);
        listsPanel.setLayout(layout);
        listsPanel.add(imageScrollPanel);

        mainPane.add(listsPanel, BorderLayout.CENTER);

        final JPanel btnPanel = new JPanel();
        addPatchBtn = new JButton("Add");
        addPatchBtn.setActionCommand("addPatchBtn");
        addPatchBtn.addActionListener(this);
        addPatchBtn.setEnabled(false);
        btnPanel.add(addPatchBtn);

        mainPane.add(btnPanel, BorderLayout.EAST);

        final JPanel bottomPanel = new JPanel();
        editBtn = new JButton("Edit Constraints");
        editBtn.setActionCommand("editBtn");
        editBtn.addActionListener(this);
        editBtn.setEnabled(false);
        bottomPanel.add(editBtn);

        startTrainingBtn = new JButton("Start Training");
        startTrainingBtn.setActionCommand("startTrainingBtn");
        startTrainingBtn.addActionListener(this);
        startTrainingBtn.setEnabled(false);
        bottomPanel.add(startTrainingBtn);

        mainPane.add(bottomPanel, BorderLayout.SOUTH);

        updateControls();

        return mainPane;
    }

    private void updateControls() {
        boolean sessionActive = false;
        boolean hasQueryImages = false;
        if (session != null) {
            sessionActive = true;
            hasQueryImages = session.getQueryPatches().length > 0;
        }

        addPatchBtn.setEnabled(sessionActive);
        startTrainingBtn.setEnabled(hasQueryImages);
        editBtn.setEnabled(false); //todo //hasQueryImages);
    }

    /**
     * Handles events.
     *
     * @param event the event.
     */
    public void actionPerformed(final ActionEvent event) {
        try {
            final String command = event.getActionCommand();
            if (command.equals("addPatchBtn")) {
                if (VisatApp.getApp().getSelectedProductSceneView() == null) {
                    throw new Exception("First open a product and an image view to be able to add new query images.");
                }

                final Dimension dim = session.getApplicationDescriptor().getPatchDimension();
                interactor = new PatchSelectionInteractor(dim.width, dim.height);
                interactor.addListener(new PatchInteractorListener());
                interactor.addListener(new InsertFigureInteractorInterceptor());
                interactor.activate();

                VisatApp.getApp().setActiveInteractor(interactor);
            } else if (command.equals("startTrainingBtn")) {
                final Patch[] processedPatches = session.getQueryPatches();

                //only add patches with features
                List<Patch> queryPatches = new ArrayList<>(processedPatches.length);
                for (Patch patch : processedPatches) {
                    if (patch.getFeatures().length > 0 && patch.getLabel() == Patch.LABEL_RELEVANT) {
                        queryPatches.add(patch);
                    }
                }
                if (queryPatches.isEmpty()) {
                    throw new Exception("No features found in the relevant query images");
                }

                session.setQueryImages(queryPatches.toArray(new Patch[queryPatches.size()]));

                getContext().getPage().showToolView(CBIRLabelingToolView.ID);
            }
        } catch (Exception e) {
            VisatApp.getApp().showErrorDialog(e.toString());
        }
    }

    private class PatchInteractorListener extends AbstractInteractorListener {

        @Override
        public void interactionStarted(Interactor interactor, InputEvent inputEvent) {
        }

        @Override
        public void interactionStopped(Interactor interactor, InputEvent inputEvent) {
            final PatchSelectionInteractor patchInteractor = (PatchSelectionInteractor) interactor;
            if (patchInteractor != null) {
                try {
                    Rectangle2D rect = patchInteractor.getPatchShape();

                    ProductSceneView productSceneView = getProductSceneView(inputEvent);
                    RenderedImage parentImage = productSceneView != null ? productSceneView.getBaseImageLayer().getImage() : null;

                    final Product product = VisatApp.getApp().getSelectedProduct();
                    addQueryImage(product, (int) rect.getX(), (int) rect.getY(), (int) rect.getWidth(), (int) rect.getHeight(), parentImage);


                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        private void addQueryImage(final Product product, final int x, final int y, final int w, final int h,
                                   final RenderedImage parentImage) throws IOException {

            Rectangle region = new Rectangle(x, y, w, h);
            PatchProcessor patchProcessor = new PatchProcessor(VisatApp.getApp().getApplicationWindow(),
                                                               product, parentImage, region, session);
            patchProcessor.executeWithBlocking();
            Patch patch = null;
            try {
                patch = patchProcessor.get();
            } catch (InterruptedException | ExecutionException e) {
                VisatApp.getApp().handleError("Failed to extract patch", e);
            }
            if (patch != null && patch.getFeatures().length > 0) {
                session.addQueryPatch(patch);
                drawer.update(session.getQueryPatches());
                updateControls();
            } else {
                VisatApp.getApp().showWarningDialog("Failed to extract features for this patch");
            }
        }

        private ProductSceneView getProductSceneView(InputEvent event) {
            ProductSceneView productSceneView = null;
            Component component = event.getComponent();
            while (component != null) {
                if (component instanceof ProductSceneView) {
                    productSceneView = (ProductSceneView) component;
                    break;
                }
                component = component.getParent();
            }
            return productSceneView;
        }
    }

    public void notifyNewSession() {
        session = CBIRSession.Instance();

        if (isControlCreated()) {
            updateControls();

            drawer.update(session.getQueryPatches());

            final Window win = getPaneWindow();
            if (win != null) {
                win.setPreferredSize(new Dimension(600, 250));
                win.setMaximumSize(new Dimension(600, 250));
                win.setSize(new Dimension(600, 250));
            }
        }
    }

    public void notifyNewTrainingImages() {
    }

    public void notifyModelTrained() {
    }
}