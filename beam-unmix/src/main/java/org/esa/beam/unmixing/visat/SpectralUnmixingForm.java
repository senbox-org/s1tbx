package org.esa.beam.unmixing.visat;

import com.bc.ceres.binding.swing.SwingBindingContext;

import javax.swing.*;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

class SpectralUnmixingForm extends JPanel {
    SpectralUnmixingFormModel formModel;
    EndmemberFormModel endmemberFormModel;

    EndmemberForm endmemberForm;
    JTextField sourceProductName;
    JTextField targetProductName;
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

        bindingContext.enable(targetProductName, "alterSourceProduct", false);
    }

    private void initComponents() {
        endmemberForm = new EndmemberForm(endmemberFormModel);
        sourceProductName = new JTextField(10);
        sourceProductName.setText(formModel.getInputProduct().getName());
        sourceProductName.setCaretPosition(0);
        sourceProductName.setEditable(false);
        targetProductName = new JTextField(15);
        targetProductName.setText(formModel.getInputProduct().getName() + "_unmixed");
        targetProductName.setCaretPosition(0);
        sourceBandNames = new JList();
        sourceBandNames.setModel(formModel.getBandListModel());
        targetBandNameSuffix = new JTextField();
        unmixingModelName = new JComboBox();
        computeErrorBands = new JCheckBox("Compute error bands");
        maxWavelengthDelta = new JTextField();

        JPanel inputPanel = createInputPanel();
        inputPanel.setBorder(BorderFactory.createTitledBorder("Input"));

        JPanel outputPanel = createOutputPanel();
        outputPanel.setBorder(BorderFactory.createTitledBorder("Output"));

        endmemberForm.setBorder(BorderFactory.createTitledBorder("Endmembers"));

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.ipadx = 2;
        gbc.ipady = 2;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.5;
        gbc.weighty = 0.5;
        add(inputPanel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0.5;
        gbc.weighty = 0.5;
        add(outputPanel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1;
        gbc.weighty = 0.5;
        gbc.gridwidth = 2;
        add(endmemberForm, gbc);
    }


    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.ipady = 2;

        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.weightx = 0;
        panel.add(new JLabel("Source product name: "), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(sourceProductName, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        panel.add(new JLabel("Source bands:"), gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        gbc.weighty = 1;
        panel.add(new JScrollPane(sourceBandNames), gbc);

        return panel;
    }


    private JPanel createOutputPanel() {
        JPanel ioPanel = new JPanel(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.ipady = 2;

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.weightx = 0;
        ioPanel.add(new JLabel("Target product name: "), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        ioPanel.add(targetProductName, gbc);

        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.weightx = 0;
        ioPanel.add(new JLabel("Target band name suffix: "), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        ioPanel.add(targetBandNameSuffix, gbc);

        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.weightx = 0;
        ioPanel.add(new JLabel("Spectral unmixing model: "), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        ioPanel.add(unmixingModelName, gbc);

        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.weightx = 0;
        ioPanel.add(new JLabel("Max. wavelength deviation: "), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        ioPanel.add(maxWavelengthDelta, gbc);


        gbc.gridy++;
        ioPanel.add(computeErrorBands, gbc);

        // spacer
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.weighty = 1;
        ioPanel.add(new JPanel(), gbc);

        return ioPanel;
    }
}
