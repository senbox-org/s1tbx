package org.esa.beam.unmixing.ui;

import com.bc.ceres.binding.swing.SwingBindingContext;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.ui.SourceProductSelector;
import org.esa.beam.framework.gpf.ui.TargetProductSelector;
import org.esa.beam.framework.gpf.ui.TargetProductSelectorModel;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.TableLayout;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class SpectralUnmixingForm extends JPanel {
    SpectralUnmixingFormModel formModel;
    EndmemberForm endmemberForm;
    SourceProductSelector sourceProductSelector;
    TargetProductSelector targetProductSelector;
    JList sourceBandNames;
    JTextField targetBandNameSuffix;
    JTextField minBandwidth;
    JComboBox unmixingModelName;
    JCheckBox computeErrorBands;

    public SpectralUnmixingForm(AppContext appContext, TargetProductSelector targetProductSelector) {
        this.targetProductSelector = targetProductSelector;
        this.formModel = new SpectralUnmixingFormModel(appContext.getSelectedProduct());
        this.endmemberForm = new EndmemberForm(appContext);
        sourceProductSelector = new SourceProductSelector(appContext, "Source product name:");
        createComponents();
        bindComponents();
    }

    public SpectralUnmixingFormModel getFormModel() {
        return formModel;
    }

    public EndmemberForm getEndmemberForm() {
        return endmemberForm;
    }
    
    public void initForm() {
        sourceProductSelector.initProductList();
        if (sourceProductSelector.getProductCount() > 0) {
            sourceProductSelector.setSelectedIndex(0);
        }
        final Product sourceProduct = formModel.getSourceProduct();
        targetProductSelector.getModel().setProductName(sourceProduct != null ? sourceProduct.getName() + "_unmixed" : "unmixed");
    }

    private void bindComponents() {
        SwingBindingContext bindingContext = new SwingBindingContext(formModel.getOperatorValueContainer());

        bindingContext.bind(unmixingModelName, "unmixingModelName");
        bindingContext.bind(targetBandNameSuffix, "targetBandNameSuffix");
        bindingContext.bind(sourceBandNames, "sourceBandNames", true);
        bindingContext.bind(computeErrorBands, "computeErrorBands");
        bindingContext.bind(minBandwidth, "minBandwidth");

    }

    private void createComponents() {
        sourceProductSelector.getProductNameComboBox().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final Product selectedProduct = sourceProductSelector.getSelectedProduct();
                formModel.setSourceProduct(selectedProduct);
                sourceBandNames.setModel(formModel.getBandListModel());
            }
        });
        sourceBandNames = new JList();
        sourceBandNames.setModel(formModel.getBandListModel());

        final TargetProductSelectorModel targetProductSelectorModel = targetProductSelector.getModel();
        targetProductSelectorModel.setSaveToFileSelected(true);
        targetProductSelectorModel.setOpenInAppSelected(true);
        targetBandNameSuffix = new JTextField();
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
        final JPanel subPanel = new JPanel(new BorderLayout());
        subPanel.add(sourceProductSelector.getProductNameComboBox(), BorderLayout.CENTER);
        subPanel.add(sourceProductSelector.getProductFileChooserButton(), BorderLayout.EAST);

        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTableAnchor(TableLayout.Anchor.WEST);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setRowFill(0, TableLayout.Fill.HORIZONTAL);
        tableLayout.setRowFill(1, TableLayout.Fill.HORIZONTAL);
        tableLayout.setTablePadding(3, 3);
        JPanel panel = new JPanel(tableLayout);
        panel.setBorder(BorderFactory.createTitledBorder("Source"));
        panel.add(sourceProductSelector.getProductNameLabel());
        panel.add(subPanel);
        panel.add(tableLayout.createVerticalSpacer());
        return panel;
    }

    private JComponent createTargetPanel() {
        final JPanel subPanel1 = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        subPanel1.add(targetProductSelector.getSaveToFileCheckBox());
        subPanel1.add(targetProductSelector.getFormatNameComboBox());
        subPanel1.add(new JLabel("       "));
        subPanel1.add(targetProductSelector.getOpenInAppCheckBox());

        final JPanel subPanel2 = new JPanel(new BorderLayout());
        subPanel2.add(targetProductSelector.getProductDirTextField(), BorderLayout.CENTER);
        subPanel2.add(targetProductSelector.getProductDirChooserButton(), BorderLayout.EAST);

        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTableAnchor(TableLayout.Anchor.WEST);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setTablePadding(3, 3);
        tableLayout.setRowPadding(2, new Insets(10, 0, 3, 0));

        final JPanel panel = new JPanel(tableLayout);
        panel.setBorder(BorderFactory.createTitledBorder("Target"));
        panel.add(targetProductSelector.getProductNameLabel());
        panel.add(targetProductSelector.getProductNameTextField());
        panel.add(subPanel1);
        panel.add(new JLabel("Directory:"));
        panel.add(subPanel2);
        return panel;
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
        panel.add(new JLabel("Source bands:"), BorderLayout.NORTH);
        panel.add(new JScrollPane(sourceBandNames), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createSubParametersPanel() {
        final TableLayout tableLayout = new TableLayout(2);
        tableLayout.setTablePadding(3, 3);
        tableLayout.setTableAnchor(TableLayout.Anchor.WEST);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setColumnWeightX(1, 1.0);
        tableLayout.setCellColspan(3, 0, 2);
        JPanel panel = new JPanel(tableLayout);

        panel.add(new JLabel("Target band name suffix: "));
        panel.add(targetBandNameSuffix);

        panel.add(new JLabel("Spectral unmixing model: "));
        panel.add(unmixingModelName);

        panel.add(new JLabel("Minimum spectral bandwidth: "));
        panel.add(minBandwidth);

        panel.add(computeErrorBands);

        panel.add(tableLayout.createVerticalSpacer());
        return panel;
    }
}
