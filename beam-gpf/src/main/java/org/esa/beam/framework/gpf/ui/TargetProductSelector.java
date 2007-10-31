package org.esa.beam.framework.gpf.ui;

import com.bc.ceres.binding.swing.SwingBindingContext;
import com.jidesoft.swing.FolderChooser;
import org.esa.beam.framework.ui.TableLayout;
import org.esa.beam.util.io.FileChooserFactory;

import javax.swing.*;
import java.awt.Dimension;
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

    public JComponent createDefaultPanel() {
        final TableLayout layout = new TableLayout(4);
        layout.setTableAnchor(TableLayout.Anchor.LINE_START);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setColumnWeightX(0, 0.0);
        layout.setColumnWeightX(1, 1.0);
        layout.setColumnWeightX(2, 0.0);
        layout.setColumnWeightX(3, 0.0);
        layout.setTablePadding(3, 3);
        layout.setCellColspan(0, 1, 2);
        layout.setCellColspan(2, 0, 2);
        layout.setCellWeightX(1, 1, 0.0);
        layout.setCellWeightX(1, 2, 1.0);

        final JPanel panel = new JPanel(layout);
        panel.add(getProductNameLabel());
        panel.add(getProductNameTextField());
        panel.add(new JLabel());
        panel.add(getSaveToFileCheckBox());
        panel.add(getFormatNameComboBox());
        panel.add(getProductDirTextField());
        panel.add(getProductDirChooserButton());
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
