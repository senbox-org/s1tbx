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

import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.selection.AbstractSelectionChangeListener;
import com.bc.ceres.swing.selection.SelectionChangeEvent;
import org.esa.beam.framework.PFAApplicationDescriptor;
import org.esa.beam.framework.PFAApplicationRegistry;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.ui.SourceProductSelector;
import org.esa.beam.search.CBIRSession;
import org.esa.beam.search.SearchToolStub;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.toolviews.cbir.TaskPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
    Instructions Panel
 */
public class CBIRStartPanel extends TaskPanel {

    private final static String instructionsStr = "Select a feature extraction application";

    private final SourceProductSelector sourceProductSelector;
    private JComboBox<String> applicationCombo = new JComboBox<>();

    private CBIRSession session = new CBIRSession();

    public CBIRStartPanel() {
        super("Content Based Image Retrieval");

        this.sourceProductSelector = new SourceProductSelector(VisatApp.getApp(), "Source Product:");
        sourceProductSelector.initProducts();

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
        return new QueryPanel(session);
    }

    public boolean validateInput() {
        return true;
    }

    private void createPanel() {

        this.add(createInstructionsPanel(null, instructionsStr), BorderLayout.NORTH);

        final TableLayout tableLayout = new TableLayout(2);
        tableLayout.setTableAnchor(TableLayout.Anchor.WEST);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setTablePadding(4, 4);
        for(int i=0; i< 3; i++) {
            tableLayout.setCellColspan(i, 0, 2);
        }

        final JPanel paramPanel = new JPanel(tableLayout);
        this.add(paramPanel, BorderLayout.SOUTH);

        paramPanel.add(new Label("Application:"));
        paramPanel.add(applicationCombo);
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