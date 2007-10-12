package org.esa.beam.framework.ui.io;

import com.bc.ceres.binding.swing.SwingBindingContext;
import com.jidesoft.swing.FolderChooser;
import org.esa.beam.framework.ui.TableLayout;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Target product selector.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class TargetProductSelector {

    private JLabel productNameLabel;
    private JTextField productNameTextField;
    private JCheckBox saveToFileCheckBox;
    private JTextField directoryTextField;
    private JButton directoryChooserButton;
    private JComboBox formatNameComboBox;
    private JCheckBox openInVisatCheckBox;

    private TargetProductSelectorModel model;

    public TargetProductSelector(TargetProductSelectorModel model) {
        this.model = model;

        initComponents();
        bindComponents();
        updateUIState();
    }

    private void initComponents() {
        productNameLabel = new JLabel("Product Name:");
        productNameTextField = new JTextField(25);
        saveToFileCheckBox = new JCheckBox("Save To:");
        directoryTextField = new JTextField(25);
        directoryChooserButton = new JButton("...");
        formatNameComboBox = new JComboBox(model.getFormatNames());
        openInVisatCheckBox = new JCheckBox("Open in VISAT");

        directoryChooserButton.setAction(new DirectoryChooserButtonAction());
        saveToFileCheckBox.addActionListener(new UIStateUpdater());
    }

    private void bindComponents() {
        final SwingBindingContext bc = new SwingBindingContext(model.getValueContainer());
   
        bc.bind(productNameTextField, "productName");
        bc.bind(saveToFileCheckBox, "saveToFileSelected");
        bc.bind(openInVisatCheckBox, "openInVisatSelected");
        bc.bind(formatNameComboBox, "formatName");
        bc.bind(directoryTextField, "directory");
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

    public JTextField getDirectoryTextField() {
        return directoryTextField;
    }

    public JButton getDirectoryChooserButton() {
        return directoryChooserButton;
    }

    public JComboBox getFormatNameComboBox() {
        return formatNameComboBox;
    }

    public JCheckBox getOpenInVisatCheckBox() {
        return openInVisatCheckBox;
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
        panel.add(productNameLabel);
        panel.add(productNameTextField);
        panel.add(new JLabel());
        panel.add(saveToFileCheckBox);
        panel.add(formatNameComboBox);
        panel.add(directoryTextField);
        panel.add(directoryChooserButton);
        panel.add(openInVisatCheckBox);

        return panel;
    }

    private void updateUIState() {
        if (model.isSaveToFileSelected()) {
            openInVisatCheckBox.setEnabled(true);
            formatNameComboBox.setEnabled(true);
            directoryTextField.setEnabled(true);
            directoryChooserButton.setEnabled(true);
        } else {
            openInVisatCheckBox.setEnabled(false);
//            openInVisatCheckBox.setSelected(true);
            formatNameComboBox.setEnabled(false);
            directoryTextField.setEnabled(false);
            directoryChooserButton.setEnabled(false);
        }
    }

    private class UIStateUpdater implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            updateUIState();
        }
    }

    private class DirectoryChooserButtonAction extends AbstractAction {

        private static final String APPROVE_BUTTON_TEXT = "Select";

        public DirectoryChooserButtonAction() {
            super("...");
        }

        public void actionPerformed(ActionEvent event) {
            JComponent parent = null;
            if (event.getSource() instanceof JComponent) {
                parent = (JComponent) event.getSource();
            }


            final JFileChooser chooser = new FolderChooser();

            if (chooser.showDialog(parent, APPROVE_BUTTON_TEXT) == JFileChooser.APPROVE_OPTION) {
                directoryTextField.setText(chooser.getSelectedFile().getPath());
            }
        }
    }
}
