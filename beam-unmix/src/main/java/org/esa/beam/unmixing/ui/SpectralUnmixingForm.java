/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.unmixing.ui;

import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.ValueSet;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.selection.AbstractSelectionChangeListener;
import com.bc.ceres.swing.selection.SelectionChangeEvent;
import com.bc.ceres.swing.selection.SelectionChangeListener;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.ui.SourceProductSelector;
import org.esa.beam.framework.gpf.ui.TargetProductSelector;
import org.esa.beam.framework.gpf.ui.TargetProductSelectorModel;
import org.esa.beam.framework.ui.AppContext;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Insets;
import java.util.ArrayList;

class SpectralUnmixingForm extends JPanel {

    AppContext appContext;
    SpectralUnmixingFormModel formModel;
    EndmemberForm endmemberForm;
    SourceProductSelector sourceProductSelector;
    TargetProductSelector targetProductSelector;
    JList sourceBandNames;
    JTextField abundanceBandNameSuffix;
    JTextField errorBandNameSuffix;
    JTextField minBandwidth;
    JComboBox unmixingModelName;
    JCheckBox computeErrorBands;

    SpectralUnmixingForm(AppContext appContext, PropertySet propertySet, TargetProductSelector targetProductSelector) {
        this.appContext = appContext;
        this.targetProductSelector = targetProductSelector;
        this.formModel = new SpectralUnmixingFormModel(appContext.getSelectedProduct(), propertySet);
        this.endmemberForm = new EndmemberForm(appContext);
        this.sourceProductSelector = new SourceProductSelector(appContext);
        createComponents();
        bindComponents();
    }

    public SpectralUnmixingFormModel getFormModel() {
        return formModel;
    }

    public EndmemberForm getEndmemberForm() {
        return endmemberForm;
    }

    void prepareShow() {
        sourceProductSelector.initProducts();
        final Product selectedProduct = appContext.getSelectedProduct();
        updateTargetProductName(selectedProduct);
        targetProductSelector.getProductNameTextField().requestFocus();
    }

    private void updateTargetProductName(Product selectedProduct) {
        final TargetProductSelectorModel selectorModel = targetProductSelector.getModel();
        if (selectedProduct != null) {
            selectorModel.setProductName(selectedProduct.getName() + "_unmixed");
        } else if (selectorModel.getProductName() == null) {
            selectorModel.setProductName("unmixed");
        }
    }

    void prepareHide() {
        sourceProductSelector.releaseProducts();
    }

    private void bindComponents() {
        BindingContext bindingContext = new BindingContext(formModel.getOperatorValueContainer());
        bindingContext.bind("unmixingModelName", unmixingModelName);
        bindingContext.bind("abundanceBandNameSuffix", abundanceBandNameSuffix);
        bindingContext.bind("errorBandNameSuffix", errorBandNameSuffix);
        bindingContext.bind("sourceBandNames", sourceBandNames, true);
        bindingContext.bind("computeErrorBands", computeErrorBands);
        bindingContext.bind("minBandwidth", minBandwidth);
    }

    private void createComponents() {
        sourceBandNames = new JList();

        final PropertyDescriptor propertyDescriptor = formModel.getOperatorValueContainer().getDescriptor(
                "sourceBandNames");
        SelectionChangeListener valueSetUpdater = new AbstractSelectionChangeListener() {

            @Override
            public void selectionChanged(SelectionChangeEvent event) {
                final Product selectedProduct = (Product) event.getSelection().getSelectedValue();
                final String[] validNames;
                if (selectedProduct != null) {
                    String[] bandNames = selectedProduct.getBandNames();
                    ArrayList<String> names = new ArrayList<String>(bandNames.length);
                    for (String bandName : bandNames) {
                        if (selectedProduct.getBand(bandName).getSpectralWavelength() > 0.0) {
                            names.add(bandName);
                        }
                    }
                    validNames = names.toArray(new String[names.size()]);
                } else {
                    validNames = new String[0];
                }
                final ValueSet valueSet = new ValueSet(validNames);
                propertyDescriptor.setValueSet(valueSet);
                formModel.setSourceProduct(selectedProduct);
                updateTargetProductName(selectedProduct);
            }
        };
        sourceProductSelector.addSelectionChangeListener(valueSetUpdater);

        final TargetProductSelectorModel targetProductSelectorModel = targetProductSelector.getModel();
        targetProductSelectorModel.setSaveToFileSelected(true);
        targetProductSelectorModel.setOpenInAppSelected(true);
        abundanceBandNameSuffix = new JTextField();
        errorBandNameSuffix = new JTextField();
        unmixingModelName = new JComboBox();
        computeErrorBands = new JCheckBox("Compute error bands");
        minBandwidth = new JTextField();

        final TableLayout tableLayout = new TableLayout(2);
        tableLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setRowFill(1, TableLayout.Fill.BOTH);
        tableLayout.setCellFill(0, 0, TableLayout.Fill.BOTH);
        tableLayout.setCellColspan(1, 0, 2);
        tableLayout.setCellColspan(2, 0, 2);
        tableLayout.setRowWeightY(1, 1.0);
        tableLayout.setTableWeightX(1.0);
        setLayout(tableLayout);
        add(createSourcePanel());
        add(createTargetPanel());
        add(createParametersPanel());
    }

    private JPanel createSourcePanel() {
        return sourceProductSelector.createDefaultPanel();
    }

    private JComponent createTargetPanel() {
        return targetProductSelector.createDefaultPanel();
    }

    private JPanel createParametersPanel() {
        final TableLayout tableLayout = new TableLayout(2);
        tableLayout.setTableAnchor(TableLayout.Anchor.CENTER);
        tableLayout.setTableFill(TableLayout.Fill.BOTH);
        tableLayout.setTablePadding(3, 3);
        tableLayout.setCellPadding(0, 0, new Insets(0, 0, 10, 10));
        tableLayout.setCellColspan(1, 0, 2);
        tableLayout.setCellColspan(2, 0, 2);
        tableLayout.setRowWeightY(0, 0.5);
        tableLayout.setRowWeightY(1, 0.0);
        tableLayout.setRowWeightY(2, 0.5);
        tableLayout.setColumnWeightX(0, 1.0);
        tableLayout.setColumnWeightX(1, 1.0);
        JPanel panel = new JPanel(tableLayout);
        panel.setBorder(BorderFactory.createTitledBorder("Parameters"));
        panel.add(createSourceBandsPanel());
        panel.add(createSubParametersPanel());
        panel.add(new JLabel("Endmembers:"));
        panel.add(endmemberForm);
        return panel;
    }

    private JPanel createSourceBandsPanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.add(new JLabel("Spectral source bands:"), BorderLayout.NORTH);
        panel.add(new JScrollPane(sourceBandNames), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createSubParametersPanel() {
        final TableLayout tableLayout = new TableLayout(2);
        tableLayout.setTablePadding(3, 3);
        tableLayout.setTableAnchor(TableLayout.Anchor.WEST);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setColumnWeightX(1, 1.0);
        tableLayout.setCellColspan(4, 0, 2);
        JPanel panel = new JPanel(tableLayout);

        panel.add(new JLabel("Abundance band name suffix: "));
        panel.add(abundanceBandNameSuffix);

        panel.add(new JLabel("Error band name suffix: "));
        panel.add(errorBandNameSuffix);

        panel.add(new JLabel("Spectral unmixing model: "));
        panel.add(unmixingModelName);

        panel.add(new JLabel("Minimum spectral bandwidth: "));
        panel.add(minBandwidth);

        panel.add(computeErrorBands);

        panel.add(tableLayout.createVerticalSpacer());
        return panel;
    }
}
