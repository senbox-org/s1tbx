package org.esa.beam.pixex.visat;

import org.esa.beam.framework.ui.ModalDialog;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Window;

class FileSelectionPatternDialog extends ModalDialog {

    private final JTextField textField;

    public FileSelectionPatternDialog(String defaultPattern, Window parent, String helpId) {
        super(parent, "File Selection Pattern", ModalDialog.ID_OK_CANCEL_HELP, helpId);
        final JPanel contentPane = new JPanel(new BorderLayout(8, 8));
        contentPane.add(new JLabel("Please define a file selection pattern. For example '*.nc'"), BorderLayout.NORTH);
        contentPane.add(new JLabel("Pattern:"), BorderLayout.WEST);
        textField = new JTextField(defaultPattern);
        contentPane.add(textField, BorderLayout.CENTER);
        setContent(contentPane);
    }

    public String getPattern() {
        final String text = textField.getText();
        return text != null ? text.trim() : text;
    }

    @Override
    public int show() {
        final int button = super.show();
        if (button == ModalDialog.ID_OK) {
            final String text = getPattern();
            if (text == null || text.length() == 0) {
                JOptionPane.showMessageDialog(getParent(), "Pattern field may not be empty.", "File Selection Pattern", JOptionPane.ERROR_MESSAGE);
                return ModalDialog.ID_CANCEL;
            }
        }
        return button;
    }
}
