/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.meris.radiometry.visat;

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.swing.binding.Binding;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.binding.PropertyPane;
import com.bc.ceres.swing.selection.AbstractSelectionChangeListener;
import com.bc.ceres.swing.selection.SelectionChangeEvent;
import org.esa.beam.dataio.envisat.EnvisatConstants;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.ui.DefaultIOParametersPanel;
import org.esa.beam.framework.gpf.ui.SourceProductSelector;
import org.esa.beam.framework.gpf.ui.TargetProductSelector;
import org.esa.beam.framework.gpf.ui.TargetProductSelectorModel;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.util.io.FileUtils;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;

class RadiometryForm extends JTabbedPane {

    private final AppContext appContext;
    private final OperatorSpi operatorSpi;
    private final PropertySet propertySet;
    private final TargetProductSelector targetProductSelector;
    private final DefaultIOParametersPanel ioParamPanel;
    private BindingContext processingParamBindingContext;

    RadiometryForm(AppContext appContext, OperatorSpi operatorSpi, PropertySet propertySet,
                   TargetProductSelector targetProductSelector) {
        this.appContext = appContext;
        this.operatorSpi = operatorSpi;
        this.propertySet = propertySet;
        this.targetProductSelector = targetProductSelector;

        ioParamPanel = createIOParamTab();
        addTab("I/O Parameters", ioParamPanel);
        addTab("Processing Parameters", createProcessingParamTab());
        final PropertyContainer targetProductPC = targetProductSelector.getModel().getValueContainer();
        FormatChangeListener formatChangeListener = new FormatChangeListener();
        targetProductPC.addPropertyChangeListener("formatName", formatChangeListener);
        targetProductPC.addPropertyChangeListener("saveToFileSelected", formatChangeListener);
        updateEnabledState(isSaveToEnvisatFormatSelected());
    }

    public void prepareShow() {
        ioParamPanel.initSourceProductSelectors();
    }

    public void prepareHide() {
        ioParamPanel.releaseSourceProductSelectors();
    }

    public Product getSourceProduct() {
        return ioParamPanel.getSourceProductSelectorList().get(0).getSelectedProduct();
    }

    private DefaultIOParametersPanel createIOParamTab() {
        final DefaultIOParametersPanel ioPanel = new DefaultIOParametersPanel(appContext, operatorSpi.getOperatorDescriptor(),
                                                                              targetProductSelector);
        final ArrayList<SourceProductSelector> sourceProductSelectorList = ioPanel.getSourceProductSelectorList();
        if (!sourceProductSelectorList.isEmpty()) {
            final SourceProductSelector sourceProductSelector = sourceProductSelectorList.get(0);
            sourceProductSelector.addSelectionChangeListener(new SourceProductChangeListener());
        }
        return ioPanel;
    }

    private JScrollPane createProcessingParamTab() {
        PropertyPane parametersPane = new PropertyPane(propertySet);
        processingParamBindingContext = parametersPane.getBindingContext();
        final JPanel parametersPanel = parametersPane.createPanel();
        parametersPanel.setBorder(new EmptyBorder(4, 4, 4, 4));
        return new JScrollPane(parametersPanel);
    }

    private void updateEnabledState(boolean isEnvisatFormatSelected) {
        Binding binding = processingParamBindingContext.getBinding("doRadToRefl");
        for (JComponent component : binding.getComponents()) {
            component.setEnabled(!isEnvisatFormatSelected);
        }
    }

    private boolean isSaveToEnvisatFormatSelected() {
        final PropertyContainer targetProductPC = targetProductSelector.getModel().getValueContainer();
        String formatName = targetProductPC.getProperty("formatName").getValue();
        final boolean isEnvisatFormatSelected = EnvisatConstants.ENVISAT_FORMAT_NAME.equals(formatName);
        boolean isSaveToFileSelected = targetProductPC.getProperty("saveToFileSelected").getValue();
        return isEnvisatFormatSelected && isSaveToFileSelected;
    }

    private class SourceProductChangeListener extends AbstractSelectionChangeListener {

        private static final String TARGET_PRODUCT_NAME_SUFFIX = "_radiometry";

        @Override
        public void selectionChanged(SelectionChangeEvent event) {
            String productName = "";
            final Product selectedProduct = (Product) event.getSelection().getSelectedValue();
            if (selectedProduct != null) {
                productName = FileUtils.getFilenameWithoutExtension(selectedProduct.getName());
            }
            final TargetProductSelectorModel targetProductSelectorModel = targetProductSelector.getModel();
            targetProductSelectorModel.setProductName(productName + TARGET_PRODUCT_NAME_SUFFIX);
        }
    }


    private class FormatChangeListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            boolean saveToEnvisatFormatSelected = isSaveToEnvisatFormatSelected();
            updateEnabledState(saveToEnvisatFormatSelected);

            if (saveToEnvisatFormatSelected) {
                propertySet.setValue("doRadToRefl", false);
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        final String message = "When selecting ENVISAT as target format, the " +
                                               "radiance to reflectance conversion can not be performed.\n" +
                                               "The parameter is now deselected and disabled.";
                        JOptionPane.showMessageDialog(RadiometryForm.this, message);
                    }
                });
            }
        }
    }
}
