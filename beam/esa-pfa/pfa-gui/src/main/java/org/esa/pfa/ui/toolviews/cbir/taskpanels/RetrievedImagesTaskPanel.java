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

import org.esa.pfa.search.CBIRSession;
import org.esa.pfa.ui.toolviews.cbir.DragScrollListener;
import org.esa.pfa.ui.toolviews.cbir.PatchDrawer;
import org.esa.pfa.ui.toolviews.cbir.TaskPanel;

import javax.swing.*;
import java.awt.*;

/**
    Labeling Panel
 */
public class RetrievedImagesTaskPanel extends TaskPanel {

    private final static String instructionsStr = "Assess the level of accuracy in the retrieval";
    private final CBIRSession session;

    public RetrievedImagesTaskPanel(final CBIRSession session) {
        super("Retrieved Images");
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
        return true;
    }

    public TaskPanel getNextPanel() {
        return new LabelingTaskPanel(session);
    }

    public boolean validateInput() throws Exception {
        session.getImagesToLabel();
        return true;
    }

    private void createPanel() {

        this.add(createInstructionsPanel(null, instructionsStr), BorderLayout.NORTH);

        final JPanel relPanel = new JPanel(new BorderLayout(2, 2));
        relPanel.setBorder(BorderFactory.createTitledBorder("Retrieved Images"));

        final PatchDrawer drawer = new PatchDrawer(session.getRetrievedImages());
        drawer.setPreferredSize(new Dimension(2000, 2000));
        final JScrollPane scrollPane1 = new JScrollPane(drawer);

        final DragScrollListener dl = new DragScrollListener(drawer);
        drawer.addMouseListener(dl);
        drawer.addMouseMotionListener(dl);

        relPanel.add(scrollPane1, BorderLayout.CENTER);

        //final JPanel listsPanel = new JPanel();
        //final BoxLayout layout = new BoxLayout(listsPanel, BoxLayout.Y_AXIS);
       // listsPanel.setLayout(layout);
       // listsPanel.add(relPanel);

        this.add(relPanel, BorderLayout.CENTER);
    }
}