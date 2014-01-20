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
import org.esa.beam.visat.toolviews.cbir.BlockDrawer;
import org.esa.beam.visat.toolviews.cbir.DragScrollListener;
import org.esa.beam.visat.toolviews.cbir.PatchDrawer;
import org.esa.beam.visat.toolviews.cbir.TaskPanel;

import javax.swing.*;
import java.awt.*;

/**
    Labeling Panel
 */
public class FeatureExtractionPanel extends TaskPanel {

    private final static String instructionsStr = "Select query images by selecting patch areas in an image view";
    private final CBIRSession session;

    public FeatureExtractionPanel(final CBIRSession session) {
        super("Feature Extraction");
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
        return new LabelingPanel(session);
    }

    public boolean validateInput() {
        return true;
    }

    private void createPanel() {

        this.add(createInstructionsPanel(null, instructionsStr), BorderLayout.NORTH);



        final JPanel listsPanel = new JPanel();
        final BoxLayout layout = new BoxLayout(listsPanel, BoxLayout.Y_AXIS);
        listsPanel.setLayout(layout);


        this.add(listsPanel, BorderLayout.SOUTH);
    }
}