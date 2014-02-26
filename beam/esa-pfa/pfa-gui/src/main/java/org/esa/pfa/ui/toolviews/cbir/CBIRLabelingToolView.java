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

import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.actions.InsertFigureInteractorInterceptor;
import org.esa.pfa.fe.op.Patch;
import org.esa.pfa.search.CBIRSession;
import org.esa.pfa.ui.toolviews.cbir.taskpanels.RetrievedImagesTaskPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
    Labeling Toolview
 */
public class CBIRLabelingToolView extends AbstractToolView implements Patch.PatchListener, ActionListener,
        CBIRSession.CBIRSessionListener {

    public final static String ID = "org.esa.pfa.ui.toolviews.cbir.CBIRLabelingToolView";

    private CBIRSession session;
    private PatchDrawer relavantDrawer;
    private PatchDrawer irrelavantDrawer;

    public CBIRLabelingToolView() {
        CBIRSession.Instance().addListener(this);
    }

    public JComponent createControl() {

        final JPanel mainPane = new JPanel(new BorderLayout(5,5));
        final JPanel relPanel = new JPanel(new BorderLayout(2, 2));
        relPanel.setBorder(BorderFactory.createTitledBorder("Relevant Images"));

        relavantDrawer = new PatchDrawer(new Patch[] {});
        final JScrollPane scrollPane1 = new JScrollPane(relavantDrawer, JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                                                                        JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        final DragScrollListener dl = new DragScrollListener(relavantDrawer);
        dl.setDraggableElements(DragScrollListener.DRAGABLE_HORIZONTAL_SCROLL_BAR);
        relavantDrawer.addMouseListener(dl);
        relavantDrawer.addMouseMotionListener(dl);

        relPanel.add(scrollPane1, BorderLayout.NORTH);

        final JPanel irrelPanel = new JPanel(new BorderLayout(2, 2));
        irrelPanel.setBorder(BorderFactory.createTitledBorder("Irrelevant Images"));

        irrelavantDrawer = new PatchDrawer(new Patch[] {});
        final JScrollPane scrollPane2 = new JScrollPane(irrelavantDrawer, JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                                                                          JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        final DragScrollListener dl2 = new DragScrollListener(irrelavantDrawer);
        dl.setDraggableElements(DragScrollListener.DRAGABLE_HORIZONTAL_SCROLL_BAR);
        irrelavantDrawer.addMouseListener(dl2);
        irrelavantDrawer.addMouseMotionListener(dl2);

        irrelPanel.add(scrollPane2, BorderLayout.NORTH);

        final JPanel listsPanel = new JPanel();
        final BoxLayout layout = new BoxLayout(listsPanel, BoxLayout.Y_AXIS);
        listsPanel.setLayout(layout);
        listsPanel.add(relPanel);
        listsPanel.add(irrelPanel);

        mainPane.add(listsPanel, BorderLayout.CENTER);

        final JPanel bottomPanel = new JPanel();
        final JButton applyBtn = new JButton("Train and Apply Classifier");
        applyBtn.setActionCommand("applyBtn");
        applyBtn.addActionListener(this);
        bottomPanel.add(applyBtn);

        mainPane.add(bottomPanel, BorderLayout.SOUTH);

        updateControls();

        return mainPane;
    }

    private void updateControls() {
        if(session != null) {
            relavantDrawer.update(session.getRelevantTrainingImages());
            irrelavantDrawer.update(session.getIrrelevantTrainingImages());
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
            if(command.equals("applyBtn")) {
                getContext().getPage().showToolView(CBIRRetrievedImagesToolView.ID);

                session.trainModel();
            }
        } catch (Exception e) {
            VisatApp.getApp().showErrorDialog(e.toString());
        }
    }

    private void listenToPatches() {
        final Patch[] relPatches = session.getRelevantTrainingImages();
        for(Patch patch : relPatches) {
            patch.addListener(this);
        }
        final Patch[] irrelPatches = session.getIrrelevantTrainingImages();
        for(Patch patch : irrelPatches) {
            patch.addListener(this);
        }
    }

    public void notifyNewSession() {
        session = CBIRSession.Instance();
    }

    public void notifyNewQueryImages() {
        listenToPatches();

        if(isControlCreated()) {
            updateControls();
        }
    }

    public void notifyModelTrained() {
    }

    public void notifyStateChanged(final Patch patch) {
        session.reassignTrainingImage(patch);

        relavantDrawer.update(session.getRelevantTrainingImages());
        irrelavantDrawer.update(session.getIrrelevantTrainingImages());

    }
}