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
package org.esa.beam.visat.toolviews.cbir.taskpanels;

import org.esa.beam.search.CBIRSession;
import org.esa.beam.search.PatchImage;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.toolviews.cbir.DragScrollListener;
import org.esa.beam.visat.toolviews.cbir.PatchDrawer;
import org.esa.beam.visat.toolviews.cbir.TaskPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
    Labeling Panel
 */
public class QueryPanel extends TaskPanel implements ActionListener {

    private final static String instructionsStr = "Select query images by selecting patch areas in an image view";
    private final CBIRSession session;
    private PatchDrawer drawer;

    public QueryPanel(final CBIRSession session) {
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

    public boolean canFinish() {
        return false;
    }

    public TaskPanel getNextPanel() {
        return new FeatureExtractionPanel(session);
    }

    public boolean validateInput() {
        return true;
    }

    private void createPanel() {

        this.add(createInstructionsPanel(null, instructionsStr), BorderLayout.NORTH);

        final JPanel imageScrollPanel = new JPanel(new BorderLayout(2, 2));
        imageScrollPanel.setBorder(BorderFactory.createTitledBorder("Query Images"));

        drawer = new PatchDrawer(session.getQueryImages());
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

        final JButton addButton = new JButton("Add");
        addButton.setActionCommand("addButton");
        addButton.addActionListener(this);
        listsPanel.add(addButton);

        this.add(listsPanel, BorderLayout.SOUTH);
    }

    /**
     * Handles events.
     *
     * @param event the event.
     */
    public void actionPerformed(final ActionEvent event) {
        try {
            final String command = event.getActionCommand();
            if (command.equals("addButton")) {
                session.addQueryImage(new PatchImage());
                drawer.update(session.getQueryImages());
            }
        } catch (Exception e) {
            VisatApp.getApp().showErrorDialog(e.toString());
        }
    }
}