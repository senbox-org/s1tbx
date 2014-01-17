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
package org.esa.beam.visat.toolviews.cbir;

import com.bc.ceres.swing.selection.AbstractSelectionChangeListener;
import com.bc.ceres.swing.selection.SelectionChangeEvent;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.ui.SourceProductSelector;
import org.esa.beam.visat.VisatApp;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
    Instructions Panel
 */
public class QueryPanel extends TaskPanel {

    private final static String instructionsStr = "Select a feature extraction application";

    private final SourceProductSelector sourceProductSelector;
    private JComboBox<String> featureExtractorCombo = new JComboBox<>();
    private JLabel patchSizeLabel = new JLabel();

    private SearchToolStub searchTool = new SearchToolStub();

    public QueryPanel() {
        super("Feature Extraction Query");

        this.sourceProductSelector = new SourceProductSelector(VisatApp.getApp(), "Source Product:");
        sourceProductSelector.initProducts();

        featureExtractorCombo.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent event) {
                final String fea = (String)featureExtractorCombo.getSelectedItem();

                Dimension patchSize = searchTool.getPatchSize(fea);
                patchSizeLabel.setVisible(true);
                patchSizeLabel.setText("Patch "+patchSize.getWidth()+" x "+ patchSize.getHeight());
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
        return new LabelingPanel();
    }

    public boolean validateInput() {
        return true;
    }

    private void createPanel() {

        final JPanel instructPanel = new JPanel(new BorderLayout(2, 2));
        instructPanel.add(createTitleLabel(), BorderLayout.NORTH);
        instructPanel.add(createTextPanel(null, instructionsStr), BorderLayout.CENTER);
        this.add(instructPanel, BorderLayout.NORTH);

        final JPanel paramPanel = new JPanel();
        final BoxLayout layout = new BoxLayout(paramPanel, BoxLayout.Y_AXIS);
        paramPanel.setLayout(layout);
        this.add(paramPanel, BorderLayout.CENTER);

        patchSizeLabel.setVisible(false);

        paramPanel.add(createSourceProductPanel());
        paramPanel.add(featureExtractorCombo);
        paramPanel.add(patchSizeLabel);
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

                final String[] featureExtractionList = searchTool.getAvailableFeatureExtractors("mission", "productType");

                featureExtractorCombo.removeAllItems();
                for(String fea : featureExtractionList) {
                    featureExtractorCombo.addItem(fea);
                }
            }
        });
        return panel;
    }
}