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

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.figure.AbstractInteractorListener;
import com.bc.ceres.swing.figure.Interactor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.actions.InsertFigureInteractorInterceptor;
import org.esa.pfa.fe.op.Patch;
import org.esa.pfa.search.CBIRSession;
import org.esa.pfa.search.SearchToolStub;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.Rectangle2D;
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
    private final static Dimension preferredDimension = new Dimension(550, 300);

    private final CBIRSession session;
    private PatchDrawer drawer;
    private PatchSelectionInteractor interactor;
    private JButton addPatchBtn, editBtn, startTrainingBtn;
    private JComboBox<String> quickLookCombo;

    public CBIRQueryToolView() {
        session = CBIRSession.getInstance();
        session.addListener(this);
    }

    public JComponent createControl() {

        final JPanel mainPane = new JPanel(new BorderLayout(5, 5));

        final JPanel topOptionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        quickLookCombo = new JComboBox<>();
        quickLookCombo.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    if (session.hasClassifier()) {
                        session.setQuicklookBandName(session.getQueryPatches(), (String) quickLookCombo.getSelectedItem());
                        drawer.update(session.getQueryPatches());
                    }
                }
            }
        });
        topOptionsPanel.add(new JLabel("Band shown:"));
        topOptionsPanel.add(quickLookCombo);
        mainPane.add(topOptionsPanel, BorderLayout.NORTH);

        final JPanel imageScrollPanel = new JPanel();
        imageScrollPanel.setLayout(new BoxLayout(imageScrollPanel, BoxLayout.X_AXIS));
        imageScrollPanel.setBorder(BorderFactory.createTitledBorder("Query Images"));

        drawer = new PatchDrawer();
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
        try {
            boolean hasClassifier = session.hasClassifier();
            boolean hasQueryImages = false;
            if (hasClassifier) {
                final Patch[] queryPatches = session.getQueryPatches();
                hasQueryImages = queryPatches.length > 0;

                if (hasQueryImages && quickLookCombo.getItemCount() == 0) {
                    final String[] bandNames = session.getAvailableQuickLooks(queryPatches[0]);
                    for (String bandName : bandNames) {
                        quickLookCombo.addItem(bandName);
                    }
                    final String defaultBandName = session.getApplicationDescriptor().getDefaultQuicklookFileName();
                    quickLookCombo.setSelectedItem(defaultBandName);
                }
            }
            quickLookCombo.setEnabled(hasClassifier);
            addPatchBtn.setEnabled(hasClassifier);
            startTrainingBtn.setEnabled(hasQueryImages);
            editBtn.setEnabled(false); //todo //hasQueryImages);
        } catch (Exception e) {
            VisatApp.getApp().handleUnknownException(e);
        }
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
                final List<Patch> queryPatches = new ArrayList<>(processedPatches.length);
                for (Patch patch : processedPatches) {
                    if (patch.getFeatures().length > 0 && patch.getLabel() == Patch.LABEL_RELEVANT) {
                        queryPatches.add(patch);
                    }
                }
                if (queryPatches.isEmpty()) {
                    throw new Exception("No features found in the relevant query images");
                }
                final Patch[] queryImages = queryPatches.toArray(new Patch[queryPatches.size()]);

                ProgressMonitorSwingWorker<Boolean, Void> worker = new ProgressMonitorSwingWorker<Boolean, Void>(getControl(), "Getting images to label") {
                    @Override
                    protected Boolean doInBackground(final ProgressMonitor pm) throws Exception {
                        pm.beginTask("Getting images...", 100);
                        try {
                            session.setQueryImages(queryImages, pm);
                            if (!pm.isCanceled()) {
                                return Boolean.TRUE;
                            }
                        } finally {
                            pm.done();
                        }
                        return Boolean.FALSE;
                    }
                };
                worker.executeWithBlocking();
                if (worker.get()) {
                    getContext().getPage().showToolView(CBIRLabelingToolView.ID);
                }
            }
        } catch (Exception e) {
            VisatApp.getApp().handleUnknownException(e);
        }
    }

    private class PatchInteractorListener extends AbstractInteractorListener {

        @Override
        public void interactionStarted(Interactor interactor, InputEvent inputEvent) {
        }

        @Override
        public void interactionStopped(Interactor interactor, InputEvent inputEvent) {
            if (!session.hasClassifier()) {
                return;
            }
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

            final Rectangle region = new Rectangle(x, y, w, h);
            final PatchProcessor patchProcessor = new PatchProcessor(getControl(), product, parentImage, region, session);
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

    @Override
    public void componentShown() {

        final Window win = getPaneWindow();
        if (win != null) {
            win.setPreferredSize(preferredDimension);
            win.setMaximumSize(preferredDimension);
            win.setSize(preferredDimension);
        }
    }

    @Override
    public void notifyNewClassifier(SearchToolStub classifier) {
        if (isControlCreated()) {
            quickLookCombo.removeAllItems();
            updateControls();

            drawer.update(session.getQueryPatches());
        }
    }

    @Override
    public void notifyDeleteClassifier(SearchToolStub classifier) {
        if (isControlCreated()) {
            quickLookCombo.removeAllItems();
            updateControls();

            drawer.update(new Patch[0]);
        }
    }

    @Override
    public void notifyNewTrainingImages(SearchToolStub classifier) {
    }

    @Override
    public void notifyModelTrained(SearchToolStub classifier) {
    }
}