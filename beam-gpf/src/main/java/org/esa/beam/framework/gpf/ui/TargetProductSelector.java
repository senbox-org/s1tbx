package org.esa.beam.framework.gpf.ui;

import com.bc.ceres.binding.swing.SwingBindingContext;
import org.esa.beam.framework.ui.TableLayout;
import org.esa.beam.util.io.FileChooserFactory;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

/**
 * WARNING: This class belongs to a preliminary API and may change in future releases.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class TargetProductSelector {

    private JLabel productNameLabel;
    private JTextField productNameTextField;
    private JCheckBox saveToFileCheckBox;
    private JLabel productDirLabel;
    private JTextField productDirTextField;
    private JButton productDirChooserButton;
    private JComboBox formatNameComboBox;

    private JCheckBox openInAppCheckBox;
    private TargetProductSelectorModel model;

    public TargetProductSelector() {
        this.model = new TargetProductSelectorModel();

        initComponents();
        bindComponents();
        updateUIState();
    }

    private void initComponents() {
        productNameLabel = new JLabel("Name: ");
        productNameTextField = new JTextField(25);
        saveToFileCheckBox = new JCheckBox("Save as:");
        productDirLabel = new JLabel("Directory:");
        productDirTextField = new JTextField(25);
        productDirChooserButton = new JButton(new ProductDirChooserAction());
        formatNameComboBox = new JComboBox(model.getFormatNames());
        openInAppCheckBox = new JCheckBox("Open in application");

        final Dimension size = new Dimension(26, 16);
        productDirChooserButton.setPreferredSize(size);
        productDirChooserButton.setMinimumSize(size);
        saveToFileCheckBox.addActionListener(new UIStateUpdater());
    }

    private void bindComponents() {
        final SwingBindingContext bc = new SwingBindingContext(model.getValueContainer());

        bc.bind(productNameTextField, "productName");
        bc.bind(saveToFileCheckBox, "saveToFileSelected");
        bc.bind(openInAppCheckBox, "openInAppSelected");
        bc.bind(formatNameComboBox, "formatName");
        bc.bind(productDirTextField, "productDir");

        model.getValueContainer().addPropertyChangeListener("productDir", new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                productDirTextField.setToolTipText(model.getProductDir().getPath());
            }
        });
    }

    public TargetProductSelectorModel getModel() {
        return model;
    }

    public JLabel getProductNameLabel() {
        return productNameLabel;
    }

    public JTextField getProductNameTextField() {
        return productNameTextField;
    }

    public JCheckBox getSaveToFileCheckBox() {
        return saveToFileCheckBox;
    }

    public JLabel getProductDirLabel() {
        return productDirLabel;
    }

    public JTextField getProductDirTextField() {
        return productDirTextField;
    }

    public JButton getProductDirChooserButton() {
        return productDirChooserButton;
    }

    public JComboBox getFormatNameComboBox() {
        return formatNameComboBox;
    }

    public JCheckBox getOpenInAppCheckBox() {
        return openInAppCheckBox;
    }

    public JPanel createDefaultPanel() {
        final JPanel subPanel1 = new JPanel(new BorderLayout(3, 3));
        subPanel1.add(getProductNameLabel(), BorderLayout.NORTH);
        subPanel1.add(getProductNameTextField(), BorderLayout.CENTER);

        final JPanel subPanel2 = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        subPanel2.add(getSaveToFileCheckBox());
        subPanel2.add(getFormatNameComboBox());

        final JPanel subPanel3 = new JPanel(new BorderLayout(3, 3));
        subPanel3.add(getProductDirLabel(), BorderLayout.NORTH);
        subPanel3.add(getProductDirTextField(), BorderLayout.CENTER);
        subPanel3.add(getProductDirChooserButton(), BorderLayout.EAST);

        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTableAnchor(TableLayout.Anchor.WEST);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setTableWeightX(1.0);

        tableLayout.setCellPadding(0, 0, new Insets(3, 3, 3, 3));
        tableLayout.setCellPadding(1, 0, new Insets(3, 3, 3, 3));
        tableLayout.setCellPadding(2, 0, new Insets(0, 24, 3, 3));
        tableLayout.setCellPadding(3, 0, new Insets(3, 3, 3, 3));

        final JPanel panel = new JPanel(tableLayout);
        panel.setBorder(BorderFactory.createTitledBorder("Target Product"));
        panel.add(subPanel1);
        panel.add(subPanel2);
        panel.add(subPanel3);
        panel.add(getOpenInAppCheckBox());

        return panel;
    }

    private void updateUIState() {
        if (model.isSaveToFileSelected()) {
            openInAppCheckBox.setEnabled(true);
            formatNameComboBox.setEnabled(true);
            productDirLabel.setEnabled(true);
            productDirTextField.setEnabled(true);
            productDirChooserButton.setEnabled(true);
        } else {
            openInAppCheckBox.setEnabled(false);
            formatNameComboBox.setEnabled(false);
            productDirTextField.setEnabled(false);
            productDirTextField.setEnabled(false);
            productDirChooserButton.setEnabled(false);
        }
    }

    public void setEnabled(boolean enabled) {
        productNameLabel.setEnabled(enabled);
        productNameTextField.setEnabled(enabled);
        saveToFileCheckBox.setEnabled(enabled);
        productDirLabel.setEnabled(enabled);
        productDirTextField.setEnabled(enabled);
        productDirChooserButton.setEnabled(enabled);
        formatNameComboBox.setEnabled(enabled);
        openInAppCheckBox.setEnabled(enabled);
    }

    private class UIStateUpdater implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            updateUIState();
        }
    }

    private class ProductDirChooserAction extends AbstractAction {

        private static final String APPROVE_BUTTON_TEXT = "Select";

        public ProductDirChooserAction() {
            super("...");
        }

        public void actionPerformed(ActionEvent event) {
            JButton button = null;
            if (event.getSource() instanceof JComponent) {
                button = (JButton) event.getSource();
            }
            final JFileChooser chooser = FileChooserFactory.getInstance().createDirChooser(model.getProductDir());
            chooser.setDialogTitle("Select Target Directory");
            if (chooser.showDialog(SwingUtilities.getWindowAncestor(button), APPROVE_BUTTON_TEXT) == JFileChooser.APPROVE_OPTION) {

                final File selectedDir = chooser.getSelectedFile();
                if (selectedDir != null) {
                    model.setProductDir(selectedDir);
                } else {
                    model.setProductDir(new File("."));
                }
            }
        }
    }
}
