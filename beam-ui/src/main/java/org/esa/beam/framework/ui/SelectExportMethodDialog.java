/*
 * Created at 31.07.2004 18:25:19
 * Copyright (c) 2004 by Norman Fomferra
 */
package org.esa.beam.framework.ui;

import org.esa.beam.framework.help.HelpSys;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class SelectExportMethodDialog {

    public final static int EXPORT_TO_CLIPBOARD = 0;
    public final static int EXPORT_TO_FILE = 1;
    public final static int EXPORT_CANCELED = -1;

    /**
     * Opens a modal dialog that asks the user which method to use in order to export the ROI pixels.
     *
     * @return {@link #EXPORT_TO_CLIPBOARD}, {@link #EXPORT_TO_FILE} or {@link #EXPORT_CANCELED}
     */
    public static int run(Component parentComponent, String title, String text, String helpID) {
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

        final JButton[] buttonRow = new JButton[]{copyToClipboardButton, writeToFileButton, cancelButton};

        final JOptionPane optionPane = new JOptionPane(text, /*I18N*/
                                                       JOptionPane.QUESTION_MESSAGE,
                                                       JOptionPane.DEFAULT_OPTION,
                                                       null,
                                                       buttonRow,
                                                       copyToClipboardButton);
        final JDialog dialog = optionPane.createDialog(parentComponent, title);
        HelpSys.enableHelpKey(optionPane, helpID);

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

        // Open modal dialog (waits until one of our buttons has been pressed)
        dialog.setVisible(true);

        int method = EXPORT_CANCELED;
        final Object value = optionPane.getValue();
        if (copyToClipboardButton.equals(value)) {
            method = EXPORT_TO_CLIPBOARD;
        } else if (writeToFileButton.equals(value)) {
            method = EXPORT_TO_FILE;
        }
        return method;
    }
}
