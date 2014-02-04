/*
 * Created at 31.07.2004 18:25:19
 * Copyright (c) 2004 by Norman Fomferra
 */
package org.esa.beam.framework.ui;

import org.esa.beam.framework.help.HelpSys;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class SelectExportMethodDialog {

    public final static int EXPORT_TO_CLIPBOARD = 0;
    public final static int EXPORT_TO_FILE = 1;
    public final static int EXPORT_CANCELED = -1;

    /**
     * Opens a modal dialog that asks the user which method to use in order to export data.
     *
     * @return {@link #EXPORT_TO_CLIPBOARD}, {@link #EXPORT_TO_FILE} or {@link #EXPORT_CANCELED}
     */
    public static int run(Component parentComponent, String title, String text, String helpID) {
        return run(parentComponent, title, text, new JCheckBox[0], helpID);
    }

    /**
     * Opens a modal dialog that asks the user which method to use in order to export data.
     *
     * @return {@link #EXPORT_TO_CLIPBOARD}, {@link #EXPORT_TO_FILE} or {@link #EXPORT_CANCELED}
     */
    public static int run(Component parentComponent, String title, String text, JCheckBox[] options, String helpID) {
        DialogDescriptor descriptor = createDialog(parentComponent, title, text, helpID, options);

        descriptor.dialog.setVisible(true);

        return getChosenMethod(descriptor);
    }

    private static int getChosenMethod(DialogDescriptor descriptor) {
        int method = EXPORT_CANCELED;
        final Object value = descriptor.optionPane.getValue();
        if (descriptor.copyToClipboardButton.equals(value)) {
            method = EXPORT_TO_CLIPBOARD;
        } else if (descriptor.writeToFileButton.equals(value)) {
            method = EXPORT_TO_FILE;
        }
        return method;
    }

    private static DialogDescriptor createDialog(Component parentComponent, String title, String text, String helpID, JCheckBox[] options) {
        final String copyToClipboardText = "Copy to Clipboard";  /*I18N*/
        final String writeToFileText = "Write to File"; /*I18N*/
        final String cancelText = "Cancel"; /*I18N*/

        final String iconDir = "/org/esa/beam/resources/images/icons/";
        final ImageIcon copyIcon = new ImageIcon(SelectExportMethodDialog.class.getResource(iconDir + "Copy16.gif"));
        final ImageIcon saveIcon = new ImageIcon(SelectExportMethodDialog.class.getResource(iconDir + "Save16.gif"));

        final JButton copyToClipboardButton = new JButton(copyToClipboardText);
        copyToClipboardButton.setMnemonic('b');
        copyToClipboardButton.setIcon(copyIcon);

        final JButton writeToFileButton = new JButton(writeToFileText);
        writeToFileButton.setMnemonic('W');
        writeToFileButton.setIcon(saveIcon);

        final JButton cancelButton = new JButton(cancelText);
        cancelButton.setMnemonic('C');
        cancelButton.setIcon(null);

        final JPanel panel = new JPanel(new GridBagLayout());
        final JPanel checkboxPanel = new JPanel(new GridBagLayout());
        final GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.LINE_START;
        c.gridx = 0;
        c.gridy = GridBagConstraints.RELATIVE;
        for (JCheckBox option : options) {
            checkboxPanel.add(option, c);
        }
        c.gridx = 0;
        c.gridy = 0;
        panel.add(checkboxPanel, c);
        final JPanel buttonPanel = new JPanel(new FlowLayout());
        c.gridy = GridBagConstraints.RELATIVE;
        buttonPanel.add(copyToClipboardButton, c);
        buttonPanel.add(writeToFileButton, c);
        buttonPanel.add(cancelButton, c);
        c.gridx = 0;
        c.gridy = 1;
        panel.add(buttonPanel, c);

        final JOptionPane optionPane = new JOptionPane(text, /*I18N*/
                                                       JOptionPane.QUESTION_MESSAGE,
                                                       JOptionPane.DEFAULT_OPTION,
                                                       null,
                                                       new JPanel[]{panel},
                                                       copyToClipboardButton);
        final JDialog dialog = optionPane.createDialog(parentComponent, title);
        dialog.getContentPane().setLayout(new BoxLayout(dialog.getContentPane(), BoxLayout.Y_AXIS));
        if (helpID != null) {
            HelpSys.enableHelpKey(optionPane, helpID);
        }

        // Create action listener for all 3 buttons (as instance of an anonymous class)
        final ActionListener actionListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                optionPane.setValue(e.getSource());
                dialog.setVisible(false);
                dialog.dispose();
            }
        };
        copyToClipboardButton.addActionListener(actionListener);
        writeToFileButton.addActionListener(actionListener);
        cancelButton.addActionListener(actionListener);

        return new DialogDescriptor(dialog, optionPane, copyToClipboardButton, writeToFileButton);
    }

    private static class DialogDescriptor {

        private final JDialog dialog;
        private final JOptionPane optionPane;
        private final JButton copyToClipboardButton;
        private final JButton writeToFileButton;

        private DialogDescriptor(JDialog dialog, JOptionPane optionPane, JButton copyToClipboardButton, JButton writeToFileButton) {
            this.dialog = dialog;
            this.optionPane = optionPane;
            this.copyToClipboardButton = copyToClipboardButton;
            this.writeToFileButton = writeToFileButton;
        }
    }
}