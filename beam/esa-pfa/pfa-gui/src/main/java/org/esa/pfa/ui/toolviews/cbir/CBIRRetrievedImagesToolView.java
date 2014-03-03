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
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.visat.VisatApp;
import org.esa.pfa.fe.op.Patch;
import org.esa.pfa.search.CBIRSession;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * Retrieved Images Panel
 */
public class CBIRRetrievedImagesToolView extends AbstractToolView implements ActionListener,
        Patch.PatchListener, CBIRSession.CBIRSessionListener {

    public final static String ID = "org.esa.pfa.ui.toolviews.cbir.CBIRRetrievedImagesToolView";

    private CBIRSession session;
    private PatchDrawer drawer;
    private int accuracy = 0;
    private Patch[] retrievedPatches;
    private JButton improveBtn;
    private JButton allRelevantBtn, allIrrelevantBtn;
    private final JLabel accuracyLabel = new JLabel();
    private JComboBox<String> quickLookCombo;

    public CBIRRetrievedImagesToolView() {
        CBIRSession.Instance().addListener(this);
    }

    public JComponent createControl() {

        final JPanel topOptionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        allRelevantBtn = new JButton("Set all relevant");
        allRelevantBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for(Patch patch : retrievedPatches) {
                    patch.setLabel(Patch.LABEL_RELEVANT);
                }
                drawer.repaint();
            }
        });
        topOptionsPanel.add(allRelevantBtn);
        allIrrelevantBtn = new JButton("Set all irrelevant");
        allIrrelevantBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for(Patch patch : retrievedPatches) {
                    patch.setLabel(Patch.LABEL_IRRELEVANT);
                }
                drawer.repaint();
            }
        });
        topOptionsPanel.add(allIrrelevantBtn);

        quickLookCombo = new JComboBox();
        quickLookCombo.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if(e.getStateChange() == ItemEvent.SELECTED) {
                    session.setQuicklookBandName(retrievedPatches, (String)quickLookCombo.getSelectedItem());
                    retrievedPatches = session.getRetrievedImages();
                    drawer.update(retrievedPatches);
                }
            }
        });
        topOptionsPanel.add(new JLabel("Band shown:"));
        topOptionsPanel.add(quickLookCombo);

        final JPanel retPanel = new JPanel(new BorderLayout(2, 2));
        retPanel.setBorder(BorderFactory.createTitledBorder("Retrieved Images"));

        drawer = new PatchDrawer(true, new Patch[] {});
        final JScrollPane scrollPane1 = new JScrollPane(drawer);

        final DragScrollListener dl = new DragScrollListener(drawer);
        drawer.addMouseListener(dl);
        drawer.addMouseMotionListener(dl);

        retPanel.add(scrollPane1, BorderLayout.CENTER);

        final JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.add(accuracyLabel);

        improveBtn = new JButton("Improve Classifier");
        improveBtn.setActionCommand("improveBtn");
        improveBtn.addActionListener(this);
        bottomPanel.add(improveBtn);


        final JPanel mainPane = new JPanel(new BorderLayout(5, 5));
        mainPane.add(topOptionsPanel, BorderLayout.NORTH);
        mainPane.add(retPanel, BorderLayout.CENTER);
        mainPane.add(bottomPanel, BorderLayout.SOUTH);

        updateControls();

        return mainPane;
    }

    private void updateControls() {
        try {
            final boolean haveRetrievedImages = retrievedPatches != null && retrievedPatches.length > 0;
            improveBtn.setEnabled(haveRetrievedImages);
            allRelevantBtn.setEnabled(haveRetrievedImages);
            allIrrelevantBtn.setEnabled(haveRetrievedImages);

            if(haveRetrievedImages) {
                float pct = accuracy/(float)retrievedPatches.length * 100;
                accuracyLabel.setText("Accuracy: "+accuracy+'/'+retrievedPatches.length+" ("+(int)pct+"%)");

                if(quickLookCombo.getItemCount() == 0) {
                    final String[] bandNames = session.getAvailableQuickLooks(retrievedPatches[0]);
                    for(String bandName : bandNames) {
                        quickLookCombo.addItem(bandName);
                    }
                    final String defaultBandName = session.getApplicationDescriptor().getDefaultQuicklookFileName();
                    quickLookCombo.setSelectedItem(defaultBandName);
                }
            }
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
            if (command.equals("improveBtn")) {
                ProgressMonitorSwingWorker<Boolean, Void> worker = new ProgressMonitorSwingWorker<Boolean, Void>(getControl(), "Getting images to label") {
                    @Override
                    protected Boolean doInBackground(ProgressMonitor pm) throws Exception {
                        pm.beginTask("Getting images...", 100);
                        try {
                            session.getImagesToLabel(pm);
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
            VisatApp.getApp().showErrorDialog(e.toString());
        }
    }

    private void listenToPatches() {
        for (Patch patch : retrievedPatches) {
            patch.addListener(this);
        }
    }

    public void notifyNewSession() {
        session = CBIRSession.Instance();
    }

    public void notifyNewTrainingImages() {
    }

    public void notifyModelTrained() {
        try {
            session.retrieveImages();

            retrievedPatches = session.getRetrievedImages();
            listenToPatches();

            accuracy = retrievedPatches.length;
            drawer.update(retrievedPatches);

            updateControls();
        } catch (Exception e) {
            VisatApp.getApp().handleUnknownException(e);
        }
    }

    public void notifyStateChanged(final Patch notifyingPatch) {
        int cnt = 0;
        for(Patch patch : retrievedPatches) {
            if (patch.getLabel() == Patch.LABEL_RELEVANT) {
                cnt++;
            }
        }
        accuracy = cnt;
        updateControls();
    }
}