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

import com.bc.ceres.swing.selection.AbstractSelectionChangeListener;
import com.bc.ceres.swing.selection.SelectionChangeEvent;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.ui.SourceProductSelector;
import org.esa.beam.util.ProductUtils;
import org.esa.pfa.fe.op.FeatureWriter;
import org.esa.pfa.search.CBIRSession;
import org.esa.pfa.search.PatchImage;
import org.esa.beam.visat.VisatApp;
import org.esa.pfa.ui.toolviews.cbir.DragScrollListener;
import org.esa.pfa.ui.toolviews.cbir.PatchDrawer;
import org.esa.pfa.ui.toolviews.cbir.TaskPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
    Labeling Panel
 */
public class QueryTaskPanel extends TaskPanel implements ActionListener {

    private final static String instructionsStr = "Select query images by selecting patch areas in an image view";
    private final CBIRSession session;
    private PatchDrawer drawer;

    //temp
    private final SourceProductSelector sourceProductSelector;

    public QueryTaskPanel(final CBIRSession session) {
        super("Query Images");
        this.session = session;

        this.sourceProductSelector = new SourceProductSelector(VisatApp.getApp(), "Source Product:");
        sourceProductSelector.initProducts();

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
        return new FeatureExtractionTaskPanel(session);
    }

    public boolean validateInput() {
        return true;
    }

    private void createPanel() {

        this.add(createInstructionsPanel(null, instructionsStr), BorderLayout.NORTH);

        final JPanel imageScrollPanel = new JPanel(new BorderLayout(2, 2));
        imageScrollPanel.setBorder(BorderFactory.createTitledBorder("Query Images"));
        imageScrollPanel.setMinimumSize(new Dimension(500, 110));

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

        //temp
        this.add(createSourceProductPanel(), BorderLayout.CENTER);

        this.add(listsPanel, BorderLayout.SOUTH);
    }


    //temp
    private static int subX = 0;
    private static int subY = 0;

    /**
     * Handles events.
     *
     * @param event the event.
     */
    public void actionPerformed(final ActionEvent event) {
        try {
            final String command = event.getActionCommand();
            if (command.equals("addButton")) {
                final Product product = sourceProductSelector.getSelectedProduct();
                if(product == null)
                    return;

                final Dimension dim = session.getApplicationDescriptor().getPatchDimension();
                final Product subset = FeatureWriter.createSubset(product, new Rectangle(subX, subY, dim.width, dim.height));
                subX += dim.width;
                subY += dim.height;

                session.addQueryProduct(subset);
                session.addQueryImage(new PatchImage(subset, ProductUtils.findSuitableQuicklookBandName(subset)));
                drawer.update(session.getQueryImages());
            }
        } catch (Exception e) {
            VisatApp.getApp().showErrorDialog(e.toString());
        }
    }

    private JPanel createSourceProductPanel() {
        final JPanel panel = sourceProductSelector.createDefaultPanel();
        sourceProductSelector.getProductNameLabel().setText("Name:");
        sourceProductSelector.getProductNameComboBox().setPrototypeDisplayValue(
                "MER_RR__1PPBCM20030730_071000_000003972018_00321_07389_0000.N1");
        sourceProductSelector.addSelectionChangeListener(new AbstractSelectionChangeListener() {
            @Override
            public void selectionChanged(SelectionChangeEvent event) {
                final Product sourceProduct = sourceProductSelector.getSelectedProduct();


            }
        });
        return panel;
    }
}