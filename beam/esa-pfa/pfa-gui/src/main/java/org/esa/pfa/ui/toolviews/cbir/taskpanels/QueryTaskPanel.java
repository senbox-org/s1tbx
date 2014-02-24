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
package org.esa.pfa.ui.toolviews.cbir.taskpanels;

import com.bc.ceres.swing.figure.AbstractInteractorListener;
import com.bc.ceres.swing.figure.Interactor;
import com.bc.ceres.swing.figure.interactions.NullInteractor;
import com.bc.ceres.swing.selection.AbstractSelectionChangeListener;
import com.bc.ceres.swing.selection.SelectionChangeEvent;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.ui.SourceProductSelector;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.actions.InsertFigureInteractorInterceptor;
import org.esa.pfa.fe.op.FeatureWriter;
import org.esa.pfa.fe.op.Patch;
import org.esa.pfa.search.CBIRSession;
import org.esa.pfa.ui.toolviews.cbir.DragScrollListener;
import org.esa.pfa.ui.toolviews.cbir.PatchDrawer;
import org.esa.pfa.ui.toolviews.cbir.PatchSelectionInteractor;
import org.esa.pfa.ui.toolviews.cbir.TaskPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
    Labeling Panel
 */
public class QueryTaskPanel extends TaskPanel implements ActionListener {

    private final static String instructionsStr = "Add query images by selecting patch areas in an image view";
    private final CBIRSession session;
    private PatchDrawer drawer;
    private PatchSelectionInteractor interactor;

    public QueryTaskPanel(final CBIRSession session) {
        super("Query Images");
        this.session = session;

        createPanel();

        repaint();
    }

    public void returnFromLaterStep() {
    }

    public boolean canRedisplayNextPanel() {
        return false;
    }

    public boolean hasNextPanel() {
        return true;
    }

    public boolean canProceedToNextPanel() {
        return session.getQueryPatches().length > 0;
    }

    public boolean canFinish() {
        return false;
    }

    public TaskPanel getNextPanel() {
        VisatApp.getApp().setActiveInteractor(NullInteractor.INSTANCE);

        return new FeatureExtractionTaskPanel(session);
    }

    public boolean validateInput() {
        return true;
    }

    private void createPanel() {

        this.add(createInstructionsPanel(null, instructionsStr), BorderLayout.NORTH);

        final JPanel imageScrollPanel = new JPanel(new BorderLayout(2, 2));
        imageScrollPanel.setBorder(BorderFactory.createTitledBorder("Query Images"));

        drawer = new PatchDrawer(session.getQueryPatches());
        drawer.setMinimumSize(new Dimension(500, 210));
        final JScrollPane scrollPane = new JScrollPane(drawer, JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                                                                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        final DragScrollListener dl = new DragScrollListener(drawer);
        dl.setDraggableElements(DragScrollListener.DRAGABLE_HORIZONTAL_SCROLL_BAR);
        drawer.addMouseListener(dl);
        drawer.addMouseMotionListener(dl);

        imageScrollPanel.add(scrollPane, BorderLayout.NORTH);

        final JPanel listsPanel = new JPanel();
        final BoxLayout layout = new BoxLayout(listsPanel, BoxLayout.Y_AXIS);
        listsPanel.setLayout(layout);
        listsPanel.add(imageScrollPanel);

        this.add(listsPanel, BorderLayout.CENTER);

        final JPanel btnPanel = new JPanel();
        final JButton addPatchButton = new JButton("Add");
        addPatchButton.setActionCommand("addPatchButton");
        addPatchButton.addActionListener(this);
        btnPanel.add(addPatchButton);

        this.add(btnPanel, BorderLayout.EAST);
    }

    /**
     * Handles events.
     *
     * @param event the event.
     */
    public void actionPerformed(final ActionEvent event) {
        try {
            final String command = event.getActionCommand();
            if (command.equals("addPatchButton")) {
                if(VisatApp.getApp().getSelectedProductSceneView() == null) {
                    throw new Exception("First open a product and an image view to be able to add new query images.");
                }

                final Dimension dim = session.getApplicationDescriptor().getPatchDimension();
                interactor = new PatchSelectionInteractor(dim.width, dim.height);
                interactor.addListener(new PatchInteractorListener());
                interactor.addListener(new InsertFigureInteractorInterceptor());
                interactor.activate();

                VisatApp.getApp().setActiveInteractor(interactor);
            }
        } catch (Exception e) {
            VisatApp.getApp().showErrorDialog(e.toString());
        }
    }

    private void addQueryImage(final Product product, final int x, final int y, final int w, final int h) throws IOException {

        final Product subset = FeatureWriter.createSubset(product, new Rectangle(x, y, w, h));
        final int patchX = x/w;
        final int patchY = y/h;

        final BufferedImage image = ProductUtils.createColorIndexedImage(
                subset.getBand(ProductUtils.findSuitableQuicklookBandName(subset)),
                com.bc.ceres.core.ProgressMonitor.NULL);
        final Patch patch = new Patch(patchX, patchY, null, subset);
        patch.setImage(image);
        patch.setLabel(Patch.LABEL_RELEVANT);
        session.addQueryPatch(patch);
        drawer.update(session.getQueryPatches());
    }

    private class PatchInteractorListener extends AbstractInteractorListener {

        @Override
        public void interactionStarted(Interactor interactor, InputEvent inputEvent) {
        }

        @Override
        public void interactionStopped(Interactor interactor, InputEvent inputEvent) {
            final PatchSelectionInteractor patchInteractor = (PatchSelectionInteractor) interactor;
            if(patchInteractor != null) {
                try {
                    Rectangle2D rect = patchInteractor.getPatchShape();

                    final Product product = VisatApp.getApp().getSelectedProduct();
                    addQueryImage(product, (int)rect.getX(), (int)rect.getY(), (int)rect.getWidth(), (int)rect.getHeight());

                    getOwner().updateState();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}