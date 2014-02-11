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

import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.pfa.fe.PFAApplicationDescriptor;
import org.esa.pfa.fe.PFAApplicationRegistry;
import org.esa.pfa.search.CBIRSession;
import org.esa.pfa.ui.toolviews.cbir.TaskPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
    Start Panel
 */
public class CBIRStartTaskPanel extends TaskPanel {

    private final static String instructionsStr = "Select a feature extraction application";

    private JComboBox<String> applicationCombo = new JComboBox<>();

    private JList<String> classifierList = new JList<String>();
    private JTextField numTrainingImages = new JTextField("12");
    private JTextField numRetrievedImages = new JTextField("50");
    private JLabel iterationsLabel = new JLabel();

    private CBIRSession session = null;

    public CBIRStartTaskPanel() {
        super("Content Based Image Retrieval");

        final PFAApplicationDescriptor[] apps = PFAApplicationRegistry.getInstance().getAllDescriptors();
        for(PFAApplicationDescriptor app : apps) {
            applicationCombo.addItem(app.getName());
        }

        applicationCombo.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent event) {
                final String fea = (String) applicationCombo.getSelectedItem();

            }
        });

        createPanel();
    }

    public void returnFromLaterStep() {
    }

    public boolean canRedisplayNextPanel() {
        return false;
    }

    public boolean hasNextPanel() {
        return true;
    }

    public boolean canFinish() {
        return false;
    }

    public TaskPanel getNextPanel() {
        return new QueryTaskPanel(session);
    }

    public boolean validateInput() {
        try {
            final int numTrainingImg = Integer.parseInt(numTrainingImages.getText());
            final int numRetrievedImg = Integer.parseInt(numRetrievedImages.getText());

            final String application = (String)applicationCombo.getSelectedItem();
            final PFAApplicationDescriptor applicationDescriptor = PFAApplicationRegistry.getInstance().getDescriptor(application);

            session = new CBIRSession(applicationDescriptor, numTrainingImg, numRetrievedImg);
            return true;
        } catch (Exception e) {
            showErrorMsg(e.getMessage());
        }
        return false;
    }

    private void createPanel() {

        this.add(createInstructionsPanel(null, instructionsStr), BorderLayout.NORTH);

        final JPanel contentPane = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = GridBagUtils.createDefaultConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.gridx = 0;
        gbc.gridy = 0;

        contentPane.add(new Label("Application:"), gbc);
        gbc.gridy++;
        gbc.gridwidth = 3;
        contentPane.add(applicationCombo, gbc);
        gbc.gridwidth = 1;
        gbc.gridy+=2;
        contentPane.add(new JLabel("Saved Classifiers:"), gbc);
        gbc.gridy++;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridy++;
        contentPane.add(new JScrollPane(classifierList), gbc);

        final JPanel optionsPane = new JPanel(new GridBagLayout());
        final GridBagConstraints gbcOpt = GridBagUtils.createDefaultConstraints();
        gbcOpt.fill = GridBagConstraints.HORIZONTAL;
        gbcOpt.anchor = GridBagConstraints.NORTHWEST;
        gbcOpt.gridx = 0;
        gbcOpt.gridy = 0;

        optionsPane.add(new JLabel("# of training images:"), gbcOpt);
        gbcOpt.gridx = 1;
        numTrainingImages.setColumns(3);
        optionsPane.add(numTrainingImages, gbcOpt);

        gbcOpt.gridy++;
        gbcOpt.gridx = 0;
        optionsPane.add(new JLabel("# of retrieved images:"), gbcOpt);
        gbcOpt.gridx = 1;
        numRetrievedImages.setColumns(3);
        optionsPane.add(numRetrievedImages, gbcOpt);
        gbcOpt.gridy++;
        gbcOpt.gridx = 0;
        contentPane.add(iterationsLabel, gbcOpt);

        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 1;
        contentPane.add(optionsPane, gbc);

        this.add(contentPane, BorderLayout.CENTER);
    }
}