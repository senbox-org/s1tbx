package org.esa.beam.unmixing.visat;

import com.bc.ceres.binding.swing.SwingBindingContext;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.TableLayout;
import org.esa.beam.framework.ui.io.SourceProductSelector;
import org.esa.beam.framework.ui.io.TargetProductSelector;
import org.esa.beam.framework.ui.io.TargetProductSelectorModel;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class SpectralUnmixingForm extends JPanel {
    SpectralUnmixingFormModel formModel;
    EndmemberFormModel endmemberFormModel;

    EndmemberForm endmemberForm;
    SourceProductSelector sourceProductSelector;
    TargetProductSelector targetProductSelector;
    TargetProductSelectorModel targetProductSelectorModel;
    JList sourceBandNames;
    JTextField targetBandNameSuffix;
    JTextField maxWavelengthDelta;
    JComboBox unmixingModelName;
    JCheckBox computeErrorBands;

    public SpectralUnmixingForm(SpectralUnmixingFormModel formModel) {
        this.formModel = formModel;
        this.endmemberFormModel = new EndmemberFormModel();
        initComponents();
        bindComponents();
    }

    public EndmemberFormModel getEndmemberPresenter() {
        return endmemberFormModel;
    }

    private void bindComponents() {
        SwingBindingContext bindingContext = new SwingBindingContext(formModel.getOperatorValueContainer());

        bindingContext.bind(unmixingModelName, "unmixingModelName");
        bindingContext.bind(targetBandNameSuffix, "targetBandNameSuffix");
        bindingContext.bind(sourceBandNames, "sourceBandNames", true);
        bindingContext.bind(computeErrorBands, "computeErrorBands");
        bindingContext.bind(maxWavelengthDelta, "maxWavelengthDelta");

    }

    private void initComponents() {
        endmemberForm = new EndmemberForm(endmemberFormModel);
        sourceProductSelector = new SourceProductSelector(new Product[]{formModel.getInputProduct()}, "Source product name:");
        if (sourceProductSelector.getProductCount() > 0) {
            sourceProductSelector.setSelectedIndex(0);
        }
        sourceProductSelector.getProductNameComboBox().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                formModel.setInputProduct(sourceProductSelector.getSelectedProduct());
                sourceBandNames.setModel(formModel.getBandListModel());
            }
        });
        sourceBandNames = new JList();
        sourceBandNames.setModel(formModel.getBandListModel());

        targetProductSelectorModel = new TargetProductSelectorModel(true);
        targetProductSelectorModel.setProductName(formModel.getInputProduct().getName() + "_unmixed");
        targetProductSelector = new TargetProductSelector(targetProductSelectorModel, "Target product name:");
        targetBandNameSuffix = new JTextField();
        unmixingModelName = new JComboBox();
        computeErrorBands = new JCheckBox("Compute error bands");
        maxWavelengthDelta = new JTextField();


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

        panel.add(new JLabel("Max. wavelength deviation: "));
        panel.add(maxWavelengthDelta);

        panel.add(computeErrorBands);

        panel.add(tableLayout.createVerticalSpacer());
        return panel;
    }
}
