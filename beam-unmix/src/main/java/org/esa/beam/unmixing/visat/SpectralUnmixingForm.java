package org.esa.beam.unmixing.visat;

import com.bc.ceres.binding.swing.SwingBindingContext;

import javax.swing.*;
import java.awt.*;

class SpectralUnmixingForm extends JPanel {
    SpectralUnmixingFormModel formModel;
    EndmemberFormModel endmemberFormModel;

    EndmemberForm endmemberForm;
    JTextField inputProductName;
    JTextField outputProductName;
    JCheckBox constrained;
    JList sourceBandNames;
    JCheckBox alterSourceProduct;
    JTextField targetBandNameSuffix;
    JComboBox unmixingModelName;

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

        bindingContext.bind(constrained, "constrained");
        bindingContext.bind(unmixingModelName, "unmixingModelName");
        bindingContext.bind(alterSourceProduct, "alterSourceProduct");
        bindingContext.bind(targetBandNameSuffix, "targetBandNameSuffix");
        bindingContext.bind(sourceBandNames, "sourceBandNames", true);

        bindingContext.enable(outputProductName, "alterSourceProduct", false);
    }

    private void initComponents() {
        endmemberForm = new EndmemberForm(endmemberFormModel);
        inputProductName = new JTextField(formModel.getInputProduct().getName());
        inputProductName.setEditable(false);
        outputProductName = new JTextField(formModel.getInputProduct().getName() + "_unmixed");
        constrained = new JCheckBox("Perform constrained unmixing");
        sourceBandNames = new JList();
        sourceBandNames.setModel(formModel.getBandListModel());
        alterSourceProduct = new JCheckBox("Alter input product, don't create output product");
        targetBandNameSuffix = new JTextField();
        unmixingModelName = new JComboBox();

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
        panel.add(new JLabel("Input product name: "), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(inputProductName, gbc);

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
        ioPanel.add(new JLabel("Output product name: "), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        ioPanel.add(outputProductName, gbc);

        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.weightx = 0;
        ioPanel.add(new JLabel("Output band name suffix: "), gbc);
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
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        ioPanel.add(alterSourceProduct, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        ioPanel.add(constrained, gbc);

        // spacer
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.weighty = 1;
        ioPanel.add(new JComponent(){}, gbc);

        return ioPanel;
    }
}
