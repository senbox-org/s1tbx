/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.collocation.visat;

import org.esa.beam.framework.gpf.ui.SourceProductSelector;
import org.esa.beam.framework.gpf.ui.TargetProductSelector;
import org.esa.beam.framework.ui.AppContext;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;

/**
 * Form for geographic collocation dialog.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class CollocationForm extends JPanel {

    private CollocationFormModel model;

    private SourceProductSelector masterProductSelector;
    private SourceProductSelector slaveProductSelector;

    private JCheckBox renameMasterComponentsCheckBox;
    private JCheckBox renameSlaveComponentsCheckBox;
    private JTextField masterComponentPatternField;
    private JTextField slaveComponentPatternField;
    private JComboBox resamplingComboBox;
    private TargetProductSelector targetProductSelector;

    public CollocationForm(final CollocationFormModel model, TargetProductSelector targetProductSelector,
                           AppContext appContext) {
        this.model = model;

        this.targetProductSelector = targetProductSelector;
        masterProductSelector = new SourceProductSelector(appContext,
                                                          "Master (pixel values are conserved):");
        slaveProductSelector = new SourceProductSelector(appContext,
                                                         "Slave (pixel values are resampled onto the master grid):");
        renameMasterComponentsCheckBox = new JCheckBox("Rename master components:");
        renameSlaveComponentsCheckBox = new JCheckBox("Rename slave components:");
        masterComponentPatternField = new JTextField();
        slaveComponentPatternField = new JTextField();
        resamplingComboBox = new JComboBox(model.getResamplingComboBoxModel());

        slaveProductSelector.getProductNameComboBox().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                model.adaptResamplingComboBoxModel();
            }
        });

        final ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateUIState();
            }
        };
        renameMasterComponentsCheckBox.addActionListener(listener);
        renameSlaveComponentsCheckBox.addActionListener(listener);

        createComponents();
        bindComponents();
    }

    public void prepareShow() {
        masterProductSelector.initProducts();
        if (masterProductSelector.getProductCount() > 0) {
            masterProductSelector.setSelectedIndex(0);
        }
        slaveProductSelector.initProducts();
        if (slaveProductSelector.getProductCount() > 1) {
            slaveProductSelector.setSelectedIndex(1);
        }
    }

    private void updateUIState() {
        masterComponentPatternField.setEnabled(renameMasterComponentsCheckBox.isSelected());
        slaveComponentPatternField.setEnabled(renameSlaveComponentsCheckBox.isSelected());
    }

    public void prepareHide() {
        masterProductSelector.releaseProducts();
        slaveProductSelector.releaseProducts();
    }

    private void createComponents() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        add(createSourceProductPanel());
        add(createTargetProductPanel());
        add(createRenamingPanel());
        add(createResamplingPanel());
    }

    private void bindComponents() {
        final BindingContext sbc = new BindingContext(model.getValueContainer());
        sbc.bind("masterProduct", masterProductSelector.getProductNameComboBox());
        sbc.bind("slaveProduct", slaveProductSelector.getProductNameComboBox());
        sbc.bind("renameMasterComponents", renameMasterComponentsCheckBox);
        sbc.bind("renameSlaveComponents", renameSlaveComponentsCheckBox);
        sbc.bind("masterComponentPattern", masterComponentPatternField);
        sbc.bind("slaveComponentPattern", slaveComponentPatternField);
    }

    private JPanel createSourceProductPanel() {
        final JPanel masterPanel = new JPanel(new BorderLayout(3, 3));
        masterPanel.add(masterProductSelector.getProductNameLabel(), BorderLayout.NORTH);
        masterProductSelector.getProductNameComboBox().setPrototypeDisplayValue(
                "MER_RR__1PPBCM20030730_071000_000003972018_00321_07389_0000.N1");
        masterPanel.add(masterProductSelector.getProductNameComboBox(), BorderLayout.CENTER);
        masterPanel.add(masterProductSelector.getProductFileChooserButton(), BorderLayout.EAST);

        final JPanel slavePanel = new JPanel(new BorderLayout(3, 3));
        slavePanel.add(slaveProductSelector.getProductNameLabel(), BorderLayout.NORTH);
        slavePanel.add(slaveProductSelector.getProductNameComboBox(), BorderLayout.CENTER);
        slavePanel.add(slaveProductSelector.getProductFileChooserButton(), BorderLayout.EAST);

        final TableLayout layout = new TableLayout(1);
        layout.setTableAnchor(TableLayout.Anchor.WEST);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setTableWeightX(1.0);
        layout.setCellPadding(0, 0, new Insets(3, 3, 3, 3));
        layout.setCellPadding(1, 0, new Insets(3, 3, 3, 3));

        final JPanel panel = new JPanel(layout);
        panel.setBorder(BorderFactory.createTitledBorder("Source Products"));
        panel.add(masterPanel);
        panel.add(slavePanel);

        return panel;
    }

    private JPanel createTargetProductPanel() {
        return targetProductSelector.createDefaultPanel();
    }

    private JPanel createRenamingPanel() {
        final TableLayout layout = new TableLayout(2);
        layout.setTableAnchor(TableLayout.Anchor.WEST);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setColumnWeightX(0, 0.0);
        layout.setColumnWeightX(1, 1.0);
        layout.setCellPadding(0, 0, new Insets(3, 3, 3, 3));
        layout.setCellPadding(1, 0, new Insets(3, 3, 3, 3));

        final JPanel panel = new JPanel(layout);
        panel.setBorder(BorderFactory.createTitledBorder("Renaming of Source Product Components"));
        panel.add(renameMasterComponentsCheckBox);
        panel.add(masterComponentPatternField);
        panel.add(renameSlaveComponentsCheckBox);
        panel.add(slaveComponentPatternField);

        return panel;
    }

    private JPanel createResamplingPanel() {
        final TableLayout layout = new TableLayout(3);
        layout.setTableAnchor(TableLayout.Anchor.LINE_START);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setColumnWeightX(0, 0.0);
        layout.setColumnWeightX(1, 0.0);
        layout.setColumnWeightX(2, 1.0);
        layout.setCellPadding(0, 0, new Insets(3, 3, 3, 3));

        final JPanel panel = new JPanel(layout);
        panel.setBorder(BorderFactory.createTitledBorder("Resampling"));
        panel.add(new JLabel("Method:"));
        panel.add(resamplingComboBox);
        panel.add(new JLabel());

        return panel;
    }

}
